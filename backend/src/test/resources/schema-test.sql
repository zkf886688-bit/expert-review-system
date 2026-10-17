CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    real_name   VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(32),
    status      TINYINT      NOT NULL DEFAULT 1,
    expert_id   BIGINT       NULL,
    data_domain VARCHAR(64)  NULL DEFAULT 'DEFAULT',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
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

CREATE TABLE expert (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64)  NOT NULL,
    gender          VARCHAR(8),
    organization    VARCHAR(256),
    title           VARCHAR(64),
    education       VARCHAR(64),
    phone           VARCHAR(32),
    email           VARCHAR(128),
    id_card         VARCHAR(256),
    bank_account    VARCHAR(256),
    region          VARCHAR(64),
    org_type        VARCHAR(32),
    fields          VARCHAR(4000),
    tags            VARCHAR(4000),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    review_count    INT          NOT NULL DEFAULT 0,
    on_time_rate    DECIMAL(5,2) DEFAULT 100.00,
    avg_quality_score DECIMAL(4,2) NULL,
    remark          TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(256) NOT NULL,
    review_type     VARCHAR(64)  NOT NULL,
    batch_name      VARCHAR(128),
    data_domain     VARCHAR(64)  NULL DEFAULT 'DEFAULT',
    start_time      DATETIME,
    deadline        DATETIME     NOT NULL,
    min_reviewers   INT          NOT NULL DEFAULT 3,
    form_config     VARCHAR(4000) NOT NULL,
    blind_review    TINYINT      NOT NULL DEFAULT 0,
    avoid_same_org  TINYINT      NOT NULL DEFAULT 1,
    avoid_duplicate_expert TINYINT NOT NULL DEFAULT 1,
    status          VARCHAR(32)  NOT NULL DEFAULT 'UNASSIGNED',
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_project (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    project_name    VARCHAR(256) NOT NULL,
    applicant       VARCHAR(128),
    organization    VARCHAR(256),
    applicant_user_id BIGINT     NULL,
    sort_order      INT          DEFAULT 0,
    review_round    INT          NOT NULL DEFAULT 1,
    rectification_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    rectification_note TEXT,
    rectification_reply TEXT,
    rectification_requested_at DATETIME,
    rectification_submitted_at DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    project_id      BIGINT       NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    material_type   VARCHAR(64)  NULL,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_assignment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    project_id      BIGINT       NOT NULL,
    expert_id       BIGINT       NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    reject_reason   VARCHAR(512),
    invited_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at    DATETIME,
    submitted_at    DATETIME,
    CONSTRAINT uk_assignment UNIQUE (task_id, project_id, expert_id)
);

CREATE TABLE review_submission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id   BIGINT       NOT NULL UNIQUE,
    task_id         BIGINT       NOT NULL,
    project_id      BIGINT       NOT NULL,
    expert_id       BIGINT       NOT NULL,
    scores          VARCHAR(4000) NOT NULL,
    total_score     DECIMAL(8,2),
    opinion         TEXT,
    conclusion      VARCHAR(64),
    attachment_path VARCHAR(512) NULL,
    attachment_name VARCHAR(256) NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    version         INT          NOT NULL DEFAULT 1,
    submitted_at    DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expert_avoidance (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    avoid_type      VARCHAR(32)  NOT NULL,
    avoid_value     VARCHAR(256) NOT NULL,
    reason          VARCHAR(512) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT,
    username        VARCHAR(64),
    action          VARCHAR(64)  NOT NULL,
    target_type     VARCHAR(64),
    target_id       BIGINT,
    detail          TEXT,
    ip              VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_notification (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(256) NOT NULL,
    content     TEXT,
    type        VARCHAR(32),
    related_id  BIGINT,
    read_flag   TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expert_credit_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    assignment_id   BIGINT,
    submission_id   BIGINT,
    record_type     VARCHAR(32)  NOT NULL,
    score           DECIMAL(4,2),
    comment         TEXT,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expert_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    material_type   VARCHAR(64)  NULL,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_config (
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   BIGINT       NULL
);

CREATE TABLE review_submission_archive (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      BIGINT       NOT NULL,
    review_round    INT          NOT NULL,
    assignment_id   BIGINT       NULL,
    expert_id       BIGINT       NOT NULL,
    expert_name     VARCHAR(64),
    scores          VARCHAR(4000),
    total_score     DECIMAL(8,2),
    opinion         TEXT,
    conclusion      VARCHAR(64),
    submitted_at    DATETIME,
    archived_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_rectification_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_role (code, name) VALUES
('ADMIN', '系统管理员'),
('REVIEW_ADMIN', '评审管理员'),
('EXPERT', '外部专家'),
('LEADER', '研究院领导'),
('APPLICANT', '项目申报人');

INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (1, 2);

INSERT INTO sys_user (username, password, real_name, status) VALUES
('leader', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '研究院领导', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'leader' AND r.code = 'LEADER';

INSERT INTO sys_user (username, password, real_name, status, data_domain) VALUES
('applicant', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '示例申报人', 1, 'DEFAULT');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'applicant' AND r.code = 'APPLICANT';
