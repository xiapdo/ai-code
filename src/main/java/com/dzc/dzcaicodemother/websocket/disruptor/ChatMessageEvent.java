package com.dzc.dzcaicodemother.websocket.disruptor;

import lombok.Data;

/**
 * Disruptor 事件 - 聊天消息
 * 用于高性能无锁队列
 */
@Data
public class ChatMessageEvent {
    private Long roomId;
    private Object message;
    private long timestamp;

    public void clear() {
        this.roomId = null;
        this.message = null;
        this.timestamp = 0;
    }
}
