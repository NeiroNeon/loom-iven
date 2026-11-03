package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * B2/§3 — «Общий сбор»: событие с целевой суммой и вкладами игроков.
 *
 *  /сбор start <цель> [часов]  — старт (админ)
 *  /сбор вклад <сумма>         — сделать вклад
 *  /сбор                       — статус: цель, собрано, %, время, топ-5
 *  /сбор топ                   — лидерборд вкладчиков
 *  /сбор cancel                — отмена с полным возвратом (админ)
 *
 * Выплата по завершении (§3, редакция «Безубыточный участник»):
 *   фонд выплат = собранное × fund.prize-rate (по умолчанию 120%);
 *     — 20% сверх собранного добавляет казна квартала: софинансирование,
 *       субсидии и коммерческая маржа проекта;
 *   базовая выплата игрока = фонд × (вклад / сумма вкладов);
 *   финальная выплата зажимается в [105% .. 150%] от СОБСТВЕННОГО вклада игрока:
 *     — каждый получает НЕ МЕНЬШЕ 105% своего вклада — правило «Безубыточного
 *       участника»: вклад вернулся с гарантированной прибылью;
 *     — и НЕ БОЛЬШЕ 150% — защита казны квартала от перекоса;
 *   если зажим поднял выплаты выше фонда — доплату покрывает казна квартала.
 *
 * Недобор: к дедлайну собрано <50% цели (fund.min-success-rate) — полный
 * возврат вкладов (100%): участник ничего не теряет.
 */
public final class FundManager implements Listener {

    private final ProgressPlugin plugin;
    private BossBar bar;

    public FundManager(ProgressPlugin plugin) {
        this.plugin = plugin;
        var st = plugin.storage();
        if (st.fundActive) rebuildBar();
    }

    private void rebuildBar() {
        removeBar();
        bar = Bukkit.createBossBar(title(), BarColor.YELLOW, BarStyle.SEGMENTED_10);
        bar.setProgress(progress());
        for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
    }

    private void removeBar() {
        if (bar != null) { bar.removeAll(); bar = null; }
    }

    private String title() {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        long hours = Math.max(0, (st.fundEndsAt - System.currentTimeMillis()) / 3600_000L);
        String ru = "§6Сбор: §e" + fmt(st.fundCollected) + " §6/ " + fmt(st.fundTarget)
                + " §7(" + pct() + "%) §8— " + hours + " ч";
        return ru;
    }

    public static String fmt(double v) {
        var core = LoomCorePlugin.get();
        return core != null && core.api() != null
                ? core.api().fmt(v)
                : String.valueOf((long) v);
    }

    public int pct() {
        var st = plugin.storage();
        if (st.fundTarget <= 0) return 0;
        return (int) Math.min(100, Math.round(st.fundCollected / st.fundTarget * 100.0));
    }

    public double progress() {
        var st = plugin.storage();
        if (st.fundTarget <= 0) return 0;
        return Math.min(1.0, st.fundCollected / st.fundTarget);
    }

    // ---------------- команды ----------------

    public boolean start(UUID initiator, String name, double target, long hours) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        if (st.fundActive) {
            // защита: поверх активного сбора нельзя стартовать новый — вклады бы пропали
            if (initiator != null) {
                core.lang().send(Bukkit.getPlayer(initiator), "fund.already-active");
            }
            return false;
        }
        st.fundActive = true;
        st.fundTarget = target;
        st.fundCollected = 0;
        st.fundEndsAt = System.currentTimeMillis() + hours * 3600_000L;
        st.fundContrib.clear();
        st.fundNames.clear();
        st.fundMilestone = 0;
        st.saveFund();
        core.lang().broadcast("fund.started",
                "%player%", name, "%target%", fmt(target), "%hours%", String.valueOf(hours));
        rebuildBar();
        return true;
    }

    // ---------------- админ-операции (GUI) ----------------

    /** Продлить дедлайн на N часов. */
    public void adminExtend(long hours) {
        var st = plugin.storage();
        if (!st.fundActive) return;
        st.fundEndsAt += hours * 3600_000L;
        st.saveFund();
        if (bar != null) bar.setTitle(title());
    }

    /** Сменить цель на лету. */
    public void adminSetTarget(double target) {
        var st = plugin.storage();
        if (!st.fundActive || target <= 0) return;
        st.fundTarget = target;
        st.fundMilestone = 0;
        st.saveFund();
        if (bar != null) bar.setTitle(title());
    }

    /** Вернуть вклад конкретному игроку (и убрать его из сбора). */
    public void adminRefundOne(UUID id) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        if (!st.fundActive) return;
        Double c = st.fundContrib.remove(id);
        if (c == null) return;
        String n = st.fundNames.getOrDefault(id, "?");
        st.fundNames.remove(id);
        st.saveFund();
        core.api().add(id, n, c);
        Player p = Bukkit.getPlayer(id);
        if (p != null) core.lang().send(p, "fund.refunded-one", "%amount%", fmt(c));
        if (bar != null) bar.setTitle(title());
    }

    public String contribute(UUID id, String name, double amount) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        if (!st.fundActive) return core.msg(id, "fund.not-active");
        double min = plugin.getConfig().getDouble("fund.min-contribution", 50);
        if (amount < min) return core.msg(id, "fund.too-small", "%min%", fmt(min));
        if (!core.api().has(id, amount)) return core.msg(id, "auction.no-money");
        core.api().withdraw(id, name, amount);
        st.fundContrib.merge(id, amount, Double::sum);
        st.fundNames.put(id, name);
        st.saveFund();
        if (bar != null) bar.setTitle(title());
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            core.lang().send(p, "fund.contributed", "%amount%", fmt(amount), "%pct%", String.valueOf(pct()));
            p.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§6Сбор: §e" + fmt(st.fundCollected) + " / " + fmt(st.fundTarget) + " §7(" + pct() + "%)"));
        }
        checkMilestones();
        if (st.fundCollected >= st.fundTarget) {
            Bukkit.getScheduler().runTask(plugin, this::finish);
        }
        return null;
    }

    public String cancel(UUID id, String name) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        if (!st.fundActive) return core.msg(id, "fund.not-active");
        // полный возврат
        for (Map.Entry<UUID, Double> e : st.fundContrib.entrySet()) {
            core.api().add(e.getKey(), st.fundNames.getOrDefault(e.getKey(), "?"), e.getValue());
        }
        core.lang().broadcast("fund.cancelled", "%player%", name,
                "%amount%", fmt(st.fundCollected));
        reset();
        return null;
    }

    private void reset() {
        var st = plugin.storage();
        st.fundActive = false;
        st.fundTarget = 0;
        st.fundCollected = 0;
        st.fundEndsAt = 0;
        st.fundContrib.clear();
        st.fundNames.clear();
        st.fundMilestone = 0;
        st.saveFund();
        removeBar();
    }

    private void checkMilestones() {
        var st = plugin.storage();
        int m = (int) (progress() * 4); // 0..4 по четвертям
        if (m > st.fundMilestone) {
            st.fundMilestone = m;
            var core = LoomCorePlugin.get();
            core.lang().broadcast("fund.milestone", "%pct%", String.valueOf(m * 25),
                    "%amount%", fmt(st.fundCollected), "%target%", fmt(st.fundTarget));
        }
    }

    // ---------------- планировщик ----------------

    public void tick() {
        var st = plugin.storage();
        if (!st.fundActive) return;
        if (bar != null) bar.setTitle(title());
        long left = st.fundEndsAt - System.currentTimeMillis();
        if (left <= 0) {
            if (st.fundCollected >= st.fundTarget * plugin.getConfig().getDouble("fund.min-success-rate", 0.5)) {
                finish();
            } else {
                refundAll();
            }
        }
    }

    /** Сбор не удался — возврат всех вкладов. */
    private void refundAll() {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        for (Map.Entry<UUID, Double> e : st.fundContrib.entrySet()) {
            core.api().add(e.getKey(), st.fundNames.getOrDefault(e.getKey(), "?"), e.getValue());
        }
        core.lang().broadcast("fund.failed", "%amount%", fmt(st.fundCollected));
        reset();
    }

    /** Успешное завершение: фонд 120% собранного, выплаты зажаты в 105–150% вклада. */
    public void finish() {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        if (!st.fundActive || st.fundContrib.isEmpty()) { reset(); return; }

        double rate = plugin.getConfig().getDouble("fund.prize-rate", 1.20);
        double minShare = plugin.getConfig().getDouble("fund.payout-min", 1.05);
        double maxShare = plugin.getConfig().getDouble("fund.payout-max", 1.50);

        double pool = st.fundCollected * rate;          // фонд выплат — 120% собранного
        double total = st.fundContrib.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) { reset(); return; }

        // сортировка для лидерборда
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(st.fundContrib.entrySet());
        sorted.sort(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()));

        double paid = 0;
        core.lang().broadcast("fund.done", "%amount%", fmt(st.fundCollected));
        int pos = 1;
        for (Map.Entry<UUID, Double> e : sorted) {
            UUID id = e.getKey();
            double contrib = e.getValue();
            double payout = pool * (contrib / total);     // базовая выплата по доле
            double lo = contrib * minShare;               // не меньше 105% вклада (безубыточность)
            double hi = contrib * maxShare;               // не больше 150% вклада (защита казны)
            payout = Math.max(lo, Math.min(hi, payout));
            paid += payout;
            core.api().add(id, st.fundNames.getOrDefault(id, "?"), payout);
            String name = st.fundNames.getOrDefault(id, "?");
            core.lang().broadcast("fund.line", "%pos%", String.valueOf(pos),
                    "%player%", name, "%contrib%", fmt(contrib), "%payout%", fmt(payout));
            Player p = Bukkit.getPlayer(id);
            if (p != null) core.lang().send(p, "fund.paid", "%amount%", fmt(payout));
            pos++;
        }
        double treasury = pool - paid;
        if (treasury >= 0) {
            core.lang().broadcast("fund.summary",
                    "%pool%", fmt(pool), "%paid%", fmt(paid), "%treasury%", fmt(treasury));
        } else {
            // зажим 105% поднял выплаты выше фонда — доплату покрыла казна квартала
            core.lang().broadcast("fund.summary-deficit",
                    "%pool%", fmt(pool), "%paid%", fmt(paid), "%gap%", fmt(-treasury));
        }
        reset();
    }

    // ---------------- лидерборд ----------------

    public Map<UUID, Double> top(int limit) {
        Map<UUID, Double> out = new LinkedHashMap<>();
        plugin.storage().fundContrib.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    // ---------------- события ----------------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var st = plugin.storage();
        if (!st.fundActive) return;
        if (bar == null) rebuildBar();
        bar.addPlayer(e.getPlayer());
    }

    private String name(UUID id) {
        String n = plugin.storage().fundNames.get(id);
        if (n != null) return n;
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }
}
