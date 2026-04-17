package com.dzc.dzcaicodemother.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mapper.ChatRoomMapper;
import com.dzc.dzcaicodemother.mapper.ChatRoomMemberMapper;
import com.dzc.dzcaicodemother.mapper.ChatRoomMessageMapper;
import com.dzc.dzcaicodemother.model.entity.ChatRoom;
import com.dzc.dzcaicodemother.model.entity.ChatRoomMember;
import com.dzc.dzcaicodemother.model.entity.ChatRoomMessage;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.ChatRoomMessageVO;
import com.dzc.dzcaicodemother.service.ChatService;
import com.dzc.dzcaicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天消息服务实现
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatRoomMessageMapper chatRoomMessageMapper;

    @Resource
    private ChatRoomMapper chatRoomMapper;

    @Resource
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Resource
    private UserService userService;

    // 敏感词集合（实际可从配置文件或数据库加载）
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "fuck", "shit", "傻逼", "混蛋", "垃圾", "诈骗", "赌博", "毒品", "色情", "暴力"
    ));

    // 允许的消息类型
    private static final Set<String> ALLOWED_MESSAGE_TYPES = new HashSet<>(Arrays.asList(
            "text", "image", "file", "ai_start", "ai_chunk", "ai_complete", "system", "app_share"
    ));

@Override
@Transactional(rollbackFor = Exception.class)
public Long sendMessage(Long roomId, String content, String messageType, User sender) {
    // 1. 参数校验
    ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
    ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
    ThrowUtils.throwIf(!ALLOWED_MESSAGE_TYPES.contains(messageType), ErrorCode.PARAMS_ERROR, "消息类型不支持");

    // 2. 校验群聊是否存在并检查权限
    ChatRoom chatRoom = chatRoomMapper.selectOneById(roomId);
    ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "群聊不存在");
    
    // 群主可以直接发送消息，否则检查是否是群成员
    if (!chatRoom.getOwnerUserId().equals(sender.getId())) {
        boolean isMember = isRoomMember(roomId, sender.getId());
        ThrowUtils.throwIf(!isMember, ErrorCode.NO_AUTH_ERROR, "您不在该群聊中");
    }

    // 3. 敏感词过滤（结构化消息类型跳过过滤，避免破坏JSON格式）
    String filteredContent = content;
    if (!"app_share".equals(messageType)) {
        filteredContent = filterSensitiveWords(content);
    }

    // 4. 构造消息实体
    ChatRoomMessage message = new ChatRoomMessage();
    message.setRoomId(roomId);
    message.setUserId(sender.getId());
    message.setMessageContent(filteredContent);
    message.setMessageType(messageType);
    message.setIsRevoked(0);
    message.setCreateTime(LocalDateTime.now());
    message.setUpdateTime(LocalDateTime.now());
    message.setIsDelete(0);

    // 5. 保存消息
    boolean saved = chatRoomMessageMapper.insert(message) > 0;
    ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "消息发送失败");

    // 6. 更新成员最后活跃时间
    updateMemberActiveTime(roomId, sender.getId());

    log.info("用户 {} 在群聊 {} 发送消息成功：messageId={}", sender.getId(), roomId, message.getId());
    return message.getId();
}

    @Override
    public void sendAiMessage(Long roomId, String aiResponse, User aiUser) {
        // AI消息由系统触发，不需要校验成员身份
        ChatRoomMessage message = new ChatRoomMessage();
        message.setRoomId(roomId);
        message.setUserId(null); // AI消息userId为null
        message.setMessageContent(aiResponse);
        message.setMessageType("ai_complete");
        message.setIsRevoked(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setIsDelete(0);

        chatRoomMessageMapper.insert(message);
        log.info("AI消息已发送到群聊 {}：messageId={}", roomId, message.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeMessage(Long messageId, User loginUser) {
        // 1. 查询消息
        ChatRoomMessage message = chatRoomMessageMapper.selectOneById(messageId);
        ThrowUtils.throwIf(message == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");

        // 2. 校验权限：只能撤回自己的消息
        boolean isOwner = isRoomOwner(message.getRoomId(), loginUser.getId());
        boolean isSender = message.getUserId() != null && message.getUserId().equals(loginUser.getId());

        ThrowUtils.throwIf(!isOwner && !isSender, ErrorCode.NO_AUTH_ERROR, "无权撤回此消息");

        // 3. 校验时间（3分钟内可撤回）
        LocalDateTime sendTime = message.getCreateTime();
        long minutesElapsed = ChronoUnit.MINUTES.between(sendTime, LocalDateTime.now());
        ThrowUtils.throwIf(minutesElapsed > 3 && !isOwner, ErrorCode.PARAMS_ERROR, "消息发送超过3分钟，无法撤回");

        // 4. 标记撤回
        ChatRoomMessage update = new ChatRoomMessage();
        update.setId(messageId);
        update.setIsRevoked(1);
        update.setRevokedTime(LocalDateTime.now());

        boolean updated = chatRoomMessageMapper.update(update) > 0;
        if (updated) {
            log.info("用户 {} 撤回消息成功：messageId={}", loginUser.getId(), messageId);
        }
        return updated;
    }

    @Override
    public Page<ChatRoomMessage> getHistoryMessages(Long roomId, Long current, Long size) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "单次最多查询50条");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoomMessage::getRoomId).eq(roomId)
                .and(ChatRoomMessage::getIsDelete).eq(0)
                .and(ChatRoomMessage::getIsRevoked).eq(0)
                .orderBy(ChatRoomMessage::getCreateTime, false); // 降序，最新的在前

        return chatRoomMessageMapper.paginate(Page.of(current, size), queryWrapper);
    }

@Override
public List<ChatRoomMessage> getRecentMessages(Long roomId, int count) {
    ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "群聊ID错误");
    ThrowUtils.throwIf(count > 100, ErrorCode.PARAMS_ERROR, "最多查询100条");

    QueryWrapper queryWrapper = QueryWrapper.create()
        .and(ChatRoomMessage::getRoomId).eq(roomId)
        .orderBy(ChatRoomMessage::getCreateTime, true)
        .limit(count);

    return chatRoomMessageMapper.selectListByQuery(queryWrapper);
}

@Override
public List<ChatRoomMessageVO> getRecentMessagesWithUserInfo(Long roomId, int count) {
    List<ChatRoomMessage> messages = getRecentMessages(roomId, count);
    if (messages.isEmpty()) {
        return java.util.Collections.emptyList();
    }

    List<Long> userIds = messages.stream()
            .map(ChatRoomMessage::getUserId)
            .filter(id -> id != null)
            .distinct()
            .toList();
    java.util.Map<Long, User> userMap = java.util.Collections.emptyMap();
    if (!userIds.isEmpty()) {
        List<User> users = userService.listByIds(userIds);
        userMap = users.stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    java.util.Map<Long, User> finalUserMap = userMap;
    return messages.stream().map(msg -> {
        ChatRoomMessageVO vo = new ChatRoomMessageVO();
        vo.setId(msg.getId());
        vo.setRoomId(msg.getRoomId());
        vo.setUserId(msg.getUserId());
        vo.setMessageContent(msg.getMessageContent());
        vo.setMessageType(msg.getMessageType());
        vo.setFileUrl(msg.getFileUrl());
        vo.setReplyToId(msg.getReplyToId());
        vo.setIsRevoked(msg.getIsRevoked());
        vo.setRevokedTime(msg.getRevokedTime());
        vo.setCreateTime(msg.getCreateTime());

        if (msg.getUserId() != null) {
            User user = finalUserMap.get(msg.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
        }
        return vo;
    }).toList();
}

    @Override
    public String filterSensitiveWords(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }

        String filtered = content;
        for (String word : SENSITIVE_WORDS) {
            if (filtered.toLowerCase().contains(word.toLowerCase())) {
                // 替换为*
                String replacement = "*".repeat(word.length());
                filtered = filtered.replaceAll("(?i)" + word, replacement);
            }
        }
        return filtered;
    }

    @Override
    public boolean isRoomMember(Long roomId, Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoomMember::getRoomId).eq(roomId)
                .and(ChatRoomMember::getUserId).eq(userId)
                .and(ChatRoomMember::getIsDelete).eq(0);

        Long count = chatRoomMemberMapper.selectCountByQuery(queryWrapper);
        return count != null && count > 0;
    }

    @Override
    public boolean isRoomOwner(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomMapper.selectOneById(roomId);
        if (chatRoom == null) {
            return false;
        }
        return chatRoom.getOwnerUserId().equals(userId);
    }

    /**
     * 更新成员最后活跃时间
     */
    private void updateMemberActiveTime(Long roomId, Long userId) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .and(ChatRoomMember::getRoomId).eq(roomId)
                    .and(ChatRoomMember::getUserId).eq(userId);

            ChatRoomMember update = new ChatRoomMember();
            update.setLastActiveTime(LocalDateTime.now());

            chatRoomMemberMapper.updateByQuery(update, queryWrapper);
        } catch (Exception e) {
            log.warn("更新成员活跃时间失败：roomId={}, userId={}", roomId, userId);
        }
    }

    @Override
    public int loadRoomChatHistoryToMemory(Long roomId, MessageWindowChatMemory chatMemory, int maxCount) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoomMessage::getRoomId).eq(roomId)
                .and(ChatRoomMessage::getMessageType).in("text", "ai_complete")
                .and(ChatRoomMessage::getIsDelete).eq(0)
                .and(ChatRoomMessage::getIsRevoked).eq(0)
                .orderBy(ChatRoomMessage::getCreateTime, true)
                .limit(maxCount);

        List<ChatRoomMessage> messages = chatRoomMessageMapper.selectListByQuery(queryWrapper);
        if (messages.isEmpty()) {
            return 0;
        }

        int loadedCount = 0;
        for (ChatRoomMessage msg : messages) {
            String content = msg.getMessageContent();
            if (StrUtil.isBlank(content)) {
                continue;
            }
            if ("text".equals(msg.getMessageType())) {
                chatMemory.add(UserMessage.from(content));
            } else if ("ai_complete".equals(msg.getMessageType())) {
                chatMemory.add(AiMessage.from(content));
            }
            loadedCount++;
        }

        log.info("群聊 {} 加载历史消息到 ChatMemory: {} 条", roomId, loadedCount);
        return loadedCount;
    }

    @Override
    public boolean containsSensitiveWords(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        for (String word : SENSITIVE_WORDS) {
            if (content.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
