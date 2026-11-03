#!/usr/bin/env bash
# check_state.sh — сверка рабочей копии с парковкой. Запускать ПЕРВЫМ ДЕЛОМ в новой сессии.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARK="/tmp/my-project"

[ -f "$PARK/.mirror_ok" ] || { echo "[check] парковка пуста — зеркало ещё не создавалось"; exit 0; }

echo "[check] парковка: $(date -u -d @$(stat -c %Y "$PARK/.mirror_ok") +%FT%TZ)"
# Ключевые маркеры свежести
for f in VERSION.txt STATE.md download/README.md; do
  if [ -f "$PARK/$f" ]; then
    a=$(stat -c %Y "$ROOT/$f" 2>/dev/null || echo 0)
    b=$(stat -c %Y "$PARK/$f" 2>/dev/null || echo 0)
    if [ "$b" -gt "$a" ]; then
      echo "[check] ВНИМАНИЕ: парковка новее по $f — возможно, рабочий стол откатился."
      echo "[check] Восстановление: bash scripts/restore_from_parking.sh"
    else
      echo "[check] ok: $f (рабочий не старше)"
    fi
  fi
done
echo "[check] версии: рабочий=$(tr -d '[:space:]' < "$ROOT/VERSION.txt" 2>/dev/null || echo ?), парковка=$(tr -d '[:space:]' < "$PARK/VERSION.txt" 2>/dev/null || echo ?)"
