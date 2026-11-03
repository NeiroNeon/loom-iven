package ru.loom.auction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import ru.loom.core.LoomCorePlugin;

/** Обработка кликов в GUI аукциона. */
public final class AuctionListeners implements Listener {

    private final AuctionPlugin plugin;

    public AuctionListeners(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // админ-модерация аукциона
        if (e.getInventory().getHolder() instanceof AuctionAdminGUI) {
            if (!(e.getView().getTopInventory().equals(e.getClickedInventory()))) return;
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player pp) AuctionAdminGUI.handleClick(e, pp, plugin);
            return;
        }
        if (!(e.getInventory().getHolder() instanceof AuctionGUI gui)) return;
        if (!(e.getView().getTopInventory().equals(e.getClickedInventory()))) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getSlot();
        var core = LoomCorePlugin.get();
        var mgr = plugin.auction();

        if (slot == 49) { p.closeInventory(); return; }
        if (slot == 45 && gui.page > 0) { gui.page--; gui.open(p); return; }
        // кламп: вперёд только если кнопка реально нарисована (нет увода за последнюю страницу)
        if (slot == 53) {
            if (e.getView().getTopInventory().getItem(53) != null
                    && e.getView().getTopInventory().getItem(53).getType() == org.bukkit.Material.ARROW) {
                gui.page++;
                gui.open(p);
            }
            return;
        }

        if (gui.mode == AuctionGUI.Mode.CLAIMS && slot == 40) {
            double payout = mgr.takePayout(p.getUniqueId());
            if (payout > 0) core.api().add(p.getUniqueId(), p.getName(), payout);
            // предметы выдачи
            for (ItemStack it : mgr.takeClaims(p.getUniqueId())) {
                var left = p.getInventory().addItem(it);
                left.values().forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
            }
            core.lang().send(p, "auction.claimed");
            p.closeInventory();
            return;
        }

        if (gui.mode == AuctionGUI.Mode.CLAIMS && slot < 45) {
            // клик по конкретному предмету выдачи
            var ids = mgr.claimsOf(p.getUniqueId());
            int idx = slot;
            if (idx >= ids.size()) return;
            UUID lid = ids.get(idx);
            ItemStack item = mgr.claimOne(p.getUniqueId(), lid);
            if (item != null) {
                var left = p.getInventory().addItem(item);
                left.values().forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
                core.lang().send(p, "auction.claimed");
            }
            gui.open(p);
            return;
        }

        if (slot < 45) {
            var lots = gui.mode == AuctionGUI.Mode.MARKET
                    ? mgr.activeLots()
                    : mgr.activeLots().stream().filter(l -> l.seller.equals(p.getUniqueId())).toList();
            int from = gui.page * 45;
            int idx = from + slot;
            if (idx >= lots.size()) return;
            Lot lot = lots.get(idx);
            // админ-модерация: ПКМ по любому лоту — принудительное снятие
            if (e.getClick() == ClickType.RIGHT && p.hasPermission("loom.admin")
                    && gui.mode == AuctionGUI.Mode.MARKET && !lot.seller.equals(p.getUniqueId())) {
                String err = mgr.adminRemove(lot.id);
                if (err != null) p.sendMessage(err);
                else p.sendMessage(core.msg(p.getUniqueId(), "auction.admin.removed"));
                gui.open(p);
                return;
            }
            if (e.getClick() == ClickType.RIGHT && gui.mode == AuctionGUI.Mode.MINE) {
                String err = mgr.cancel(p.getUniqueId(), lot);
                if (err != null) p.sendMessage(err);
                else core.lang().send(p, "auction.cancelled");
                gui.open(p);
                return;
            }
            if (gui.mode != AuctionGUI.Mode.MARKET) return;
            // выкуп, если есть и ставка минимальна
            if (lot.buyout > 0 && core.api().has(p.getUniqueId(), lot.buyout)) {
                String err = mgr.buyout(p.getUniqueId(), p.getName(), lot);
                if (err == null) { gui.open(p); return; }
            }
            // ставка по минимальному шагу
            double next = mgr.minNextBid(lot);
            if (!core.api().has(p.getUniqueId(), next)) {
                p.sendMessage(core.msg(p.getUniqueId(), "auction.no-money"));
                return;
            }
            String err = mgr.bid(p.getUniqueId(), p.getName(), lot, next);
            if (err != null) p.sendMessage(err);
            gui.open(p);
        }
    }
}
