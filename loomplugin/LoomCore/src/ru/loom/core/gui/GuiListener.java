package ru.loom.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Маршрутизация кликов в GUI-инвентарях Loom (holder = LoomMenu). */
public final class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof LoomMenu menu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        // клики по краям/вне сетки
        if (e.getSlot() < 0 || e.getSlot() >= e.getView().getTopInventory().getSize()) return;
        boolean close = menu.handleClick(e.getSlot(), p, e.getClick());
        if (close) p.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof LoomMenu) {
            e.setCancelled(true);
        }
    }
}
