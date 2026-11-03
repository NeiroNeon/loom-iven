package ru.loom.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.bedrock.BedrockUi;
import ru.loom.core.menu.MenuAction;

import java.util.List;

/**
 * Страницы «Команды»: страница категорий → страница команд категории.
 * Описание каждой команды — в лоре её предмета; клик открывает следующий
 * логический GUI или выполняет действие. Категория «admin» видима только
 * с правом loom.admin. Bedrock — формы той же структуры.
 */
public final class CommandPages implements LoomMenu {

    private static final int PER_PAGE = 45;

    /** Категории команд (id → иконка/ключи). */
    private static final String[] CATS = {"core", "economy", "progress", "trade", "admin"};
    private static final Material[] CAT_ICONS = {
            Material.EMERALD, Material.GOLD_INGOT, Material.FILLED_MAP,
            Material.GOLDEN_CHESTPLATE, Material.COMMAND_BLOCK};

    private final Inventory inv;
    private final String category; // null — страница категорий
    private final int page;
    private final int pages;

    private CommandPages(Player p, String category, int page) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        this.category = category;
        List<MenuAction> items = category == null ? List.of() : core.menus().visibleCommands(p, category);
        this.pages = category == null ? 1 : Math.max(1, (items.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        String title = GuiUtil.t(p, lang, "gui.commands.title");
        if (category != null) {
            title += " §8· " + GuiUtil.t(p, lang, "gui.cmd.cat." + category);
        }
        if (pages > 1) {
            title += GuiUtil.t(p, lang, "gui.page.suffix",
                    "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages));
        }
        this.inv = Bukkit.createInventory(this, 54, title);
        redraw(p, lang);
    }

    public static void open(Player p) {
        if (BedrockCheck.isBedrock(p)) {
            catsForm(p);
            return;
        }
        p.openInventory(new CommandPages(p, null, 0).inv);
    }

    private void redraw(Player p, String lang) {
        inv.clear();
        var core = LoomCorePlugin.get();
        if (category == null) {
            List<String> cats = core.menus().commandCategories(p);
            int slot = 0;
            for (String cat : cats) {
                if (slot >= PER_PAGE) break;
                int idx = List.of(CATS).indexOf(cat);
                Material icon = idx >= 0 ? CAT_ICONS[idx] : Material.BOOK;
                List<MenuAction> cmds = core.menus().visibleCommands(p, cat);
                List<String> lore = new java.util.ArrayList<>();
                lore.add(GuiUtil.t(p, lang, "gui.cmd.cat." + cat + "-lore"));
                lore.add("§8" + GuiUtil.t(p, lang, "gui.cmd.count", "%count%", String.valueOf(cmds.size())));
                lore.add("§a" + GuiUtil.t(p, lang, "gui.cmd.open-hint"));
                inv.setItem(slot++, GuiUtil.raw(icon, GuiUtil.t(p, lang, "gui.cmd.cat." + cat), lore));
            }
        } else {
            List<MenuAction> cmds = core.menus().visibleCommands(p, category);
            int from = page * PER_PAGE;
            for (int i = 0; i < PER_PAGE && from + i < cmds.size(); i++) {
                MenuAction c = cmds.get(from + i);
                List<String> lore = new java.util.ArrayList<>(c.lore(p, lang));
                if (!c.usage().isEmpty()) {
                    lore.add("§8" + GuiUtil.t(p, lang, "gui.cmd.usage", "%cmd%", c.usage()));
                }
                lore.add("§a" + GuiUtil.t(p, lang, "gui.cmd.open-hint"));
                inv.setItem(i, GuiUtil.raw(c.icon(p), c.name(p, lang), lore));
            }
        }
        // служебный ряд
        inv.setItem(45, GuiUtil.raw(Material.SPECTRAL_ARROW,
                GuiUtil.t(p, lang, "gui.back"), List.of(GuiUtil.t(p, lang, "gui.back-lore"))));
        if (category != null && page > 0) {
            inv.setItem(46, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.prev", "%page%", String.valueOf(page)), List.of()));
        }
        inv.setItem(47, GuiUtil.raw(Material.PAPER, GuiUtil.t(p, lang, "gui.page.info",
                "%page%", String.valueOf(category == null ? 1 : page + 1),
                "%pages%", String.valueOf(pages)), List.of()));
        inv.setItem(49, GuiUtil.raw(Material.ARROW, GuiUtil.t(p, lang, "gui.menu.open"), List.of()));
        inv.setItem(52, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        if (category != null && page < pages - 1) {
            inv.setItem(53, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.next", "%page%", String.valueOf(page + 2)), List.of()));
        }
        GuiUtil.fillAll(inv);
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        var core = LoomCorePlugin.get();
        if (slot == 52) return true;
        if (slot == 45) { // назад
            if (category == null) {
                MenuGUI.open(p);
            } else {
                p.openInventory(new CommandPages(p, null, 0).inv);
            }
            return false;
        }
        if (slot == 46 && category != null && page > 0) {
            p.openInventory(new CommandPages(p, category, page - 1).inv);
            return false;
        }
        if (slot == 53 && category != null && page < pages - 1) {
            p.openInventory(new CommandPages(p, category, page + 1).inv);
            return false;
        }
        if (slot == 49) {
            MenuGUI.open(p);
            return false;
        }
        if (slot < PER_PAGE) {
            if (category == null) {
                List<String> cats = core.menus().commandCategories(p);
                if (slot < cats.size()) {
                    p.openInventory(new CommandPages(p, cats.get(slot), 0).inv);
                }
            } else {
                List<MenuAction> cmds = core.menus().visibleCommands(p, category);
                int idx = page * PER_PAGE + slot;
                if (idx < cmds.size()) {
                    MenuAction c = cmds.get(idx);
                    p.closeInventory();
                    Bukkit.getScheduler().runTask(core, () -> {
                        try {
                            c.click(p);
                        } catch (Exception ex) {
                            core.getLogger().warning("Команды: ошибка " + c.id() + ": " + ex);
                        }
                    });
                }
            }
        }
        return false;
    }

    // ---------------- Bedrock ----------------

    private static void catsForm(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        List<String> cats = core.menus().commandCategories(p);
        List<String> buttons = new java.util.ArrayList<>();
        for (String cat : cats) buttons.add(GuiUtil.t(p, lang, "gui.cmd.cat." + cat));
        BedrockUi.buttons(p, GuiUtil.t(p, lang, "gui.commands.title"),
                GuiUtil.t(p, lang, "gui.menu.form-content"), buttons, idx -> {
                    if (idx >= 0 && idx < cats.size()) {
                        Bukkit.getScheduler().runTask(core, () -> cmdsForm(p, cats.get(idx), 0));
                    }
                });
    }

    /** Команды категории для Bedrock (постранично по 20 кнопок). */
    private static void cmdsForm(Player p, String category, int page) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        List<MenuAction> cmds = core.menus().visibleCommands(p, category);
        int per = 20;
        int pages = Math.max(1, (cmds.size() + per - 1) / per);
        final int pg = Math.max(0, Math.min(page, pages - 1));
        List<String> buttons = new java.util.ArrayList<>();
        int from = pg * per;
        for (int i = from; i < from + per && i < cmds.size(); i++) {
            buttons.add(cmds.get(i).name(p, lang));
        }
        if (pg < pages - 1) buttons.add("→");
        buttons.add("← " + GuiUtil.t(p, lang, "gui.back"));
        String title = GuiUtil.t(p, lang, "gui.commands.title") + " · "
                + GuiUtil.t(p, lang, "gui.cmd.cat." + category);
        BedrockUi.buttons(p, title, "", buttons, idx -> {
            int n = idx;
            if (pg < pages - 1 && n == buttons.size() - 2) { // следующая
                Bukkit.getScheduler().runTask(core, () -> cmdsForm(p, category, pg + 1));
                return;
            }
            if (n == buttons.size() - 1) { // назад к категориям
                Bukkit.getScheduler().runTask(core, () -> catsForm(p));
                return;
            }
            int cmdIdx = from + n;
            if (n >= 0 && cmdIdx < cmds.size()) {
                MenuAction c = cmds.get(cmdIdx);
                Bukkit.getScheduler().runTask(core, () -> {
                    try {
                        c.click(p);
                    } catch (Exception ex) {
                        core.getLogger().warning("Команды (форма): ошибка: " + ex);
                    }
                });
            }
        });
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
