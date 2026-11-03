package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Кастомные достижения: 36 ачивок в 10 категориях (v1.4).
 * Прогресс из счётчиков ProgressListeners; награды — монеты и/или ключи.
 */
public final class AchievementsManager {

    public static final class Achievement {
        public final String id, category;
        public final long goal;
        public final double coins;
        public final int keys;
        Achievement(String id, String category, long goal, double coins, int keys) {
            this.id = id; this.category = category; this.goal = goal; this.coins = coins; this.keys = keys;
        }
    }

    private final ProgressPlugin plugin;
    private final Map<String, Achievement> byId = new LinkedHashMap<>();
    private final List<String> categories = new ArrayList<>();

    public AchievementsManager(ProgressPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        byId.clear();
        categories.clear();
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("achievements");
        if (cats == null) return;
        for (String cat : cats.getKeys(false)) {
            categories.add(cat);
            ConfigurationSection list = cats.getConfigurationSection(cat);
            if (list == null) continue;
            for (String id : list.getKeys(false)) {
                ConfigurationSection a = list.getConfigurationSection(id);
                if (a == null) continue;
                byId.put(id, new Achievement(id, cat, a.getLong("goal", 1),
                        a.getDouble("coins", 0), a.getInt("keys", 0)));
            }
        }
    }

    public List<String> categories() {
        return categories;
    }

    public Map<String, Achievement> byCategory(String cat) {
        Map<String, Achievement> out = new LinkedHashMap<>();
        byId.values().stream().filter(a -> a.category.equals(cat)).forEach(a -> out.put(a.id, a));
        return out;
    }

    public Achievement get(String id) {
        return byId.get(id);
    }

    public int total() {
        return byId.size();
    }

    /** Добавить прогресс; вернёт true, если ачивка завершена сейчас. */
    public boolean addProgress(UUID id, String achId, long delta) {
        Achievement a = byId.get(achId);
        if (a == null) return false;
        var st = plugin.storage();
        var done = st.achDone.computeIfAbsent(id, k -> new ArrayList<>());
        if (done.contains(achId)) return false;
        Map<String, Long> prog = st.achProgress.computeIfAbsent(id, k -> new LinkedHashMap<>());
        long v = prog.merge(achId, delta, Long::sum);
        if (v >= a.goal) {
            done.add(achId);
            prog.remove(achId);
            var core = LoomCorePlugin.get();
            if (a.coins > 0) core.api().add(id, nameOf(id), a.coins);
            if (a.keys > 0) plugin.keys().giveKey(id, a.keys);
            core.lang().broadcast("ach.done",
                    "%player%", nameOf(id),
                    "%ach%", core.msg(id, "ach.name." + achId),
                    "%coins%", LoomCorePlugin.get().api().fmt(a.coins),
                    "%keys%", String.valueOf(a.keys));
            return true;
        }
        return false;
    }

    public long progressOf(UUID id, String achId) {
        Map<String, Long> m = plugin.storage().achProgress.get(id);
        return m != null ? m.getOrDefault(achId, 0L) : 0L;
    }

    public boolean isDone(UUID id, String achId) {
        List<String> d = plugin.storage().achDone.get(id);
        return d != null && d.contains(achId);
    }

    public int doneCount(UUID id) {
        List<String> d = plugin.storage().achDone.get(id);
        return d != null ? d.size() : 0;
    }

    private String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }
}
