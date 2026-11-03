package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Локализация RU/EN на игрока (хранится в data/lang.yml).
 * Файлы сообщений: lang/messages_ru.yml и lang/messages_en.yml (выгружаются при первом старте).
 */
public final class LangManager {

    private final LoomCorePlugin plugin;
    private YamlConfiguration ru;
    private YamlConfiguration en;
    private final File playerFile;
    private final Map<UUID, String> langs = new ConcurrentHashMap<>();

    public LangManager(LoomCorePlugin plugin) {
        this.plugin = plugin;
        extractDefault("lang/messages_ru.yml");
        extractDefault("lang/messages_en.yml");
        this.ru = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lang/messages_ru.yml"));
        this.en = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lang/messages_en.yml"));
        this.playerFile = new File(plugin.getDataFolder(), "data/lang.yml");
        if (playerFile.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(playerFile);
            for (String k : y.getKeys(false)) {
                try { langs.put(UUID.fromString(k), y.getString(k, "ru")); }
                catch (IllegalArgumentException ignored) { }
            }
        }
    }

    private void extractDefault(String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (plugin.getResource(path) == null) return;
        // языковые файлы — часть пака: перезаписываем из jar при каждом старте
        // (пользовательские правки делаются в config.yml, не в messages_*.yml)
        if (!f.exists() || f.length() != resourceSize(path)) {
            f.getParentFile().mkdirs();
            plugin.saveResource(path, true);
        }
    }

    private long resourceSize(String path) {
        try (var in = plugin.getResource(path)) {
            byte[] buf = in.readAllBytes();
            return buf.length;
        } catch (Exception e) {
            return -1;
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : langs.entrySet()) y.set(e.getKey().toString(), e.getValue());
        try {
            playerFile.getParentFile().mkdirs();
            y.save(playerFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить lang.yml: " + ex.getMessage());
        }
    }

    public String langOf(UUID id) {
        return langs.getOrDefault(id, plugin.getConfig().getString("lang.default", "ru"));
    }

    public void setLang(UUID id, String code) {
        langs.put(id, "en".equalsIgnoreCase(code) ? "en" : "ru");
    }

    public YamlConfiguration file(String code) {
        return "en".equals(code) ? en : ru;
    }

    /** Сырое сообщение с подстановками (для конкретного игрока — язык его настроек). */
    public String raw(UUID id, String key, String... kv) {
        return raw(langOf(id), key, kv);
    }

    /** Сырое сообщение с подстановками. */
    public String raw(String code, String key, String... kv) {
        YamlConfiguration f = file(code);
        String s = f.getString(key, ru.getString(key, key));
        if (s != null) {
            String prefix = f.getString("prefix", ru.getString("prefix", ""));
            // «именные» ключи (GUI, названия рангов) никогда не получают чат-префикс
            if (prefix != null && !prefix.isEmpty() && !plainKey(key)) s = prefix + s;
            for (int i = 0; i + 1 < kv.length; i += 2) {
                s = s.replace(kv[i], kv[i + 1] == null ? "" : kv[i + 1]);
            }
        }
        return s != null ? s : key;
    }

    private static boolean plainKey(String key) {
        return key.startsWith("raw.") || key.startsWith("gui.") || key.startsWith("auction.gui.")
                || key.startsWith("ladder.") || key.startsWith("cosmetic.");
    }

    public void send(CommandSender to, String key, String... kv) {
        String code = "ru";
        if (to instanceof Player p) code = langOf(p.getUniqueId());
        to.sendMessage(raw(code, key, kv));
    }

    public void broadcast(String key, String... kv) {
        Bukkit.broadcastMessage(raw(plugin.getConfig().getString("lang.default", "ru"), key, kv));
        Bukkit.broadcastMessage(raw("en", key, kv));
    }

    /** Broadcast на двух языках (RU-строка, EN-строка). */
    public void broadcastDual(String ruMsg, String enMsg) {
        Bukkit.broadcastMessage(ruMsg);
        Bukkit.broadcastMessage(enMsg);
    }
}
