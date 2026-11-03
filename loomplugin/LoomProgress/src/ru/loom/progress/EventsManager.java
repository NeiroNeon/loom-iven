package ru.loom.progress;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import ru.loom.core.LoomCorePlugin;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ивенты по расписанию с делением новичок / Хранитель+ (v1.4).
 *  Пн — выходной; Вт — двойной дроп (новички); Ср — сбор железа (новички);
 *  Чт — выходной; Пт — большая охота (Хранитель+); Сб — босс-битва (Хранитель+);
 *  Вс — жатва (новички).
 * Награды топ-3 раздельные по группам.
 */
public final class EventsManager {

    public enum Kind { NONE, DOUBLE_DROP, ORE_COLLECT, HARVEST, HUNT, BOSS_BATTLE }

    public static final class ActiveEvent {
        public final Kind kind;
        public final boolean veterans; // true = Хранитель+
        public long endsAt;
        public final Map<UUID, Long> points = new HashMap<>();

        ActiveEvent(Kind k, boolean veterans, long endsAt) {
            this.kind = k; this.veterans = veterans; this.endsAt = endsAt;
        }
    }

    private final ProgressPlugin plugin;
    private ActiveEvent current;

    public EventsManager(ProgressPlugin plugin) {
        this.plugin = plugin;
    }

    public ActiveEvent current() {
        return current;
    }

    /** Проверка расписания каждый тик (минута). */
    public void tick() {
        var core = LoomCorePlugin.get();
        if (current != null && System.currentTimeMillis() >= current.endsAt) {
            finish();
        }
        if (current != null) return;

        if (!plugin.getConfig().getBoolean("events.enabled", true)) return;
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        int startHour = plugin.getConfig().getInt("events.start-hour", 18);
        int durationHours = plugin.getConfig().getInt("events.duration-hours", 3);
        if (now.getHour() != startHour || now.getMinute() != 0) return;

        DayOfWeek d = now.getDayOfWeek();
        switch (d) {
            case TUESDAY -> start(Kind.DOUBLE_DROP, false, durationHours);
            case WEDNESDAY -> start(Kind.ORE_COLLECT, false, durationHours);
            case SUNDAY -> start(Kind.HARVEST, false, durationHours);
            case FRIDAY -> start(Kind.HUNT, true, durationHours);
            case SATURDAY -> start(Kind.BOSS_BATTLE, true, durationHours);
            default -> { }
        }
    }

    private void start(Kind kind, boolean veterans, int hours) {
        current = new ActiveEvent(kind, veterans, System.currentTimeMillis() + hours * 3600_000L);
        var core = LoomCorePlugin.get();
        core.lang().broadcast("event.start." + kind.name().toLowerCase(),
                "%hours%", String.valueOf(hours),
                "%group%", veterans ? core.lang().raw("ru", "event.group.veterans")
                                    : core.lang().raw("ru", "event.group.newbies"));
    }

    public void addPoint(UUID id, long n) {
        if (current == null) return;
        current.points.merge(id, n, Long::sum);
    }

    /** Активен ли ивент для этой группы (учитывая деление новичок/Хранитель+). */
    public boolean activeFor(UUID id) {
        var core = LoomCorePlugin.get();
        if (current == null || core == null) return false;
        boolean veteran = core.api().ladderOrder(id) >= core.ranks().ladderOrder(core.ranks().ladderThresholdMinutes("keeper"));
        return current.veterans == veteran;
    }

    private void finish() {
        var core = LoomCorePlugin.get();
        if (current == null) return;
        var top = current.points.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .toList();
        String groupKey = current.veterans ? "event.group.veterans" : "event.group.newbies";
        core.lang().broadcast("event.finished",
                "%group%", core.lang().raw("ru", groupKey),
                "%event%", core.lang().raw("ru", "event.name." + current.kind.name().toLowerCase()));
        // награды из конфига: списки [500,300,200] (фикс: раньше читались как .1/.2/.3 и игнорировались)
        List<Double> rewards = plugin.getConfig()
                .getDoubleList("events.rewards." + current.kind.name().toLowerCase());
        int pos = 1;
        for (var e : top) {
            double coins = pos <= rewards.size() ? rewards.get(pos - 1) : 500.0 * pos;
            core.api().add(e.getKey(), nameOf(e.getKey()), coins);
            // топ-1 получает косметический ключ
            if (pos == 1) plugin.keys().giveKey(e.getKey(), 1);
            core.lang().broadcast("event.winner",
                    "%pos%", String.valueOf(pos), "%player%", nameOf(e.getKey()),
                    "%points%", String.valueOf(e.getValue()), "%reward%", LoomCorePlugin.get().api().fmt(coins));
            pos++;
        }
        current = null;
    }

    // ---------------- админ-операции (GUI) ----------------

    /** Форс-старт ивента по имени вида (double_drop/ore_collect/harvest/hunt/boss_battle). */
    public boolean forceStart(String kindName, int hours) {
        Kind k;
        try { k = Kind.valueOf(kindName.toUpperCase()); } catch (IllegalArgumentException ex) { return false; }
        boolean veterans = k == Kind.HUNT || k == Kind.BOSS_BATTLE;
        start(k, veterans, hours);
        return true;
    }

    /** Форс-финиш с выплатами топ-3. */
    public void forceFinish() { finish(); }

    /** Имя активного ивента или null. */
    public String currentName() { return current == null ? null : current.kind.name().toLowerCase(); }

    private String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }
}
