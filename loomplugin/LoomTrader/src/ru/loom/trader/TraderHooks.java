package ru.loom.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.menu.MenuAction;
import ru.loom.core.stats.StatsSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Интеграция LoomTrader с ядром: пункт меню «Дейлики Ивлия», секция
 * статистики профиля (дейлики и недельный заказ), переменные HUD
 * %quests% и %order%.
 */
public final class TraderHooks {

    private TraderHooks() { }

    public static void register(LoomTraderPlugin plugin) {
        var core = LoomCorePlugin.get();
        if (core == null) return;

        core.menus().register(new MenuAction() {
            @Override public String id() { return "quests"; }
            @Override public int order() { return 45; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.WHEAT; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.menu.item.quests"); }
            @Override public List<String> lore(Player p, String lang) {
                int done = plugin.storage().doneToday(p.getUniqueId()).size();
                String extra = "\n§8Сегодня сдано: §7" + done + "/" + plugin.dailyCount();
                List<String> out = new ArrayList<>(List.of(GuiUtil.t(p, lang, "gui.menu.item.quests-lore")));
                out.add(extra);
                return out;
            }
            @Override public void click(Player p) { new TraderGUI(p.getUniqueId()).open(p); }
        });

        core.stats().register("trader", new StatsSection() {
            @Override public String title(Player p, String lang) {
                return core.lang().raw(lang, "gui.stats.trader-title");
            }
            @Override public List<String> lines(Player p, String lang) {
                List<String> out = new ArrayList<>();
                int done = plugin.storage().doneToday(p.getUniqueId()).size();
                out.add(core.lang().raw(lang, "gui.stats.quests", "%done%", String.valueOf(done)));
                var st = plugin.storage();
                if (st.orderActive) {
                    long mine = st.orderContrib.getOrDefault(p.getUniqueId(), 0L);
                    String itemKey = "raw.item.name." + st.orderMat;
                    String item = core.lang().file("ru").contains(itemKey)
                            ? core.lang().raw("ru", itemKey)
                            : st.orderMat;
                    out.add(core.lang().raw(lang, "gui.stats.order",
                            "%item%", item,
                            "%contrib%", TraderUtil.fmt(mine),
                            "%share%", String.valueOf(st.orderCollected > 0
                                    ? Math.round(mine * 100.0 / st.orderCollected) : 0)));
                }
                return out;
            }
        });

        // ---- страницы «Команды»: торговля ----
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.quests"; }
            @Override public int order() { return 20; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.WHEAT; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.quests"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.quests-lore"));
            }
            @Override public void click(Player p) { new TraderGUI(p.getUniqueId()).open(p); }
            @Override public String category() { return "trade"; }
            @Override public String usage() { return "/квесты"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.order"; }
            @Override public int order() { return 21; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.CHEST; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.order"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.order-lore"));
            }
            @Override public void click(Player p) {
                var lines = new ArrayList<String>();
                var st = plugin.storage();
                if (st.orderActive) {
                    lines.add("§7Заказ: §f" + st.orderMat + " §8— §7собрано §f"
                            + TraderUtil.fmt(st.orderCollected) + "/" + TraderUtil.fmt(st.orderGoal));
                    lines.add("§7Фонд: §6" + TraderUtil.fmt(st.orderFund) + " монет");
                    lines.add("§7Ваш вклад: §f" + TraderUtil.fmt(st.orderContrib.getOrDefault(p.getUniqueId(), 0L)));
                    lines.add("§8Сдача: /заказ сдать <кол-во|все> или GUI квестов.");
                } else {
                    lines.add("§7Сейчас недельного заказа нет.");
                }
                ru.loom.core.gui.ListMenu.open(p, "gui.cmd.order-title", lines, null);
            }
            @Override public String category() { return "trade"; }
            @Override public String usage() { return "/заказ"; }
        });

        // ---- админ-секция: торговец ----
        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.trader"; }
            @Override public int order() { return 50; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.EMERALD; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.admin.trader"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.trader-lore"));
            }
            @Override public void click(Player p) {
                if (ru.loom.core.gui.BedrockCheck.isBedrock(p)) {
                    ru.loom.core.bedrock.BedrockUi.buttons(p,
                            GuiUtil.t(p, "en".equals(core.api().lang(p.getUniqueId())) ? "ru" : "ru",
                                    "gui.admin.trader"), "",
                            List.of("§aРеролл доски", "§cУбрать НПС", "§7Закрыть"),
                            idx -> {
                                if (idx == 0) plugin.daily().regenerate(true);
                                else if (idx == 1) plugin.npc().remove();
                            });
                    return;
                }
                ru.loom.core.gui.ListMenu.open(p, "gui.admin.trader-status", traderStatus(plugin), pl -> { });
                p.sendMessage(core.lang().raw(core.api().lang(p.getUniqueId()), "gui.admin.trader-hint"));
            }
        });

        // ---- переменные HUD ----
        core.hud().registerVar("quests", (p, lang) -> {
            boolean en = "en".equals(lang);
            int done = plugin.storage().doneToday(p.getUniqueId()).size();
            String label = en ? "Dailies" : "Квесты";
            return "<color:#8FBF74>" + label + "</color> <color:#A89C8A>" + done + "/" + plugin.dailyCount() + "</color>";
        });
        core.hud().registerVar("order", (p, lang) -> {
            var st = plugin.storage();
            if (!st.orderActive) return "";
            boolean en = "en".equals(lang);
            String label = en ? "Order" : "Заказ";
            int pct = st.orderGoal > 0 ? (int) Math.min(100, Math.round(st.orderCollected * 100.0 / st.orderGoal)) : 0;
            return "<color:#E6B366>" + label + "</color> <color:#A89C8A>" + pct + "% · "
                    + TraderUtil.fmt(st.orderCollected) + "/" + TraderUtil.fmt(st.orderGoal) + "</color>";
        });
    }

    /** Строки статуса торговца для админа. */
    private static List<String> traderStatus(LoomTraderPlugin plugin) {
        var st = plugin.storage();
        List<String> out = new ArrayList<>();
        out.add("§7НПС: " + (st.npcUuid != null ? "§aна месте" : "§cне поставлен (/торговец spawn)"));
        out.add("§7Доска на дату: §f" + st.dailyDate + " §8(" + st.dailyIds.size() + " заказов)");
        out.add("§7Недельный заказ: " + (st.orderActive
                ? "§a" + st.orderMat + " " + TraderUtil.fmt(st.orderCollected) + "/" + TraderUtil.fmt(st.orderGoal)
                : "§8нет"));
        return out;
    }
}
