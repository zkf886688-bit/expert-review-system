package com.review.system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @Value("${info.app.name:外审专家管理系统}")
    private String appName;

    @Value("${info.app.version:1.0.0")
    private String version;

    @Value("${info.app.phase:19}")
    private String phase;

    @GetMapping(value = "/", produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> home(@org.springframework.web.bind.annotation.RequestHeader(value = "Accept", defaultValue = "") String accept) {
        if (accept.contains("text/html")) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildHtml());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildJson());
    }

    private Map<String, Object> buildJson() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", appName);
        body.put("version", version);
        body.put("phase", phase);
        body.put("status", "running");
        body.put("health", "/api/public/health");
        body.put("actuatorHealth", "/actuator/health");
        body.put("prometheus", "/actuator/prometheus");
        body.put("swagger", "/swagger-ui.html");
        body.put("login", "POST /api/auth/login");
        body.put("frontend", "请访问前端 http://localhost:5173 （本端口 8080 为 API 服务）");
        return body;
    }

    private String buildHtml() {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>%s</title>
                  <style>
                    body { font-family: "Segoe UI", "Microsoft YaHei", sans-serif; max-width: 640px; margin: 48px auto; padding: 0 16px; color: #303133; }
                    h1 { font-size: 22px; margin-bottom: 8px; }
                    p { color: #606266; line-height: 1.6; }
                    .ok { color: #67c23a; font-weight: 600; }
                    a { color: #409eff; text-decoration: none; }
                    a:hover { text-decoration: underline; }
                    ul { padding-left: 20px; }
                    code { background: #f5f7fa; padding: 2px 6px; border-radius: 4px; }
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  <p class="ok">后端 API 已启动（v%s · Phase %s）</p>
                  <p>8080 端口是<strong>接口服务</strong>，不是网页登录入口。请用浏览器打开前端：</p>
                  <p><a href="http://localhost:5173/">http://localhost:5173/</a></p>
                  <p>常用链接：</p>
                  <ul>
                    <li><a href="/api/public/health">健康检查</a> <code>/api/public/health</code></li>
                    <li><a href="/swagger-ui.html">API 文档</a> <code>/swagger-ui.html</code></li>
                    <li><a href="/actuator/health">Actuator</a> <code>/actuator/health</code></li>
                    <li><a href="/actuator/prometheus">Prometheus 指标</a> <code>/actuator/prometheus</code></li>
                  </ul>
                </body>
                </html>
                """.formatted(appName, appName, version, phase);
    }
}
