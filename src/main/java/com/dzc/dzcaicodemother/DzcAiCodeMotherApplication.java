package com.dzc.dzcaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;

//@SpringBootApplication
//排除Redis 中的 Embedding 模型, 因为这个项目回用不到
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
//Spring Data Redis 的注解
@EnableCaching
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
//自动扫描指定包的路径,将其中的接口Mybatis映射器关联起来
@MapperScan("com.dzc.dzcaicodemother.mapper")
public class DzcAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(DzcAiCodeMotherApplication.class, args);
    }

}
