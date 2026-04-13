package com.dzc.dzcaicodemother.service;

import com.dzc.dzcaicodemother.model.entity.ChatRoom;
import com.dzc.dzcaicodemother.model.entity.ChatRoomMessage;
import com.dzc.dzcaicodemother.model.entity.User;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

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
}
