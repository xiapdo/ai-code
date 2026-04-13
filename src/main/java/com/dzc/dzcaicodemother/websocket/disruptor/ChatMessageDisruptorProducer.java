package com.dzc.dzcaicodemother.websocket.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Disruptor 生产者
 * 用于发布聊天消息事件
 */
@Component
@Slf4j
public class ChatMessageDisruptorProducer {

    @Resource
    private ChatMessageDisruptorConfig disruptorConfig;

    // 事件转换器（复用，避免重复创建）
    private static final EventTranslatorOneArg<ChatMessageEvent, MessageWrapper> TRANSLATOR =
            (event, sequence, wrapper) -> {
                event.setRoomId(wrapper.roomId);
                event.setMessage(wrapper.message);
                event.setTimestamp(System.currentTimeMillis());
            };

    /**
     * 发布消息
     */
    public void publish(Long roomId, Object message) {
        try {
            RingBuffer<ChatMessageEvent> ringBuffer = disruptorConfig.getRingBuffer();
            ringBuffer.publishEvent(TRANSLATOR, new MessageWrapper(roomId, message));
        } catch (Exception e) {
            log.error("Disruptor发布消息失败: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    /**
     * 内部包装类，用于传递数据
     */
    private static class MessageWrapper {
        final Long roomId;
        final Object message;

        MessageWrapper(Long roomId, Object message) {
            this.roomId = roomId;
            this.message = message;
        }
    }
}
