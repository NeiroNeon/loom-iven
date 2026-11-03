package ru.loom.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.menu.MenuAction;

import java.util.List;
import java.util.UUID;

/**
 * Главное меню сервера (/меню): пагинированные страницы кнопок всех функций
 * + служебный ряд (страницы, язык, команды, админ, закрыть).
 * Пустые ячейки заполнены чёрной тонированной панелью.
 */
public final class MenuGUI implements LoomMenu {

    static final int PER_PAGE = 45;
    static final int SLOT_PREV = 45;
    static final int SLOT_PAGE = 47;
    static final int SLOT_LANG = 48;
    static final int SLOT_CMDS = 49;
    static final int SLOT_ADMIN = 51;
    static final int SLOT_CLOSE = 52;
    static final int SLOT_NEXT = 53;

    private final UUID viewer;
    private final Inventory inv;
    private final List<MenuAction> actions;
    private final int page;
    private final int pages;

    public MenuGUI(Player p) {
        this(p, 0);
    }

    private MenuGUI(Player p, int page) {
        var core = LoomCorePlugin.get();
        this.viewer = p.getUniqueId();
        String lang = core != null ? core.api().lang(viewer) : "ru";
        this.actions = core != null ? core.menus().visible(p) : List.of();
        this.pages = Math.max(1, (actions.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        String title = GuiUtil.t(p, lang, "gui.menu.title");
        if (pages > 1) {
            title += GuiUtil.t(p, lang, "gui.page.suffix",
                    "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages));
        }
        this.inv = Bukkit.createInventory(this, 54, title);
        redraw(p);
    }

    public static void open(Player p) {
        if (BedrockCheck.isBedrock(p)) {
            MenuForms.hubForm(p);
            return;
        }
        p.openInventory(new MenuGUI(p).inv);
    }

    private void show(Player p) {
        p.openInventory(inv);
    }

    private void redraw(Player p) {
        inv.clear();
        String lang = lang();
        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < actions.size(); i++) {
            MenuAction a = actions.get(from + i);
            inv.setItem(i, GuiUtil.raw(a.icon(p), a.name(p, lang), a.lore(p, lang)));
        }
        if (page > 0) {
            inv.setItem(SLOT_PREV, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.prev", "%page%", String.valueOf(page)), List.of()));
        }
        inv.setItem(SLOT_PAGE, GuiUtil.raw(Material.PAPER, GuiUtil.t(p, lang, "gui.page.info",
                "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages)), List.of()));
        inv.setItem(SLOT_LANG, GuiUtil.raw(Material.REPEATING_COMMAND_BLOCK,
                GuiUtil.t(p, lang, "gui.menu.lang", "%lang%", lang.toUpperCase()),
                List.of(GuiUtil.t(p, lang, "gui.menu.lang-lore"))));
        inv.setItem(SLOT_CMDS, GuiUtil.item(p, Material.BOOK, lang,
                "gui.menu.help", "gui.menu.help-lore"));
        if (p.hasPermission(GuiUtil.ADMIN_PERM)) {
            inv.setItem(SLOT_ADMIN, GuiUtil.item(p, Material.COMMAND_BLOCK, lang,
                    "gui.admin.item", "gui.admin.item-lore"));
        }
        inv.setItem(SLOT_CLOSE, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        if (page < pages - 1) {
            inv.setItem(SLOT_NEXT, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.next", "%page%", String.valueOf(page + 2)), List.of()));
        }
        GuiUtil.fillAll(inv);
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        var core = LoomCorePlugin.get();
        if (slot == SLOT_CLOSE) return true;
        if (slot == SLOT_PREV && page > 0) {
            new MenuGUI(p, page - 1).show(p);
            return false;
        }
        if (slot == SLOT_NEXT && page < pages - 1) {
            new MenuGUI(p, page + 1).show(p);
            return false;
        }
        if (slot == SLOT_LANG) {
            String next = "ru".equals(core.api().lang(viewer)) ? "en" : "ru";
            core.api().setLang(viewer, next);
            new MenuGUI(p, page).show(p);
            return false;
        }
        if (slot == SLOT_CMDS) {
            CommandPages.open(p);
            return false;
        }
        if (slot == SLOT_ADMIN && p.hasPermission(GuiUtil.ADMIN_PERM)) {
            AdminGUI.open(p);
            return false;
        }
        if (slot < PER_PAGE) {
            int idx = page * PER_PAGE + slot;
            if (idx < actions.size()) {
                MenuAction a = actions.get(idx);
                p.closeInventory();
                // действия могут открывать свои GUI/формы — выполняем на следующем тике
                Bukkit.getScheduler().runTask(core, () -> {
                    try {
                        a.click(p);
                    } catch (Exception ex) {
                        core.getLogger().warning("Меню: ошибка действия " + a.id() + ": " + ex);
                    }
                });
            }
        }
        return false;
    }

    private String lang() {
        var core = LoomCorePlugin.get();
        return core != null ? core.api().lang(viewer) : "ru";
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
