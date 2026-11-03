package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Дейлики Ивлия: раз в сутки доска получает до 10 заказов из пула (59).
 * Выбор детерминированный (seed = дата), поэтому все игроки видят одну доску.
 * Каждый заказ сдаётся один раз в день на игрока → максимум 10/день.
 * Награда мгновенная: изъятие предметов → монеты (× reward-multiplier).
 */
final class DailyManager {

    private final LoomTraderPlugin plugin;

    DailyManager(LoomTraderPlugin plugin) { this.plugin = plugin; }

    static String todayStr() { return LocalDate.now().toString(); }

    /** Эффективная награда с учётом множителя из конфига. */
    long rewardOf(QuestPool.Quest q) {
        double mult = plugin.getConfig().getDouble("daily.reward-multiplier", 1.0);
        return Math.round(q.reward() * mult);
    }

    /** Доска на сегодня (генерация при смене даты). Возвращает true, если доска обновилась. */
    boolean rollIfNeeded() {
        var st = plugin.storage();
        String today = todayStr();
        if (today.equals(st.dailyDate) && st.dailyIds.size() == plugin.dailyCount()) return false;
        regenerate(false);
        return true;
    }

    /** (Пере)генерация доски: count случайных заказов из пула, seed = дата. */
    void regenerate(boolean announce) {
        var st = plugin.storage();
        int count = plugin.dailyCount();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < QuestPool.POOL.size(); i++) idx.add(i);
        // авто-смена дня — детерминированно от даты (переживает рестарт);
        // ручной реролл админом — по-настоящему случайный (v1.8)
        Random rnd = announce
                ? new Random(System.nanoTime())
                : new Random(todayStr().hashCode() * 1000003L);
        for (int i = idx.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = idx.get(i); idx.set(i, idx.get(j)); idx.set(j, t);
        }
        st.dailyDate = todayStr();
        st.dailyIds.clear();
        for (int i = 0; i < Math.min(count, idx.size()); i++) st.dailyIds.add(QuestPool.POOL.get(idx.get(i)).id());
        st.pruneDone();
        st.saveTrader();
        if (announce) {
            var core = LoomCorePlugin.get();
            if (core != null) core.lang().broadcast("trader.board-reset",
                    "%count%", String.valueOf(st.dailyIds.size()));
        }
    }

    List<QuestPool.Quest> today() {
        List<QuestPool.Quest> list = new ArrayList<>();
        for (String id : plugin.storage().dailyIds) {
            QuestPool.Quest q = QuestPool.byId(id);
            if (q != null) list.add(q);
        }
        return list;
    }

    int doneToday(java.util.UUID id) { return plugin.storage().doneToday(id).size(); }

    /** Сдача заказа игроком. Возвращает true, если сдан. */
    boolean complete(Player p, QuestPool.Quest q) {
        var core = LoomCorePlugin.get();
        if (core == null) return false;
        String lang = core.api().lang(p.getUniqueId());
        var st = plugin.storage();

        if (st.doneToday(p.getUniqueId()).contains(q.id())) {
            core.lang().send(p, "trader.daily-already");
            return false;
        }
        String name = TraderUtil.questName(lang, q);
        int have = TraderUtil.count(p, q);
        if (have < q.amount()) {
            core.lang().send(p, "trader.daily-none",
                    "%need%", String.valueOf(q.amount()),
                    "%item%", name,
                    "%have%", String.valueOf(have));
            return false;
        }
        TraderUtil.take(p, q, q.amount());
        long reward = rewardOf(q);
        core.api().add(p.getUniqueId(), p.getName(), reward);

        st.doneToday(p.getUniqueId()).add(q.id());
        int done = doneToday(p.getUniqueId());
        st.saveTrader();

        core.lang().send(p, "trader.daily-done",
                "%quest%", name, "%amount%", String.valueOf(q.amount()),
                "%reward%", TraderUtil.fmt(reward), "%done%", String.valueOf(done));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        if (done >= plugin.dailyCount()) {
            core.lang().send(p, "trader.all-done");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1f);
        }
        return true;
    }

    /** Сдача по id (/квесты сдать <id>). */
    boolean completeById(Player p, String id) {
        QuestPool.Quest q = QuestPool.byId(id);
        if (q == null || !plugin.storage().dailyIds.contains(q.id())) {
            var core = LoomCorePlugin.get();
            if (core != null) core.lang().send(p, "trader.not-on-board");
            return false;
        }
        return complete(p, q);
    }

    /** Текстовое перечисление доски (чат). */
    void listBoard(org.bukkit.command.CommandSender to, String lang) {
        var core = LoomCorePlugin.get();
        if (core == null) return;
        var st = plugin.storage();
        int done = to instanceof Player p ? doneToday(p.getUniqueId()) : 0;
        core.lang().send(to, "trader.board-header",
                "%count%", String.valueOf(st.dailyIds.size()), "%done%", String.valueOf(done));
        for (QuestPool.Quest q : today()) {
            boolean d = to instanceof Player p && st.doneToday(p.getUniqueId()).contains(q.id());
            core.lang().send(to, "trader.quest-line",
                    "%id%", q.id(),
                    "%quest%", TraderUtil.questName(lang, q),
                    "%amount%", String.valueOf(q.amount()),
                    "%reward%", TraderUtil.fmt(rewardOf(q)),
                    "%category%", core.lang().raw(lang, "raw.trader.cat." + q.cat()));
            if (d) to.sendMessage("§a  ✔ уже сдано");
        }
    }

    /** Лог сдач игрока за сегодня. */
    void log(org.bukkit.command.CommandSender to, java.util.UUID id, String lang) {
        var core = LoomCorePlugin.get();
        if (core == null) return;
        var st = plugin.storage();
        var done = st.doneToday(id);
        core.lang().send(to, "trader.log-header", "%done%", String.valueOf(done.size()));
        if (done.isEmpty()) { core.lang().send(to, "trader.log-none"); return; }
        for (QuestPool.Quest q : today()) {
            if (!done.contains(q.id())) continue;
            core.lang().send(to, "trader.log-line",
                    "%quest%", TraderUtil.questName(lang, q),
                    "%amount%", String.valueOf(q.amount()),
                    "%reward%", TraderUtil.fmt(rewardOf(q)));
        }
    }
}
