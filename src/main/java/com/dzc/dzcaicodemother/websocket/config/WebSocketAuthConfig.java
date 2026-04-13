package com.dzc.dzcaicodemother.websocket.config;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 配置 + 认证
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketAuthConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用内存消息代理
        config.enableSimpleBroker("/topic", "/queue");
        // 设置应用前缀 - 前端发送消息时使用 /app
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户前缀 - 点对点消息使用 /user
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("[WebSocket] 注册 STOMP 端点: /ws");

        // SockJS 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();

        // 原生 WebSocket 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    /**
     * 配置客户端入站通道 - 用于认证
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    log.info("[WebSocket] 收到 STOMP 命令: {}", accessor.getCommand());

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                        log.info("[WebSocket] Session 属性: {}", sessionAttrs);

                        String userId = (String) sessionAttrs.get("userId");
                        log.info("[WebSocket] 从 Session 获取 userId: {}", userId);

                        if (userId != null) {
                            accessor.setUser(new StompPrincipal(userId));
                            log.info("[WebSocket] 用户 {} 认证成功", userId);
                        } else {
                            log.warn("[WebSocket] 用户认证失败：Session 中没有 userId");
                        }
                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        log.info("[WebSocket] 用户订阅: {}", accessor.getDestination());
                    } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        log.info("[WebSocket] 用户发送消息到: {}", accessor.getDestination());
                    }
                }
                return message;
            }
        });
    }

    /**
     * 简单的 Principal 实现
     */
    public static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * HTTP Session 传递拦截器
     */
    public static class HttpSessionHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            log.info("[WebSocket] Handshake 开始");

            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                HttpSession session = servletRequest.getServletRequest().getSession(false);

                log.info("[WebSocket] Session ID: {}", session != null ? session.getId() : "null");

                if (session != null) {
                    // 从 user_login 属性获取用户对象
                    Object userObj = session.getAttribute("user_login");
                    log.info("[WebSocket] Session 中 user_login: {}", userObj);

                    if (userObj != null && userObj instanceof com.dzc.dzcaicodemother.model.entity.User) {
                        com.dzc.dzcaicodemother.model.entity.User user =
                                (com.dzc.dzcaicodemother.model.entity.User) userObj;
                        attributes.put("userId", user.getId().toString());
                        log.info("[WebSocket] Handshake 成功 - 用户ID: {}", user.getId());
                    } else {
                        log.warn("[WebSocket] Handshake - Session 中无 user_login 或类型不正确: {}",
                                userObj != null ? userObj.getClass() : "null");
                    }
                } else {
                    log.warn("[WebSocket] Handshake - 无 Session");
                }
            } else {
                log.warn("[WebSocket] Handshake - 非 Servlet 请求: {}", request.getClass());
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            if (exception != null) {
                log.error("[WebSocket] Handshake 异常: ", exception);
            }
        }
    }
}
