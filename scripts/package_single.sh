#!/usr/bin/env bash
# package_single.sh — ОДИН архив релиза: download/loom-iven-v<VER>.zip без дублей и старых версий.
# Состав: server-pack/ (готов к запуску: без мира/логов/кэша/data-папок, но с настроенными
# конфигами), loomplugin/ (исходники), scripts/, site/, docs/, README/STATE/VERSION + CHANGELOG
# из git-репозитория. Заменяет прежний набор server-pack.zip + sources-and-scripts.zip + site.zip.
# Кириллические имена записывает с UTF-8-флагом (python zipfile).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DL="$ROOT/download"
VER="$(cat "$ROOT/VERSION.txt" 2>/dev/null || echo unknown)"
NAME="loom-iven-$VER"
STAGE="$(mktemp -d)/$NAME"
mkdir -p "$STAGE"
echo "[pack] версия: $VER"

# ---------- 1. server-pack (исключения как в release.sh + настроенные конфиги обратно) ----------
SP="$STAGE/server-pack"
mkdir -p "$SP"
rsync -a "$ROOT/server-pack/" "$SP/" \
  --exclude 'world/' --exclude 'world_nether/' --exclude 'world_the_end/' \
  --exclude 'logs/' --exclude 'cache/' --exclude 'crash-reports/' \
  --exclude 'usercache.json' --exclude '*.log' \
  --exclude 'plugins-bootheavy/' \
  --exclude 'plugins/LoomCore/data/' --exclude 'plugins/LoomAuction/data/' \
  --exclude 'plugins/LoomProgress/data/' --exclude 'plugins/LoomTrader/data/' \
  --exclude 'plugins/LoomPlaytimeRanks/data/' --exclude 'plugins/GrimAC/data/' \
  --exclude 'plugins/LuckPerms/' --exclude 'plugins/Plan/' \
  --exclude 'plugins/Essentials/' --exclude 'plugins/GriefPrevention/' \
  --exclude 'plugins/CoreProtect/' --exclude 'plugins/ProtocolLib/' \
  --exclude 'plugins/DiscordSRV/' --exclude 'plugins/PlaceholderAPI/' \
  --exclude 'plugins/TAB/' --exclude 'plugins/ViaVersion/' \
  --exclude 'plugins/WorldGuard/' --exclude 'plugins/Geyser-Spigot/'
# тяжёлые runtime-папки исключены, но НАСТРОЕННЫЕ конфиги должны поставляться:
mkdir -p "$SP/plugins/Essentials" "$SP/plugins/TAB" "$SP/plugins/LuckPerms" "$SP/plugins/Geyser-Spigot"
for f in plugins/Essentials/config.yml plugins/TAB/config.yml plugins/TAB/groups.yml \
         plugins/LuckPerms/global-groups.yml plugins/Geyser-Spigot/config.yml \
         НАСТРОЙКА_ПРЕФИКСОВ.txt; do
  [ -f "$ROOT/server-pack/$f" ] && cp "$ROOT/server-pack/$f" "$SP/$f"
done

# ---------- 2. исходники, скрипты, сайт, доки ----------
rsync -a --exclude '_out/' "$ROOT/loomplugin/" "$STAGE/loomplugin/"
mkdir -p "$STAGE/scripts" "$STAGE/docs"
for s in "$ROOT"/scripts/*; do
  case "$(basename "$s")" in
    gen_discord_doc.js|discord_content_a.js|discord_content_b.js|fix_toc_pages.py|gen_report.js|gen_chart.py|patch_footers.py|patch_toc_fix.py) continue;;
  esac
  cp "$s" "$STAGE/scripts/"
done
mkdir -p "$STAGE/site"
cp "$ROOT"/site/*.html "$ROOT"/site/*.css "$ROOT"/site/*.js "$STAGE/site/"
for d in СПИСОК_КОМАНД.md НАСТРОЙКА_ПРЕФИКСОВ.md НАСТРОЙКА_ПРЕФИКСОВ.txt КВЕСТЫ_И_ТОРГОВЕЦ.md АУДИТ_BAT3_v1.8.md НАСТРОЙКА_DISCORD.md; do
  [ -f "$ROOT/docs/$d" ] && cp "$ROOT/docs/$d" "$STAGE/docs/"
done
cp "$ROOT/README.md" "$ROOT/STATE.md" "$ROOT/VERSION.txt" "$STAGE/"
cp "$ROOT/README.md" "$STAGE/docs/РУКОВОДСТВО_СЕРВЕРА.md"
[ -f "$DL/loom-iven-github/CHANGELOG.md" ] && cp "$DL/loom-iven-github/CHANGELOG.md" "$STAGE/"
[ -f "$DL/loom-iven-github/ПУБЛИКАЦИЯ.md" ] && cp "$DL/loom-iven-github/ПУБЛИКАЦИЯ.md" "$STAGE/docs/"

# ---------- 3. zip (UTF-8-имена) ----------
OUT="$DL/$NAME.zip"
rm -f "$OUT" "$DL"/loom-iven-v*.zip.tmp
python3 - "$STAGE" "$OUT" << 'PYEOF'
import sys, os, zipfile
src, out = sys.argv[1], sys.argv[2]
base = os.path.dirname(src)
n = 0
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    for root, dirs, files in os.walk(src):
        dirs.sort(); files.sort()
        for f in files:
            p = os.path.join(root, f)
            z.write(p, os.path.relpath(p, base))
            n += 1
print(f"[pack] файлов: {n}")
PYEOF

# ---------- 4. контроль: ключевые файлы и размер ----------
python3 - "$OUT" << 'PYEOF'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1])
names = z.namelist()
need = ["server-pack/paper-26.3-231.jar", "server-pack/server.properties",
        "server-pack/plugins/LoomCore.jar", "server-pack/plugins/LuckPerms/global-groups.yml",
        "loomplugin/LoomCore/src/ru/loom/core/LoomCorePlugin.java",
        "site/index.html", "README.md", "STATE.md", "VERSION.txt", "CHANGELOG.md"]
miss = [n for n in need if not any(x.endswith("/" + n) for x in names)]
print("OK: все ключевые файлы в архиве" if not miss else f"MISS: {miss}")
print("ZIP OK" if z.testzip() is None else "ZIP ПОВРЕЖДЁН")
PYEOF
du -sh "$OUT"
echo "[pack] готово: $OUT"
