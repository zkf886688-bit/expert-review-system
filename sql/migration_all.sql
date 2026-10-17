-- One-shot upgrade for existing expert_review database
-- Run: mysql -u root -p expert_review < sql/migration_all.sql
-- Ignore "Duplicate column" errors if already applied

USE expert_review;

-- Phase 2 (safe IF NOT EXISTS objects only; run migration_phase2.sql for full Phase 2)
-- Phase 3
CREATE TABLE IF NOT EXISTS expert_credit_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    assignment_id   BIGINT,
    submission_id   BIGINT,
    record_type     VARCHAR(32)  NOT NULL,
    score           DECIMAL(4,2),
    comment         TEXT,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit_expert (expert_id)
);

-- Phase 3 column (skip if exists)
-- ALTER TABLE expert ADD COLUMN avg_quality_score DECIMAL(4,2) NULL;

-- Phase 5 role
INSERT INTO sys_role (code, name)
SELECT 'APPLICANT', 'Applicant' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'APPLICANT');

-- Phase 5 columns (skip if exists)
-- ALTER TABLE sys_user ADD COLUMN data_domain VARCHAR(64) NULL DEFAULT 'DEFAULT';
-- ALTER TABLE review_task ADD COLUMN data_domain VARCHAR(64) NULL DEFAULT 'DEFAULT';
-- ALTER TABLE review_project ADD COLUMN applicant_user_id BIGINT NULL;

UPDATE sys_user SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;
UPDATE review_task SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;

-- Phase 11
CREATE TABLE IF NOT EXISTS sys_config (
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   BIGINT       NULL
);

-- Phase 12 (ignore if column exists)
-- ALTER TABLE review_task ADD COLUMN avoid_duplicate_expert TINYINT(1) NOT NULL DEFAULT 1;

-- Phase 13
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
