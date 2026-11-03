package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Глобальный недельный заказ: весь сервер приносит один предмет на общую цель,
 * призовой фонд (монеты) делится пропорционально доле вклада каждого.
 *
 *  выплата_i = фонд × вклад_i / сумма_вкладов (минимум 1 монета при вкладе > 0);
 *  цель достигнута → заказ завершается сразу; срок вышел без цели → фонд всё равно
 *  делится по долям (усилие не пропадает); отмена админом — без выплат.
 *
 * Ротация: каждый понедельник 00:00 берётся следующий пункт weekly.rotation
 * (конфиг: "МАТЕРИАЛ:цель:фонд"), старт — автоматически.
 */
final class OrderManager {

    private final LoomTraderPlugin plugin;
    private BossBar bar;

    /** Выплата одному игроку. */
    private record Pay(UUID id, String name, long contrib, long payout, double share) { }

    OrderManager(LoomTraderPlugin plugin) {
        this.plugin = plugin;
        var st = plugin.storage();
        if (st.orderActive) rebuildBar();
    }

    // ---------------- жизненный цикл ----------------

    /** Автостарт понедельного заказа из ротации (по номеру ISO-недели). */
    /** Ключ текущей ISO-недели (для защиты от повторного автозапуска). */
    private static String weekKey() {
        return LocalDate.now().getYear() + "W" + LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    void ensureWeekly() {
        var st = plugin.storage();
        if (st.orderActive || !plugin.getConfig().getBoolean("order.auto-weekly", true)) return;
        // неделя уже отыграна (финиш/отмена) — не перезапускаем тот же заказ (v1.8)
        if (weekKey().equals(st.orderLastWeek)) return;
        List<String> rot = plugin.getConfig().getStringList("order.rotation");
        if (rot.isEmpty()) return;
        int week = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        String entry = rot.get((week - 1) % rot.size());
        String[] p = entry.split(":");
        if (p.length < 3) return;
        try {
            Material.valueOf(p[0].toUpperCase());
            long goal = Long.parseLong(p[1].trim());
            long fund = Long.parseLong(p[2].trim());
            // конец недели — ближайший понедельник 00:00
            long ends = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            start("ротация недели", p[0].toUpperCase(), goal, fund, ends, true);
        } catch (Exception ex) {
            plugin.getLogger().warning("Неверный пункт order.rotation: " + entry);
        }
    }

    void start(String by, String matName, long goal, long fund, long endsAt, boolean auto) {
        var st = plugin.storage();
        st.orderActive = true;
        st.orderLastWeek = null;
        st.orderMat = matName.toUpperCase();
        st.orderGoal = Math.max(1, goal);
        st.orderFund = Math.max(1, fund);
        st.orderCollected = 0;
        st.orderEndsAt = endsAt;
        st.orderStartedBy = by;
        st.orderMilestone = 0;
        st.orderContrib.clear();
        st.orderNames.clear();
        st.saveOrder();
        rebuildBar();
        var core = LoomCorePlugin.get();
        if (core != null) {
            core.lang().broadcast("order.started",
                    "%goal%", TraderUtil.fmt(st.orderGoal),
                    "%item%", itemName("ru"),
                    "%fund%", TraderUtil.fmt(st.orderFund));
        }
        plugin.getLogger().info((auto ? "Автостарт" : "Старт") + " недельного заказа: "
                + st.orderGoal + "×" + st.orderMat + ", фонд " + st.orderFund);
    }

    /** tick() каждую минуту: срок вышел → выплаты по долям даже без цели. */
    void tick() {
        var st = plugin.storage();
        if (!st.orderActive) { ensureWeekly(); return; }
        if (System.currentTimeMillis() > st.orderEndsAt) finish(false);
    }

    // ---------------- вклад ----------------

    /** Сдача предметов игроком. count=0 или «все» — сдать всё, что есть. */
    void contribute(Player p, long count) {
        var core = LoomCorePlugin.get();
        var st = plugin.storage();
        if (core == null || !st.orderActive) { if (core != null) core.lang().send(p, "order.none"); return; }

        Material mat = Material.matchMaterial(st.orderMat);
        if (mat == null) return;
        var quest = new QuestPool.Quest("order", "resources", mat, 1, 0, null);
        int have = TraderUtil.count(p, quest);
        long want = count <= 0 ? have : Math.min(count, have);
        if (want <= 0) {
            if (count > 0) {
                String lang = core.api().lang(p.getUniqueId());
                core.lang().send(p, "order.contribute-none",
                        "%need%", TraderUtil.fmt(count),
                        "%item%", TraderUtil.itemName(lang, mat),
                        "%have%", TraderUtil.fmt(have));
            } else {
                core.lang().send(p, "order.contribute-min");
            }
            return;
        }
        long removed = TraderUtil.take(p, quest, (int) Math.min(want, Integer.MAX_VALUE));
        if (removed <= 0) { core.lang().send(p, "order.contribute-min"); return; }

        UUID id = p.getUniqueId();
        long total = st.orderContrib.merge(id, removed, Long::sum);
        st.orderNames.put(id, p.getName());
        st.orderCollected += removed;
        st.saveOrder();
        updateBar();
        checkMilestone();

        double share = st.orderCollected > 0 ? total * 100.0 / st.orderCollected : 0;
        long projected = Math.round(st.orderFund * (double) total / Math.max(1, st.orderCollected));
        core.lang().send(p, "order.contribute-ok",
                "%amount%", TraderUtil.fmt(removed),
                "%item%", itemName(core.api().lang(id)),
                "%total%", TraderUtil.fmt(total),
                "%share%", TraderUtil.share(share));
        if (st.orderCollected >= st.orderGoal) finish(true);
    }

    private void checkMilestone() {
        var st = plugin.storage();
        int pct = pct();
        int[] marks = {25, 50, 75};
        if (st.orderMilestone < marks.length && pct >= marks[st.orderMilestone]) {
            st.orderMilestone++;
            st.saveOrder();
            var core = LoomCorePlugin.get();
            if (core != null) core.lang().broadcast("order.milestone",
                    "%pct%", String.valueOf(marks[st.orderMilestone - 1]),
                    "%amount%", TraderUtil.fmt(st.orderCollected),
                    "%goal%", TraderUtil.fmt(st.orderGoal),
                    "%item%", itemName("ru"));
        }
    }

    // ---------------- завершение и выплаты ----------------

    void finish(boolean goalReached) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        st.orderActive = false;
        st.orderLastWeek = weekKey(); // неделя отыграна — не перезапускать (v1.8)
        st.saveOrder();
        removeBar();
        if (core == null) return;

        // выплаты: floor(фонд × вклад / сумма), минимум 1 монета при вкладе > 0
        long total = st.orderContrib.values().stream().mapToLong(Long::longValue).sum();
        List<Pay> pays = new ArrayList<>();
        if (total > 0) {
            long budget = st.orderFund;
            for (var e : st.orderContrib.entrySet()) {
                long raw = (long) Math.floor(st.orderFund * (double) e.getValue() / total);
                pays.add(new Pay(e.getKey(), st.orderNames.getOrDefault(e.getKey(), "???"),
                        e.getValue(), raw, e.getValue() * 100.0 / total));
            }
            // минимум 1 монета — от большего вклада к меньшему, пока хватает фонда
            pays.sort(Comparator.comparingLong(Pay::contrib).reversed());
            for (int i = 0; i < pays.size(); i++) {
                Pay pay = pays.get(i);
                if (pay.payout() >= 1) continue;
                if (budget - paid(pays) >= 1) {
                    pays.set(i, new Pay(pay.id(), pay.name(), pay.contrib(), 1, pay.share()));
                }
            }
            for (Pay pay : pays) {
                if (pay.payout() > 0) {
                    core.api().add(pay.id(), pay.name(), pay.payout());
                    Player pl = Bukkit.getPlayer(pay.id);
                    if (pl != null) {
                        core.lang().send(pl, "order.payout",
                                "%amount%", TraderUtil.fmt(pay.payout()),
                                "%share%", TraderUtil.share(pay.payout() * 100.0 / st.orderFund),
                                "%contrib%", TraderUtil.fmt(pay.contrib()));
                    }
                }
            }
        }

        core.lang().broadcast(goalReached ? "order.done" : "order.done-failed",
                "%collected%", TraderUtil.fmt(st.orderCollected),
                "%item%", itemName("ru"),
                "%fund%", TraderUtil.fmt(st.orderFund));
        pays.sort(Comparator.comparingLong(Pay::payout).reversed());
        int pos = 1;
        for (Pay pay : pays) {
            if (pos > 5) break;
            core.lang().broadcast("order.top-line",
                    "%pos%", String.valueOf(pos),
                    "%player%", pay.name(),
                    "%amount%", TraderUtil.fmt(pay.contrib),
                    "%item%", itemName("ru"),
                    "%share%", TraderUtil.share(pay.share()));
            pos++;
        }
        long paidSum = paid(pays);
        core.lang().broadcast("order.paid-total",
                "%paid%", TraderUtil.fmt(paidSum), "%pool%", TraderUtil.fmt(st.orderFund));
        st.saveOrder();
    }

    private long paid(List<Pay> pays) {
        return pays.stream().mapToLong(Pay::payout).sum();
    }

    void cancel(String by) {
        var st = plugin.storage();
        st.orderActive = false;
        st.orderLastWeek = weekKey();
        st.saveOrder();
        removeBar();
        var core = LoomCorePlugin.get();
        if (core != null) core.lang().broadcast("order.cancelled", "%player%", by);
    }

    // ---------------- статус и топ ----------------

    int pct() {
        var st = plugin.storage();
        if (st.orderGoal <= 0) return 0;
        return (int) Math.min(100, st.orderCollected * 100 / st.orderGoal);
    }

    void status(org.bukkit.command.CommandSender to) {
        var core = LoomCorePlugin.get();
        var st = plugin.storage();
        if (core == null) return;
        if (!st.orderActive) { core.lang().send(to, "order.none"); return; }
        String lang = to instanceof Player p ? core.api().lang(p.getUniqueId()) : "ru";
        core.lang().send(to, "order.status",
                "%collected%", TraderUtil.fmt(st.orderCollected),
                "%goal%", TraderUtil.fmt(st.orderGoal),
                "%item%", TraderUtil.itemName(lang, Material.matchMaterial(st.orderMat)),
                "%pct%", String.valueOf(pct()),
                "%time%", TraderUtil.timeLeft(st.orderEndsAt - System.currentTimeMillis()));
        if (to instanceof Player p) {
            UUID id = p.getUniqueId();
            long my = st.orderContrib.getOrDefault(id, 0L);
            double share = st.orderCollected > 0 ? my * 100.0 / st.orderCollected : 0;
            long projected = Math.round(st.orderFund * (double) my / Math.max(1, st.orderCollected));
            core.lang().send(to, "order.fund",
                    "%fund%", TraderUtil.fmt(st.orderFund),
                    "%contrib%", TraderUtil.fmt(my),
                    "%share%", TraderUtil.share(share),
                    "%proj%", TraderUtil.fmt(projected));
        }
        core.lang().send(to, "order.hint");
    }

    void top(org.bukkit.command.CommandSender to) {
        var core = LoomCorePlugin.get();
        var st = plugin.storage();
        if (core == null) return;
        if (!st.orderActive || st.orderContrib.isEmpty()) { core.lang().send(to, "order.none"); return; }
        String lang = to instanceof Player p ? core.api().lang(p.getUniqueId()) : "ru";
        core.lang().send(to, "order.top-header");
        List<Map.Entry<UUID, Long>> list = new ArrayList<>(st.orderContrib.entrySet());
        list.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        int pos = 1;
        for (var e : list) {
            if (pos > 10) break;
            core.lang().send(to, "order.top-line",
                    "%pos%", String.valueOf(pos),
                    "%player%", st.orderNames.getOrDefault(e.getKey(), "???"),
                    "%amount%", TraderUtil.fmt(e.getValue()),
                    "%item%", TraderUtil.itemName(lang, Material.matchMaterial(st.orderMat)),
                    "%share%", TraderUtil.share(e.getValue() * 100.0 / Math.max(1, st.orderCollected)));
            pos++;
        }
    }

    // ---------------- boss-bar ----------------

    private String itemName(String lang) {
        Material mat = Material.matchMaterial(plugin.storage().orderMat);
        return mat != null ? TraderUtil.itemName(lang, mat) : plugin.storage().orderMat;
    }

    private void rebuildBar() {
        removeBar();
        var st = plugin.storage();
        if (!st.orderActive) return;
        bar = Bukkit.createBossBar(barTitle(), BarColor.GREEN, BarStyle.SEGMENTED_10);
        bar.setProgress(progress());
        for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
    }

    private void updateBar() {
        if (bar == null) { rebuildBar(); return; }
        bar.setTitle(barTitle());
        bar.setProgress(progress());
    }

    private String barTitle() {
        var st = plugin.storage();
        return "§aЗаказ: §f" + TraderUtil.fmt(st.orderCollected) + " §7/ §f"
                + TraderUtil.fmt(st.orderGoal) + " §a" + itemName("ru") + " §7(" + pct() + "%)";
    }

    private double progress() {
        var st = plugin.storage();
        if (st.orderGoal <= 0) return 0;
        return Math.min(1.0, (double) st.orderCollected / st.orderGoal);
    }

    void attach(Player p) {
        if (bar != null) bar.addPlayer(p);
    }

    private void removeBar() {
        if (bar != null) { bar.removeAll(); bar = null; }
    }
}
