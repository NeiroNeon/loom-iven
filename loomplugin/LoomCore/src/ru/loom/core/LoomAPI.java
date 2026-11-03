package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.util.UUID;

/**
 * Публичный API LoomCore для остальных плагинов (LoomAuction, LoomProgress, ...).
 */
public final class LoomAPI {

    private final LoomCorePlugin plugin;

    LoomAPI(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Экономика ----------

    public double coins(UUID id) {
        return plugin.economy().get(id);
    }

    public boolean has(UUID id, double amount) {
        return plugin.economy().get(id) >= amount;
    }

    public void add(UUID id, String name, double amount) {
        plugin.economy().add(id, name, amount);
        Player p = Bukkit.getPlayer(id);
        if (p != null && amount >= 0) {
            plugin.lang().send(p, "coins.gain", "%amount%", fmt(amount));
        }
    }

    public boolean withdraw(UUID id, String name, double amount) {
        boolean ok = plugin.economy().withdraw(id, amount);
        if (ok) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) plugin.lang().send(p, "coins.spend", "%amount%", fmt(amount));
        }
        return ok;
    }

    public void set(UUID id, String name, double amount) {
        plugin.economy().set(id, name, amount);
    }

    public static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return String.format("%.2f", v);
    }

    // ---------- Язык ----------

    public String lang(UUID id) {
        return plugin.lang().langOf(id);
    }

    public void setLang(UUID id, String code) {
        plugin.lang().setLang(id, code);
    }

    /** Локализованная строка (без игрока — для broadcast: указывайте код языка). */
    public String msg(String code, String key, String... kv) {
        return plugin.lang().raw(code, key, kv);
    }

    /** Локализованная строка для игрока (язык берётся из его настроек). */
    public String msg(UUID id, String key, String... kv) {
        return plugin.lang().raw(langOf(id), key, kv);
    }

    private String langOf(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return plugin.lang().langOf(id);
    }

    // ---------- Лестница активности ----------

    /** Ключ ранга лестницы: novice | resident | keeper | legend */
    public String ladderRank(UUID id) {
        long minutes = plugin.ranks().playtimeMinutes(id);
        return plugin.ranks().ladderKey(minutes);
    }

    public int ladderOrder(UUID id) {
        long minutes = plugin.ranks().playtimeMinutes(id);
        return plugin.ranks().ladderOrder(minutes);
    }

    /** Ключ косметического ранга за часы: none | veteran | elder | virtuoso | icon */
    public String cosmeticRank(UUID id) {
        long minutes = plugin.ranks().playtimeMinutes(id);
        return plugin.ranks().cosmeticKey(minutes);
    }

    public long playtimeMinutes(UUID id) {
        return plugin.ranks().playtimeMinutes(id);
    }

    public boolean isAtLeast(UUID id, String ladderKey) {
        return plugin.ranks().ladderOrder(plugin.ranks().playtimeMinutes(id))
                >= plugin.ranks().ladderOrder(plugin.ranks().ladderThresholdMinutes(ladderKey));
    }

    // ---------- АФК ----------

    public boolean isAFK(UUID id) {
        Player p = Bukkit.getPlayer(id);
        return p == null || plugin.afk().isIdle(p);
    }
}
