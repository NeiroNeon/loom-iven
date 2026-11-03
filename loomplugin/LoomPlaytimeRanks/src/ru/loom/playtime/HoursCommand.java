package ru.loom.playtime;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.util.Map;
import java.util.UUID;

/** /часы (hours) — свои часы, прогресс до следующего ранга, топ сервера. */
public final class HoursCommand implements CommandExecutor {

    private final PlaytimePlugin plugin;

    public HoursCommand(PlaytimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (!(s instanceof Player p)) {
            // консоль: /hours top|топ
            if (args.length >= 1 && (args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("топ"))) top(s);
            return true;
        }
        UUID id = p.getUniqueId();
        var core = LoomCorePlugin.get();
        long mins = plugin.minutesOf(id);
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            top(p);
            return true;
        }
        String lang = core != null ? core.api().lang(id) : "ru";
        if (core == null) return true;
        String curKey = core.api().ladderRank(id);
        String cur = core.lang().raw(lang, "ladder." + curKey);
        String cos = core.api().cosmeticRank(id);
        String cosName = core.lang().raw(lang, "cosmetic." + cos);

        long[] nextMins = nextThreshold(mins);
        long hours = mins / 60;
        p.sendMessage(core.lang().raw(lang, "hours.self",
                "%hours%", String.valueOf(hours),
                "%rank%", cur, "%cosmetic%", cosName,
                "%next_hours%", nextMins == null ? "—" : String.valueOf(nextMins[0] / 60),
                "%left%", nextMins == null ? "—" : String.valueOf(Math.max(0, (nextMins[0] - mins) / 60))));
        return true;
    }

    /** Следующий порог лестницы (в минутах) или null, если всё пройдено. */
    private long[] nextThreshold(long mins) {
        long[][] thresholds = {{24 * 60}, {120 * 60}, {360 * 60}};
        for (long[] t : thresholds) if (mins < t[0]) return t;
        return null;
    }

    private void top(CommandSender s) {
        var core = LoomCorePlugin.get();
        s.sendMessage(core != null ? core.lang().raw("ru", "hours.top.header") : "§6Топ по часам:");
        int pos = 1;
        for (Map.Entry<UUID, Long> e : plugin.top(10).entrySet()) {
            s.sendMessage("§e" + pos + ". §f" + plugin.name(e.getKey()) + " §7— §6" + (e.getValue() / 60) + " ч");
            pos++;
        }
    }
}
