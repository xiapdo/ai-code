package com.dzc.dzcaicodemother;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class DzcAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(DzcAiCodeMotherApplication.class, args);
    }

}
