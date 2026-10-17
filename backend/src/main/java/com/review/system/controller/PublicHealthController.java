package com.review.system.controller;



import com.review.system.observability.HealthSummaryBuilder;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;



import java.time.Instant;

import java.util.LinkedHashMap;

import java.util.Map;



@RestController

@RequestMapping("/api/public")

public class PublicHealthController {



    private final HealthSummaryBuilder healthSummaryBuilder;



    @Value("${info.app.name:外审专家管理系统}")

    private String appName;



    @Value("${info.app.version:1.0.0}")

    private String version;



    @Value("${info.app.phase:19}")

    private String phase;



    public PublicHealthController(HealthSummaryBuilder healthSummaryBuilder) {

        this.healthSummaryBuilder = healthSummaryBuilder;

    }



    @GetMapping("/health")

    public Map<String, Object> health() {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("status", healthSummaryBuilder.status().getCode());

        body.put("app", appName);

        body.put("version", version);

        body.put("phase", phase);

        body.put("timestamp", Instant.now().toString());

        body.put("components", healthSummaryBuilder.componentSummary());

        body.put("hint", "status 为 UP 表示服务正常；components 含数据库、磁盘、邮件、短信等子项状态");

        return body;

    }

}

