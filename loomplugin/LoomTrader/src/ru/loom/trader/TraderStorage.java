package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Хранилище LoomTrader (YAML, автосохранение).
 *  data/trader.yml — НПС, доска дня, сдачи игроков по дням;
 *  data/order.yml  — недельный заказ (цель, фонд, вклады).
 */
final class TraderStorage {

    private final LoomTraderPlugin plugin;
    private final File dir;

    // ---- НПС
    public String npcUuid = null;
    public String orderLastWeek = null; // ISO-неделя последнего завершённого заказа
    public double npcX, npcY, npcZ, npcYaw;
    public String npcWorld = null;

    // ---- доска дня
    public String dailyDate = "";
    public final List<String> dailyIds = new ArrayList<>();
    /** дата → uuid → сданные id */
    public final Map<String, Map<UUID, Set<String>>> doneByDay = new HashMap<>();

    // ---- недельный заказ
    public boolean orderActive = false;
    public String orderMat = "";
    public long orderGoal = 0, orderCollected = 0, orderFund = 0;
    public long orderEndsAt = 0;
    public String orderStartedBy = "";
    public int orderMilestone = 0;
    public final Map<UUID, Long> orderContrib = new HashMap<>();
    public final Map<UUID, String> orderNames = new HashMap<>();

    TraderStorage(LoomTraderPlugin plugin) {
        this.plugin = plugin;
        this.dir = plugin.getDataFolder();
        loadTrader();
        loadOrder();
    }

    private File f(String name) { return new File(dir, "data/" + name); }

    // ================= trader.yml =================
    private void loadTrader() {
        File file = f("trader.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        npcUuid = y.getString("npc.uuid", null);
        npcWorld = y.getString("npc.world", null);
        npcX = y.getDouble("npc.x", 0);
        npcY = y.getDouble("npc.y", -1);
        npcZ = y.getDouble("npc.z", 0);
        npcYaw = (float) y.getDouble("npc.yaw", 0);
        dailyDate = y.getString("daily.date", "");
        dailyIds.clear();
        dailyIds.addAll(y.getStringList("daily.ids"));
        ConfigurationSection done = y.getConfigurationSection("done");
        if (done != null) for (String day : done.getKeys(false)) {
            ConfigurationSection ps = done.getConfigurationSection(day);
            if (ps == null) continue;
            Map<UUID, Set<String>> m = new HashMap<>();
            for (String u : ps.getKeys(false)) {
                try {
                    Set<String> ids = new HashSet<>(ps.getStringList(u));
                    m.put(UUID.fromString(u), ids);
                } catch (IllegalArgumentException ignored) { }
            }
            doneByDay.put(day, m);
        }
    }

    public void saveTrader() {
        YamlConfiguration y = new YamlConfiguration();
        if (npcUuid != null) {
            y.set("npc.uuid", npcUuid);
            y.set("npc.world", npcWorld);
            y.set("npc.x", npcX);
            y.set("npc.y", npcY);
            y.set("npc.z", npcZ);
            y.set("npc.yaw", npcYaw);
        }
        y.set("daily.date", dailyDate);
        y.set("daily.ids", new ArrayList<>(dailyIds));
        for (var day : doneByDay.entrySet()) {
            for (var e : day.getValue().entrySet()) {
                y.set("done." + day.getKey() + "." + e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
        try {
            f("trader.yml").getParentFile().mkdirs();
            y.save(f("trader.yml"));
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось сохранить trader.yml: " + ex.getMessage());
        }
    }

    /** Сдачи только за последние 3 дня — остальное чистим. */
    public void pruneDone() {
        String today = DailyManager.todayStr();
        doneByDay.keySet().removeIf(d -> !d.equals(today));
    }

    public Set<String> doneToday(UUID id) {
        return doneByDay.computeIfAbsent(DailyManager.todayStr(), k -> new HashMap<>())
                .computeIfAbsent(id, k -> new HashSet<>());
    }

    /** Сброс сдач дейликов игроку за сегодня (админ-GUI/команда). */
    public void clearDoneToday(UUID id) {
        var day = doneByDay.get(DailyManager.todayStr());
        if (day != null) {
            var set = day.get(id);
            if (set != null) set.clear();
            saveTrader();
        }
    }

    // ================= order.yml =================
    private void loadOrder() {
        File file = f("order.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        orderActive = y.getBoolean("active", false);
        orderMat = y.getString("material", "");
        orderGoal = y.getLong("goal", 0);
        orderCollected = y.getLong("collected", 0);
        orderFund = y.getLong("fund", 0);
        orderEndsAt = y.getLong("endsAt", 0);
        orderStartedBy = y.getString("startedBy", "");
        orderLastWeek = y.getString("lastWeek", null);
        orderMilestone = y.getInt("milestone", 0);
        ConfigurationSection c = y.getConfigurationSection("contrib");
        if (c != null) for (String k : c.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                orderContrib.put(id, c.getLong(k));
                orderNames.put(id, y.getString("names." + k, "???"));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void saveOrder() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("active", orderActive);
        y.set("material", orderMat);
        y.set("goal", orderGoal);
        y.set("collected", orderCollected);
        y.set("fund", orderFund);
        y.set("endsAt", orderEndsAt);
        y.set("startedBy", orderStartedBy);
        y.set("lastWeek", orderLastWeek);
        y.set("milestone", orderMilestone);
        for (var e : orderContrib.entrySet()) {
            String u = e.getKey().toString();
            y.set("contrib." + u, e.getValue());
            y.set("names." + u, orderNames.getOrDefault(e.getKey(), "???"));
        }
        try {
            f("order.yml").getParentFile().mkdirs();
            y.save(f("order.yml"));
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось сохранить order.yml: " + ex.getMessage());
        }
    }

    public Location npcLocation() {
        if (npcWorld == null || npcY < 0) return null;
        World w = Bukkit.getWorld(npcWorld);
        if (w == null) return null;
        return new Location(w, npcX, npcY, npcZ, (float) npcYaw, 0f);
    }

    public void saveAll() { saveTrader(); saveOrder(); }
}
