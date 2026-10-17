package com.review.system.service;

import com.review.system.entity.AuditLog;
import com.review.system.mapper.AuditLogMapper;
import com.review.system.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public void log(String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setUserId(SecurityUtils.currentUserId());
        log.setUsername(SecurityUtils.currentUsername());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(resolveIp());
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    private String resolveIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = req.getRemoteAddr();
            }
            return ip;
        }
        return null;
    }
}
