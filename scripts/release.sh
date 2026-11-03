#!/usr/bin/env bash
# release.sh — сборка релиза Loom Iven: zip-пак с версией из VERSION.txt.
# Проверка ключевых jar + канонических свойств сервера.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/version.sh"
PL="$ROOT/server-pack/plugins"
OUTDIR="$ROOT/download"
mkdir -p "$OUTDIR"
REL="$OUTDIR/loom-iven-$LOOM_VERSION"
rm -rf "$REL"; mkdir -p "$REL"

# --- состав релиза: сервер-пак без мира/логов ---
cd "$ROOT/server-pack"
zip -qr "$REL/server-pack.zip" . \
  -x "world/*" "world_nether/*" "world_the_end/*" "logs/*" "cache/*" \
     "usercache.json" "*.log" "plugins-bootheavy/*" "crash-reports/*" \
  -x "plugins/LoomCore/data/*" "plugins/LoomAuction/data/*" \
     "plugins/LoomProgress/data/*" "plugins/LoomTrader/data/*" \
     "plugins/LoomPlaytimeRanks/data/*" "plugins/GrimAC/data/*" \
     "plugins/LuckPerms/*" "plugins/Plan/*" \
     "plugins/Essentials/*" "plugins/GriefPrevention/*" "plugins/CoreProtect/*" \
     "plugins/ProtocolLib/*" "plugins/DiscordSRV/*" "plugins/PlaceholderAPI/*" \
     "plugins/TAB/*" "plugins/ViaVersion/*" "plugins/WorldGuard/*" "plugins/Geyser-Spigot/*"
# тяжёлые runtime-папки плагинов не включаем: дефолтные конфиги сгенерируются при первом старте

# --- исходники и скрипты ---
cd "$ROOT"
zip -qr "$REL/sources-and-scripts.zip" loomplugin scripts docs README.md STATE.md VERSION.txt -x "loomplugin/_out/*"

# --- сайт сервера ---
SITE="$ROOT/site"
if [ -d "$SITE" ]; then
  mkdir -p "$REL/site"
  cp "$SITE"/*.html "$SITE"/*.css "$SITE"/*.js "$REL/site/" 2>/dev/null || true
  (cd "$REL" && zip -qr site.zip site)
fi

# --- документация в корень релиза ---
cp README.md STATE.md VERSION.txt "$REL/" 2>/dev/null || true
mkdir -p "$REL/docs"
cp docs/*.md "$REL/docs/" 2>/dev/null || true

# --- допаковка НАСТРОЕННЫХ конфигов (папки рантайма исключены выше, но чат/префиксы должны поставляться) ---
python3 - "$REL/server-pack.zip" "$ROOT/server-pack" << 'PYEOF2'
import sys, zipfile, os
zp, spdir = sys.argv[1], sys.argv[2]
extras = ["plugins/Essentials/config.yml", "plugins/TAB/config.yml", "plugins/TAB/groups.yml",
          "plugins/LuckPerms/global-groups.yml", "plugins/Geyser-Spigot/config.yml"]
with zipfile.ZipFile(zp, "a") as z:
    for f in extras:
        full = os.path.join(spdir, f)
        if os.path.isfile(full):
            z.write(full, f)
            print("OK   + " + f)
        else:
            print("MISS " + f)
PYEOF2

# --- проверка ключевых jar в архиве (по листингу, без SIGPIPE-ловушек) ---
python3 - "$REL/server-pack.zip" << 'PYEOF'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1])
names = z.namelist()
jars = [n for n in names if n.startswith("plugins/") and n.endswith(".jar")]
required_plugins = ["LoomCore.jar", "LoomPlaytimeRanks.jar", "LoomAuction.jar",
            "LoomProgress.jar", "LoomTrader.jar", "LuckPerms.jar", "TAB.jar", "GrimAC.jar",
            "GriefPrevention.jar", "DiscordSRV.jar", "Vault.jar", "ViaVersion.jar",
            "ViaBackwards.jar", "WorldEdit.jar", "WorldGuard.jar", "EssentialsX.jar",
            "PlaceholderAPI.jar", "ProtocolLib.jar", "packetevents-spigot.jar",
            "CoreProtect.jar", "Chunky.jar", "Geyser-Spigot.jar", "Floodgate.jar"]
ok = True
for r in required_plugins:
    found = any(r in n for n in jars)
    print(("OK   " if found else "MISS ") + r)
    ok = ok and found
paper = any("paper-26.3-231.jar" in n for n in names)
print(("OK   " if paper else "MISS ") + "paper-26.3-231.jar")
ok = ok and paper
sp = [n for n in names if n.endswith("server.properties")]
print("OK   server.properties" if sp else "MISS server.properties")
print(f"\njar в архиве: {len(jars)} + paper")
sys.exit(0 if ok else 1)
PYEOF

# --- размер и итог ---
du -sh "$REL"/* | sed 's/^/  /'
echo "=== Релиз $LOOM_VERSION собран: $REL ==="