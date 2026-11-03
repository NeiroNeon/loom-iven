package ru.loom.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.loom.core.LoomAPI;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /монеты (coins) — баланс и топ. */
public final class CoinsCommand implements CommandExecutor, TabCompleter {

    private final LoomCorePlugin plugin;

    public CoinsCommand(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (!(s instanceof Player p)) {
            if (args.length >= 1) {
                var t = Bukkit.getOfflinePlayer(args[0]);
                s.sendMessage("§e" + args[0] + ": §6" + LoomAPI.fmt(plugin.api().coins(t.getUniqueId())));
            }
            return true;
        }
        if (args.length >= 1) {
            String a0 = args[0].toLowerCase();
            if (a0.equals("топ") || a0.equals("top")) {
                var top = plugin.economy().top(10);
                if (top.isEmpty()) {
                    p.sendMessage(plugin.lang().raw(p.getUniqueId(), "coins.top.empty"));
                    return true;
                }
                p.sendMessage(plugin.lang().raw(p.getUniqueId(), "coins.top.header"));
                int i = 1;
                for (var e : top.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                    p.sendMessage(plugin.lang().raw(p.getUniqueId(), "coins.top.line",
                            "%pos%", String.valueOf(i++), "%name%",
                            name == null ? "—" : name,
                            "%amount%", LoomAPI.fmt(e.getValue())));
                }
                return true;
            }
            var t = Bukkit.getOfflinePlayer(args[0]);
            p.sendMessage(plugin.lang().raw(p.getUniqueId(), "coins.balance.other",
                    "%player%", args[0], "%amount%", LoomAPI.fmt(plugin.api().coins(t.getUniqueId()))));
            return true;
        }
        UUID id = p.getUniqueId();
        String rank = plugin.lang().raw(plugin.api().lang(id), "ladder." + plugin.api().ladderRank(id));
        p.sendMessage(plugin.lang().raw(id, "coins.balance.self",
                "%amount%", LoomAPI.fmt(plugin.api().coins(id)), "%rank%", rank,
                "%hours%", String.valueOf(plugin.api().playtimeMinutes(id) / 60)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if ("топ".startsWith(args[0].toLowerCase()) || "top".startsWith(args[0].toLowerCase())) {
                out.add("топ");
            }
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(pl.getName());
            }
        }
        return out;
    }
}
