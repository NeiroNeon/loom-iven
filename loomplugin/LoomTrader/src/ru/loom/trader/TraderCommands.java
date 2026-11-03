package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Команды LoomTrader 1.0.0:
 *  /квесты [сдать <id> | лог | список]   — дейлики Ивлия;
 *  /заказ [сдать <кол-во|все> | топ | start | finish | cancel] — недельный заказ;
 *  /торговец [spawn | remove | реролл | reload] — админ.
 */
final class TraderCommands implements CommandExecutor, TabCompleter {

    private final LoomTraderPlugin plugin;
    private final LoomCorePlugin core;

    TraderCommands(LoomTraderPlugin plugin) {
        this.plugin = plugin;
        this.core = LoomCorePlugin.get();
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (core == null) return true;
        String lang = s instanceof Player p ? core.api().lang(p.getUniqueId()) : "ru";
        UUID id = s instanceof Player p ? p.getUniqueId() : null;

        switch (cmd.getName().toLowerCase()) {
            case "quests" -> quests(s, id, lang, args);
            case "order" -> order(s, id, lang, args);
            case "trader" -> trader(s, args);
        }
        return true;
    }

    // ---------------- /квесты ----------------
    private void quests(CommandSender s, UUID id, String lang, String[] args) {
        if (id == null) { plugin.daily().listBoard(s, "ru"); return; }
        Player p = (Player) s;

        if (args.length == 0) {
            plugin.npc().open(p);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "сдать", "deliver", "handin" -> {
                if (args.length < 2) { core.lang().send(p, "trader.usage"); return; }
                plugin.daily().completeById(p, args[1]);
            }
            case "лог", "log" -> plugin.daily().log(p, id, lang);
            case "список", "list" -> plugin.daily().listBoard(p, lang);
            default -> core.lang().send(p, "trader.usage");
        }
    }

    // ---------------- /заказ ----------------
    private void order(CommandSender s, UUID id, String lang, String[] args) {
        var om = plugin.order();
        if (args.length == 0) { om.status(s); return; }
        switch (args[0].toLowerCase()) {
            case "сдать", "deliver", "contribute" -> {
                if (id == null) { s.sendMessage("Только для игроков."); return; }
                if (args.length < 2) { core.lang().send(s, "order.hint"); return; }
                long n;
                if (args[1].equalsIgnoreCase("все") || args[1].equalsIgnoreCase("all")) n = 0;
                else try { n = Long.parseLong(args[1].replace(" ", "").replace("_", "")); }
                catch (NumberFormatException ex) { core.lang().send(s, "order.hint"); return; }
                om.contribute((Player) s, n);
            }
            case "топ", "top" -> om.top(s);
            case "start", "запустить" -> {
                if (!s.hasPermission("loom.admin")) { core.lang().send(s, "trader.no-perm"); return; }
                if (args.length < 4) {
                    s.sendMessage("§c/заказ start <предмет> <цель> <фонд> [дней]");
                    return;
                }
                try {
                    var mat = org.bukkit.Material.matchMaterial(args[1].toUpperCase());
                    if (mat == null) { s.sendMessage("§cНет такого предмета: " + args[1]); return; }
                    long goal = Long.parseLong(args[2].replace(" ", "").replace("_", ""));
                    long fund = Long.parseLong(args[3].replace(" ", "").replace("_", ""));
                    long days = args.length >= 5 ? Long.parseLong(args[4]) : 7;
                    long ends = System.currentTimeMillis() + days * 86_400_000L;
                    om.start(s.getName(), args[1], goal, fund, ends, false);
                    core.lang().send(s, "order.admin-started",
                            "%goal%", TraderUtil.fmt(goal),
                            "%item%", TraderUtil.itemName("ru", mat),
                            "%fund%", TraderUtil.fmt(fund),
                            "%days%", String.valueOf(days));
                } catch (NumberFormatException ex) {
                    s.sendMessage("§c/заказ start <предмет> <цель> <фонд> [дней]");
                }
            }
            case "finish", "финиш" -> {
                if (!s.hasPermission("loom.admin")) { core.lang().send(s, "trader.no-perm"); return; }
                if (!plugin.storage().orderActive) { core.lang().send(s, "order.none"); return; }
                om.finish(true);
                core.lang().send(s, "order.admin-finished");
            }
            case "cancel", "отмена" -> {
                if (!s.hasPermission("loom.admin")) { core.lang().send(s, "trader.no-perm"); return; }
                if (!plugin.storage().orderActive) { core.lang().send(s, "order.none"); return; }
                om.cancel(s.getName());
            }
            default -> core.lang().send(s, "order.hint");
        }
    }

    // ---------------- /торговец (админ) ----------------
    private void trader(CommandSender s, String[] args) {
        if (!s.hasPermission("loom.admin")) { core.lang().send(s, "trader.no-perm"); return; }
        String sub = args.length == 0 ? "info" : args[0].toLowerCase();
        switch (sub) {
            case "spawn", "поставить" -> {
                Location loc;
                if (s instanceof Player p && args.length < 4) {
                    loc = p.getLocation();
                } else if (args.length >= 4) {
                    World w = Bukkit.getWorld(args.length >= 5 ? args[4] : "world");
                    if (w == null) { s.sendMessage("§cМир не найден: " + (args.length >= 5 ? args[4] : "world")); return; }
                    try {
                        loc = new Location(w, Double.parseDouble(args[1]), Double.parseDouble(args[2]),
                                Double.parseDouble(args[3]), args.length >= 6 ? Float.parseFloat(args[5]) : 0f, 0f);
                    } catch (NumberFormatException ex) {
                        s.sendMessage("§c/торговец spawn [x y z [yaw] [мир]]");
                        return;
                    }
                } else {
                    s.sendMessage("§cИз консоли: /торговец spawn <x> <y> <z> [yaw] [мир]");
                    return;
                }
                plugin.npc().remove();
                plugin.npc().spawn(loc);
                core.lang().send(s, "trader.npc-spawned",
                        "%world%", loc.getWorld().getName(),
                        "%x%", String.valueOf(loc.getBlockX()),
                        "%y%", String.valueOf(loc.getBlockY()),
                        "%z%", String.valueOf(loc.getBlockZ()));
            }
            case "remove", "убрать" -> { plugin.npc().remove(); core.lang().send(s, "trader.npc-removed"); }
            case "реролл", "reroll" -> {
                plugin.daily().regenerate(true);
                core.lang().send(s, "trader.rerolled", "%count%",
                        String.valueOf(plugin.storage().dailyIds.size()));
            }
            case "reload", "перезагрузить" -> {
                plugin.reloadConfig();
                core.lang().send(s, "trader.reloaded");
            }
            case "move", "переместить" -> {
                // v1.8: переместить НПС к себе (remove + spawn на месте админа)
                if (!(s instanceof Player adm)) { s.sendMessage("Только для игроков."); return; }
                plugin.npc().remove();
                plugin.npc().spawn(adm.getLocation());
                core.lang().send(s, "trader.npc-spawned",
                        "%world%", adm.getWorld().getName(),
                        "%x%", String.valueOf(adm.getLocation().getBlockX()),
                        "%y%", String.valueOf(adm.getLocation().getBlockY()),
                        "%z%", String.valueOf(adm.getLocation().getBlockZ()));
            }
            case "reset", "сброс" -> {
                // v1.8: сброс сдач дейликов игроку за сегодня
                if (args.length < 2) { s.sendMessage("§7/торговец reset <ник>"); return; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { s.sendMessage("§cИгрок не в сети."); return; }
                plugin.storage().clearDoneToday(target.getUniqueId());
                s.sendMessage("§aСдачи дейликов сброшены: §f" + target.getName());
            }
            default -> s.sendMessage("§7/торговец spawn | remove | move | reset <ник> | реролл | reload");
        }
    }

    // ---------------- tab-complete ----------------
    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = cmd.getName().toLowerCase();
        if (args.length == 1) {
            if (name.equals("quests")) {
                for (String w : List.of("сдать", "лог", "список"))
                    if (w.startsWith(args[0].toLowerCase())) out.add(w);
            } else if (name.equals("order")) {
                for (String w : List.of("сдать", "топ", "start", "finish", "cancel"))
                    if (w.startsWith(args[0].toLowerCase())) out.add(w);
            } else if (name.equals("trader") && s.hasPermission("loom.admin")) {
                for (String w : List.of("spawn", "remove", "move", "reset", "реролл", "reload"))
                    if (w.startsWith(args[0].toLowerCase())) out.add(w);
            }
        } else if (args.length == 2 && name.equals("quests") && args[0].equalsIgnoreCase("сдать")) {
            for (String id : plugin.storage().dailyIds)
                if (id.startsWith(args[1].toLowerCase())) out.add(id);
        } else if (args.length == 2 && name.equals("order") && args[0].equalsIgnoreCase("сдать")) {
            out.add("все");
        }
        return out;
    }
}
