package com.dzc.dzcaicodemother.websocket.dto;

import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class ChatMessageRequest {
    private String content;
    private String messageType; // text/image/file
    private Long replyToId;
}
