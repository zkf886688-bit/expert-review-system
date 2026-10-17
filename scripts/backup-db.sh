#!/usr/bin/env bash
# MySQL 备份
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${DB_HOST:-127.0.0.1}"
PORT="${DB_PORT:-3306}"
USER="${DB_USER:-root}"
DATABASE="${DB_NAME:-expert_review}"
OUT_DIR="${BACKUP_DIR:-$ROOT/backups}"
mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="$OUT_DIR/${DATABASE}_${STAMP}.sql"
echo "备份 $DATABASE -> $OUT_FILE"
mysqldump -h "$HOST" -P "$PORT" -u "$USER" -p --single-transaction --routines --triggers "$DATABASE" > "$OUT_FILE"
echo "[OK] 备份完成: $OUT_FILE"
