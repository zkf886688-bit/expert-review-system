package com.review.system.service;

import com.review.system.common.BusinessException;
import com.review.system.dto.NotificationTemplateVO;
import com.review.system.dto.TemplatePreviewVO;
import com.review.system.entity.SysConfig;
import com.review.system.mapper.SysConfigMapper;
import com.review.system.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.DependsOn("schemaAutoFix")
public class NotificationTemplateService {

    public static final List<String> TEMPLATE_KEYS = List.of(
            "invite-title", "invite-content",
            "deadline-title", "deadline-content",
            "overdue-title", "overdue-content",
            "return-title", "return-content",
            "applicant-progress-title", "applicant-progress-content"
    );

    private final SysConfigMapper configMapper;
    private final AuditService auditService;

    @Value("${app.notification.templates.invite-title:评审邀请}")
    private String defaultInviteTitle;
    @Value("${app.notification.templates.invite-content:您收到评审任务「{taskName}」的项目「{projectName}」邀请，请及时确认。}")
    private String defaultInviteContent;
    @Value("${app.notification.templates.deadline-title:评审截止提醒}")
    private String defaultDeadlineTitle;
    @Value("${app.notification.templates.deadline-content:任务「{taskName}」将于 {days} 天后截止，请及时完成评审。}")
    private String defaultDeadlineContent;
    @Value("${app.notification.templates.overdue-title:评审已逾期}")
    private String defaultOverdueTitle;
    @Value("${app.notification.templates.overdue-content:任务「{taskName}」已超过截止时间。}")
    private String defaultOverdueContent;
    @Value("${app.notification.templates.return-title:评审退回修改}")
    private String defaultReturnTitle;
    @Value("${app.notification.templates.return-content:管理员已将您的评审退回，请修改后重新提交。}")
    private String defaultReturnContent;
    @Value("${app.notification.templates.applicant-progress-title:评审进度更新}")
    private String defaultApplicantProgressTitle;
    @Value("${app.notification.templates.applicant-progress-content:您的项目「{projectName}」已有 {submittedCount}/{minReviewers} 位专家提交评审意见。}")
    private String defaultApplicantProgressContent;

    private volatile Map<String, String> cache = Map.of();

    @PostConstruct
    public void init() {
        reloadSafely();
    }

    public void reload() {
        reloadSafely();
    }

    private void reloadSafely() {
        try {
            ensureSeeded();
            Map<String, String> loaded = new LinkedHashMap<>();
            for (String key : TEMPLATE_KEYS) {
                SysConfig cfg = configMapper.selectById(key);
                loaded.put(key, cfg != null ? cfg.getConfigValue() : defaultFor(key));
            }
            cache = loaded;
        } catch (Exception e) {
            log.warn("通知模板表 sys_config 不可用，已回退到 application.yml 默认模板。"
                    + " 请在 MySQL 中执行 sql/fix_startup.sql 修复数据库: {}", e.getMessage());
            cache = defaultsMap();
        }
    }

    private void ensureSeeded() {
        for (String key : TEMPLATE_KEYS) {
            if (configMapper.selectById(key) == null) {
                SysConfig cfg = new SysConfig();
                cfg.setConfigKey(key);
                cfg.setConfigValue(defaultFor(key));
                cfg.setUpdatedAt(LocalDateTime.now());
                configMapper.insert(cfg);
            }
        }
    }

    public String inviteTitle(Map<String, String> vars) {
        return render(get("invite-title"), vars);
    }

    public String inviteContent(Map<String, String> vars) {
        return render(get("invite-content"), vars);
    }

    public String deadlineTitle(Map<String, String> vars) {
        return render(get("deadline-title"), vars);
    }

    public String deadlineContent(Map<String, String> vars) {
        return render(get("deadline-content"), vars);
    }

    public String overdueTitle(Map<String, String> vars) {
        return render(get("overdue-title"), vars);
    }

    public String overdueContent(Map<String, String> vars) {
        return render(get("overdue-content"), vars);
    }

    public String returnTitle(Map<String, String> vars) {
        return render(get("return-title"), vars);
    }

    public String returnContent(Map<String, String> vars) {
        return render(get("return-content"), vars);
    }

    public String applicantProgressTitle(Map<String, String> vars) {
        return render(get("applicant-progress-title"), vars);
    }

    public String applicantProgressContent(Map<String, String> vars) {
        return render(get("applicant-progress-content"), vars);
    }

    public NotificationTemplateVO listTemplates() {
        Map<String, String> defaults = defaultsMap();
        Map<String, String> placeholders = Map.of(
                "taskName", "任务名称",
                "projectName", "项目名称",
                "days", "剩余天数",
                "submittedCount", "已提交专家数",
                "minReviewers", "最低评审人数");
        Map<String, String> sampleVars = Map.of(
                "taskName", "2026年度立项评审",
                "projectName", "示例项目A",
                "days", "3",
                "submittedCount", "2",
                "minReviewers", "3");
        return NotificationTemplateVO.builder()
                .templates(new LinkedHashMap<>(cache))
                .defaults(defaults)
                .placeholders(placeholders)
                .sampleVars(sampleVars)
                .build();
    }

    @Transactional
    public void updateTemplates(Map<String, String> templates) {
        if (templates == null || templates.isEmpty()) {
            throw new BusinessException("模板不能为空");
        }
        Long userId = SecurityUtils.currentUserId();
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            if (!TEMPLATE_KEYS.contains(entry.getKey())) {
                throw new BusinessException("非法模板键: " + entry.getKey());
            }
            if (!StringUtils.hasText(entry.getValue())) {
                throw new BusinessException("模板内容不能为空: " + entry.getKey());
            }
        }
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            SysConfig cfg = configMapper.selectById(entry.getKey());
            if (cfg == null) {
                cfg = new SysConfig();
                cfg.setConfigKey(entry.getKey());
                cfg.setConfigValue(entry.getValue().trim());
                cfg.setUpdatedBy(userId);
                cfg.setUpdatedAt(LocalDateTime.now());
                configMapper.insert(cfg);
            } else {
                cfg.setConfigValue(entry.getValue().trim());
                cfg.setUpdatedBy(userId);
                cfg.setUpdatedAt(LocalDateTime.now());
                configMapper.updateById(cfg);
            }
        }
        reload();
        auditService.log("UPDATE_TEMPLATES", "SYSTEM", null, "更新通知模板 " + templates.size() + " 项");
    }

    @Transactional
    public void resetToDefaults() {
        Long userId = SecurityUtils.currentUserId();
        for (String key : TEMPLATE_KEYS) {
            SysConfig cfg = configMapper.selectById(key);
            String value = defaultFor(key);
            if (cfg == null) {
                cfg = new SysConfig();
                cfg.setConfigKey(key);
                cfg.setConfigValue(value);
                cfg.setUpdatedBy(userId);
                cfg.setUpdatedAt(LocalDateTime.now());
                configMapper.insert(cfg);
            } else {
                cfg.setConfigValue(value);
                cfg.setUpdatedBy(userId);
                cfg.setUpdatedAt(LocalDateTime.now());
                configMapper.updateById(cfg);
            }
        }
        reload();
        auditService.log("RESET_TEMPLATES", "SYSTEM", null, "通知模板恢复默认");
    }

    public TemplatePreviewVO preview(String templateKey, Map<String, String> vars) {
        if (!TEMPLATE_KEYS.contains(templateKey)) {
            throw new BusinessException("非法模板键: " + templateKey);
        }
        Map<String, String> sample = sampleVars();
        if (vars != null) {
            sample.putAll(vars);
        }
        String titleKey = templateKey.endsWith("-content")
                ? templateKey.replace("-content", "-title")
                : templateKey;
        String contentKey = titleKey.replace("-title", "-content");
        return TemplatePreviewVO.builder()
                .title(render(get(titleKey), sample))
                .content(render(get(contentKey), sample))
                .build();
    }

    public String render(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        String result = template;
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace("{" + entry.getKey() + "}", value);
            }
        }
        return result;
    }

    private String get(String key) {
        return cache.getOrDefault(key, defaultFor(key));
    }

    private Map<String, String> defaultsMap() {
        Map<String, String> defaults = new LinkedHashMap<>();
        for (String key : TEMPLATE_KEYS) {
            defaults.put(key, defaultFor(key));
        }
        return defaults;
    }

    private Map<String, String> sampleVars() {
        Map<String, String> sample = new LinkedHashMap<>();
        sample.put("taskName", "2026年度立项评审");
        sample.put("projectName", "示例项目A");
        sample.put("days", "3");
        sample.put("submittedCount", "2");
        sample.put("minReviewers", "3");
        return sample;
    }

    private String defaultFor(String key) {
        return switch (key) {
            case "invite-title" -> defaultInviteTitle;
            case "invite-content" -> defaultInviteContent;
            case "deadline-title" -> defaultDeadlineTitle;
            case "deadline-content" -> defaultDeadlineContent;
            case "overdue-title" -> defaultOverdueTitle;
            case "overdue-content" -> defaultOverdueContent;
            case "return-title" -> defaultReturnTitle;
            case "return-content" -> defaultReturnContent;
            case "applicant-progress-title" -> defaultApplicantProgressTitle;
            case "applicant-progress-content" -> defaultApplicantProgressContent;
            default -> "";
        };
    }
}
