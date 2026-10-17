package com.review.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.review.system.mapper")
@EnableScheduling
public class ExpertReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpertReviewApplication.class, args);
    }
}
