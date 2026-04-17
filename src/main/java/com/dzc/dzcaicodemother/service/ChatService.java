package com.dzc.dzcaicodemother.service;

import java.util.List;

import com.dzc.dzcaicodemother.model.entity.ChatRoomMessage;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.ChatRoomMessageVO;
import com.mybatisflex.core.paginate.Page;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * 聊天消息服务
 *
 * @author dzc
 */
public interface ChatService {

    /**
     * 发送消息
     */
    Long sendMessage(Long roomId, String content, String messageType, User sender);

    /**
     * 发送AI消息
     */
    void sendAiMessage(Long roomId, String aiResponse, User aiUser);

    /**
     * 撤回消息（3分钟内）
     */
    boolean revokeMessage(Long messageId, User loginUser);

    /**
     * 获取历史消息
     */
    Page<ChatRoomMessage> getHistoryMessages(Long roomId, Long current, Long size);

    /**
     * 获取最新消息列表（包含用户信息）
     */
    List<ChatRoomMessageVO> getRecentMessagesWithUserInfo(Long roomId, int count);

    /**
     * 获取最新消息列表
     */
    List<ChatRoomMessage> getRecentMessages(Long roomId, int count);

    /**
     * 消息内容敏感词过滤
     */
    String filterSensitiveWords(String content);

    /**
     * 判断用户是否是群成员
     */
    boolean isRoomMember(Long roomId, Long userId);

    /**
     * 判断用户是否是群主
     */
    boolean isRoomOwner(Long roomId, Long userId);

    /**
     * 加载群聊历史消息到 ChatMemory
     */
    int loadRoomChatHistoryToMemory(Long roomId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 判断内容是否包含敏感词
     */
    boolean containsSensitiveWords(String content);
}
