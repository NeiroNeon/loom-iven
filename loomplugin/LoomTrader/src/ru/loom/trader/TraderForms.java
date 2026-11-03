package ru.loom.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.bedrock.BedrockUi;
import ru.loom.core.gui.GuiUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Bedrock-формы (Floodgate/Cumulus) доски Ивлии и недельного заказа — v1.8.
 * Java-игроки получают сундук-GUI (TraderGUI), Bedrock — нативные формы:
 * доска → детали заказа → сдача; недельный заказ → статус → «сдать всё».
 * Ответы форм приходят из потока Geyser — BedrockUi сам возвращает в главный поток.
 */
final class TraderForms {

    private TraderForms() { }

    /** Bedrock-игрок? (иначе вызывающий код открывает сундук-GUI) */
    static boolean isBedrock(Player p) {
        return BedrockUi.isBedrock(p.getUniqueId());
    }

    /** Доска заказов: кнопки заказов + недельный заказ + обновление (закрытие — крестик формы). */
    static void board(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = LoomTraderPlugin.get();
        if (core == null || plugin == null) return;
        String lang = core.api().lang(p.getUniqueId());
        var daily = plugin.daily();
        List<QuestPool.Quest> quests = daily.today();
        var done = plugin.storage().doneToday(p.getUniqueId());

        List<String> buttons = new ArrayList<>();
        for (QuestPool.Quest q : quests) {
            buttons.add(core.lang().raw(lang, "raw.trader.form-btn",
                    "%quest%", TraderUtil.questName(lang, q),
                    "%amount%", String.valueOf(q.amount()),
                    "%reward%", TraderUtil.fmt(daily.rewardOf(q)),
                    "%status%", done.contains(q.id())
                            ? core.lang().raw(lang, "raw.trader.form-btn-done")
                            : core.lang().raw(lang, "raw.trader.form-btn-open")));
        }
        int orderIdx = buttons.size();
        buttons.add(core.lang().raw(lang, "raw.trader.form-btn-order"));
        int refreshIdx = buttons.size();
        buttons.add(core.lang().raw(lang, "raw.trader.form-btn-refresh"));

        String head = core.lang().raw(lang, "raw.trader.form-board-head",
                "%done%", String.valueOf(daily.doneToday(p.getUniqueId())),
                "%count%", String.valueOf(plugin.dailyCount()));

        BedrockUi.buttons(p,
                core.lang().raw(lang, "raw.trader.form-board-title"), head, buttons, idx -> {
                    if (idx == orderIdx) { order(p); return; }
                    if (idx == refreshIdx) { board(p); return; }
                    if (idx >= 0 && idx < quests.size()) detail(p, quests.get(idx));
                });
    }

    /** Детали заказа: нужно/есть/награда/категория + сдача. */
    static void detail(Player p, QuestPool.Quest q) {
        var core = LoomCorePlugin.get();
        var plugin = LoomTraderPlugin.get();
        if (core == null || plugin == null) return;
        String lang = core.api().lang(p.getUniqueId());
        var daily = plugin.daily();
        boolean isDone = plugin.storage().doneToday(p.getUniqueId()).contains(q.id());
        int have = TraderUtil.count(p, q);

        List<String> lore = List.of(
                core.lang().raw(lang, "raw.trader.lore-need",
                        "%amount%", String.valueOf(q.amount()),
                        "%item%", TraderUtil.questName(lang, q)),
                core.lang().raw(lang, "raw.trader.lore-have",
                        "%have%", String.valueOf(have), "%amount%", String.valueOf(q.amount())),
                core.lang().raw(lang, "raw.trader.lore-reward",
                        "%reward%", TraderUtil.fmt(daily.rewardOf(q))),
                core.lang().raw(lang, "raw.trader.lore-category",
                        "%category%", core.lang().raw(lang, "raw.trader.cat." + q.cat())));

        List<String> buttons = new ArrayList<>();
        buttons.add(isDone
                ? core.lang().raw(lang, "raw.trader.lore-done")
                : core.lang().raw(lang, "raw.trader.form-detail-btn",
                        "%have%", String.valueOf(have), "%amount%", String.valueOf(q.amount())));
        buttons.add(core.lang().raw(lang, "raw.trader.form-back"));

        BedrockUi.buttons(p,
                core.lang().raw(lang, "raw.trader.form-detail-title",
                        "%quest%", TraderUtil.questName(lang, q)),
                String.join("\n", lore), buttons, idx -> {
                    if (idx == 0) daily.complete(p, q);
                    board(p);
                });
    }

    /** Недельный заказ: статус + «сдать всё» (count = 0 → всё, что есть). */
    static void order(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = LoomTraderPlugin.get();
        if (core == null || plugin == null) return;
        String lang = core.api().lang(p.getUniqueId());
        var st = plugin.storage();

        String content;
        if (!st.orderActive) {
            content = core.lang().raw(lang, "order.none");
        } else {
            Material mat = Material.matchMaterial(st.orderMat);
            String item = mat != null ? TraderUtil.itemName(lang, mat) : st.orderMat;
            long left = Math.max(0, st.orderEndsAt - System.currentTimeMillis());
            content = core.lang().raw(lang, "raw.trader.form-order-head",
                    "%item%", item,
                    "%collected%", TraderUtil.fmt(st.orderCollected),
                    "%goal%", TraderUtil.fmt(st.orderGoal),
                    "%pct%", plugin.order().pct() + "%",
                    "%fund%", TraderUtil.fmt(st.orderFund),
                    "%time%", GuiUtil.dur(left, lang));
        }

        List<String> buttons = List.of(
                core.lang().raw(lang, "raw.trader.form-order-btn-all"),
                core.lang().raw(lang, "raw.trader.form-back"));

        BedrockUi.buttons(p, core.lang().raw(lang, "raw.trader.form-order-title"), content, buttons, idx -> {
            if (idx == 0) plugin.order().contribute(p, 0);
            board(p);
        });
    }
}
