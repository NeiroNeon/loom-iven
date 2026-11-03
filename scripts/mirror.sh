#!/usr/bin/env bash
# mirror.sh — зеркалирование рабочего состояния в долговечную парковку /tmp/my-project (PolarFS).
# Мир (server-pack/world*) и логи не зеркалим — они тяжёлые и восстанавливаемые генерацией.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARK="/tmp/my-project"
mkdir -p "$PARK"

rsync -a --delete \
  --exclude 'server-pack/world*' \
  --exclude 'server-pack/logs' \
  --exclude 'server-pack/cache' \
  --exclude 'server-pack/usercache.json' \
  --exclude 'server-pack/*.log' \
  --exclude '.git' \
  --exclude 'tool-results' \
  --exclude 'upload' \
  "$ROOT/" "$PARK/"

touch "$PARK/.mirror_ok"
echo "[mirror] $(date -u +%FT%TZ) — скопировано в $PARK:"
du -sh "$PARK"/* 2>/dev/null | sort -rh | head -12
