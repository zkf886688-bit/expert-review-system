-- 演示评审任务（一般由后端启动时 DemoTaskSeeder 自动写入/刷新截止时间）
-- 若需手动执行：mysql -u root -p expert_review < sql/demo_tasks.sql
-- 重启后端后，【演示】任务截止时间会按当前日期自动顺延（app.demo.seed-tasks=true）

SELECT COUNT(*) AS demo_task_count FROM review_task WHERE name LIKE '【演示】%';
