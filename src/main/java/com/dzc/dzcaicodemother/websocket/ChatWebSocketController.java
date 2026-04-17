package com.dzc.dzcaicodemother.websocket;

import com.dzc.dzcaicodemother.ai.ChatRoomAiService;
import com.dzc.dzcaicodemother.ai.ChatRoomAiServiceFactory;
import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.service.ChatService;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.websocket.dto.ChatMessageRequest;
import com.dzc.dzcaicodemother.websocket.vo.ChatMessageVO;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@Slf4j
public class ChatWebSocketController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Resource
    private ChatRoomAiServiceFactory chatRoomAiServiceFactory;

    private static final String AI_TRIGGER_CHAT = "@AI";
    private static final String AI_TRIGGER_FRONTEND = "@AI-前端";
    private static final String AI_REFUSE_MESSAGE = "抱歉，我对这个领域不太了解，换个话题聊聊吧~";
    private static final String AI_USER_NAME = "AI 助手";
    private static final int MAX_QUEUE_SIZE = 3;

    private final Map<Long, Queue<Runnable>> aiTaskQueues = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> aiProcessing = new ConcurrentHashMap<>();
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(4);

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            Principal principal) {

        log.info("[WebSocket] 收到发送消息请求: roomId={}, principal={}, content={}",
                roomId, principal != null ? principal.getName() : "null", request.getContent());

        try {
            if (principal == null) {
                log.error("[WebSocket] Principal 为 null，用户未认证");
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
            }

            String userIdStr = principal.getName();
            Long userId = Long.valueOf(userIdStr);
            User loginUser = userService.getById(userId);

            if (loginUser == null) {
                log.error("[WebSocket] 用户不存在: userId={}", userId);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户不存在");
            }

            log.info("[WebSocket] 用户 {} 发送消息到群聊 {}: {}", userId, roomId, request.getContent());

            boolean isMember = chatService.isRoomMember(roomId, userId);
            if (!isMember) {
                log.error("[WebSocket] 用户 {} 不是群聊 {} 的成员", userId, roomId);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不在该群聊中");
            }

            Long messageId = chatService.sendMessage(
                    roomId,
                    request.getContent(),
                    request.getMessageType(),
                    loginUser
            );

            log.info("[WebSocket] 消息已保存到数据库: messageId={}", messageId);

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

            String destination = "/topic/chat/" + roomId;
            messagingTemplate.convertAndSend(destination, messageVO);
            log.info("[WebSocket] 消息广播成功到 {}", destination);

            String content = request.getContent();
            if (content != null) {
                String aiMode = detectAiTrigger(content);
                if (aiMode != null) {
                    String prompt = extractPrompt(content, aiMode);
                    triggerAiResponse(roomId, prompt, aiMode);
                }
            }

        } catch (Exception e) {
            log.error("[WebSocket] 发送消息失败: roomId={}, error={}", roomId, e.getMessage(), e);
        }
    }

    private String detectAiTrigger(String content) {
        if (content.startsWith(AI_TRIGGER_FRONTEND)) {
            return "frontend";
        }
        if (content.startsWith(AI_TRIGGER_CHAT)) {
            return "chat";
        }
        return null;
    }

    private String extractPrompt(String content, String mode) {
        if ("frontend".equals(mode)) {
            String prompt = content.substring(AI_TRIGGER_FRONTEND.length()).trim();
            return prompt.isEmpty() ? "请给我一些前端开发的最佳实践建议" : prompt;
        }
        String prompt = content.substring(AI_TRIGGER_CHAT.length()).trim();
        return prompt.isEmpty() ? "你好" : prompt;
    }

    private void triggerAiResponse(Long roomId, String prompt, String mode) {
        if (chatService.containsSensitiveWords(prompt)) {
            handleSensitiveWordResponse(roomId);
            return;
        }

        Runnable aiTask = () -> {
            try {
                handleAiStreamResponse(roomId, prompt, mode);
            } catch (Exception e) {
                log.error("[AI] 群聊 {} AI响应异常: {}", roomId, e.getMessage(), e);
                pushSystemMessage(roomId, "AI 助手暂时无法响应，请稍后再试");
            } finally {
                aiProcessing.put(roomId, false);
                processNextAiTask(roomId);
            }
        };

        synchronized (aiTaskQueues) {
            Queue<Runnable> queue = aiTaskQueues.computeIfAbsent(roomId, k -> new LinkedList<>());
            if (aiProcessing.getOrDefault(roomId, false)) {
                if (queue.size() >= MAX_QUEUE_SIZE) {
                    pushSystemMessage(roomId, "AI 正忙，请稍后再试");
                    return;
                }
                queue.add(aiTask);
                log.info("[AI] 群聊 {} AI请求排队，当前排队数: {}", roomId, queue.size());
            } else {
                aiProcessing.put(roomId, true);
                aiExecutor.submit(aiTask);
            }
        }
    }

    private void processNextAiTask(Long roomId) {
        synchronized (aiTaskQueues) {
            Queue<Runnable> queue = aiTaskQueues.get(roomId);
            if (queue != null && !queue.isEmpty()) {
                Runnable nextTask = queue.poll();
                aiProcessing.put(roomId, true);
                aiExecutor.submit(nextTask);
            }
        }
    }

    private void handleSensitiveWordResponse(Long roomId) {
        Long aiMessageId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

        ChatMessageVO completeVO = buildAiMessageVO(roomId, AI_REFUSE_MESSAGE, "ai_complete", aiMessageId);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, completeVO);

        chatService.sendAiMessage(roomId, AI_REFUSE_MESSAGE, null);
        log.info("[AI] 群聊 {} 敏感词拦截，AI拒绝响应", roomId);
    }

    private void handleAiStreamResponse(Long roomId, String prompt, String mode) {
        Long aiMessageId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

        ChatMessageVO startVO = buildAiMessageVO(roomId, "", "ai_start", aiMessageId);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, startVO);
        log.info("[AI] 群聊 {} AI开始响应, aiMessageId={}", roomId, aiMessageId);

        ChatRoomAiService aiService = chatRoomAiServiceFactory.getChatRoomAiService(roomId, mode);
        TokenStream tokenStream = "frontend".equals(mode)
                ? aiService.chatFrontend(prompt)
                : aiService.chat(prompt);

        StringBuilder fullResponse = new StringBuilder();

        tokenStream
                .onPartialResponse(chunk -> {
                    fullResponse.append(chunk);
                    ChatMessageVO chunkVO = buildAiMessageVO(roomId, chunk, "ai_chunk", aiMessageId);
                    messagingTemplate.convertAndSend("/topic/chat/" + roomId, chunkVO);
                })
                .onCompleteResponse(response -> {
                    String completeContent = fullResponse.toString();
                    ChatMessageVO completeVO = buildAiMessageVO(roomId, completeContent, "ai_complete", aiMessageId);
                    messagingTemplate.convertAndSend("/topic/chat/" + roomId, completeVO);

                    chatService.sendAiMessage(roomId, completeContent, null);
                    log.info("[AI] 群聊 {} AI响应完成, 长度={}", roomId, completeContent.length());
                })
                .onError(error -> {
                    log.error("[AI] 群聊 {} AI流式响应错误: {}", roomId, error.getMessage());
                    pushSystemMessage(roomId, "AI 助手暂时无法响应，请稍后再试");
                })
                .start();
    }

    private ChatMessageVO buildAiMessageVO(Long roomId, String content, String messageType, Long aiMessageId) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setRoomId(roomId);
        vo.setMessageContent(content);
        vo.setMessageType(messageType);
        vo.setUserName(AI_USER_NAME);
        vo.setAiMessageId(aiMessageId);
        vo.setCreateTime(java.time.LocalDateTime.now().toString());
        vo.setIsRevoked(0);
        return vo;
    }

    private void pushSystemMessage(Long roomId, String content) {
        ChatMessageVO systemMessage = new ChatMessageVO();
        systemMessage.setRoomId(roomId);
        systemMessage.setMessageContent(content);
        systemMessage.setMessageType("system");
        systemMessage.setUserName("系统");
        systemMessage.setCreateTime(java.time.LocalDateTime.now().toString());
        systemMessage.setIsRevoked(0);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, systemMessage);
    }

    @SubscribeMapping("/chat/{roomId}")
    public void subscribeChatRoom(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null) {
            log.error("[WebSocket] 订阅失败：用户未认证");
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }

        String userIdStr = principal.getName();
        Long userId = Long.valueOf(userIdStr);

        log.info("[WebSocket] 用户 {} 订阅群聊 {} 消息", userId, roomId);

        boolean isMember = chatService.isRoomMember(roomId, userId);
        if (!isMember) {
            log.error("[WebSocket] 用户 {} 无权订阅群聊 {}", userId, roomId);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限订阅此群聊");
        }

        log.info("[WebSocket] 用户 {} 订阅群聊 {} 成功", userId, roomId);
    }

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

    public void broadcastAiMessage(Long roomId, String aiResponse) {
        ChatMessageVO aiMessage = new ChatMessageVO();
        aiMessage.setRoomId(roomId);
        aiMessage.setMessageContent(aiResponse);
        aiMessage.setMessageType("ai_complete");
        aiMessage.setUserName(AI_USER_NAME);
        aiMessage.setCreateTime(java.time.LocalDateTime.now().toString());
        aiMessage.setIsRevoked(0);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, aiMessage);
        log.info("[WebSocket] AI消息广播到群聊 {}", roomId);
    }

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
