# server-configs — настроенные конфиги релиза v1.8.0

Это **настроенные** конфиги из рабочего сервера (не дефолты). Применяются поверх
свежераспакованного `server-pack.zip` или сгенерированного сервера:

```bash
cp -r server-configs/server.properties server-pack/
cp -r server-configs/bukkit.yml server-configs/spigot.yml server-pack/
cp -r server-configs/config server-pack/          # paper-global.yml, paper-world-defaults.yml
cp -r server-configs/plugins/<Плагин> server-pack/plugins/
```

## Состав

| Каталог/файл | Что настроено |
|---|---|
| `server.properties` | порты 25565/19132-кроссплей/RCON 25575; **секреты — плейсхолдеры `CHANGE_ME_*`** |
| `bukkit.yml`, `spigot.yml`, `config/paper-*.yml` | базовая настройка ядра |
| `plugins/LuckPerms/global-groups.yml` | **14 групп с плашками** (лестница + донат + персонал), веса |
| `plugins/LuckPerms/config.yml` | storage yaml, primary groups |
| `plugins/TAB/` | шапка/подвал с логотипом, плашки у ников, сортировка по рангам |
| `plugins/Geyser-Spigot/config.yml` | auth-type floodgate (Bedrock-вход открыт) |
| `plugins/Essentials/config.yml` | русская локаль, экономика через Vault |
| `plugins/DiscordSRV/config.yml` | шаблон с `BotToken: "BOTTOKEN"` — вставьте свой токен |
| `plugins/Loom*/config.yml` (+lang) | конфиги и языки всех 5 своих плагинов |

ВАЖНО: `server.properties` и `DiscordSRV/config.yml` в репозитории сознательно содержат
плейсхолдеры вместо секретов. Реальные значения (RCON-пароль, Discord-токен бота) задайте
локально и **не коммитьте их**.
