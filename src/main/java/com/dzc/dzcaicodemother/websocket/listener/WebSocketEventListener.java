package com.dzc.dzcaicodemother.websocket.listener;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.dzc.dzcaicodemother.mapper.ChatRoomMemberMapper;
import com.dzc.dzcaicodemother.model.entity.ChatRoomMember;
import com.mybatisflex.core.query.QueryWrapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 连接事件监听器
 * 用于在用户连接/断开 WebSocket 时更新群聊成员的在线状态
 *
 * @author dzc
 */
@Component
@Slf4j
public class WebSocketEventListener {

    @Resource
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() == null) {
            return;
        }

        String userIdStr = accessor.getUser().getName();
        try {
            Long userId = Long.valueOf(userIdStr);
            updateUserOnlineStatus(userId, 1);
            log.info("[WebSocket] 用户 {} 上线，更新在线状态", userId);
        } catch (NumberFormatException e) {
            log.warn("[WebSocket] 无法解析用户ID: {}", userIdStr);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() == null) {
            return;
        }

        String userIdStr = accessor.getUser().getName();
        try {
            Long userId = Long.valueOf(userIdStr);
            updateUserOnlineStatus(userId, 0);
            log.info("[WebSocket] 用户 {} 下线，更新在线状态", userId);
        } catch (NumberFormatException e) {
            log.warn("[WebSocket] 无法解析用户ID: {}", userIdStr);
        }
    }

    private void updateUserOnlineStatus(Long userId, int status) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .and(ChatRoomMember::getUserId).eq(userId)
                    .and(ChatRoomMember::getIsDelete).eq(0);

            ChatRoomMember update = new ChatRoomMember();
            update.setOnlineStatus(status);

            chatRoomMemberMapper.updateByQuery(update, queryWrapper);
        } catch (Exception e) {
            log.error("[WebSocket] 更新用户在线状态失败：userId={}, status={}", userId, status, e);
        }
    }
}
