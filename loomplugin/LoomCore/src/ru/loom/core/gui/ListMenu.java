package ru.loom.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.bedrock.BedrockUi;

import java.util.List;
import java.util.function.Consumer;

/**
 * Универсальное «меню-список»: строки данных (топы, зал славы, логи, статусы)
 * как предметы. Пагинация, филлер, кнопка возврата. Bedrock — SimpleForm.
 */
public final class ListMenu implements LoomMenu {

    private static final int PER_PAGE = 45;

    private final Inventory inv;
    private final String title;
    private final List<String> lines;
    private final Consumer<Player> back; // null → кнопка «в меню»
    private final int page;
    private final int pages;

    private ListMenu(Player p, String title, List<String> lines, Consumer<Player> back, int page) {
        String lang = LoomCorePlugin.get().api().lang(p.getUniqueId());
        this.title = title;
        this.lines = lines;
        this.back = back;
        this.pages = Math.max(1, (lines.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        String fullTitle = title;
        if (pages > 1) {
            fullTitle += GuiUtil.t(p, lang, "gui.page.suffix",
                    "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages));
        }
        this.inv = Bukkit.createInventory(this, 54, fullTitle);
        redraw(p, lang);
    }

    /** Открыть список. back == null → вместо «назад» показывается «в меню». */
    public static void open(Player p, String titleKey, List<String> lines, Consumer<Player> back) {
        if (BedrockCheck.isBedrock(p)) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(p.getUniqueId());
            StringBuilder sb = new StringBuilder(GuiUtil.t(p, lang, titleKey)).append("\n\n");
            for (String line : lines) sb.append(line.replace("§", "&")).append('\n');
            BedrockUi.buttons(p, GuiUtil.t(p, lang, titleKey), sb.toString(),
                    List.of(GuiUtil.t(p, lang, "gui.menu.open"), GuiUtil.t(p, lang, "gui.close")),
                    idx -> {
                        if (idx == 0) MenuGUI.open(p);
                    });
            return;
        }
        p.openInventory(new ListMenu(p, GuiUtil.t(p, LoomCorePlugin.get().api().lang(p.getUniqueId()), titleKey), lines, back, 0).inv);
    }

    private void redraw(Player p, String lang) {
        inv.clear();
        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < lines.size(); i++) {
            String line = lines.get(from + i);
            inv.setItem(i, GuiUtil.raw(Material.PAPER, "§f" + line, List.of()));
        }
        if (page > 0) {
            inv.setItem(45, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.prev", "%page%", String.valueOf(page)), List.of()));
        }
        inv.setItem(47, GuiUtil.raw(Material.PAPER, GuiUtil.t(p, lang, "gui.page.info",
                "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages)), List.of()));
        if (back != null) {
            inv.setItem(49, GuiUtil.raw(Material.SPECTRAL_ARROW,
                    GuiUtil.t(p, lang, "gui.back"), List.of(GuiUtil.t(p, lang, "gui.back-lore"))));
        } else {
            inv.setItem(49, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.menu.open"), List.of()));
        }
        inv.setItem(52, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        if (page < pages - 1) {
            inv.setItem(53, GuiUtil.raw(Material.ARROW,
                    GuiUtil.t(p, lang, "gui.next", "%page%", String.valueOf(page + 2)), List.of()));
        }
        GuiUtil.fillAll(inv);
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        if (slot == 52) return true;
        if (slot == 45 && page > 0) {
            p.openInventory(new ListMenu(p, title, lines, back, page - 1).inv);
            return false;
        }
        if (slot == 53 && page < pages - 1) {
            p.openInventory(new ListMenu(p, title, lines, back, page + 1).inv);
            return false;
        }
        if (slot == 49) {
            if (back != null) {
                Consumer<Player> b = back;
                p.closeInventory();
                Bukkit.getScheduler().runTask(LoomCorePlugin.get(), () -> b.accept(p));
            } else {
                MenuGUI.open(p);
            }
        }
        return false;
    }

    @Override
    public org.bukkit.inventory.Inventory getInventory() {
        return inv;
    }
}
