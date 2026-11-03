# Loom Iven — Minecraft-сервер (Paper 26.3) · публичный репозиторий

> Кроссплатформенный survival+SMP-сервер: Java-клиенты 26.x (ViaVersion/ViaBackwards) и
> Bedrock-клиенты (Geyser + Floodgate). 5 собственных плагинов, GUI-каркас для Java и Bedrock,
> сезонный пропуск, НПС-торговец, статический сайт.

Этот репозиторий — **источник истины проекта**. Дальнейшая разработка ведётся от последнего
релиза здесь (тег), а не из старых архивов. Готовый к запуску сервер-пак (~280 МБ) публикуется
не в git, а как **asset GitHub Release** к соответствующему тегу.

| | |
|---|---|
| Ядро | Paper 26.3 (build 231), Java 25+ |
| Свои плагины | LoomCore · LoomPlaytimeRanks · LoomAuction · LoomProgress · LoomTrader — все 1.8.0 |
| Сторонние плагины | 19 (LuckPerms, TAB, GrimAC, Geyser/Floodgate, EssentialsX и др. — см. [PLUGINS.md](PLUGINS.md)) |
| Сайт | статический, без внешних зависимостей, RU/EN, 2 темы — каталог `site/` |
| Документация | [docs/](docs/) — список команд, префиксы, квесты, аудит Батча 3 |

## Структура

```
├── docs/                 документация + руководство сервера (РУКОВОДСТВО_СЕРВЕРА.md)
├── loomplugin/           исходники 5 плагинов (Java, пакет ru.loom.*)
├── scripts/              сборка, релиз, смоук-тесты, загрузка инструментария
├── server-configs/       НАСТРОЕННЫЕ конфиги из релиза v1.8.0 (сервер + все ключевые плагины)
├── site/                 сайт сервера (9 страниц, RU/EN, тёмная/светлая темы)
└── .github/workflows/    CI: сборка плагинов; GitHub Pages для сайта
```

## Запуск сервера

Скачайте `server-pack.zip` со страницы [Releases](../../releases) (тег v1.8.0), распакуйте:

```bash
cd server-pack
java -Xms512M -Xmx2048M -jar paper-26.3-231.jar --nogui   # Java 25+
```

Мир генерируется при первом старте. Bedrock-вход: UDP-порт 19132 (Geyser, auth-type floodgate).
RCON: порт 25575 — пароль в `server.properties` задайте свой (в паке стоит рабочий).

## Сборка из исходников

```bash
bash scripts/fetch_toolchain.sh   # JDK 25 + зависимости для classpath (идемпотентно)
bash scripts/fetch_plugins.sh     # все 19 сторонних плагинов
bash scripts/build.sh             # компиляция 5 своих плагинов → server-pack/plugins/
bash scripts/release.sh           # zip-релиз в download/ (пак + исходники + сайт)
```

Для быстрой проверки сборки без скачивания всех плагинов:
`bash scripts/ci_deps.sh` (только 4 jar для classpath) → `bash scripts/build.sh`.

## Конфигурация

`server-configs/` содержит настроенные конфиги релиза v1.8.0: 14 групп LuckPerms с
плашками-привилегиями, TAB (шапка/подвал с логотипом), Geyser (floodgate), Essentials, все
конфиги своих плагинов. Скопируйте нужные файлы в `server-pack/` поверх свежесгенерированных.
`server.properties` в репозитории — с плейсхолдерами секретов (`CHANGE_ME_*`); рабочие значения
генерируются сервером или задаются вручную.

## Обновление репозитория из рабочего дерева (для разработчиков)

```bash
bash scripts/publish_github.sh               # синхронизация + коммит
LOOM_TAG=v1.8.1 bash scripts/publish_github.sh   # + аннотированный тег
git push origin main --follow-tags
```

Тяжёлые файлы (server-pack, zip-архивы, tools/) в git не попадают — они идут в Release.

## English summary

Cross-platform survival+SMP Minecraft server on Paper 26.3 (Java 25+): 5 custom plugins
(economy & playtime ranks, auction, events/season pass, NPC trader), chest-GUI + Bedrock
native forms, 14 LuckPerms groups with badge prefixes, static bilingual website in `site/`.
Run: grab `server-pack.zip` from Releases. Build: `scripts/fetch_toolchain.sh` →
`scripts/build.sh`. Full server manual: `docs/РУКОВОДСТВО_СЕРВЕРА.md` (Russian).

## Сообщество

- Discord: [discord.gg/2cuM7kpR](https://discord.gg/2cuM7kpR)
- Telegram: [t.me/SYNAPSE_of](https://t.me/SYNAPSE_of)

## Лицензия

Код проекта — все права защищены (см. [LICENSE](LICENSE)). Сторонние плагины сохраняют свои
лицензии и распространяются своими авторами — [PLUGINS.md](PLUGINS.md).
