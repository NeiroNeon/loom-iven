package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.gui.LoomMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Панели прогресса: достижения (по категориям), сезонный пропуск (день,
 * забор награды), косметика (надеть активную). Для Bedrock — те же действия
 * формами Floodgate.
 */
public final class ProgressPanels implements LoomMenu {

    public enum Kind { ACH, PASS, COS }

    private static final Material[] CAT_ICONS = {
            Material.GRASS_BLOCK, Material.IRON_INGOT, Material.COOKED_BEEF, Material.DIAMOND,
            Material.EMERALD, Material.EXPERIENCE_BOTTLE, Material.ENCHANTING_TABLE,
            Material.BREWING_STAND, Material.NETHERITE_INGOT, Material.TOTEM_OF_UNDYING};

    private final UUID viewer;
    private final Kind kind;
    private final Inventory inv;
    private final LoomCorePlugin core = LoomCorePlugin.get();

    private ProgressPanels(Player p, Kind kind) {
        var core = LoomCorePlugin.get();
        this.viewer = p.getUniqueId();
        this.kind = kind;
        String lang = core.api().lang(viewer);
        this.inv = Bukkit.createInventory(this, 27, GuiUtil.t(p, lang, "gui.panel." + kind.name().toLowerCase() + ".title"));
        redraw(p);
    }

    public static void open(Player p, Kind kind) {
        if (ru.loom.core.bedrock.BedrockUi.available()
                && ru.loom.core.bedrock.BedrockUi.isBedrock(p.getUniqueId())) {
            switch (kind) {
                case ACH -> achForm(p);
                case PASS -> PassGUI.open(p); // v1.8: полный пропуск-форма
                case COS -> cosForm(p);
            }
            return;
        }
        if (kind == Kind.PASS) {
            PassGUI.open(p); // v1.8: полный GUI пропуска вместо мини-панели
            return;
        }
        p.openInventory(new ProgressPanels(p, kind).inv);
    }

    private void redraw(Player p) {
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(viewer);
        inv.clear();
        switch (kind) {
            case ACH -> redrawAch(p, lang, plugin);
            case PASS -> redrawPass(p, lang, plugin);
            case COS -> redrawCos(p, lang, plugin);
        }
        inv.setItem(18, GuiUtil.raw(Material.ARROW, GuiUtil.t(p, lang, "gui.menu.open"), List.of()));
        inv.setItem(26, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        GuiUtil.fillAll(inv);
    }

    // ---------------- достижения ----------------

    private void redrawAch(Player p, String lang, ProgressPlugin plugin) {
        var am = plugin.achievements();
        List<String> cats = am.categories();
        for (int i = 0; i < cats.size() && i < 25; i++) {
            String cat = cats.get(i);
            int done = 0;
            var map = am.byCategory(cat);
            for (var a : map.values()) if (am.isDone(viewer, a.id)) done++;
            inv.setItem(i, GuiUtil.raw(CAT_ICONS[i % CAT_ICONS.length],
                    core.lang().raw(lang, "ach.cat." + cat),
                    List.of(GuiUtil.t(p, lang, "gui.panel.ach.done", "%done%", String.valueOf(done),
                            "%total%", String.valueOf(map.size())),
                            GuiUtil.t(p, lang, "gui.panel.ach.hint"))));
        }
    }

    // ---------------- сезонный пропуск ----------------

    private void redrawPass(Player p, String lang, ProgressPlugin plugin) {
        var sp = plugin.season();
        var st = plugin.storage();
        int day = sp.dayOf(viewer);
        boolean donate = sp.hasDonateTrack(viewer);
        var claimed = st.passClaimed.getOrDefault(viewer, List.of());

        List<String> status = List.of(
                GuiUtil.t(p, lang, "gui.panel.pass.day", "%day%", String.valueOf(day)),
                GuiUtil.t(p, lang, "gui.panel.pass.track", "%track%",
                        core.lang().raw(lang, donate ? "pass.track.donate" : "pass.track.free")),
                GuiUtil.t(p, lang, "gui.panel.pass.claimed", "%n%", String.valueOf(claimed.size())));
        inv.setItem(4, GuiUtil.raw(Material.NETHER_STAR,
                GuiUtil.t(p, lang, "gui.panel.pass.status-name"), status));

        String freeId = "f" + day;
        boolean freeTaken = claimed.contains(freeId);
        inv.setItem(12, GuiUtil.raw(freeTaken ? Material.LIME_DYE : Material.EMERALD,
                GuiUtil.t(p, lang, "gui.panel.pass.claim-free",
                        "%day%", String.valueOf(day)),
                List.of(freeTaken
                        ? GuiUtil.t(p, lang, "gui.panel.pass.taken")
                        : GuiUtil.t(p, lang, "gui.panel.pass.click-claim"))));

        if (donate) {
            String dId = "d" + day;
            boolean dTaken = claimed.contains(dId);
            inv.setItem(14, GuiUtil.raw(dTaken ? Material.LIME_DYE : Material.GOLD_INGOT,
                    GuiUtil.t(p, lang, "gui.panel.pass.claim-donate",
                            "%day%", String.valueOf(day)),
                    List.of(dTaken
                            ? GuiUtil.t(p, lang, "gui.panel.pass.taken")
                            : GuiUtil.t(p, lang, "gui.panel.pass.click-claim"))));
        } else {
            inv.setItem(14, GuiUtil.raw(Material.AMETHYST_SHARD,
                    GuiUtil.t(p, lang, "gui.panel.pass.buy"),
                    List.of(GuiUtil.t(p, lang, "gui.panel.pass.buy-lore", "%price%",
                            LoomCorePlugin.get().api().fmt(
                                    plugin.getConfig().getDouble("season.donate-price-coins", 25000))))));
        }
        inv.setItem(20, GuiUtil.item(p, Material.BOOK, lang, "gui.panel.pass.list", "gui.panel.pass.list-lore"));
    }

    // ---------------- косметика ----------------

    private void redrawCos(Player p, String lang, ProgressPlugin plugin) {
        var km = plugin.keys();
        var st = plugin.storage();
        var owned = st.cosmeticsOwned.getOrDefault(viewer, List.of());
        inv.setItem(4, GuiUtil.raw(Material.CHEST,
                GuiUtil.t(p, lang, "gui.panel.cos.keys-name"),
                List.of(GuiUtil.t(p, lang, "gui.panel.cos.keys-value",
                        "%n%", String.valueOf(km.keysOf(viewer))))));
        // «Открыть ключ» прямо в GUI
        int n = km.keysOf(viewer);
        inv.setItem(10, GuiUtil.raw(n > 0 ? Material.TRIPWIRE_HOOK : Material.GRAY_DYE,
                GuiUtil.t(p, lang, "gui.panel.cos.open-key"),
                List.of(n > 0
                        ? GuiUtil.t(p, lang, "gui.panel.cos.open-key-hint", "%n%", String.valueOf(n))
                        : GuiUtil.t(p, lang, "gui.panel.cos.no-keys"))));
        int slot = 12;
        for (String c : owned) {
            if (slot > 24) break;
            boolean active = c.equals(km.active(viewer));
            inv.setItem(slot++, GuiUtil.raw(active ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME,
                    core.lang().raw(lang, "cosmetics." + c),
                    List.of(active
                            ? GuiUtil.t(p, lang, "gui.panel.cos.active")
                            : GuiUtil.t(p, lang, "gui.panel.cos.click-wear"))));
        }
        if (owned.isEmpty()) {
            inv.setItem(16, GuiUtil.item(p, Material.BOOK, lang, "gui.panel.cos.none", "gui.panel.cos.none-lore"));
        }
    }

    // ---------------- клики ----------------

    @Override
    public boolean handleClick(int slot, Player p) {
        var plugin = ProgressPlugin.get();
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        if (slot == 26) return true;
        if (slot == 18) {
            ru.loom.core.gui.MenuGUI.open(p);
            return false;
        }
        switch (kind) {
            case ACH -> {
                var cats = plugin.achievements().categories();
                if (slot < cats.size()) {
                    // содержимое категории — GUI-список вместо чата
                    List<String> lines = new ArrayList<>();
                    var am = plugin.achievements();
                    for (var a : am.byCategory(cats.get(slot)).values()) {
                        boolean done = am.isDone(viewer, a.id);
                        long prog = am.progressOf(viewer, a.id);
                        lines.add((done ? "§a✔ " : "§6") + core.msg(viewer, "ach.name." + a.id)
                                + " §8— §7" + Math.min(prog, a.goal) + "/" + a.goal
                                + " §8— §6" + core.api().fmt(a.coins) + " монет"
                                + (a.keys > 0 ? " §d+" + a.keys + " ключ" : ""));
                    }
                    ru.loom.core.gui.ListMenu.open(p, "gui.panel.ach.cat-title", lines,
                            pl -> open(pl, Kind.ACH));
                }
            }
            case PASS -> {
                var sp = plugin.season();
                int day = sp.dayOf(viewer);
                if (slot == 12) {
                    String err = sp.claim(viewer, "f" + day);
                    if (err != null) p.sendMessage(err);
                    else p.sendMessage(core.msg(viewer, "pass.claimed"));
                    redraw(p);
                } else if (slot == 14) {
                    if (sp.hasDonateTrack(viewer)) {
                        String err = sp.claim(viewer, "d" + day);
                        if (err != null) p.sendMessage(err);
                        else p.sendMessage(core.msg(viewer, "pass.claimed"));
                    } else if (sp.buyDonateTrack(viewer, p.getName())) {
                        p.sendMessage(core.msg(viewer, "pass.bought-you"));
                    } else {
                        p.sendMessage(core.msg(viewer, "pass.buy-fail"));
                    }
                    redraw(p);
                } else if (slot == 20) {
                    PassGUI.open(p);
                }
            }
            case COS -> {
                var km = plugin.keys();
                var st = plugin.storage();
                var owned = st.cosmeticsOwned.getOrDefault(viewer, List.of());
                if (slot == 10) {
                    if (km.keysOf(viewer) > 0) {
                        String won = km.openKey(viewer);
                        if (won != null) p.sendMessage(core.msg(viewer, "keys.opened"));
                    } else {
                        p.sendMessage(core.msg(viewer, "keys.none"));
                    }
                    redraw(p);
                    return false;
                }
                int idx = slot - 12;
                if (idx >= 0 && idx < owned.size()) {
                    km.activate(viewer, owned.get(idx));
                    p.sendMessage(core.msg(viewer, "keys.worn"));
                    redraw(p);
                }
            }
        }
        return false;
    }

    // ---------------- Bedrock-формы ----------------

    static void achForm(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        var am = plugin.achievements();
        StringBuilder sb = new StringBuilder(core.lang().raw(lang, "ach.header",
                "%done%", String.valueOf(am.doneCount(p.getUniqueId())),
                "%total%", String.valueOf(am.total())));
        List<String> buttons = new ArrayList<>();
        for (String cat : am.categories()) {
            int done = 0;
            for (var a : am.byCategory(cat).values()) if (am.isDone(p.getUniqueId(), a.id)) done++;
            buttons.add(core.lang().raw(lang, "ach.cat." + cat) + " — " + done + "/" + am.byCategory(cat).size());
        }
        ru.loom.core.bedrock.BedrockUi.buttons(p,
                core.lang().raw(lang, "gui.panel.ach.title"), sb.toString(), buttons, idx -> {
                    if (idx >= 0 && idx < am.categories().size()) {
                        // содержимое категории — GUI-список (работает и на Bedrock)
                        String cat = am.categories().get(idx);
                        List<String> lines = new ArrayList<>();
                        for (var a : am.byCategory(cat).values()) {
                            boolean done = am.isDone(p.getUniqueId(), a.id);
                            long prog = am.progressOf(p.getUniqueId(), a.id);
                            lines.add((done ? "✔ " : "") + core.msg(p.getUniqueId(), "ach.name." + a.id)
                                    + " — " + Math.min(prog, a.goal) + "/" + a.goal
                                    + " — " + core.api().fmt(a.coins) + " монет"
                                    + (a.keys > 0 ? " +" + a.keys + " ключ" : ""));
                        }
                        ru.loom.core.gui.ListMenu.open(p, "gui.panel.ach.cat-title", lines,
                                pl -> open(pl, Kind.ACH));
                    }
                });
    }

    static void passForm(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        var sp = plugin.season();
        var st = plugin.storage();
        int day = sp.dayOf(p.getUniqueId());
        boolean donate = sp.hasDonateTrack(p.getUniqueId());
        var claimed = st.passClaimed.getOrDefault(p.getUniqueId(), List.of());

        String content = core.lang().raw(lang, "gui.panel.pass.day", "%day%", String.valueOf(day)) + "\n"
                + core.lang().raw(lang, "gui.panel.pass.track", "%track%",
                core.lang().raw(lang, donate ? "pass.track.donate" : "pass.track.free"));
        List<String> buttons = new ArrayList<>();
        buttons.add((claimed.contains("f" + day) ? "✔ " : "")
                + core.lang().raw(lang, "gui.panel.pass.claim-free", "%day%", String.valueOf(day)));
        if (donate) {
            buttons.add((claimed.contains("d" + day) ? "✔ " : "")
                    + core.lang().raw(lang, "gui.panel.pass.claim-donate", "%day%", String.valueOf(day)));
        } else {
            buttons.add(core.lang().raw(lang, "gui.panel.pass.buy") + " — "
                    + core.api().fmt(plugin.getConfig().getDouble("season.donate-price-coins", 25000)));
        }
        buttons.add(core.lang().raw(lang, "gui.close"));
        ru.loom.core.bedrock.BedrockUi.buttons(p,
                core.lang().raw(lang, "gui.panel.pass.title"), content, buttons, idx -> {
                    if (idx == 0) {
                        String err = sp.claim(p.getUniqueId(), "f" + day);
                        if (err != null) p.sendMessage(err);
                        else p.sendMessage(core.msg(p.getUniqueId(), "pass.claimed"));
                    } else if (idx == 1) {
                        if (donate) {
                            String err = sp.claim(p.getUniqueId(), "d" + day);
                            if (err != null) p.sendMessage(err);
                            else p.sendMessage(core.msg(p.getUniqueId(), "pass.claimed"));
                        } else if (sp.buyDonateTrack(p.getUniqueId(), p.getName())) {
                            p.sendMessage(core.msg(p.getUniqueId(), "pass.bought-you"));
                        } else {
                            p.sendMessage(core.msg(p.getUniqueId(), "pass.buy-fail"));
                        }
                    }
                });
    }

    static void cosForm(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        var km = plugin.keys();
        var st = plugin.storage();
        var owned = st.cosmeticsOwned.getOrDefault(p.getUniqueId(), List.of());
        String content = core.lang().raw(lang, "gui.panel.cos.keys-value",
                "%n%", String.valueOf(km.keysOf(p.getUniqueId())));
        if (owned.isEmpty()) content = core.lang().raw(lang, "gui.panel.cos.none");
        List<String> buttons = new ArrayList<>();
        for (String c : owned) {
            buttons.add((c.equals(km.active(p.getUniqueId())) ? "✔ " : "")
                    + core.lang().raw(lang, "cosmetics." + c));
        }
        buttons.add(core.lang().raw(lang, "gui.close"));
        ru.loom.core.bedrock.BedrockUi.buttons(p,
                core.lang().raw(lang, "gui.panel.cos.title"), content, buttons, idx -> {
                    if (idx >= 0 && idx < owned.size()) {
                        km.activate(p.getUniqueId(), owned.get(idx));
                        p.sendMessage(core.msg(p.getUniqueId(), "keys.worn"));
                    }
                });
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
