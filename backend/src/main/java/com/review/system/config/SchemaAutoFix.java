package com.review.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动补齐缺失表/字段，避免旧库未跑迁移脚本时后端无法启动。
 */
@Slf4j
@Component("schemaAutoFix")
@RequiredArgsConstructor
public class SchemaAutoFix implements org.springframework.beans.factory.InitializingBean {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS sys_config (
                        config_key   VARCHAR(128) PRIMARY KEY,
                        config_value TEXT         NOT NULL,
                        updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        updated_by   BIGINT       NULL
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS expert_material (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        expert_id BIGINT NOT NULL,
                        file_name VARCHAR(256) NOT NULL,
                        file_path VARCHAR(512) NOT NULL,
                        file_size BIGINT,
                        material_type VARCHAR(64) NULL,
                        uploaded_by BIGINT,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_expert_material (expert_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS project_rectification_material (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        project_id BIGINT NOT NULL,
                        file_name VARCHAR(256) NOT NULL,
                        file_path VARCHAR(512) NOT NULL,
                        file_size BIGINT,
                        uploaded_by BIGINT,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_rect_material_project (project_id)
                    )
                    """);
            addColumnIfMissing("expert", "avg_quality_score", "DECIMAL(4,2) NULL");
            addColumnIfMissing("review_task", "avoid_duplicate_expert", "TINYINT(1) NOT NULL DEFAULT 1");
            addColumnIfMissing("review_task", "data_domain", "VARCHAR(64) NULL DEFAULT 'DEFAULT'");
            addColumnIfMissing("sys_user", "data_domain", "VARCHAR(64) NULL DEFAULT 'DEFAULT'");
            addColumnIfMissing("review_project", "applicant_user_id", "BIGINT NULL");
            addColumnIfMissing("review_project", "review_round", "INT NOT NULL DEFAULT 1");
            addColumnIfMissing("review_project", "rectification_status", "VARCHAR(32) NOT NULL DEFAULT 'NONE'");
            addColumnIfMissing("review_project", "rectification_note", "TEXT NULL");
            addColumnIfMissing("review_project", "rectification_reply", "TEXT NULL");
            addColumnIfMissing("review_project", "rectification_requested_at", "DATETIME NULL");
            addColumnIfMissing("review_project", "rectification_submitted_at", "DATETIME NULL");
            addColumnIfMissing("review_material", "material_type", "VARCHAR(64) NULL");
            addColumnIfMissing("review_submission", "attachment_path", "VARCHAR(512) NULL");
            addColumnIfMissing("review_submission", "attachment_name", "VARCHAR(256) NULL");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS expert_avoidance (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        expert_id BIGINT NOT NULL,
                        avoid_type VARCHAR(32) NOT NULL,
                        avoid_value VARCHAR(256) NOT NULL,
                        reason VARCHAR(512) NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_avoid_expert (expert_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS review_submission_archive (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        project_id BIGINT NOT NULL,
                        review_round INT NOT NULL,
                        assignment_id BIGINT NULL,
                        expert_id BIGINT NOT NULL,
                        expert_name VARCHAR(64) NULL,
                        scores JSON NULL,
                        total_score DECIMAL(8,2) NULL,
                        opinion TEXT NULL,
                        conclusion VARCHAR(64) NULL,
                        submitted_at DATETIME NULL,
                        archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_archive_project_round (project_id, review_round)
                    )
                    """);
            log.info("数据库结构自检完成");
        } catch (Exception e) {
            log.warn("数据库结构自检失败，请手动执行 sql/fix_startup.sql: {}", e.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                        """,
                Integer.class, table, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("已添加列 {}.{}", table, column);
        }
    }
}
