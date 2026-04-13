package com.dzc.dzcaicodemother.websocket;

import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.service.ChatService;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.websocket.dto.ChatMessageRequest;
import com.dzc.dzcaicodemother.websocket.vo.ChatMessageVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 聊天控制器
 * 基于 STOMP 协议
 */
@Controller
@Slf4j
public class ChatWebSocketController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 发送消息到群聊
     * 路径: /app/chat/{roomId}/send
     * 广播到: /topic/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            Principal principal) {

        log.info("[WebSocket] 收到发送消息请求: roomId={}, principal={}, content={}",
                roomId, principal != null ? principal.getName() : "null", request.getContent());

        try {
            // 检查认证
            if (principal == null) {
                log.error("[WebSocket] Principal 为 null，用户未认证");
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
            }

            // 获取当前用户
            String userIdStr = principal.getName();
            Long userId = Long.valueOf(userIdStr);
            User loginUser = userService.getById(userId);

            if (loginUser == null) {
                log.error("[WebSocket] 用户不存在: userId={}", userId);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户不存在");
            }

            log.info("[WebSocket] 用户 {} 发送消息到群聊 {}: {}", userId, roomId, request.getContent());

            // 校验是否是群成员
            boolean isMember = chatService.isRoomMember(roomId, userId);
            if (!isMember) {
                log.error("[WebSocket] 用户 {} 不是群聊 {} 的成员", userId, roomId);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不在该群聊中");
            }

            // 发送消息到数据库
            Long messageId = chatService.sendMessage(
                    roomId,
                    request.getContent(),
                    request.getMessageType(),
                    loginUser
            );

            log.info("[WebSocket] 消息已保存到数据库: messageId={}", messageId);

            // 构建消息VO - 使用前端期望的字段名
            ChatMessageVO messageVO = new ChatMessageVO();
            messageVO.setId(messageId);
            messageVO.setRoomId(roomId);
            messageVO.setUserId(userId);
            messageVO.setUserName(loginUser.getUserName());
            messageVO.setUserAvatar(loginUser.getUserAvatar());
            messageVO.setMessageContent(request.getContent());
            messageVO.setMessageType(request.getMessageType());
            messageVO.setCreateTime(java.time.LocalDateTime.now().toString());
            messageVO.setIsRevoked(0);

            // 广播到 /topic/chat/{roomId}
            String destination = "/topic/chat/" + roomId;
            log.info("[WebSocket] 准备广播消息到: {}", destination);
            log.info("[WebSocket] 消息内容: {}", messageVO);

            messagingTemplate.convertAndSend(destination, messageVO);

            log.info("[WebSocket] 消息广播成功到 {}", destination);

        } catch (Exception e) {
            log.error("[WebSocket] 发送消息失败: roomId={}, error={}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 订阅群聊消息
     * 路径: /topic/chat/{roomId}
     */
    @SubscribeMapping("/chat/{roomId}")
    public void subscribeChatRoom(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null) {
            log.error("[WebSocket] 订阅失败：用户未认证");
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }

        String userIdStr = principal.getName();
        Long userId = Long.valueOf(userIdStr);

        log.info("[WebSocket] 用户 {} 订阅群聊 {} 消息", userId, roomId);

        // 校验是否是群成员
        boolean isMember = chatService.isRoomMember(roomId, userId);
        if (!isMember) {
            log.error("[WebSocket] 用户 {} 无权订阅群聊 {}", userId, roomId);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限订阅此群聊");
        }

        log.info("[WebSocket] 用户 {} 订阅群聊 {} 成功", userId, roomId);
    }

    /**
     * 撤回消息
     */
    @MessageMapping("/chat/{roomId}/revoke/{messageId}")
    public void revokeMessage(
            @DestinationVariable Long roomId,
            @DestinationVariable Long messageId,
            Principal principal) {

        if (principal == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }

        String userIdStr = principal.getName();
        Long userId = Long.valueOf(userIdStr);
        User loginUser = userService.getById(userId);

        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户不存在");
        }

        boolean revoked = chatService.revokeMessage(messageId, loginUser);
        if (revoked) {
            ChatMessageVO revokeVO = new ChatMessageVO();
            revokeVO.setId(messageId);
            revokeVO.setRoomId(roomId);
            revokeVO.setMessageType("revoke");
            revokeVO.setCreateTime(java.time.LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/chat/" + roomId, revokeVO);
            log.info("[WebSocket] 用户 {} 撤回消息: messageId={}", userId, messageId);
        }
    }

    /**
     * AI消息推送（供系统调用）
     */
    public void broadcastAiMessage(Long roomId, String aiResponse) {
        ChatMessageVO aiMessage = new ChatMessageVO();
        aiMessage.setRoomId(roomId);
        aiMessage.setMessageContent(aiResponse);
        aiMessage.setMessageType("ai_complete");
        aiMessage.setUserName("AI助手");
        aiMessage.setCreateTime(java.time.LocalDateTime.now().toString());
        aiMessage.setIsRevoked(0);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, aiMessage);
        log.info("[WebSocket] AI消息广播到群聊 {}", roomId);
    }

    /**
     * 用户加入通知
     */
    public void notifyUserJoined(Long roomId, User user) {
        ChatMessageVO systemMessage = new ChatMessageVO();
        systemMessage.setRoomId(roomId);
        systemMessage.setMessageContent(user.getUserName() + " 加入了群聊");
        systemMessage.setMessageType("system");
        systemMessage.setUserName("系统");
        systemMessage.setCreateTime(java.time.LocalDateTime.now().toString());
        systemMessage.setIsRevoked(0);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, systemMessage);
        log.info("[WebSocket] 用户加入通知广播到群聊 {}", roomId);
    }

    /**
     * 用户离开通知
     */
    public void notifyUserLeft(Long roomId, User user) {
        ChatMessageVO systemMessage = new ChatMessageVO();
        systemMessage.setRoomId(roomId);
        systemMessage.setMessageContent(user.getUserName() + " 离开了群聊");
        systemMessage.setMessageType("system");
        systemMessage.setUserName("系统");
        systemMessage.setCreateTime(java.time.LocalDateTime.now().toString());
        systemMessage.setIsRevoked(0);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, systemMessage);
        log.info("[WebSocket] 用户离开通知广播到群聊 {}", roomId);
    }
}
