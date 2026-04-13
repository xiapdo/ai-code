package com.dzc.dzcaicodemother.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mapper.ChatRoomMapper;
import com.dzc.dzcaicodemother.mapper.ChatRoomMemberMapper;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomCreateRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomQueryRequest;
import com.dzc.dzcaicodemother.model.dto.chat.ChatRoomUpdateRequest;
import com.dzc.dzcaicodemother.model.entity.ChatRoom;
import com.dzc.dzcaicodemother.model.entity.ChatRoomMember;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.service.ChatRoomService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 群聊服务实现
 */
@Service
@Slf4j
public class ChatRoomServiceImpl extends ServiceImpl<ChatRoomMapper, ChatRoom> implements ChatRoomService {

    @Resource
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    private static final int MAX_MEMBERS_LIMIT = 10;

    @Override
    public ChatRoom getById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "群聊ID无效");
        ChatRoom chatRoom = this.mapper.selectOneById(id);
        if (chatRoom == null) {
            log.warn("查询群聊失败：roomId={} 在数据库中不存在", id);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "群聊不存在");
        }
        return chatRoom;
    }

    @Override
    public Page<ChatRoom> listMyCreatedRooms(ChatRoomQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageSize = request.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询20条");

        QueryWrapper queryWrapper = this.getQueryWrapper(request);
        queryWrapper.and(ChatRoom::getOwnerUserId).eq(loginUser.getId());
        return this.page(Page.of(request.getPageNum(), pageSize), queryWrapper);
    }

    @Override
    public Page<ChatRoom> listMyJoinedRooms(ChatRoomQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageSize = request.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询20条");

        QueryWrapper memberQuery = QueryWrapper.create()
                .and(ChatRoomMember::getUserId).eq(loginUser.getId())
                .and(ChatRoomMember::getIsDelete).eq(0);

        List<ChatRoomMember> members = chatRoomMemberMapper.selectListByQuery(memberQuery);
        if (members.isEmpty()) {
            return Page.of(request.getPageNum(), pageSize, 0);
        }

        List<Long> roomIds = members.stream().map(ChatRoomMember::getRoomId).toList();

        QueryWrapper roomQuery = QueryWrapper.create()
                .and(ChatRoom::getId).in(roomIds)
                .and(ChatRoom::getStatus).eq(1)
                .and(ChatRoom::getIsDelete).eq(0)
                .and(ChatRoom::getOwnerUserId).ne(loginUser.getId());

        return this.page(Page.of(request.getPageNum(), pageSize), roomQuery);
    }

    @Override
    public Page<ChatRoom> listMyAllRooms(ChatRoomQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageSize = request.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询20条");

        List<Long> allRoomIds = new ArrayList<>();

        QueryWrapper createdQuery = QueryWrapper.create()
                .and(ChatRoom::getOwnerUserId).eq(loginUser.getId())
                .and(ChatRoom::getStatus).eq(1)
                .and(ChatRoom::getIsDelete).eq(0);
        List<ChatRoom> createdRooms = this.mapper.selectListByQuery(createdQuery);
        if (!createdRooms.isEmpty()) {
            allRoomIds.addAll(createdRooms.stream().map(ChatRoom::getId).toList());
        }

        QueryWrapper memberQuery = QueryWrapper.create()
                .and(ChatRoomMember::getUserId).eq(loginUser.getId())
                .and(ChatRoomMember::getIsDelete).eq(0);
        List<ChatRoomMember> members = chatRoomMemberMapper.selectListByQuery(memberQuery);
        if (!members.isEmpty()) {
            List<Long> joinedRoomIds = members.stream()
                    .map(ChatRoomMember::getRoomId)
                    .filter(id -> !allRoomIds.contains(id))
                    .toList();
            allRoomIds.addAll(joinedRoomIds);
        }

        if (allRoomIds.isEmpty()) {
            return Page.of(request.getPageNum(), pageSize, 0);
        }

        QueryWrapper roomQuery = QueryWrapper.create(QueryWrapper.create()
                .and(ChatRoom::getId).in(allRoomIds)
                .and(ChatRoom::getStatus).eq(1)
                .and(ChatRoom::getIsDelete).eq(0)
                .orderBy(ChatRoom::getCreateTime));

        return this.page(Page.of(request.getPageNum(), pageSize), roomQuery);
    }

    @Override
    public ChatRoom getChatRoomDetail(Long roomId, User loginUser) {
        ChatRoom chatRoom = this.getById(roomId);
        // 群主可以直接查看，不需要检查成员资格
        if (!chatRoom.getOwnerUserId().equals(loginUser.getId())) {
            boolean isMember = isRoomMember(roomId, loginUser.getId());
            ThrowUtils.throwIf(!isMember, ErrorCode.NO_AUTH_ERROR, "无权限查看此群聊");
        }
        return chatRoom;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dissolveRoom(Long roomId, User loginUser) {
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(!chatRoom.getOwnerUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有群主才能解散群聊");

        ChatRoom update = new ChatRoom();
        update.setId(roomId);
        update.setStatus(2);
        boolean updated = this.updateById(update);

        if (updated) {
            QueryWrapper memberUpdateWrapper = QueryWrapper.create()
                    .and(ChatRoomMember::getRoomId).eq(roomId);

            ChatRoomMember memberUpdate = new ChatRoomMember();
            memberUpdate.setIsDelete(1);
            chatRoomMemberMapper.updateByQuery(memberUpdate, memberUpdateWrapper);

            log.info("用户 {} 解散群聊成功：roomId={}", loginUser.getId(), roomId);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateInviteCode(Long roomId, User loginUser) {
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(!chatRoom.getOwnerUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有群主才能生成邀请码");

        // 检查是否已有有效的邀请码，如果有则复用
        if (chatRoom.getInviteCode() != null && chatRoom.getInviteExpiryTime() != null
                && LocalDateTime.now().isBefore(chatRoom.getInviteExpiryTime())) {
            log.info("复用已有邀请码：roomId={}, inviteCode={}", roomId, chatRoom.getInviteCode());
            return chatRoom.getInviteCode();
        }

        String inviteCode = RandomUtil.randomString(24);

        ChatRoom update = new ChatRoom();
        update.setId(roomId);
        update.setInviteCode(inviteCode);
        update.setInviteExpiryTime(LocalDateTime.now().plusMinutes(5));
        this.updateById(update);

        log.info("用户 {} 生成邀请码成功：roomId={}, inviteCode={}", loginUser.getId(), roomId, inviteCode);
        return inviteCode;
    }

    @Override
    public boolean validateInviteCode(String inviteCode) {
        if (StrUtil.isBlank(inviteCode) || inviteCode.length() != 24) {
            return false;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoom::getInviteCode).eq(inviteCode)
                .and(ChatRoom::getStatus).eq(1)
                .and(ChatRoom::getIsDelete).eq(0);

        ChatRoom chatRoom = this.mapper.selectOneByQuery(queryWrapper);
        if (chatRoom == null) {
            return false;
        }

        LocalDateTime expiryTime = chatRoom.getInviteExpiryTime();
        return expiryTime != null && LocalDateTime.now().isBefore(expiryTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinRoomByInviteCode(String inviteCode, User loginUser) {
        ThrowUtils.throwIf(!validateInviteCode(inviteCode),
                ErrorCode.PARAMS_ERROR, "邀请码无效或已过期");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoom::getInviteCode).eq(inviteCode);
        ChatRoom chatRoom = this.mapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "群聊不存在");

        Long roomId = chatRoom.getId();

        boolean isAlreadyMember = isRoomMember(roomId, loginUser.getId());
        if (isAlreadyMember) {
            log.warn("用户 {} 已经是群聊 {} 的成员，重复加入", loginUser.getId(), roomId);
            return true;
        }

        ThrowUtils.throwIf(chatRoom.getMemberCount() >= MAX_MEMBERS_LIMIT,
                ErrorCode.OPERATION_ERROR, "群聊已满员（最多" + MAX_MEMBERS_LIMIT + "人）");

        ChatRoomMember newMember = ChatRoomMember.builder()
                .roomId(roomId)
                .userId(loginUser.getId())
                .memberRole("member")
                .joinTime(LocalDateTime.now())
                .isMuted(0)
                .onlineStatus(0)
                .build();
        boolean memberSaved = chatRoomMemberMapper.insert(newMember) > 0;
        ThrowUtils.throwIf(!memberSaved, ErrorCode.OPERATION_ERROR, "加入群聊失败");

        ChatRoom update = new ChatRoom();
        update.setId(roomId);
        update.setMemberCount(chatRoom.getMemberCount() + 1);
        this.updateById(update);

        log.info("用户 {} 通过邀请码加入群聊成功：roomId={}", loginUser.getId(), roomId);
        return true;
    }

    @Override
    public boolean updateRoomInfo(Long roomId, ChatRoomUpdateRequest request, User loginUser) {
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(!chatRoom.getOwnerUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有群主才能修改群聊信息");

        ChatRoom update = new ChatRoom();
        update.setId(roomId);

        if (StrUtil.isNotBlank(request.getRoomName())) {
            update.setRoomName(request.getRoomName());
        }
        if (StrUtil.isNotBlank(request.getRoomAvatar())) {
            update.setRoomAvatar(request.getRoomAvatar());
        }
        if (StrUtil.isNotBlank(request.getDescription())) {
            update.setDescription(request.getDescription());
        }

        boolean updated = this.updateById(update);
        if (updated) {
            log.info("用户 {} 更新群聊信息成功：roomId={}", loginUser.getId(), roomId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean kickMember(Long roomId, Long memberUserId, User loginUser) {
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(!chatRoom.getOwnerUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有群主才能踢出成员");
        ThrowUtils.throwIf(memberUserId.equals(loginUser.getId()),
                ErrorCode.PARAMS_ERROR, "不能踢出自己");

        QueryWrapper kickWrapper = QueryWrapper.create()
                .and(ChatRoomMember::getRoomId).eq(roomId)
                .and(ChatRoomMember::getUserId).eq(memberUserId);

        ChatRoomMember update = new ChatRoomMember();
        update.setIsDelete(1);
        boolean kicked = chatRoomMemberMapper.updateByQuery(update, kickWrapper) > 0;

        if (kicked) {
            ChatRoom roomUpdate = new ChatRoom();
            roomUpdate.setId(roomId);
            roomUpdate.setMemberCount(Math.max(0, chatRoom.getMemberCount() - 1));
            this.updateById(roomUpdate);

            log.info("用户 {} 将用户 {} 从群聊 {} 踢出",
                    loginUser.getId(), memberUserId, roomId);
        }

        return kicked;
    }

    @Override
    public List<ChatRoomMember> listRoomMembers(Long roomId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoomMember::getRoomId).eq(roomId)
                .and(ChatRoomMember::getIsDelete).eq(0);
        return chatRoomMemberMapper.selectListByQuery(queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatRoomQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        QueryWrapper queryWrapper = new QueryWrapper();

        Long id = request.getId();
        String roomName = request.getRoomName();
        Long ownerUserId = request.getOwnerUserId();
        Integer status = request.getStatus();

        if (id != null) {
            queryWrapper.and(ChatRoom::getId).eq(id);
        }
        if (ownerUserId != null) {
            queryWrapper.and(ChatRoom::getOwnerUserId).eq(ownerUserId);
        }
        if (status != null) {
            queryWrapper.and(ChatRoom::getStatus).eq(status);
        }
        if (StrUtil.isNotBlank(roomName)) {
            queryWrapper.and(ChatRoom::getRoomName).like(roomName);
        }

        return queryWrapper;
    }

    @Override
    public long countMyCreatedRooms(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .and(ChatRoom::getOwnerUserId).eq(userId)
                .and(ChatRoom::getIsDelete).eq(0);
        Long count = this.mapper.selectCountByQuery(queryWrapper);
        return count != null ? count : 0;
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
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveRoom(Long roomId, User loginUser) {
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(chatRoom.getOwnerUserId().equals(loginUser.getId()),
                ErrorCode.PARAMS_ERROR, "群主不能退出群聊，请选择解散群聊");

        boolean isMember = isRoomMember(roomId, loginUser.getId());
        ThrowUtils.throwIf(!isMember, ErrorCode.PARAMS_ERROR, "您不在该群聊中");

        QueryWrapper leaveWrapper = QueryWrapper.create()
                .and(ChatRoomMember::getRoomId).eq(roomId)
                .and(ChatRoomMember::getUserId).eq(loginUser.getId());

        ChatRoomMember update = new ChatRoomMember();
        update.setIsDelete(1);
        boolean left = chatRoomMemberMapper.updateByQuery(update, leaveWrapper) > 0;

        if (left) {
            ChatRoom roomUpdate = new ChatRoom();
            roomUpdate.setId(roomId);
            roomUpdate.setMemberCount(Math.max(0, chatRoom.getMemberCount() - 1));
            this.updateById(roomUpdate);

            log.info("用户 {} 退出群聊：roomId={}", loginUser.getId(), roomId);
        }

        return left;
    }

    @Override
    public ChatRoom createRoomWithInviteCode(ChatRoomCreateRequest request, User loginUser) {
        String roomName = request.getRoomName();
        String description = request.getDescription();
        ThrowUtils.throwIf(StrUtil.isBlank(roomName), ErrorCode.PARAMS_ERROR, "群聊名称不能为空");
        ThrowUtils.throwIf(roomName.length() > 256, ErrorCode.PARAMS_ERROR, "群聊名称长度不能超过256字符");

        log.info("[创建群聊] 用户 {} 开始创建群聊: {}", loginUser.getId(), roomName);

        String inviteCode = RandomUtil.randomString(24);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        Long roomId = transactionTemplate.execute(status -> {
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setRoomName(roomName);
            chatRoom.setDescription(description);
            chatRoom.setOwnerUserId(loginUser.getId());
            chatRoom.setMemberCount(1);
            chatRoom.setStatus(1);
            chatRoom.setInviteCode(inviteCode);
            chatRoom.setInviteExpiryTime(LocalDateTime.now().plusMinutes(5));
            chatRoom.setCreateTime(LocalDateTime.now());
            chatRoom.setUpdateTime(LocalDateTime.now());
            chatRoom.setIsDelete(0);

            int rows = this.mapper.insert(chatRoom);
            ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "创建群聊失败");

            Long id = chatRoom.getId();
            log.info("[创建群聊] 群聊插入成功，roomId={}", id);

            ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.OPERATION_ERROR, "创建群聊失败，ID无效");

            ChatRoomMember ownerMember = ChatRoomMember.builder()
                    .roomId(id)
                    .userId(loginUser.getId())
                    .memberRole("owner")
                    .joinTime(LocalDateTime.now())
                    .isMuted(0)
                    .onlineStatus(0)
                    .build();
            int memberRows = chatRoomMemberMapper.insert(ownerMember);
            ThrowUtils.throwIf(memberRows <= 0, ErrorCode.OPERATION_ERROR, "添加群主失败");

            log.info("[创建群聊] 群主成员添加成功，邀请码生成成功: {}", inviteCode);

            return id;
        });

        ThrowUtils.throwIf(roomId == null, ErrorCode.OPERATION_ERROR, "创建群聊失败");

        ChatRoom result = this.mapper.selectOneById(roomId);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "创建成功但查询失败");

        log.info("[创建群聊] 用户 {} 创建群聊并生成邀请码成功：roomId={}", loginUser.getId(), roomId);

        return result;
    }
}
