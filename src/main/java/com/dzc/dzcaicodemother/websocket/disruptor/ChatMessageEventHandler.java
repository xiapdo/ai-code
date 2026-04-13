package com.dzc.dzcaicodemother.websocket.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Disruptor 事件处理器
 * 处理消息广播
 */
@Component
@Slf4j
public class ChatMessageEventHandler implements EventHandler<ChatMessageEvent>, WorkHandler<ChatMessageEvent> {

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void onEvent(ChatMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        processEvent(event);
    }

    @Override
    public void onEvent(ChatMessageEvent event) throws Exception {
        processEvent(event);
    }

    private void processEvent(ChatMessageEvent event) {
        try {
            if (event.getRoomId() != null && event.getMessage() != null) {
                // 广播到 /topic/chat/{roomId}
                String destination = "/topic/chat/" + event.getRoomId();

                log.info("[WebSocket广播] 准备发送消息到: {}, 消息内容: {}",
                        destination, event.getMessage());

                messagingTemplate.convertAndSend(destination, event.getMessage());

                log.info("[WebSocket广播] 消息发送成功到: {}, roomId={}", destination, event.getRoomId());
            } else {
                log.warn("[WebSocket广播] 消息内容为空: roomId={}, message={}",
                        event.getRoomId(), event.getMessage());
            }
        } catch (Exception e) {
            log.error("[WebSocket广播] 处理消息失败: roomId={}, error={}", event.getRoomId(), e.getMessage(), e);
        } finally {
            event.clear();
        }
    }
}
