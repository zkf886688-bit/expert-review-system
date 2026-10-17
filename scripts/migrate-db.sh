#!/usr/bin/env bash
# 一键升级已有 expert_review 数据库（Linux / macOS / Git Bash）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL="$ROOT/sql/fix_startup.sql"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_NAME="${DB_NAME:-expert_review}"

echo "将执行 $SQL 到 $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
read -rsp "MySQL 密码: " DB_PASS
echo
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL"
echo "数据库升级完成"
