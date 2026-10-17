-- 修复 MySQL 中文乱码（问号）问题
-- 用法: mysql -u root -p expert_review < sql/fix_encoding.sql

USE expert_review;

ALTER DATABASE expert_review CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE review_task CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE review_project CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE expert CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_role CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_notification CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE audit_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE review_submission CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修复已变成问号的常见示例数据（可按需修改）
UPDATE review_task
SET name = '2026年度立项评审',
    review_type = '立项',
    batch_name = '第一批',
    form_config = '[{"key":"innovation","name":"创新性","maxScore":30,"weight":0.25},{"key":"feasibility","name":"可行性","maxScore":25,"weight":0.25},{"key":"team","name":"团队基础","maxScore":20,"weight":0.20},{"key":"value","name":"应用价值","maxScore":15,"weight":0.15},{"key":"budget","name":"经费合理性","maxScore":10,"weight":0.15}]'
WHERE name LIKE '2026%' AND name LIKE '%?%';

UPDATE review_project
SET project_name = '示例项目A',
    applicant = '张三',
    organization = '示例单位'
WHERE project_name LIKE '%?%' OR applicant LIKE '%?%' OR organization LIKE '%?%';

UPDATE sys_role SET name = '系统管理员' WHERE code = 'ADMIN';
UPDATE sys_role SET name = '评审管理员' WHERE code = 'REVIEW_ADMIN';
UPDATE sys_role SET name = '外部专家' WHERE code = 'EXPERT';
UPDATE sys_role SET name = '研究院领导' WHERE code = 'LEADER';
UPDATE sys_role SET name = '项目申报人' WHERE code = 'APPLICANT';

UPDATE sys_user SET real_name = '系统管理员' WHERE username = 'admin';
UPDATE sys_user SET real_name = '研究院领导' WHERE username = 'leader';
UPDATE sys_user SET real_name = '示例申报人' WHERE username = 'applicant';

SELECT 'fix_encoding.sql 执行完成' AS message;
