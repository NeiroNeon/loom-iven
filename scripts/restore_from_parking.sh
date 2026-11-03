#!/usr/bin/env bash
# restore_from_parking.sh — восстановление рабочей копии из парковки (PolarFS).
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARK="/tmp/my-project"
[ -f "$PARK/.mirror_ok" ] || { echo "[restore] парковка пуста — восстанавливать нечего"; exit 1; }
rsync -a --exclude '.git' --exclude 'server-pack/world*' --exclude 'server-pack/logs' "$PARK/" "$ROOT/"
echo "[restore] восстановлено из $PARK"
bash "$ROOT/scripts/check_state.sh" || true
