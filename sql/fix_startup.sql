-- 一键修复常见缺失表/字段（MySQL Workbench / Navicat 中执行）
-- 选中 expert_review 库后运行本脚本

USE expert_review;

CREATE TABLE IF NOT EXISTS sys_config (
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   BIGINT       NULL
);

CREATE TABLE IF NOT EXISTS expert_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    material_type   VARCHAR(64)  NULL,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expert_material (expert_id)
);

CREATE TABLE IF NOT EXISTS project_rectification_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rect_material_project (project_id)
);

-- 以下 ALTER 若报 Duplicate column 可忽略
SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'expert' AND COLUMN_NAME = 'avg_quality_score') = 0,
    'ALTER TABLE expert ADD COLUMN avg_quality_score DECIMAL(4,2) NULL COMMENT ''平均质量评分''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_task' AND COLUMN_NAME = 'avoid_duplicate_expert') = 0,
    'ALTER TABLE review_task ADD COLUMN avoid_duplicate_expert TINYINT(1) NOT NULL DEFAULT 1',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'review_round') = 0,
    'ALTER TABLE review_project ADD COLUMN review_round INT NOT NULL DEFAULT 1',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'rectification_status') = 0,
    'ALTER TABLE review_project ADD COLUMN rectification_status VARCHAR(32) NOT NULL DEFAULT ''NONE''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'rectification_note') = 0,
    'ALTER TABLE review_project ADD COLUMN rectification_note TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'rectification_reply') = 0,
    'ALTER TABLE review_project ADD COLUMN rectification_reply TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'rectification_requested_at') = 0,
    'ALTER TABLE review_project ADD COLUMN rectification_requested_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'rectification_submitted_at') = 0,
    'ALTER TABLE review_project ADD COLUMN rectification_submitted_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'data_domain') = 0,
    'ALTER TABLE sys_user ADD COLUMN data_domain VARCHAR(64) NULL DEFAULT ''DEFAULT''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_task' AND COLUMN_NAME = 'data_domain') = 0,
    'ALTER TABLE review_task ADD COLUMN data_domain VARCHAR(64) NULL DEFAULT ''DEFAULT''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'review_project' AND COLUMN_NAME = 'applicant_user_id') = 0,
    'ALTER TABLE review_project ADD COLUMN applicant_user_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_user SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;
UPDATE review_task SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'applicant' AND r.code = 'APPLICANT'
AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

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
);

SELECT 'fix_startup.sql 执行完成' AS message;
