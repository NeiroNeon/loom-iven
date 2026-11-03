package ru.loom.playtime;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LoomPlaytimeRanks 1.4 — учёт наигранных часов и движение по лестнице
 * (Новичок → Постоялец → Хранитель → Легенда) с синхронизацией групп LuckPerms.
 * Часы отдаёт LoomCore (анти-АФК учитывается). Хранилище: data/playtime.yml.
 */
public final class PlaytimePlugin extends JavaPlugin {

    private static PlaytimePlugin instance;

    private final Map<UUID, Long> minutes = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastRank = new ConcurrentHashMap<>();

    private File file;

    public static PlaytimePlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        file = new File(getDataFolder(), "data/playtime.yml");
        load();

        // Часы берём из LoomCore (анти-АФК внутри)
        if (Bukkit.getPluginManager().getPlugin("LoomCore") != null) {
            var core = ru.loom.core.LoomCorePlugin.get();
            if (core != null) {
                core.ranks().setPlaytimeSource(this::minutesOf);
                getLogger().info("LoomCore подключён — лестница двигается от наших часов.");
            }
        }

        Bukkit.getPluginManager().registerEvents(new PlaytimeListener(this), this);

        var hours = getCommand("hours");
        if (hours != null) hours.setExecutor(new HoursCommand(this));

        // Тик: +1 минута активным игрокам раз в минуту
        Bukkit.getScheduler().runTaskTimer(this, this::tickMinute, 20L * 60, 20L * 60);
        // Автосохранение каждые 5 минут
        Bukkit.getScheduler().runTaskTimer(this, this::save, 20L * 300, 20L * 300);

        getLogger().info("LoomPlaytimeRanks включён: " + minutes.size() + " игроков в базе.");
    }

    @Override
    public void onDisable() {
        save();
        instance = null;
    }

    private void tickMinute() {
        long activeAfterIdle = 0; // активность решает LoomCore.isAFK
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Bukkit.getPluginManager().getPlugin("LoomCore") != null) {
                var core = ru.loom.core.LoomCorePlugin.get();
                if (core != null && core.afk().isIdle(p)) continue;
            }
            UUID id = p.getUniqueId();
            long prev = minutes.getOrDefault(id, 0L);
            minutes.put(id, prev + 1);
            names.put(id, p.getName());
            checkLadder(p, prev + 1);
        }
    }

    /** Порог лестницы из LoomCore, продвижение — здесь. */
    private void checkLadder(Player p, long nowMinutes) {
        var core = ru.loom.core.LoomCorePlugin.get();
        if (core == null) return;
        String now = core.ranks().ladderKey(nowMinutes);
        String before = lastRank.getOrDefault(p.getUniqueId(), "novice");
        lastRank.put(p.getUniqueId(), now);
        if (now.equals(before)) return;
        // порядок растёт только вверх
        if (order(now) <= order(before)) return;
        syncLuckPerms(p, now);
        String hours = String.valueOf(nowMinutes / 60);
        core.lang().broadcast("rankup." + now, "%player%", p.getName(), "%hours%", hours);
    }

    private int order(String key) {
        return switch (key) {
            case "resident" -> 1;
            case "keeper" -> 2;
            case "legend" -> 3;
            default -> 0;
        };
    }

    /**
     * Группы LuckPerms через консольные команды (v1.8: remove+add вместо set,
     * чтобы не слетали донат-привилегии и стафф-оверлеи игрока).
     */
    public void syncLuckPerms(Player p, String ladderKey) {
        String group = getConfig().getString("luckperms.group." + ladderKey, ladderKey);
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return;
        String[] ladder = {"novice", "resident", "keeper", "legend"};
        for (String g : ladder) {
            if (!g.equals(group)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + p.getName() + " parent remove " + g);
            }
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + p.getName() + " parent add " + group);
    }

    public void addMinutes(UUID id, long m) {
        minutes.merge(id, m, Long::sum);
    }

    public long minutesOf(UUID id) {
        return minutes.getOrDefault(id, 0L);
    }

    public Map<UUID, Long> top(int limit) {
        Map<UUID, Long> out = new LinkedHashMap<>();
        minutes.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    public String name(UUID id) {
        String n = names.get(id);
        if (n != null) return n;
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                minutes.put(id, y.getLong(k + ".minutes"));
                names.put(id, y.getString(k + ".name", "?"));
                lastRank.put(id, y.getString(k + ".rank", "novice"));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : minutes.entrySet()) {
            String b = e.getKey().toString();
            y.set(b + ".minutes", e.getValue());
            y.set(b + ".name", names.getOrDefault(e.getKey(), "?"));
            y.set(b + ".rank", lastRank.getOrDefault(e.getKey(), "novice"));
        }
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            getLogger().warning("save playtime.yml: " + ex.getMessage());
        }
    }
}
