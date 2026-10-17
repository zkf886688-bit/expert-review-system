#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== backend mvn test ==="
(cd "$ROOT/backend" && mvn -s ../maven-settings.xml test)

echo "=== frontend build ==="
(cd "$ROOT/frontend" && npm run build)

echo "=== frontend e2e ==="
(cd "$ROOT/frontend" && npm run test:e2e)

echo "All tests passed"
