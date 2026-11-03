#!/usr/bin/env bash
# smoke_test.sh — полный boot+smoke цикл за ОДИН вызов:
# старт Paper → ожидание "Done" → RCON smoke-тесты всех ключевых команд → stop.
# Параметры: $1 = цель Общего сбора для теста (по умолчанию 10000)
set -uo pipefail
cd /home/z/my-project/server-pack || exit 1
LOG=/tmp/smoke-boot.log
GOAL="${1:-10000}"
PASS=0; FAIL=0; FAILED=()

echo "=== [1/4] Старт Paper (heap 1024M, --noconsole) ==="
rm -f "$LOG"
timeout -s TERM 210 ../tools/jdk25/bin/java -Xms512M -Xmx1024M -jar paper-26.3-231.jar --nogui --noconsole > "$LOG" 2>&1 &
JAVA_PID=$!

echo "=== [2/4] Ожидание 'Done' (до 150 c) ==="
DONE=0
for i in $(seq 1 150); do
  if grep -q "Done (" "$LOG" 2>/dev/null; then DONE=1; break; fi
  if ! kill -0 "$JAVA_PID" 2>/dev/null; then break; fi
  sleep 1
done
if [ "$DONE" -ne 1 ]; then
  echo "ПРОВАЛ: сервер не дошёл до 'Done' за 150 c"
  tail -20 "$LOG"; kill -9 "$JAVA_PID" 2>/dev/null; exit 1
fi
grep "Done (" "$LOG" | head -1
BOOT_SEC=$i
echo "Boot: ${BOOT_SEC} c"
sleep 3   # RCON-листенеру нужно время после "Done" (v1.8: фикс гонки первого теста)

echo "=== [3/4] RCON smoke-тесты ==="
RCON="python3 /home/z/my-project/scripts/rcon.py"
t() { # t <имя> <команда одним куском или частями — склеиваются через пробел>
  local name="$1"; shift
  local out rc try
  out=$($RCON "$*"); rc=$?
  # v1.8: один ретрай при пустом ответе (гонка подключения RCON)
  if [ $rc -ne 0 ]; then
    sleep 2
    out=$($RCON "$*"); rc=$?
  fi
  if [ $rc -eq 0 ]; then
    echo "OK   $name → $(echo "$out" | head -2 | tr '\n' ' ' | cut -c1-110)"
    PASS=$((PASS+1))
  else
    echo "FAIL $name (rc=$rc) → $(echo "$out" | head -2 | tr '\n' ' ' | cut -c1-110)"
    FAIL=$((FAIL+1)); FAILED+=("$name")
  fi
}

t "list"        "list"
t "version"     "version"
t "меню"        "меню"
t "профиль"     "профиль"
t "монеты"      "монеты"
t "часы"        "часы"
t "часы топ"    "часы" "топ"
t "ah"          "ah"
t "ивент"       "ивент"
t "ачивки"      "ачивки"
t "пропуск"     "пропуск"
t "ключи"       "ключи"
t "ментор"      "ментор"
t "слава"       "слава"
t "квартал"     "квартал"
t "сбор"        "сбор"
t "сбор start"  "сбор" "start" "$GOAL"
t "сбор после"  "сбор"
t "сбор топ"    "сбор" "топ"
t "сбор cancel" "сбор" "cancel"
t "квесты"      "квесты"
t "квесты список" "квесты" "список"
t "заказ"        "заказ"
t "заказ start"  "заказ" "start" "WHEAT" "1000000" "20000" "1"
t "заказ после"  "заказ"
t "заказ топ"    "заказ" "топ"
t "заказ finish" "заказ" "finish"
t "торговец spawn" "торговец" "spawn" "0" "100" "0"
t "торговец remove" "торговец" "remove"
t "luckperms"   "luckperms" "groups"

echo "=== [4/4] Остановка через RCON ==="
$RCON "stop" >/dev/null 2>&1
# ждём завершения до 60 c, затем control
WAITED=0
while kill -0 "$JAVA_PID" 2>/dev/null && [ $WAITED -lt 60 ]; do sleep 2; WAITED=$((WAITED+2)); done
if kill -0 "$JAVA_PID" 2>/dev/null; then kill -9 "$JAVA_PID" 2>/dev/null; echo "Сервер снят по KILL (graceful не уложился)"; else echo "Сервер завершился штатно (${WAITED} c)"; fi

echo
echo "=== ИТОГ: PASS=$PASS FAIL=$FAIL ${FAILED[*]:+провалы: ${FAILED[*]}} ==="
grep -iE "ERROR" "$LOG" | rg -v "Could not|CoreProtect|Spark|Token|DiscordSRV.*token" | head -5
grep -c "Enabling" "$LOG" | xargs echo "Плагинов включено:"
exit $([ $FAIL -eq 0 ] && echo 0 || echo 1)
