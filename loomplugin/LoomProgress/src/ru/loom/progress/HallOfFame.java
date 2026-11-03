package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import ru.loom.core.LoomCorePlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Зал славы (v1.4): топы по часам, монетам, достижениям и менторству.
 * Часы берутся из LoomPlaytimeRanks (включая офлайн-игроков).
 */
public final class HallOfFame {

    private final ProgressPlugin plugin;

    public HallOfFame(ProgressPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Long> topHours(int n) {
        var pt = ru.loom.playtime.PlaytimePlugin.get();
        Map<UUID, Long> out = new LinkedHashMap<>();
        if (pt != null) out.putAll(pt.top(n));
        return out;
    }

    public Map<UUID, Double> topCoins(int n) {
        var core = LoomCorePlugin.get();
        return core.economy().top(n);
    }

    public void send(UUID viewerId, java.util.function.Consumer<String> out) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(viewerId);
        out.accept(core.lang().raw(lang, "fame.header"));

        out.accept(core.lang().raw(lang, "fame.hours"));
        int pos = 1;
        for (Map.Entry<UUID, Long> e : topHours(5).entrySet()) {
            out.accept(core.lang().raw(lang, "fame.line",
                    "%pos%", String.valueOf(pos), "%player%", nameOf(e.getKey()),
                    "%value%", (e.getValue() / 60) + " ч"));
            pos++;
        }
        out.accept(core.lang().raw(lang, "fame.coins"));
        pos = 1;
        for (Map.Entry<UUID, Double> e : topCoins(5).entrySet()) {
            out.accept(core.lang().raw(lang, "fame.line",
                    "%pos%", String.valueOf(pos), "%player%", nameOf(e.getKey()),
                    "%value%", LoomCorePlugin.get().api().fmt(e.getValue()) + " монет"));
            pos++;
        }
        out.accept(core.lang().raw(lang, "fame.ach"));
        pos = 1;
        // топ по достижениям среди офлайн+онлайн: пробегаем базу прогресса
        var all = plugin.storage().achDone.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(5).toList();
        for (var e : all) {
            out.accept(core.lang().raw(lang, "fame.line",
                    "%pos%", String.valueOf(pos), "%player%", nameOf(e.getKey()),
                    "%value%", e.getValue().size() + " ачивок"));
            pos++;
        }
        out.accept(core.lang().raw(lang, "fame.mentors"));
        pos = 1;
        var mentors = plugin.storage().students.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(5).toList();
        for (var e : mentors) {
            out.accept(core.lang().raw(lang, "fame.line",
                    "%pos%", String.valueOf(pos), "%player%", nameOf(e.getKey()),
                    "%value%", e.getValue().size() + " учеников"));
            pos++;
        }
    }

    private String nameOf(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        String n = op.getName();
        if (n != null) return n;
        var pt = ru.loom.playtime.PlaytimePlugin.get();
        return pt != null ? pt.name(id) : "???";
    }
}
