#!/usr/bin/env python3
"""audit_site.py — статический аудит сайта Loom Iven:
1) i18n-ключи из HTML (data-i18n, data-i18n-*, плейсхолдеры) vs словари i18n.js
2) внутренние ссылки/якоря
3) эмодзи в HTML/CSS/JS (должны быть заменены SVG)
4) обязательные элементы: ссылки создателя, sticky footer, темы, языки
"""
import re, sys, html
from pathlib import Path

SITE = Path("/home/z/my-project/site")
issues = []
warn = []

pages = sorted(SITE.glob("*.html"))
js = (SITE / "i18n.js").read_text(encoding="utf-8")
css = (SITE / "styles.css").read_text(encoding="utf-8")

# ---------- 1. словарь EN (var EN = { ... }; RU живёт в HTML) ----------
m_en = re.search(r"var\s+EN\s*=\s*\{", js)
en_keys: set[str] = set()
en_vals: dict[str, str] = {}
if m_en:
    start = m_en.end() - 1
    depth = 0
    for i in range(start, len(js)):
        if js[i] == "{":
            depth += 1
        elif js[i] == "}":
            depth -= 1
            if depth == 0:
                break
    body = js[start : i + 1]
    for m in re.finditer(r"'([A-Za-z0-9_.\-]+)'\s*:\s*'((?:\\.|[^'\\])*)'", body, re.S):
        en_keys.add(m.group(1))
        en_vals[m.group(1)] = m.group(2)
else:
    warn.append("не найден var EN = {...} в i18n.js")
ru_keys = en_keys  # RU — исходный текст в HTML, ключи те же

# ---------- 2. ключи, используемые на страницах ----------
used: dict[str, list[str]] = {}
for p in pages:
    t = p.read_text(encoding="utf-8")
    for m in re.finditer(r'data-i18n(?:-([a-z]+))?\s*=\s*["\']([^"\']+)["\']', t):
        kind, key = m.group(1) or "", m.group(2)
        used.setdefault(key, []).append(f"{p.name}#{kind or 'txt'}")
    # i18n.js-словарь может использоваться и из JS-кода страниц: t('ключ')
    for m in re.finditer(r"""(?:t|i18n|tr|translate)\(\s*['"]([a-z0-9_.\-]+)['"]""", t, re.I):
        used.setdefault(m.group(1), []).append(f"{p.name}:js-call")

missing_ru = sorted(k for k in used if ru_keys and k not in ru_keys)
missing_en = sorted(k for k in used if en_keys and k not in en_keys)
if missing_ru:
    issues.append("Ключей НЕТ в ru: " + ", ".join(missing_ru[:40]))
if missing_en:
    issues.append("Ключей НЕТ в en: " + ", ".join(missing_en[:40]))

# ---------- 2b. RU-остатки внутри EN-строк (реальные дыры перевода) ----------
ALLOW_RU = re.compile(
    r"/[а-яё][а-яё]*(?:\s+[а-яё]+)*"   # русские команды с аргументами: /заказ сдать все
    r"|«[^»]*»"
    r"|\b(лум|ивен|ивлия|ивлий)\b",
    re.I,
)
cyr = re.compile(r"[а-яё]+", re.I)
for k, v in sorted(en_vals.items()):
    stripped = ALLOW_RU.sub("", v)
    hits = cyr.findall(stripped)
    if hits:
        issues.append(f"i18n.js: в EN-значении '{k}' русский текст: {' '.join(hits[:6])}")

# ---------- 3. ссылки ----------
names = {p.name for p in pages}
for p in pages:
    t = p.read_text(encoding="utf-8")
    for m in re.finditer(r'href=["\']([^"\']+)["\']', t):
        href = html.unescape(m.group(1))
        if href.startswith(("http", "mailto:", "tel:", "#", "javascript:", "data:")):
            if href.startswith("#"):
                anchor = href[1:]
                # #i-* — ссылки на SVG-спрайт (<use>), он инжектится i18n.js
                if anchor and not anchor.startswith("i-") and \
                   f'id="{anchor}"' not in t and f"id='{anchor}'" not in t:
                    issues.append(f"{p.name}: битый якорь {href}")
            continue
        path = href.split("#")[0].split("?")[0]
        if path and path not in names and not Path(SITE / path).exists():
            issues.append(f"{p.name}: битая ссылка → {href}")

# ---------- 4. эмодзи ----------
emoji_re = re.compile(
    "[\U0001F300-\U0001FAFF\U0001F000-\U0001F02F\U0000FE0F\U00002764\U0001F900-\U0001F9FF"
    "\U00002600-\U000026FF]"  # ↔/→/← (21xx) — типографика, не эмодзи
)
for f in list(pages) + [SITE / "styles.css", SITE / "i18n.js"]:
    t = f.read_text(encoding="utf-8")
    hits = set(emoji_re.findall(t))
    if hits:
        issues.append(f"{f.name}: эмодзи {''.join(sorted(hits))[:20]}")

# ---------- 5. обязательные элементы (на главной и в подвале) ----------
idx = (SITE / "index.html").read_text(encoding="utf-8")
for needle, what in [
    ("t.me/SYNAPSE_of", "ссылка Telegram создателя"),
    ("discord.gg/2cuM7kpR", "ссылка Discord"),
]:
    if needle not in idx:
        issues.append(f"index.html: нет {what}")
if "position: sticky" not in css and "position:sticky" not in css:
    if "margin-top:auto" not in css and "margin-top: auto" not in css and "flex-grow" not in css:
        warn.append("styles.css: не найден явный sticky/flex-футер — проверить визуально")
for f in ("terms.html", "privacy.html", "offer.html"):
    if not (SITE / f).exists():
        issues.append(f"нет legal-страницы {f}")

# ---------- 6. пустые EN-значения ----------
empties = [k for k, v in en_vals.items() if not v.strip()]
if empties:
    warn.append("пустые EN-значения: " + ", ".join(empties[:20]))

# ключи в EN-словаре, не используемые ни на одной странице (мёртвые)
dead = sorted(k for k in en_keys if k not in used)
if dead:
    warn.append(f"мёртвых EN-ключей: {len(dead)} (примеры: {', '.join(dead[:10])})")

print(f"страниц: {len(pages)}; ключей использовано: {len(used)}; ru-словарь: {len(ru_keys)}; en-словарь: {len(en_keys)}")
for w in warn:
    print("WARN:", w)
if issues:
    print(f"\n=== ПРОБЛЕМЫ ({len(issues)}) ===")
    for i in issues:
        print(" -", i)
    sys.exit(1)
print("\n=== Статический аудит: чисто ===")
