#!/usr/bin/env bash
# 部署前自检（Linux / macOS / Git Bash）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail=0
ok() { echo "[OK] $1"; }
warn() { echo "[WARN] $1"; fail=1; }

if [ -f .env ]; then ok ".env 已存在"; else warn "缺少 .env，请复制 .env.example 并填写"; fi

if [ -f .env ]; then
  if grep -q "change-me" .env 2>/dev/null; then
    warn ".env 中仍含 change-me 占位符，请修改 JWT_SECRET / MYSQL_ROOT_PASSWORD"
  fi
  jwt_secret="$(grep -E '^JWT_SECRET=' .env | head -n1 | cut -d= -f2- | tr -d '\r' || true)"
  if [ -n "${jwt_secret:-}" ] && [ "${#jwt_secret}" -lt 32 ]; then
    warn "JWT_SECRET 长度不足 32 字符"
  fi
  db_pass="$(grep -E '^MYSQL_ROOT_PASSWORD=' .env | head -n1 | cut -d= -f2- | tr -d '\r' || true)"
  if [ -n "${db_pass:-}" ] && { [ "$db_pass" = "root" ] || [ "${#db_pass}" -lt 12 ]; }; then
    warn "MYSQL_ROOT_PASSWORD 过弱，请使用至少 12 位强密码"
  fi
fi

if command -v curl >/dev/null 2>&1; then
  if curl -sf http://127.0.0.1:8080/api/public/health >/dev/null 2>&1; then
    ok "后端健康检查 http://127.0.0.1:8080/api/public/health"
  else
    warn "后端未响应（若尚未启动可忽略）"
  fi
fi

if [ "$fail" -eq 0 ]; then
  echo "部署自检通过"
else
  echo "部署自检有警告，请处理后再上线"
  exit 1
fi
