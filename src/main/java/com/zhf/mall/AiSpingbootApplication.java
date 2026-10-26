package com.zhf.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 应用启动入口，组件扫描覆盖当前根包。 */
@SpringBootApplication
public class AiSpingbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSpingbootApplication.class, args);
    }
}
