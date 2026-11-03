package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.loom.core.LoomCorePlugin;

/** Слушатели LoomTrader: клики GUI, защита НПС, клик по НПС, boss-bar на входе. */
final class TraderListeners implements Listener {

    private final LoomTraderPlugin plugin;

    TraderListeners(LoomTraderPlugin plugin) { this.plugin = plugin; }

    // ---------- доска: клики ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TraderGUI gui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;   // только верхний инвентарь

        int slot = e.getSlot();
        if (slot == TraderGUI.SLOT_CLOSE) { p.closeInventory(); return; }
        QuestPool.Quest q = gui.questAt(slot);
        if (q != null) {
            plugin.daily().complete(p, q);
            p.openInventory(e.getInventory());                     // обновить статус/титул
        }
    }

    // ---------- НПС: клик открывает доску ----------
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (plugin.npc().isNPC(e.getRightClicked())) {
            e.setCancelled(true);
            plugin.npc().open(e.getPlayer());
        }
    }

    @EventHandler
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        if (plugin.npc().isNPC(e.getRightClicked())) {
            e.setCancelled(true);
            plugin.npc().open(e.getPlayer());
        }
    }

    // ---------- НПС: защита ----------
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (plugin.npc().isNPC(e.getEntity())) e.setCancelled(true);
    }

    // ---------- boss-bar заказа новым игрокам ----------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.storage().orderActive) plugin.order().attach(e.getPlayer());
        // доска дня: если дата сменилась, пока никого не было на сервере
        Bukkit.getScheduler().runTask(plugin, () -> plugin.daily().rollIfNeeded());
    }
}
