CREATE DATABASE IF NOT EXISTS expert_review DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE expert_review;

-- 用户与角色
CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    real_name   VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(32),
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    expert_id   BIGINT       NULL COMMENT '专家账号关联专家库',
    data_domain VARCHAR(64)  NULL DEFAULT 'DEFAULT' COMMENT '数据域，REVIEW_ADMIN 可见范围',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 专家库
CREATE TABLE expert (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64)  NOT NULL,
    gender          VARCHAR(8),
    organization    VARCHAR(256),
    title           VARCHAR(64),
    education       VARCHAR(64),
    phone           VARCHAR(32),
    email           VARCHAR(128),
    id_card         VARCHAR(256) COMMENT '加密存储',
    bank_account    VARCHAR(256) COMMENT '加密存储',
    region          VARCHAR(64),
    org_type        VARCHAR(32) COMMENT '高校/企业/科研院所',
    fields          JSON COMMENT '专业领域列表',
    tags            JSON COMMENT '标签列表',
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/BLACKLIST/PENDING',
    review_count    INT          NOT NULL DEFAULT 0,
    on_time_rate    DECIMAL(5,2) DEFAULT 100.00,
    avg_quality_score DECIMAL(4,2) NULL COMMENT '平均质量评分',
    remark          TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_expert_name (name),
    INDEX idx_expert_org (organization),
    INDEX idx_expert_status (status)
);

-- 评审任务
CREATE TABLE review_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(256) NOT NULL,
    review_type     VARCHAR(64)  NOT NULL COMMENT '揭榜挂帅/立项/中期/结题等',
    batch_name      VARCHAR(128),
    data_domain     VARCHAR(64)  NULL DEFAULT 'DEFAULT' COMMENT '任务所属数据域',
    start_time      DATETIME,
    deadline        DATETIME     NOT NULL,
    min_reviewers   INT          NOT NULL DEFAULT 3,
    form_config     JSON         NOT NULL COMMENT '评分项配置',
    blind_review    TINYINT      NOT NULL DEFAULT 0 COMMENT '盲审模式',
    avoid_same_org  TINYINT      NOT NULL DEFAULT 1 COMMENT '同单位回避',
    avoid_duplicate_expert TINYINT(1) NOT NULL DEFAULT 1 COMMENT '同任务专家不重复',
    status          VARCHAR(32)  NOT NULL DEFAULT 'UNASSIGNED',
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_status (status)
);

CREATE TABLE review_project (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    project_name    VARCHAR(256) NOT NULL,
    applicant       VARCHAR(128),
    organization    VARCHAR(256),
    applicant_user_id BIGINT     NULL COMMENT '关联申报人账号',
    sort_order      INT          DEFAULT 0,
    review_round    INT          NOT NULL DEFAULT 1 COMMENT '评审轮次',
    rectification_status VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/REQUESTED/SUBMITTED/RE_REVIEWING',
    rectification_note TEXT NULL COMMENT '管理员整改要求',
    rectification_reply TEXT NULL COMMENT '申报人整改说明',
    rectification_requested_at DATETIME NULL,
    rectification_submitted_at DATETIME NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_task (task_id)
);

CREATE TABLE review_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    project_id      BIGINT       NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_material_task (task_id)
);

-- 专家分配
CREATE TABLE review_assignment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    project_id      BIGINT       NOT NULL,
    expert_id       BIGINT       NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/SUBMITTED/OVERDUE',
    reject_reason   VARCHAR(512),
    invited_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at    DATETIME,
    submitted_at    DATETIME,
    UNIQUE KEY uk_assignment (task_id, project_id, expert_id),
    INDEX idx_assignment_expert (expert_id),
    INDEX idx_assignment_status (status)
);

-- 评审提交
CREATE TABLE review_submission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id   BIGINT       NOT NULL UNIQUE,
    task_id         BIGINT       NOT NULL,
    project_id      BIGINT       NOT NULL,
    expert_id       BIGINT       NOT NULL,
    scores          JSON         NOT NULL COMMENT '各项评分',
    total_score     DECIMAL(8,2),
    opinion         TEXT,
    conclusion      VARCHAR(64) COMMENT '推荐立项/不推荐/修改后通过等',
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED',
    version         INT          NOT NULL DEFAULT 1,
    submitted_at    DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_submission_project (project_id)
);

-- 审计日志
CREATE TABLE audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT,
    username        VARCHAR(64),
    action          VARCHAR(64)  NOT NULL,
    target_type     VARCHAR(64),
    target_id       BIGINT,
    detail          TEXT,
    ip              VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_time (created_at)
);

CREATE TABLE sys_notification (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(256) NOT NULL,
    content     TEXT,
    type        VARCHAR(32),
    related_id  BIGINT,
    read_flag   TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notify_user (user_id, read_flag)
);

CREATE TABLE expert_credit_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    assignment_id   BIGINT,
    submission_id   BIGINT,
    record_type     VARCHAR(32)  NOT NULL COMMENT 'QUALITY/ON_TIME/REJECT/OVERDUE',
    score           DECIMAL(4,2),
    comment         TEXT,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit_expert (expert_id)
);

CREATE TABLE expert_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    material_type   VARCHAR(64)  NULL COMMENT '简历/职称证明/身份证/保密协议等',
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expert_material (expert_id)
);

CREATE TABLE sys_config (
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   BIGINT       NULL
);

CREATE TABLE project_rectification_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rect_material_project (project_id)
);

-- 初始角色
INSERT INTO sys_role (code, name) VALUES
('ADMIN', '系统管理员'),
('REVIEW_ADMIN', '评审管理员'),
('EXPERT', '外部专家'),
('LEADER', '研究院领导'),
('APPLICANT', '项目申报人');

-- 默认账号 password: admin123
INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (1, 2);

-- 领导账号 password: leader123（启动时 DataInitializer 会重置为 BCrypt）
INSERT INTO sys_user (username, password, real_name, status) VALUES
('leader', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '研究院领导', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'leader' AND r.code = 'LEADER';
