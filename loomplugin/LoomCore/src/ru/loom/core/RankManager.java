package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Лестница активности и косметические ранги за часы.
 * Источник часов — SQLite/YAML база LoomPlaytimeRanks (мягкая зависимость).
 * Если плагин часов не найден — часы ведутся локально (примитивно, онлайн-время).
 */
public final class RankManager {

    /** Лестница: ключ → (порог в минутах, порядок, отображение RU/EN задаётся в конфиге). */
    public record Ladder(String key, long thresholdMinutes, int order) { }

    private final LoomCorePlugin plugin;
    private final Map<String, Long> ladder = new LinkedHashMap<>();
    private final Map<String, Long> cosmetics = new LinkedHashMap<>();
    private PlaytimeSource source;

    public interface PlaytimeSource {
        long minutes(UUID id);
    }

    public RankManager(LoomCorePlugin plugin) {
        this.plugin = plugin;
        ladder.put("novice", minutes(0));
        ladder.put("resident", hours(24));
        ladder.put("keeper", hours(120));
        ladder.put("legend", hours(360));
        cosmetics.put("veteran", hours(168));
        cosmetics.put("elder", hours(336));
        cosmetics.put("virtuoso", hours(504));
        cosmetics.put("icon", hours(672));
        // перезапись из конфига
        var cfg = plugin.getConfig().getConfigurationSection("ladder");
        if (cfg != null) for (String k : cfg.getKeys(false)) ladder.put(k, hours(cfg.getLong(k)));
        var cos = plugin.getConfig().getConfigurationSection("cosmetic-ranks");
        if (cos != null) for (String k : cos.getKeys(false)) cosmetics.put(k, hours(cos.getLong(k)));
    }

    public void setPlaytimeSource(PlaytimeSource src) {
        this.source = src;
    }

    public long playtimeMinutes(UUID id) {
        if (source != null) return source.minutes(id);
        // фолбэк: считаем онлайн-время по данным сессий (грубая оценка)
        Player p = Bukkit.getPlayer(id);
        if (p == null) return 0;
        return plugin.afk().sessionActiveMinutes(id);
    }

    public String ladderKey(long minutes) {
        String key = "novice";
        for (Map.Entry<String, Long> e : ladder.entrySet()) {
            if (minutes >= e.getValue()) key = e.getKey();
        }
        return key;
    }

    public int ladderOrder(long minutes) {
        int order = 0;
        for (Map.Entry<String, Long> e : ladder.entrySet()) {
            if (minutes >= e.getValue()) order = Math.max(order, e.getValue().intValue() >= 0 ? ladderOrderOf(e.getKey()) : 0);
        }
        return order;
    }

    private int ladderOrderOf(String key) {
        int i = 0;
        for (String k : ladder.keySet()) {
            if (k.equals(key)) return i;
            i++;
        }
        return 0;
    }

    public long ladderThresholdMinutes(String key) {
        return ladder.getOrDefault(key, 0L);
    }

    public String cosmeticKey(long minutes) {
        String key = "none";
        for (Map.Entry<String, Long> e : cosmetics.entrySet()) {
            if (minutes >= e.getValue()) key = e.getKey();
        }
        return key;
    }

    public long cosmeticThresholdMinutes(String key) {
        return cosmetics.getOrDefault(key, Long.MAX_VALUE);
    }

    public static long hours(long h) { return h * 60L; }
    public static long minutes(long m) { return m; }
}
