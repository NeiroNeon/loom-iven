# НАСТРОЙКА DISCORD — Loom Iven (DiscordSRV 1.30.5)

> Полная настройка самого Discord-сервера (категории, каналы, роли, права, модерация,
> чек-лист запуска) — в документе «Discord-сервер Loom Iven — полное руководство»
> (`Discord_сервер_Loom_Iven_руководство.docx` в релизе v1.6/docs). Здесь — только мост DiscordSRV.

DiscordSRV установлен, но отключён до ввода токена (без токена он сам гасится — это норма).

## Шаги (займёт ~10 минут)
1. **Создать бота**: https://discord.com/developers/applications → New Application → Bot → Reset Token → скопировать токен.
2. **Права бота** (OAuth2 → URL Generator): scopes `bot` + `applications.commands`; права: View Channels, Send Messages, Manage Webhooks, Read Message History.
3. **Пригласить бота** на сервер Discord по сгенерированной ссылке.
4. **Вставить токен**: откройте `server-pack/plugins/DiscordSRV/config.yml` → ключ `BotToken: "ВАШ_ТОКЕН"` (токен никому не пересылайте).
5. **Привязать каналы** в том же конфиге:
   ```yaml
   Channels:
     "000000000000000000": "global"   # ID текстового канала Discord: server-канал
   ```
   ID канала: ПКМ по каналу в Discord → «Копировать ID» (включите режим разработчика в настройках Discord).
6. **Запустить сервер** и проверить в консоли: `[DiscordSRV] Successfully logged in as ...`.
7. Консоль в Discord (опционально): `ConsoleChannel: "ID_КАНАЛА"` — логи сервера в приватный канал.

## Что даст Discord (по утверждённому плану)
- Синхронизация чата Minecraft ↔ Discord.
- Профили игроков: ник ментора и его код видны в Discord (позже — на сайте).
- Оповещения: старт/финиш «Общего сбора», итоги ивентов, повышение рангов.

## Безопасность
- Токен хранится только в config.yml на сервере; в git не попадает (папка plugins/DiscordSRV не трекается, релизы собираются без data-папок).
- Если токен утёк — Reset Token в панели разработчика и обновить конфиг.
