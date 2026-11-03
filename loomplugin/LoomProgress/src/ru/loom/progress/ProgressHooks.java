package ru.loom.progress;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.menu.MenuAction;
import ru.loom.core.stats.StatsSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Интеграция LoomProgress с ядром: пункты главного меню (сбор, достижения,
 * пропуск, косметика), секция статистики профиля и переменная HUD %fund%.
 */
public final class ProgressHooks {

    private ProgressHooks() { }

    public static void register(ProgressPlugin plugin) {
        var core = LoomCorePlugin.get();
        if (core == null) return;

        // ---- пункты меню ----
        core.menus().register(new MenuAction() {
            @Override public String id() { return "fund"; }
            @Override public int order() { return 40; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.GOLDEN_CHESTPLATE; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.menu.item.fund"); }
            @Override public List<String> lore(Player p, String lang) {
                var st = plugin.storage();
                String extra = st.fundActive
                        ? "\n§8Сейчас: §7" + FundManager.fmt(st.fundCollected) + "/" + FundManager.fmt(st.fundTarget)
                        : "\n§8Сейчас сбор не идёт.";
                List<String> out = new ArrayList<>(List.of(GuiUtil.t(p, lang, "gui.menu.item.fund-lore")));
                out.add(extra);
                return out;
            }
            @Override public void click(Player p) { FundGUI.open(p); }
        });

        core.menus().register(new MenuAction() {
            @Override public String id() { return "ach"; }
            @Override public int order() { return 50; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.FILLED_MAP; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.menu.item.ach"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.ach-lore"));
            }
            @Override public void click(Player p) { ProgressPanels.open(p, ProgressPanels.Kind.ACH); }
        });

        core.menus().register(new MenuAction() {
            @Override public String id() { return "pass"; }
            @Override public int order() { return 60; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.NETHER_STAR; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.menu.item.pass"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.pass-lore"));
            }
            @Override public void click(Player p) { PassGUI.open(p); }
        });

        core.menus().register(new MenuAction() {
            @Override public String id() { return "cosmetics"; }
            @Override public int order() { return 70; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.ENDER_CHEST; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.menu.item.cosmetics"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.cosmetics-lore"));
            }
            @Override public void click(Player p) { ProgressPanels.open(p, ProgressPanels.Kind.COS); }
        });

        // ---- секция статистики профиля ----
        core.stats().register("progress", new StatsSection() {
            @Override public String title(Player p, String lang) {
                return core.lang().raw(lang, "gui.stats.progress-title");
            }
            @Override public List<String> lines(Player p, String lang) {
                List<String> out = new ArrayList<>();
                var am = plugin.achievements();
                out.add(core.lang().raw(lang, "gui.stats.ach",
                        "%done%", String.valueOf(am.doneCount(p.getUniqueId())),
                        "%total%", String.valueOf(am.total())));
                var sp = plugin.season();
                out.add(core.lang().raw(lang, "gui.stats.pass",
                        "%day%", String.valueOf(sp.dayOf(p.getUniqueId()))));
                var st = plugin.storage();
                if (st.fundActive) {
                    double my = st.fundContrib.getOrDefault(p.getUniqueId(), 0.0);
                    out.add(core.lang().raw(lang, "gui.stats.fund", "%amount%", FundManager.fmt(my)));
                }
                if (plugin.mentors().canMentor(p.getUniqueId())) {
                    out.add(core.lang().raw(lang, "gui.stats.mentor",
                            "%code%", plugin.mentors().myCode(p.getUniqueId()),
                            "%n%", String.valueOf(plugin.mentors().studentsOf(p.getUniqueId()).size())));
                }
                return out;
            }
        });

        // ---- переменная HUD: %fund% ----
        core.hud().registerVar("fund", (p, lang) -> {
            var st = plugin.storage();
            if (!st.fundActive) return "";
            boolean en = "en".equals(lang);
            String label = en ? "Fund" : "Сбор";
            return "<color:#E6B366>" + label + "</color> <color:#A89C8A>"
                    + plugin.fund().pct() + "% · "
                    + FundManager.fmt(st.fundCollected) + "/" + FundManager.fmt(st.fundTarget) + "</color>";
        });

        // ---- страницы «Команды»: прогресс ----
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.pass"; }
            @Override public int order() { return 40; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.NETHER_STAR; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.pass"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.pass-lore"));
            }
            @Override public void click(Player p) { PassGUI.open(p); }
            @Override public String category() { return "progress"; }
            @Override public String usage() { return "/пропуск"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.ach"; }
            @Override public int order() { return 41; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.FILLED_MAP; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.ach"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.ach-lore"));
            }
            @Override public void click(Player p) { ProgressPanels.open(p, ProgressPanels.Kind.ACH); }
            @Override public String category() { return "progress"; }
            @Override public String usage() { return "/ачивки"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.keys"; }
            @Override public int order() { return 42; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.TRIPWIRE_HOOK; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.keys"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.keys-lore"));
            }
            @Override public void click(Player p) { ProgressPanels.open(p, ProgressPanels.Kind.COS); }
            @Override public String category() { return "progress"; }
            @Override public String usage() { return "/ключи"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.events"; }
            @Override public int order() { return 43; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.WHEAT; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.events"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.events-lore"));
            }
            @Override public void click(Player p) { plugin.eventsCommandGui(p); }
            @Override public String category() { return "progress"; }
            @Override public String usage() { return "/ивент"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.fame"; }
            @Override public int order() { return 44; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.GOLD_BLOCK; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.fame"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.fame-lore"));
            }
            @Override public void click(Player p) { plugin.fameCommandGui(p); }
            @Override public String category() { return "progress"; }
            @Override public String usage() { return "/слава"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.fund"; }
            @Override public int order() { return 45; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.GOLDEN_CHESTPLATE; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.fund"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.fund-lore"));
            }
            @Override public void click(Player p) { FundGUI.open(p); }
            @Override public String category() { return "progress"; }
            @Override public String usage() { return "/сбор"; }
        });

        // ---- админ-секции ----
        ProgressAdmin.register(plugin);
    }
}
