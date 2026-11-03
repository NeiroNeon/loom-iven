package ru.loom.progress;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Хранилище всех данных LoomProgress (YAML, автосохранение).
 * Файлы: ach.yml (достижения), season.yml (пропуск), keys.yml (ключи+косметика),
 * mentors.yml (менторы), quarter.yml (участки), fund.yml (общий сбор).
 */
public final class Storage {

    private final ProgressPlugin plugin;
    private final File dir;

    // ---- достижения: uuid → (achId → прогресс) ; готовые: uuid → set(id)
    public final Map<UUID, Map<String, Long>> achProgress = new HashMap<>();
    public final Map<UUID, List<String>> achDone = new HashMap<>();

    // ---- сезонный пропуск (v1.8 — реальные сутки)
    public final Map<UUID, Boolean> passDonate = new HashMap<>();       // купил донат-ветку
    public final Map<UUID, List<String>> passClaimed = new HashMap<>(); // id наград
    public final Map<UUID, Long> passStart = new HashMap<>();           // активация цикла (epoch ms)
    public final Map<UUID, Integer> passChrono = new HashMap<>();       // хроно-предметы (шт)
    public final Map<UUID, Long> weeklyKeyDay = new HashMap<>();        // последний еженедельный донат-ключ (epoch day)
    public final Map<UUID, String> prevSeasonCosmetic = new HashMap<>();// миграция косметики сезона
    public String seasonId = "S1";
    public long seasonStart = 0L;

    // ---- ключи и косметика
    public final Map<UUID, Integer> keys = new HashMap<>();
    public final Map<UUID, List<String>> cosmeticsOwned = new HashMap<>();
    public final Map<UUID, String> activeCosmetic = new HashMap<>();
    public final Map<UUID, Long> hoursAtLastKey = new HashMap<>();      // часы, за которые ключ уже выдан

    // ---- менторы: код → ментор ; ментор → ученики ; ученик → ментор
    public final Map<String, UUID> codes = new HashMap<>();
    public final Map<UUID, List<UUID>> students = new HashMap<>();
    public final Map<UUID, UUID> mentorOf = new HashMap<>();

    // ---- квартал: номер участка → владелец
    public final Map<Integer, UUID> plotOwners = new HashMap<>();

    // ---- общий сбор (активное событие)
    public boolean fundActive = false;
    public double fundTarget = 0;
    public double fundCollected = 0;
    public long fundEndsAt = 0;
    public final Map<UUID, Double> fundContrib = new HashMap<>();
    public final Map<UUID, String> fundNames = new HashMap<>();
    public int fundMilestone = 0; // сколько вех (по 25%) объявлено

    public Storage(ProgressPlugin plugin) {
        this.plugin = plugin;
        this.dir = plugin.getDataFolder();
        loadAll();
    }

    private File f(String name) { return new File(dir, "data/" + name); }

    public void loadAll() {
        loadAch(); loadSeason(); loadKeys(); loadMentors(); loadQuarter(); loadFund();
    }

    public void saveAll() {
        saveAch(); saveSeason(); saveKeys(); saveMentors(); saveQuarter(); saveFund();
    }

    // ================= достижения =================
    private void loadAch() {
        File file = f("ach.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection prog = y.getConfigurationSection("progress");
        if (prog != null) for (String k : prog.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                Map<String, Long> m = new HashMap<>();
                ConfigurationSection ps = prog.getConfigurationSection(k);
                if (ps != null) for (String a : ps.getKeys(false)) m.put(a, ps.getLong(a));
                achProgress.put(id, m);
            } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection done = y.getConfigurationSection("done");
        if (done != null) for (String k : done.getKeys(false)) {
            try { achDone.put(UUID.fromString(k), new ArrayList<>(done.getStringList(k))); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    public void saveAch() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> e : achProgress.entrySet()) {
            for (Map.Entry<String, Long> p : e.getValue().entrySet())
                y.set("progress." + e.getKey() + "." + p.getKey(), p.getValue());
        }
        for (Map.Entry<UUID, List<String>> e : achDone.entrySet())
            y.set("done." + e.getKey(), e.getValue());
        save(y, f("ach.yml"));
    }

    // ================= сезон =================
    private void loadSeason() {
        File file = f("season.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        seasonId = y.getString("seasonId", "S1");
        seasonStart = y.getLong("seasonStart", 0L);
        ConfigurationSection dt = y.getConfigurationSection("donate");
        if (dt != null) for (String k : dt.getKeys(false)) {
            try { passDonate.put(UUID.fromString(k), dt.getBoolean(k)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection cl = y.getConfigurationSection("claimed");
        if (cl != null) for (String k : cl.getKeys(false)) {
            try { passClaimed.put(UUID.fromString(k), new ArrayList<>(cl.getStringList(k))); }
            catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection ps = y.getConfigurationSection("start");
        if (ps != null) for (String k : ps.getKeys(false)) {
            try { passStart.put(UUID.fromString(k), ps.getLong(k)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection pc = y.getConfigurationSection("chrono");
        if (pc != null) for (String k : pc.getKeys(false)) {
            try { passChrono.put(UUID.fromString(k), pc.getInt(k)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection wk = y.getConfigurationSection("weeklyKey");
        if (wk != null) for (String k : wk.getKeys(false)) {
            try { weeklyKeyDay.put(UUID.fromString(k), wk.getLong(k)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection psc = y.getConfigurationSection("prevSeasonCosmetic");
        if (psc != null) for (String k : psc.getKeys(false)) {
            try { prevSeasonCosmetic.put(UUID.fromString(k), psc.getString(k)); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    public void saveSeason() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("seasonId", seasonId);
        y.set("seasonStart", seasonStart);
        for (Map.Entry<UUID, Boolean> e : passDonate.entrySet()) y.set("donate." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, List<String>> e : passClaimed.entrySet()) y.set("claimed." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, Long> e : passStart.entrySet()) y.set("start." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, Integer> e : passChrono.entrySet()) y.set("chrono." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, Long> e : weeklyKeyDay.entrySet()) y.set("weeklyKey." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, String> e : prevSeasonCosmetic.entrySet()) y.set("prevSeasonCosmetic." + e.getKey(), e.getValue());
        save(y, f("season.yml"));
    }

    // ================= ключи/косметика =================
    private void loadKeys() {
        File file = f("keys.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection k = y.getConfigurationSection("keys");
        if (k != null) for (String s : k.getKeys(false)) {
            try { keys.put(UUID.fromString(s), k.getInt(s)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection o = y.getConfigurationSection("owned");
        if (o != null) for (String s : o.getKeys(false)) {
            try { cosmeticsOwned.put(UUID.fromString(s), new ArrayList<>(o.getStringList(s))); }
            catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection a = y.getConfigurationSection("active");
        if (a != null) for (String s : a.getKeys(false)) {
            try { activeCosmetic.put(UUID.fromString(s), a.getString(s)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection h = y.getConfigurationSection("hoursAtLastKey");
        if (h != null) for (String s : h.getKeys(false)) {
            try { hoursAtLastKey.put(UUID.fromString(s), h.getLong(s)); } catch (IllegalArgumentException ignored) { }
        }
    }

    public void saveKeys() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> e : keys.entrySet()) y.set("keys." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, List<String>> e : cosmeticsOwned.entrySet()) y.set("owned." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, String> e : activeCosmetic.entrySet()) y.set("active." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, Long> e : hoursAtLastKey.entrySet()) y.set("hoursAtLastKey." + e.getKey(), e.getValue());
        save(y, f("keys.yml"));
    }

    // ================= менторы =================
    private void loadMentors() {
        File file = f("mentors.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection c = y.getConfigurationSection("codes");
        if (c != null) for (String s : c.getKeys(false)) {
            try { codes.put(s, UUID.fromString(c.getString(s))); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection st = y.getConfigurationSection("students");
        if (st != null) for (String s : st.getKeys(false)) {
            try {
                List<UUID> list = new ArrayList<>();
                for (String u : st.getStringList(s)) list.add(UUID.fromString(u));
                students.put(UUID.fromString(s), list);
            } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection mo = y.getConfigurationSection("mentorOf");
        if (mo != null) for (String s : mo.getKeys(false)) {
            try { mentorOf.put(UUID.fromString(s), UUID.fromString(mo.getString(s))); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    public void saveMentors() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, UUID> e : codes.entrySet()) y.set("codes." + e.getKey(), e.getValue().toString());
        for (Map.Entry<UUID, List<UUID>> e : students.entrySet()) {
            y.set("students." + e.getKey(), e.getValue().stream().map(UUID::toString).toList());
        }
        for (Map.Entry<UUID, UUID> e : mentorOf.entrySet()) y.set("mentorOf." + e.getKey(), e.getValue().toString());
        save(y, f("mentors.yml"));
    }

    // ================= квартал =================
    private void loadQuarter() {
        File file = f("quarter.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection p = y.getConfigurationSection("owners");
        if (p != null) for (String s : p.getKeys(false)) {
            try {
                plotOwners.put(Integer.parseInt(s), UUID.fromString(p.getString(s)));
            } catch (Exception ignored) { }
        }
    }

    public void saveQuarter() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<Integer, UUID> e : plotOwners.entrySet())
            y.set("owners." + e.getKey(), e.getValue().toString());
        save(y, f("quarter.yml"));
    }

    // ================= общий сбор =================
    private void loadFund() {
        File file = f("fund.yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        fundActive = y.getBoolean("active", false);
        fundTarget = y.getDouble("target", 0);
        fundCollected = y.getDouble("collected", 0);
        fundEndsAt = y.getLong("endsAt", 0);
        fundMilestone = y.getInt("milestone", 0);
        ConfigurationSection c = y.getConfigurationSection("contrib");
        if (c != null) for (String s : c.getKeys(false)) {
            try {
                fundContrib.put(UUID.fromString(s), c.getDouble(s));
                fundNames.put(UUID.fromString(s), c.getString(s + "#name", "?"));
            } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection n = y.getConfigurationSection("names");
        if (n != null) for (String s : n.getKeys(false)) {
            try { fundNames.put(UUID.fromString(s), n.getString(s, "?")); } catch (IllegalArgumentException ignored) { }
        }
    }

    public void saveFund() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("active", fundActive);
        y.set("target", fundTarget);
        y.set("collected", fundCollected);
        y.set("endsAt", fundEndsAt);
        y.set("milestone", fundMilestone);
        for (Map.Entry<UUID, Double> e : fundContrib.entrySet()) y.set("contrib." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, String> e : fundNames.entrySet()) y.set("names." + e.getKey(), e.getValue());
        save(y, f("fund.yml"));
    }

    private void save(YamlConfiguration y, File file) {
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("save " + file.getName() + ": " + ex.getMessage());
        }
    }
}
