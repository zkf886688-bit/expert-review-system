package com.review.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.mapper.ReviewProjectMapper;
import com.review.system.mapper.ReviewTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 修复因 JDBC/表字符集不正确导致的中文乱码（问号）数据。
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
@org.springframework.context.annotation.DependsOn("schemaAutoFix")
public class EncodingDataFix implements org.springframework.beans.factory.InitializingBean {

    private static final String DEFAULT_FORM_CONFIG_JSON = """
            [
              {"key":"innovation","name":"创新性","maxScore":30,"weight":0.25},
              {"key":"feasibility","name":"可行性","maxScore":25,"weight":0.25},
              {"key":"team","name":"团队基础","maxScore":20,"weight":0.20},
              {"key":"value","name":"应用价值","maxScore":15,"weight":0.15},
              {"key":"budget","name":"经费合理性","maxScore":10,"weight":0.15}
            ]
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void afterPropertiesSet() {
        try {
            ensureUtf8mb4();
            repairSysRoles();
            repairSysUsers();
            repairCorruptedTasks();
            repairCorruptedProjects();
        } catch (Exception e) {
            log.warn("中文编码数据修复未完成，可手动执行 sql/fix_encoding.sql: {}", e.getMessage());
        }
    }

    private void ensureUtf8mb4() {
        jdbcTemplate.execute("ALTER DATABASE " + currentDatabase() + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        for (String table : List.of(
                "review_task", "review_project", "expert", "sys_user", "sys_role",
                "sys_config", "sys_notification", "audit_log", "review_submission")) {
            convertTable(table);
        }
        log.info("数据库/表字符集已校验为 utf8mb4");
    }

    private void convertTable(String table) {
        Integer exists = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                        """,
                Integer.class, table);
        if (exists != null && exists > 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private String currentDatabase() {
        return jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
    }

    private void repairSysRoles() {
        Map<String, String> roleNames = Map.of(
                "ADMIN", "系统管理员",
                "REVIEW_ADMIN", "评审管理员",
                "EXPERT", "外部专家",
                "LEADER", "研究院领导",
                "APPLICANT", "项目申报人"
        );
        roleNames.forEach((code, name) -> {
            int updated = jdbcTemplate.update(
                    "UPDATE sys_role SET name = ? WHERE code = ? AND (name <> ? OR name LIKE '%?%')",
                    name, code, name);
            if (updated > 0) {
                log.info("已修复角色名称: {} -> {}", code, name);
            }
        });
    }

    private void repairSysUsers() {
        Map<String, String> userNames = Map.of(
                "admin", "系统管理员",
                "leader", "研究院领导",
                "applicant", "示例申报人"
        );
        userNames.forEach((username, realName) -> {
            int updated = jdbcTemplate.update(
                    """
                            UPDATE sys_user SET real_name = ?
                            WHERE username = ? AND (real_name IS NULL OR real_name <> ? OR real_name LIKE '%?%')
                            """,
                    realName, username, realName);
            if (updated > 0) {
                log.info("已修复用户姓名: {} -> {}", username, realName);
            }
        });
    }

    private void repairCorruptedTasks() {
        List<ReviewTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<>());
        for (ReviewTask task : tasks) {
            boolean changed = false;
            if (isQuestionMarkCorrupted(task.getName()) && task.getName().startsWith("2026")) {
                task.setName("2026年度立项评审");
                changed = true;
            }
            if (isQuestionMarkCorrupted(task.getReviewType())) {
                task.setReviewType("立项");
                changed = true;
            }
            if (isQuestionMarkCorrupted(task.getBatchName())) {
                task.setBatchName("第一批");
                changed = true;
            }
            if (formConfigCorrupted(task.getFormConfig())) {
                task.setFormConfig(parseDefaultFormConfig());
                changed = true;
            }
            if (changed) {
                taskMapper.updateById(task);
                log.info("已修复乱码任务: id={}, name={}", task.getId(), task.getName());
            }
        }
    }

    private void repairCorruptedProjects() {
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<>());
        for (ReviewProject project : projects) {
            boolean changed = false;
            if (isQuestionMarkCorrupted(project.getProjectName())) {
                project.setProjectName("示例项目A");
                changed = true;
            }
            if (isQuestionMarkCorrupted(project.getApplicant())) {
                project.setApplicant("张三");
                changed = true;
            }
            if (isQuestionMarkCorrupted(project.getOrganization())) {
                project.setOrganization("示例单位");
                changed = true;
            }
            if (changed) {
                projectMapper.updateById(project);
                log.info("已修复乱码项目: id={}, projectName={}", project.getId(), project.getProjectName());
            }
        }
    }

    private boolean isQuestionMarkCorrupted(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return text.contains("?");
    }

    private boolean formConfigCorrupted(List<Map<String, Object>> formConfig) {
        if (formConfig == null || formConfig.isEmpty()) {
            return false;
        }
        return formConfig.stream().anyMatch(item -> {
            Object name = item.get("name");
            return name != null && name.toString().contains("?");
        });
    }

    private List<Map<String, Object>> parseDefaultFormConfig() {
        try {
            return objectMapper.readValue(DEFAULT_FORM_CONFIG_JSON, new TypeReference<>() {});
        } catch (Exception e) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(formItem("innovation", "创新性", 30, 0.25));
            list.add(formItem("feasibility", "可行性", 25, 0.25));
            list.add(formItem("team", "团队基础", 20, 0.20));
            list.add(formItem("value", "应用价值", 15, 0.15));
            list.add(formItem("budget", "经费合理性", 10, 0.15));
            return list;
        }
    }

    private Map<String, Object> formItem(String key, String name, int maxScore, double weight) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("name", name);
        item.put("maxScore", maxScore);
        item.put("weight", weight);
        return item;
    }
}
