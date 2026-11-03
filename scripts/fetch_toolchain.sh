#!/usr/bin/env bash
# fetch_toolchain.sh — восстановление инструментария Loom Iven (JDK + зависимости).
# Идемпотентен: всё качается только при отсутствии. Используется при каждой новой сессии.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/tools"
DEPS="$TOOLS/deps"
mkdir -p "$TOOLS" "$DEPS"

UA="LoomIvenBuilder/1.5 (Minecraft server pack; contact: admin@loom-iven.local)"
log() { printf '[fetch] %s\n' "$*"; }

# ---------- JDK 25 (сервер 26.3 требует Java >= 25; javac нужен для сборки плагинов) ----------
JDK_DIR="$TOOLS/jdk25"
if [ ! -x "$JDK_DIR/bin/javac" ]; then
  log "JDK 25 отсутствует — скачиваю Temurin..."
  ARCH=$(uname -m); case "$ARCH" in x86_64) A=x64;; aarch64) A=aarch64;; *) A=x64;; esac
  META=$(curl -sf --max-time 30 -H "User-Agent: $UA" \
    "https://api.adoptium.net/v3/assets/latest/25/hotspot?os=linux&architecture=$A&image_type=jdk" || echo "")
  URL=$(echo "$META" | python3 -c "
import json,sys
try:
    d=json.load(sys.stdin)
    print(d[0]['binary']['package']['link'])
except Exception:
    print('')
")
  if [ -z "$URL" ]; then
    # запасной путь: прямой каталог Temurin
    URL="https://github.com/adoptium/temurin25-binaries/releases/latest"
    log "API Adoptium недоступен, ставлю через apt..."
    (sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-25-jdk-headless) \
      || apt-get install -y -qq openjdk-25-jdk-headless || true
  else
    log "URL: $URL"
    curl -sfL --max-time 300 -H "User-Agent: $UA" "$URL" -o /tmp/jdk25.tgz \
      && rm -rf "$JDK_DIR" && mkdir -p "$JDK_DIR" \
      && tar -xzf /tmp/jdk25.tgz -C "$JDK_DIR" --strip-components=1 \
      && rm -f /tmp/jdk25.tgz
  fi
fi
[ -x "$JDK_DIR/bin/javac" ] && log "JDK 25 готов: $JDK_DIR" || log "ВНИМАНИЕ: javac не установлен!"

# ---------- Зависимости для classpath (paper-api тянет часть из них в рантайме) ----------
dl() { # dl <имя_файла> <url>
  local f="$DEPS/$1" u="$2"
  if [ -s "$f" ]; then log "есть: $1"; return 0; fi
  for i in 1 2 3 4 5; do
    curl -sfL --max-time 180 -H "User-Agent: $UA" "$u" -o "$f" && { log "скачано: $1"; return 0; }
    sleep $((i*3))
  done
  log "НЕ УДАЛОСЬ: $1 ($u)"; return 1
}

M2="https://repo1.maven.org/maven2"
PMC="https://repo.papermc.io/repository/maven-public"
# paper-api 26.3.build.231 (для компиляции плагинов; сама сборка Paper качается отдельно)
dl "paper-api-26.3.build.231.jar" "$PMC/io/papermc/paper/paper-api/26.3.build.231-stable/paper-api-26.3.build.231-stable.jar"
dl "gson-2.11.0.jar" "$M2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar"
dl "guava-33.3.1-jre.jar" "$M2/com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.jar"
dl "snakeyaml-2.2.jar" "$M2/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar"
dl "annotations-26.0.2.jar" "$M2/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
dl "adventure-api-4.17.0.jar" "$M2/net/kyori/adventure-api/4.17.0/adventure-api-4.17.0.jar"
dl "adventure-key-4.17.0.jar" "$M2/net/kyori/adventure-key/4.17.0/adventure-key-4.17.0.jar"
dl "adventure-text-serializer-plain-4.17.0.jar" "$M2/net/kyori/adventure-text-serializer-plain/4.17.0/adventure-text-serializer-plain-4.17.0.jar"
dl "examination-api-1.3.0.jar" "$M2/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar"
dl "examination-string-1.3.0.jar" "$M2/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar"
dl "adventure-text-minimessage-4.17.0.jar" "$M2/net/kyori/adventure-text-minimessage/4.17.0/adventure-text-minimessage-4.17.0.jar"
dl "datafixerupper-8.0.16.jar" "$M2/com/mojang/datafixerupper/8.0.16/datafixerupper-8.0.16.jar" || true

log "Готово. tools/: $(du -sh "$TOOLS" 2>/dev/null | cut -f1)"
