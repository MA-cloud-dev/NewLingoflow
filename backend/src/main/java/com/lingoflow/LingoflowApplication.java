package com.lingoflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lingoflow.mapper")
public class LingoflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(LingoflowApplication.class, args);
    }
}
