package com.dzc.dzcaicodemother.ai;

import com.dzc.dzcaicodemother.service.ChatService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class ChatRoomAiServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatService chatService;

    @Resource
    private ApplicationContext applicationContext;

    private final Cache<String, ChatRoomAiService> serviceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("群聊AI服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    public ChatRoomAiService getChatRoomAiService(long roomId, String mode) {
        String cacheKey = roomId + "_" + mode;
        return serviceCache.get(cacheKey, key -> createChatRoomAiService(roomId, mode));
    }

    private ChatRoomAiService createChatRoomAiService(long roomId, String mode) {
        log.info("为群聊 roomId: {} 创建新的 AI 服务实例, 模式: {}", roomId, mode);

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(roomId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();

        chatService.loadRoomChatHistoryToMemory(roomId, chatMemory, 20);

        StreamingChatModel streamingChatModel = applicationContext
                .getBean("streamingChatModelPrototype", StreamingChatModel.class);

        return AiServices.builder(ChatRoomAiService.class)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }
}
