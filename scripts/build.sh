#!/usr/bin/env bash
# build.sh — компиляция пяти плагинов Loom (порядок: Core → PlaytimeRanks → Auction → Progress → Trader).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVAC="$ROOT/tools/jdk25/bin/javac"
JARTOOL="$ROOT/tools/jdk25/bin/jar"
DEPS="$ROOT/tools/deps"
PL="$ROOT/server-pack/plugins"
OUT="$ROOT/loomplugin/_out"
mkdir -p "$OUT"

CP="$DEPS/paper-api-26.3.build.231.jar:$DEPS/gson-2.11.0.jar:$DEPS/guava-33.3.1-jre.jar:$DEPS/snakeyaml-2.2.jar:$DEPS/annotations-26.0.2.jar:$DEPS/adventure-api-5.2.0.jar:$DEPS/adventure-key-5.2.0.jar:$DEPS/adventure-nbt-5.2.0.jar:$DEPS/adventure-text-serializer-plain-5.2.0.jar:$DEPS/adventure-text-serializer-gson-5.2.0.jar:$DEPS/adventure-text-serializer-legacy-5.2.0.jar:$DEPS/adventure-text-minimessage-5.2.0.jar:$DEPS/examination-api-1.3.0.jar:$DEPS/examination-string-1.3.0.jar:$DEPS/bungeecord-chat.jar:$PL/LuckPerms.jar:$PL/PlaceholderAPI.jar:$PL/Floodgate.jar:$PL/ProtocolLib.jar"

build() { # build <Каталог> <выходной jar> [допCP]
  local src="$ROOT/loomplugin/$1" jar="$2" extra="${3:-}"
  echo "=== Сборка $1 ==="
  find "$src/src" -name '*.java' > /tmp/srcs.txt
  "$JAVAC" -encoding UTF-8 -cp "$CP$extra" -d "$OUT" @/tmp/srcs.txt
  # надёжная упаковка: только СВОИ классы (пакет плагина), затем ресурсы
  local pkg
  case "$1" in
    LoomCore)          pkg="ru/loom/core";;
    LoomPlaytimeRanks) pkg="ru/loom/playtime";;
    LoomAuction)       pkg="ru/loom/auction";;
    LoomProgress)      pkg="ru/loom/progress";;
    LoomTrader)        pkg="ru/loom/trader";;
    *) echo "неизвестный плагин $1"; return 1;;
  esac
  rm -f "$OUT/$jar"
  (cd "$OUT" && find "$pkg" -name '*.class' | sort > /tmp/classes.txt)
  (cd "$OUT" && "$JARTOOL" cf "$OUT/$jar" @/tmp/classes.txt)
  (cd "$src/resources" && "$JARTOOL" uf "$OUT/$jar" .)
  cp "$OUT/$jar" "$PL/$jar"
  echo "    → $jar ($(du -h "$OUT/$jar" | cut -f1))"
}

rm -rf "$OUT/ru"

build "LoomCore"          "LoomCore.jar"
build "LoomPlaytimeRanks" "LoomPlaytimeRanks.jar" ":$OUT/LoomCore.jar"
build "LoomAuction"       "LoomAuction.jar"       ":$OUT/LoomCore.jar"
build "LoomProgress"      "LoomProgress.jar"      ":$OUT/LoomCore.jar:$OUT/LoomPlaytimeRanks.jar"
build "LoomTrader"        "LoomTrader.jar"        ":$OUT/LoomCore.jar"

rm -rf "$OUT/ru"
echo "=== Все плагины собраны ==="
