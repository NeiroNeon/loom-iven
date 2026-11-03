package ru.loom.auction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.BedrockCheck;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.gui.ListMenu;
import ru.loom.core.menu.MenuAction;

import java.util.List;

/** Админ-GUI аукциона: модерация лотов (снять), пауза, статистика. */
public final class AuctionAdminGUI implements InventoryHolder {

    private final Inventory inv;

    private AuctionAdminGUI(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        this.inv = Bukkit.createInventory(this, 54,
                core.lang().raw(lang, "auction.admin.title"));
        redraw(p);
    }

    public static void open(Player p) {
        if (!p.hasPermission(GuiUtil.ADMIN_PERM)) return;
        if (BedrockCheck.isBedrock(p)) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(p.getUniqueId());
            var mgr = AuctionPlugin.get().auction();
            var stats = mgr.adminStats();
            ru.loom.core.bedrock.BedrockUi.buttons(p,
                    core.lang().raw(lang, "auction.admin.title"),
                    String.join("\n", stats),
                    List.of(mgr.isFrozen() ? "§aСнять паузу" : "§cВключить паузу",
                            core.lang().raw(lang, "gui.close")),
                    idx -> {
                        if (idx == 0) {
                            mgr.setFrozen(!mgr.isFrozen());
                            p.sendMessage(mgr.isFrozen()
                                    ? "§cАукцион поставлен на паузу."
                                    : "§aАукцион снова работает.");
                        }
                    });
            return;
        }
        p.openInventory(new AuctionAdminGUI(p).inv);
    }

    private void redraw(Player p) {
        inv.clear();
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        var mgr = AuctionPlugin.get().auction();
        int slot = 0;
        for (Lot lot : mgr.activeLots()) {
            if (slot >= 45) break;
            ItemStack it = lot.item.clone();
            var m = it.getItemMeta();
            java.util.ArrayList<String> lore = m.hasLore()
                    ? new java.util.ArrayList<>(m.getLore())
                    : new java.util.ArrayList<>();
            lore.add("§8──────────");
            lore.add("§7Продавец: §f" + lot.sellerName);
            lore.add("§7Ставка: §6" + core.api().fmt(lot.currentBid)
                    + (lot.bidderName != null ? " §8(" + lot.bidderName + ")" : ""));
            lore.add("§cПКМ — снять лот (ставку вернуть)");
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(slot++, it);
        }
        inv.setItem(46, GuiUtil.raw(mgr.isFrozen() ? Material.BLUE_ICE : Material.ICE,
                core.lang().raw(lang, "auction.admin.freeze"),
                List.of(core.lang().raw(lang, "auction.admin.freeze-lore"))));
        inv.setItem(48, GuiUtil.raw(Material.PAPER,
                core.lang().raw(lang, "auction.admin.stats"), List.of()));
        inv.setItem(49, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        GuiUtil.fillAll(inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    /** Регистрация админ-секции в ядре. */
    public static void register(AuctionPlugin plugin) {
        var core = LoomCorePlugin.get();
        if (core == null) return;
        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.auction"; }
            @Override public int order() { return 40; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.GOLD_BLOCK; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "auction.admin.menu");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "auction.admin.menu-lore"));
            }
            @Override public void click(Player p) { AuctionAdminGUI.open(p); }
        });
    }

    /** Обработка кликов (вызывается из AuctionListeners). */
    public static void handleClick(InventoryClickEvent e, Player p, AuctionPlugin plugin) {
        int slot = e.getSlot();
        var mgr = plugin.auction();
        var core = LoomCorePlugin.get();
        if (slot == 49) { p.closeInventory(); return; }
        if (slot == 46) {
            mgr.setFrozen(!mgr.isFrozen());
            p.sendMessage(mgr.isFrozen() ? "§cАукцион поставлен на паузу."
                    : "§aАукцион снова работает.");
            if (e.getInventory().getHolder() instanceof AuctionAdminGUI g) g.redraw(p);
            return;
        }
        if (slot == 48) {
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () ->
                    ListMenu.open(p, "auction.admin.stats-title", mgr.adminStats(), null));
            return;
        }
        if (slot < 45) {
            var lots = mgr.activeLots();
            int idx = slot;
            if (idx >= lots.size()) return;
            if (e.getClick() == ClickType.RIGHT) {
                String err = mgr.adminRemove(lots.get(idx).id);
                if (err != null) p.sendMessage(err);
                else p.sendMessage(core.msg(p.getUniqueId(), "auction.admin.removed"));
                if (e.getInventory().getHolder() instanceof AuctionAdminGUI g) g.redraw(p);
            }
        }
    }
}
