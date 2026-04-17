package com.dzc.dzcaicodemother.websocket.vo;

import lombok.Data;

/**
 * 聊天消息VO
 * 字段名与前端 ChatRoomMessageVO 保持一致，确保WebSocket消息格式兼容
 */
@Data
public class ChatMessageVO {
    private Long id;              // 对应前端的 id
    private Long roomId;
    private Long userId;          // 对应前端的 userId
    private String userName;      // 对应前端的 userName
    private String userAvatar;    // 对应前端的 userAvatar
    private String messageContent; // 对应前端的 messageContent
    private String messageType;   // text/image/file/ai_start/ai_chunk/ai_complete/system
    private String fileUrl;       // 对应前端的 fileUrl
    private Long replyToId;       // 对应前端的 replyToId
    private Integer isRevoked;    // 对应前端的 isRevoked (number类型)
    private String revokedTime;   // 对应前端的 revokedTime
    private String createTime;    // 对应前端的 createTime
    private Long aiMessageId;     // AI流式消息关联ID，同一次AI响应共享
}
