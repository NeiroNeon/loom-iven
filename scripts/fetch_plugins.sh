#!/usr/bin/env bash
# fetch_plugins.sh — загрузка сторонних плагинов (проверенные источники, идемпотентно).
# GitHub releases → для стабильных; Modrinth/Hangar/Maven — для остальных. Rate-limit-safe.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PL="$ROOT/server-pack/plugins"
mkdir -p "$PL"
UA="LoomIvenBuilder/1.5 (Minecraft server pack)"
UAH="Mozilla/5.0 (X11; Linux x86_64) LoomIvenBuilder/1.5"
log() { printf '[plugins] %s\n' "$*"; }
DEPTH="${FETCH_DEPTH:-0}"

once() { # once <имя_файла> <url>
  local out="$PL/$1" url="$2"
  [ -s "$out" ] && { log "есть: $1"; return 0; }
  curl -sfL --max-time 300 -H "User-Agent: $UA" "$url" -o "$out" \
    && log "скачано: $1 ($(du -h "$out" | cut -f1))" \
    || { log "ОШИБКА: $1"; return 1; }
}

# GitHub latest-release (через redirect + expanded_assets) — с ретраями
gh_get() {
  local repo="$1" re="$2" out="$3"
  [ -s "$PL/$out" ] && { log "есть: $out"; return 0; }
  local tag assets name
  for i in 1 2 3; do
    tag=$(curl -s --max-time 30 -H "User-Agent: $UAH" -o /dev/null -w '%{redirect_url}' \
      "https://github.com/$repo/releases/latest" | sed -n 's|.*/tag/\([^/?#]*\).*|\1|p')
    [ -z "$tag" ] && { sleep 5; continue; }
    assets=$(curl -s --max-time 30 -H "User-Agent: $UAH" \
      "https://github.com/$repo/releases/expanded_assets/$tag" \
      | grep -o "href=\"[^\"]*\"" | sed 's/^href="//; s/"$//' | grep '/releases/download/' | sort -u)
    name=$(echo "$assets" | sed 's|.*/||' | grep -E "$re" | head -1)
    [ -z "$name" ] && { sleep 4; continue; }
    once "$out" "https://github.com/$repo/releases/download/$tag/$name" && return 0
    sleep 5
  done
  log "НЕ УДАЛОСЬ: $out ($repo)"; return 1
}

# Modrinth: первая версия с bukkit-лоадером
modrinth_get() {
  local slug="$1" out="$2"
  [ -s "$PL/$out" ] && { log "есть: $out"; return 0; }
  local url
  url=$(curl -sf --max-time 30 "https://api.modrinth.com/v2/project/$slug/version" -H "User-Agent: $UA" | python3 -c "
import json,sys
d=json.load(sys.stdin)
for v in d:
    if 'bukkit' in v.get('loaders', []):
        print(v['files'][0]['url']); break
")
  [ -n "$url" ] && once "$out" "$url"
}

# Hangar: последняя версия PAPER-платформы
hangar_get() {
  local slug="$1" out="$2"
  [ -s "$PL/$out" ] && { log "есть: $out"; return 0; }
  local url
  url=$(curl -sf --max-time 30 "https://hangar.papermc.io/api/v1/projects/$slug/versions" -H "Accept: application/json" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(d['result'][0]['name'])
" | head -1)
  [ -z "$url" ] && { log "нет версий на Hangar: $slug"; return 1; }
  local dl
  dl=$(curl -sf --max-time 30 "https://hangar.papermc.io/api/v1/projects/$slug/versions/$url" -H "Accept: application/json" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(d['downloads']['PAPER']['downloadUrl'])
")
  [ -n "$dl" ] && once "$out" "$dl"
}

# Maven: release-версия
mvn_get() {
  local base="$1" path="$2" out="$3"
  [ -s "$PL/$out" ] && { log "есть: $out"; return 0; }
  local ver
  ver=$(curl -sf --max-time 30 -H "User-Agent: $UAH" "$base/$path/maven-metadata.xml" | grep -o '<release>[^<]*</release>' | sed 's/<[^>]*>//g')
  [ -z "$ver" ] && { log "нет release: $path"; return 1; }
  once "$out" "$base/$path/$ver/$path-$ver.jar"
}

# --- прямые URL (Geyser/Floodgate/LuckPerms) ---
once "Geyser-Spigot.jar" "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
once "Floodgate.jar"    "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"
LP_URL=$(curl -sf --max-time 20 -H "User-Agent: $UAH" "https://metadata.luckperms.net/data/all" | python3 -c "import json,sys; print(json.load(sys.stdin)['downloads']['bukkit'])" 2>/dev/null)
[ -n "${LP_URL:-}" ] && once "LuckPerms.jar" "$LP_URL"

# --- GitHub releases ---
gh_get "MilkBowl/Vault"                 "Vault\.jar$"                          "Vault.jar"
gh_get "PlaceholderAPI/PlaceholderAPI"  "PlaceholderAPI-2\.[0-9.]+\.jar$"      "PlaceholderAPI.jar"
gh_get "dmulloy2/ProtocolLib"           "ProtocolLib\.jar$"                    "ProtocolLib.jar"
gh_get "retrooper/packetevents"         "packetevents-spigot-2\.[0-9.]+\.jar$" "packetevents-spigot.jar"
gh_get "NEZNAMY/TAB"                    "TAB\.v[0-9][0-9.]*\.jar$"             "TAB.jar"
gh_get "EssentialsX/Essentials"         "EssentialsX-2\.[0-9.]+\.jar$"         "EssentialsX.jar"
gh_get "DiscordSRV/DiscordSRV"          "DiscordSRV-Build-[0-9][^/]*\.jar$"    "DiscordSRV.jar"
gh_get "plan-player-analytics/Plan"     "Plan-[0-9][^/]*\.jar$"                "Plan.jar"
gh_get "ViaVersion/ViaVersion"          "ViaVersion-[0-9][^/]*\.jar$"          "ViaVersion.jar"
gh_get "ViaVersion/ViaBackwards"        "ViaBackwards-[0-9][^/]*\.jar$"        "ViaBackwards.jar"

# --- Modrinth / Hangar / Maven (там, где GitHub не отдаёт jar) ---
modrinth_get "grimac"          "GrimAC.jar"
modrinth_get "griefprevention" "GriefPrevention.jar"
modrinth_get "coreprotect"     "CoreProtect.jar"
hangar_get   "Chunky"          "Chunky.jar"
mvn_get "https://maven.enginehub.org/repo" "com/sk89q/worldedit/worldedit-bukkit"   "WorldEdit.jar"
mvn_get "https://maven.enginehub.org/repo" "com/sk89q/worldguard/worldguard-bukkit" "WorldGuard.jar"

log "=== Итог: $(ls "$PL"/*.jar 2>/dev/null | wc -l) jar в plugins/ (spark не публикуется — опционален) ==="
