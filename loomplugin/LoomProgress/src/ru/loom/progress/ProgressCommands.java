package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Все команды LoomProgress 1.5.0.
 *  /ивент  /ачивки  /пропуск  /ключи  /ментор  /слава  /квартал  /участок  /сбор
 */
public final class ProgressCommands implements CommandExecutor, TabCompleter {

    private final ProgressPlugin plugin;

    public ProgressCommands(ProgressPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Только для игроков (кроме /сбор start).");
            // консоль может запустить/отменить сбор
            if (cmd.getName().equalsIgnoreCase("fund") && args.length >= 2) {
                var st = plugin.storage();
                if (args[0].equalsIgnoreCase("start")) {
                    try {
                        double target = Double.parseDouble(args[1]);
                        long hours = args.length >= 3 ? Long.parseLong(args[2]) : 72;
                        plugin.fund().start(null, "CONSOLE", target, hours);
                        s.sendMessage("§aСбор запущен: цель " + target + ", " + hours + " ч.");
                    } catch (NumberFormatException ex) {
                        s.sendMessage("§c/сбор start <цель> [часов]");
                    }
                } else if (args[0].equalsIgnoreCase("cancel")) {
                    plugin.fund().cancel(null, "CONSOLE");
                }
            }
            return true;
        }
        UUID id = p.getUniqueId();
        var core = LoomCorePlugin.get();

        switch (cmd.getName().toLowerCase()) {
            case "events" -> events(p, id, args);
            case "ach" -> ach(p, id, args);
            case "pass" -> pass(p, id, args);
            case "keys" -> keys(p, id, args);
            case "mentor" -> mentor(p, id, args);
            case "fame" -> fame(p, id);
            case "quarter" -> quarter(p, id, args);
            case "plot" -> plot(p, id);
            case "fund" -> fund(p, id, args);
            default -> { return false; }
        }
        return true;
    }

    // ---------------- ивенты ----------------

    /** GUI-список статуса ивентов (страница «Команды»). */
    public void eventsGui(Player p) {
        events(p, p.getUniqueId(), new String[0]);
    }

    /** GUI-список зала славы. */
    public void fameGui(Player p) {
        fame(p, p.getUniqueId());
    }

    private void events(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        List<String> lines = new ArrayList<>();
        var ev = plugin.events().current();
        if (ev == null) {
            lines.add(core.msg(id, "event.none"));
        } else {
            String group = ev.veterans ? core.msg(id, "event.group.veterans") : core.msg(id, "event.group.newbies");
            boolean mine = plugin.events().activeFor(id);
            long minsLeft = Math.max(0, (ev.endsAt - System.currentTimeMillis()) / 60000L);
            lines.add(core.msg(id, "event.current",
                    "%event%", core.msg(id, "event.name." + ev.kind.name().toLowerCase()),
                    "%group%", group,
                    "%left%", String.valueOf(minsLeft),
                    "%points%", String.valueOf(ev.points.getOrDefault(id, 0L)),
                    "%status%", mine ? "§aваши" : "§7не ваши"));
        }
        lines.add("§8—");
        lines.add(core.msg(id, "event.schedule"));
        ru.loom.core.gui.ListMenu.open(p, "gui.cmd.events-title", lines, null);
    }

    // ---------------- ачивки ----------------

    private void ach(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        var am = plugin.achievements();
        p.sendMessage(core.msg(id, "ach.header",
                "%done%", String.valueOf(am.doneCount(id)), "%total%", String.valueOf(am.total())));
        if (args.length == 0) {
            ProgressPanels.open(p, ProgressPanels.Kind.ACH);
            return;
        }
        String cat = args[0].toLowerCase();
        if (!am.categories().contains(cat)) {
            p.sendMessage(core.msg(id, "ach.no-cat"));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (var a : am.byCategory(cat).values()) {
            boolean done = am.isDone(id, a.id);
            long prog = am.progressOf(id, a.id);
            lines.add((done ? "§a✔ " : "§6") + core.msg(id, "ach.name." + a.id)
                    + " §8— §7" + Math.min(prog, a.goal) + "/" + a.goal
                    + " §8— §6" + core.api().fmt(a.coins) + " монет"
                    + (a.keys > 0 ? " §d+" + a.keys + " ключ" : ""));
        }
        ru.loom.core.gui.ListMenu.open(p, "gui.panel.ach.cat-title", lines, null);
    }

    // ---------------- сезонный пропуск ----------------

    private void pass(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        var sp = plugin.season();
        if (args.length >= 2 && args[0].equalsIgnoreCase("claim")) {
            String err = sp.claim(id, args[1].toLowerCase());
            if (err != null) p.sendMessage(err);
            else p.sendMessage(core.msg(id, "pass.claimed"));
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("buy")) {
            if (sp.buyDonateTrack(id, p.getName())) p.sendMessage(core.msg(id, "pass.bought-you"));
            else p.sendMessage(core.msg(id, "pass.buy-fail"));
            return;
        }
        // v1.8: полный GUI пропуска вместо текстового списка
        PassGUI.open(p);
    }

    // ---------------- ключи и косметика ----------------

    private void keys(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        var km = plugin.keys();
        int n = km.keysOf(id);
        if (args.length == 0) {
            // v1.8: панель косметики в GUI вместо текстового списка
            ProgressPanels.open(p, ProgressPanels.Kind.COS);
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("open")) {
            if (n <= 0) { p.sendMessage(core.msg(id, "keys.none")); return; }
            String won = km.openKey(id);
            if (won != null) p.sendMessage(core.msg(id, "keys.opened"));
            return;
        }
        p.sendMessage(core.msg(id, "keys.balance", "%n%", String.valueOf(n)));
        var owned = km.owned(id);
        p.sendMessage(core.msg(id, "keys.owned", "%n%", String.valueOf(owned.size())));
        for (String c : owned) {
            String active = c.equals(km.active(id)) ? " §a✔ активна" : "";
            p.sendMessage("§d• " + core.msg(id, "cosmetics." + c, "%fallback%", c) + active
                    + " §7— §o/ключи wear " + c);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("wear")) {
            String c = args[1].toLowerCase();
            if (km.owns(id, c)) {
                km.activate(id, c);
                p.sendMessage(core.msg(id, "keys.worn"));
            } else p.sendMessage(core.msg(id, "keys.not-owned"));
        }
    }

    // ---------------- менторы ----------------

    private void mentor(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        var mm = plugin.mentors();
        // подтверждение кода доступно любому игроку (ученик — обычно новичок)
        if (args.length >= 2 && (args[0].equalsIgnoreCase("код") || args[0].equalsIgnoreCase("code"))) {
            String err = mm.confirm(id, p.getName(), args[1]);
            if (err != null) p.sendMessage(err);
            return;
        }
        if (!mm.canMentor(id)) {
            p.sendMessage(core.msg(id, "mentor.need-elder"));
            p.sendMessage(core.msg(id, "mentor.code-hint"));
            return;
        }
        String code = mm.myCode(id);
        p.sendMessage(core.msg(id, "mentor.your-code", "%code%", code));
        p.sendMessage(mm.profileLine(id));
        UUID student = mm.mentorOf(id);
        if (student != null) {
            p.sendMessage(core.msg(id, "mentor.you-student", "%mentor%", nameOf(student)));
        }
        p.sendMessage(core.msg(id, "mentor.use", "%n%", String.valueOf(mm.studentsOf(id).size())));
    }

    // ---------------- зал славы ----------------

    private void fame(Player p, UUID id) {
        List<String> lines = new ArrayList<>();
        plugin.fame().send(id, lines::add);
        ru.loom.core.gui.ListMenu.open(p, "gui.cmd.fame-title", lines, null);
    }

    // ---------------- квартал / участки ----------------

    private void quarter(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        if (args.length >= 1 && args[0].equalsIgnoreCase("build")) {
            if (!p.hasPermission("loom.admin")) { p.sendMessage("§cНужны права лома."); return; }
            String err = plugin.quarter().buildAll(p);
            if (err != null) p.sendMessage(err);
            return;
        }
        p.sendMessage(core.msg(id, "quarter.info",
                "%price%", LoomCorePlugin.get().api().fmt(plugin.getConfig().getDouble("quarter.plot-price", 3000))));
        p.sendMessage(core.msg(id, "quarter.hint"));
    }

    private void plot(Player p, UUID id) {
        String err = plugin.quarter().claim(p);
        if (err != null) p.sendMessage(err);
    }

    // ---------------- общий сбор (B2) ----------------

    private void fund(Player p, UUID id, String[] args) {
        var core = LoomCorePlugin.get();
        var fm = plugin.fund();
        var st = plugin.storage();

        if (args.length >= 1 && args[0].equalsIgnoreCase("start")) {
            if (!p.hasPermission("loom.admin")) { p.sendMessage("§cНужны права лома."); return; }
            if (args.length < 2) { p.sendMessage("§7/сбор start <цель> [часов]"); return; }
            try {
                double target = Double.parseDouble(args[1]);
                long hours = args.length >= 3 ? Long.parseLong(args[2]) : 72;
                fm.start(id, p.getName(), target, hours);
            } catch (NumberFormatException ex) {
                p.sendMessage("§cЦель — число.");
            }
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
            if (!p.hasPermission("loom.admin")) { p.sendMessage("§cНужны права лома."); return; }
            String err = fm.cancel(id, p.getName());
            if (err != null) p.sendMessage(err);
            return;
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("вклад") || args[0].equalsIgnoreCase("contribute"))) {
            if (args.length < 2) { p.sendMessage("§7/сбор вклад <сумма>"); return; }
            try {
                double amount = Double.parseDouble(args[1]);
                String err = fm.contribute(id, p.getName(), amount);
                if (err != null) p.sendMessage(err);
            } catch (NumberFormatException ex) {
                p.sendMessage("§cСумма — число.");
            }
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("топ")) {
            p.sendMessage(core.msg(id, "fund.top-header"));
            int pos = 1;
            for (Map.Entry<UUID, Double> e : fm.top(10).entrySet()) {
                p.sendMessage(core.msg(id, "fund.top-line",
                        "%pos%", String.valueOf(pos),
                        "%player%", nameOf(e.getKey()),
                        "%amount%", LoomCorePlugin.get().api().fmt(e.getValue())));
                pos++;
            }
            return;
        }
        // статус
        if (!st.fundActive) {
            p.sendMessage(core.msg(id, "fund.not-active"));
            return;
        }
        p.sendMessage(core.msg(id, "fund.status",
                "%target%", FundManager.fmt(st.fundTarget),
                "%collected%", FundManager.fmt(st.fundCollected),
                "%pct%", String.valueOf(fm.pct()),
                "%hours%", String.valueOf(Math.max(0, (st.fundEndsAt - System.currentTimeMillis()) / 3600_000L))));
        p.sendMessage(core.msg(id, "fund.my-contrib",
                "%amount%", FundManager.fmt(st.fundContrib.getOrDefault(id, 0.0))));
        p.sendMessage(core.msg(id, "fund.hint"));
    }

    private String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }

    // ---------------- tab-complete ----------------

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = cmd.getName().toLowerCase();
        if (args.length == 1) {
            switch (name) {
                case "fund" -> {
                    for (String v : List.of("вклад", "топ")) {
                        if (v.startsWith(args[0].toLowerCase())) out.add(v);
                    }
                    if (s.hasPermission("loom.admin")) {
                        for (String v : List.of("start", "cancel")) {
                            if (v.startsWith(args[0].toLowerCase())) out.add(v);
                        }
                    }
                }
                case "ach" -> {
                    for (String c : plugin.achievements().categories()) {
                        if (c.startsWith(args[0].toLowerCase())) out.add(c);
                    }
                }
                case "keys" -> {
                    for (String v : List.of("open", "wear")) {
                        if (v.startsWith(args[0].toLowerCase())) out.add(v);
                    }
                }
                case "mentor" -> {
                    if ("код".startsWith(args[0].toLowerCase())) out.add("код");
                }
                case "quarter" -> {
                    if ("build".startsWith(args[0].toLowerCase())) out.add("build");
                }
                default -> { }
            }
        }
        return out;
    }
}
