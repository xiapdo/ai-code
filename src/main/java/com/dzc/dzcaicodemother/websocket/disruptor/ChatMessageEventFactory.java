package com.dzc.dzcaicodemother.websocket.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * Disruptor 事件工厂
 */
public class ChatMessageEventFactory implements EventFactory<ChatMessageEvent> {
    @Override
    public ChatMessageEvent newInstance() {
        return new ChatMessageEvent();
    }
}
