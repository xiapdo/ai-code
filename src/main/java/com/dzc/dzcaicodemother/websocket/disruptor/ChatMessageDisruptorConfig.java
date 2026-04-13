package com.dzc.dzcaicodemother.websocket.disruptor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Disruptor 配置类
 * 高性能无锁队列，用于 WebSocket 消息广播
 */
@Configuration
@Slf4j
public class ChatMessageDisruptorConfig {

    // RingBuffer 大小，必须是 2 的幂次方
    private static final int BUFFER_SIZE = 1024 * 1024; // 1048576

    @Resource
    private ChatMessageEventHandler eventHandler;

    private Disruptor<ChatMessageEvent> disruptor;

    /**
     * 初始化 Disruptor
     */
    @PostConstruct
    public void init() {
        log.info("初始化 Disruptor，RingBuffer 大小：{}", BUFFER_SIZE);

        // 创建事件工厂
        EventFactory<ChatMessageEvent> eventFactory = new ChatMessageEventFactory();

        // 创建 Disruptor
        // 单生产者模式（多消费者）
        disruptor = new Disruptor<>(
                eventFactory,
                BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE, // 单生产者
                new YieldingWaitStrategy() // 高性能等待策略
        );

        // 绑定事件处理器（单消费者模式，避免线程安全问题）
        // 注意：Spring 的 SimpMessagingTemplate 不是线程安全的，不能多消费者并行使用
        disruptor.handleEventsWith(eventHandler);

        // 启动 Disruptor
        disruptor.start();

        log.info("Disruptor 启动成功");
    }

    /**
     * 获取 RingBuffer
     */
    public RingBuffer<ChatMessageEvent> getRingBuffer() {
        if (disruptor == null) {
            throw new IllegalStateException("Disruptor 未初始化");
        }
        return disruptor.getRingBuffer();
    }

    /**
     * 关闭 Disruptor
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭 Disruptor...");
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }
}
