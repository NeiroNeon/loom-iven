#!/usr/bin/env bash
# ci_deps.sh — только 4 jar, нужные для classpath сборки плагинов Loom
# (полный fetch_plugins.sh в CI не нужен: качает все 23 плагина).
# Источники те же, что в fetch_plugins.sh. LOOM_PL_DIR переопределяет каталог
# назначения (по умолчанию server-pack/plugins — как у сборки).
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PL="${LOOM_PL_DIR:-$ROOT/server-pack/plugins}"
mkdir -p "$PL"
UA="LoomIvenBuilder/1.8 (Minecraft server pack; GitHub CI)"
UAH="Mozilla/5.0 (X11; Linux x86_64) LoomIvenBuilder/1.8"
log() { printf '[ci-deps] %s\n' "$*"; }

once() { # once <имя_файла> <url>
  local out="$PL/$1" url="$2"
  [ -s "$out" ] && { log "есть: $1"; return 0; }
  curl -sfL --max-time 300 -H "User-Agent: $UA" "$url" -o "$out" \
    && log "скачано: $1 ($(du -h "$out" | cut -f1))" \
    || { log "ОШИБКА: $1"; return 1; }
}
gh_get() { # gh_get <repo> <regex имени asset> <имя файла>
  local repo="$1" re="$2" out="$3"
  [ -s "$PL/$out" ] && { log "есть: $out"; return 0; }
  local tag assets name
  for i in 1 2 3; do
    tag=$(curl -s --max-time 30 -H "User-Agent: $UAH" -o /dev/null -w '%{redirect_url}' \
      "https://github.com/$repo/releases/latest" | sed -n 's|.*/tag/\([^/?#]*\).*|\1|p')
    [ -z "$tag" ] && { sleep 5; continue; }
    assets=$(curl -s --max-time 30 -H "User-Agent: $UAH" \
      "https://github.com/$repo/releases/expanded_assets/$tag" \
      | grep -o 'href="[^"]*"' | sed 's/^href="//; s/"$//' | grep '/releases/download/' | sort -u)
    name=$(echo "$assets" | sed 's|.*/||' | grep -E "$re" | head -1)
    [ -z "$name" ] && { sleep 4; continue; }
    once "$out" "https://github.com/$repo/releases/download/$tag/$name" && return 0
    sleep 5
  done
  log "НЕ УДАЛОСЬ: $out ($repo)"; return 1
}

# Floodgate (API форм Bedrock)
once "Floodgate.jar" "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"
# LuckPerms (API групп/префиксов)
LP_URL=$(curl -sf --max-time 20 -H "User-Agent: $UAH" "https://metadata.luckperms.net/data/all" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['downloads']['bukkit'])" 2>/dev/null)
[ -n "${LP_URL:-}" ] && once "LuckPerms.jar" "$LP_URL" || log "ОШИБКА: LuckPerms (metadata)"
# PlaceholderAPI и ProtocolLib (GitHub releases)
gh_get "PlaceholderAPI/PlaceholderAPI" "PlaceholderAPI-2\.[0-9.]+\.jar$" "PlaceholderAPI.jar"
gh_get "dmulloy2/ProtocolLib"          "ProtocolLib\.jar$"               "ProtocolLib.jar"

FAIL=0
for j in Floodgate.jar LuckPerms.jar PlaceholderAPI.jar ProtocolLib.jar; do
  [ -s "$PL/$j" ] || { log "отсутствует: $j"; FAIL=1; }
done
[ "$FAIL" = "0" ] && log "все 4 jar на месте" || log "есть пропуски — сборка может не собраться"
exit $FAIL
