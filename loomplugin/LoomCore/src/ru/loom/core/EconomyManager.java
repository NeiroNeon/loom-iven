package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Экономика сервера — монеты. Хранилище: YAML (data/coins.yml), автосохранение.
 */
public final class EconomyManager {

    private final LoomCorePlugin plugin;
    private final File file;
    private final Map<UUID, Double> coins = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public EconomyManager(LoomCorePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/coins.yml");
        load();
    }

    private void load() {
        coins.clear();
        names.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                coins.put(id, y.getDouble(key + ".coins", 0.0));
                names.put(id, y.getString(key + ".name", "?"));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Double> e : coins.entrySet()) {
            String base = e.getKey().toString();
            y.set(base + ".coins", e.getValue());
            y.set(base + ".name", names.getOrDefault(e.getKey(), "?"));
        }
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить coins.yml: " + ex.getMessage());
        }
    }

    public double get(UUID id) {
        return coins.getOrDefault(id, 0.0);
    }

    public void add(UUID id, String name, double amount) {
        if (amount == 0) return;
        double v = Math.max(0, get(id) + amount);
        coins.put(id, v);
        names.put(id, name != null ? name : names.getOrDefault(id, "?"));
    }

    public boolean withdraw(UUID id, double amount) {
        if (amount < 0) return false;
        double cur = get(id);
        if (cur < amount) return false;
        coins.put(id, cur - amount);
        return true;
    }

    public void set(UUID id, String name, double amount) {
        coins.put(id, Math.max(0, amount));
        names.put(id, name != null ? name : names.getOrDefault(id, "?"));
    }

    /** Топ-лист богачей (для /coins top и зала славы). */
    public Map<UUID, Double> top(int limit) {
        Map<UUID, Double> sorted = new LinkedHashMap<>();
        coins.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    public String name(UUID id) {
        String n = names.get(id);
        if (n != null && !n.equals("?")) return n;
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }

    public void touchName(Player p) {
        names.put(p.getUniqueId(), p.getName());
    }
}
