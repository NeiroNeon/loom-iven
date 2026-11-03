# Сторонние компоненты сервер-пака

Все версии зафиксированы в релизе **v1.8.0**. Загрузка — `scripts/fetch_plugins.sh`
(источники: официальные сайты, GitHub Releases, Modrinth, Maven). Лицензии — в
репозиториях соответствующих проектов; каждый компонент распространяется своим
правообладателем и в этот репозиторий **не входит**.

| Компонент | Версия | Назначение | Источник |
|---|---|---|---|
| Paper | 26.3-231 | ядро сервера | papermc.io |
| LuckPerms | 5.5.88 | группы, права, префиксы-плашки (14 групп) | luckperms.net |
| Vault | 1.7.3 | мост экономики/прав | github.com/MilkBowl/Vault |
| TAB | 6.2.1 | таб-лист, шапка/подвал, сортировка | github.com/NEZNAMY/TAB |
| GrimAC | 2.3.75 | античит | grim.ac |
| packetevents-spigot | 2.x | пакетная прослойка для GrimAC | github.com/retrooper/packetevents |
| GriefPrevention | 16.18.7 | приват территорий | github.com/TechFortress/GriefPrevention |
| WorldEdit | 7.4.6 | редактирование мира | enginehub.org |
| WorldGuard | 7.0.20 | защита регионов | enginehub.org |
| EssentialsX | 2.22.0 | базовые команды | essentialsx.net |
| CoreProtect | 24.1 | логи блоков (ждёт апдейта под 26.3, отключается сам) | coreprotect.net |
| Chunky | 1.5.3 | предгенерация мира | github.com/pop4959/Chunky |
| DiscordSRV | 1.30.5 | Discord ↔ чат (нужен свой токен) | github.com/DiscordSRV/DiscordSRV |
| PlaceholderAPI | 2.12.3 | плейсхолдеры %loom_coins%, %loom_rank%… | github.com/PlaceholderAPI/PlaceholderAPI |
| ViaVersion | 5.12.2 | Java-клиенты новых версий | viaversion.com |
| ViaBackwards | 5.12.2 | Java-клиенты старых версий | viaversion.com |
| Geyser-Spigot | 2.12.1 | Bedrock-кроссплей (UDP 19132) | geysermc.org |
| Floodgate | 2.2.5 | auth Bedrock + нативные формы | geysermc.org |
| ProtocolLib | 5.4.0 | пакеты (НПС Ивлия) | github.com/dmulloy2/ProtocolLib |
| Plan | 3.x (bootheavy) | аналитика (опционально) | plan.playeranalytics.net |

Собственные плагины (исходники в `loomplugin/`): **LoomCore**, **LoomPlaytimeRanks**,
**LoomAuction**, **LoomProgress**, **LoomTrader** — v1.8.0.
