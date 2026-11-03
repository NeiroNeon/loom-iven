package ru.loom.auction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** GUI аукциона: витрина, мои лоты, выдача. Инвентари с holder-навигацией. */
public final class AuctionGUI implements InventoryHolder {

    public enum Mode { MARKET, MINE, CLAIMS }

    public final UUID viewer;
    public final Mode mode;
    public int page;
    private final Inventory inv;

    public AuctionGUI(UUID viewer, Mode mode, int page) {
        this.viewer = viewer;
        this.mode = mode;
        this.page = page;
        this.inv = Bukkit.createInventory(this, 54, title());
    }

    private String title() {
        var core = LoomCorePlugin.get();
        UUID v = viewer;
        String lang = core != null ? core.api().lang(v) : "ru";
        String base = switch (mode) {
            case MARKET -> core != null ? core.lang().raw(lang, "auction.gui.market") : "Аукцион";
            case MINE -> core != null ? core.lang().raw(lang, "auction.gui.mine") : "Мои лоты";
            case CLAIMS -> core != null ? core.lang().raw(lang, "auction.gui.claims") : "Получение";
        };
        return base + " §7(" + (page + 1) + ")";
    }

    public void open(Player p) {
        var core = LoomCorePlugin.get();
        var mgr = AuctionPlugin.get().auction();
        inv.clear();

        List<Lot> lots;
        if (mode == Mode.MARKET) lots = mgr.activeLots();
        else if (mode == Mode.MINE) lots = mgr.activeLots().stream()
                .filter(l -> l.seller.equals(viewer)).toList();
        else lots = new ArrayList<>(); // выдача рисуется отдельно

        int perPage = 45;
        int from = page * perPage;
        for (int i = 0; i < perPage && from + i < lots.size(); i++) {
            inv.setItem(i, decorate(lots.get(from + i)));
        }
        // выдача: предметы как предметы, клик по предмету — получить его
        if (mode == Mode.CLAIMS) {
            int slot = 0;
            for (UUID lid : mgr.claimsOf(viewer)) {
                if (slot >= perPage) break;
                Lot l = mgr.findLot(lid);
                if (l != null) {
                    inv.setItem(slot, decorateClaim(l));
                    slot++;
                }
            }
            if (slot == 0) {
                inv.setItem(22, nav(Material.BARRIER, "§7Пока нечего получать",
                        "§8Лоты появятся здесь после покупки/возврата."));
            }
        }
        // нижняя панель
        if (page > 0) inv.setItem(45, nav(Material.ARROW, "§e← " + (core != null ? core.lang().raw(core.api().lang(viewer), "gui.prev") : "Назад")));
        if (from + perPage < lots.size()) inv.setItem(53, nav(Material.ARROW, "§e" + (core != null ? core.lang().raw(core.api().lang(viewer), "gui.next") : "Далее") + " →"));
        inv.setItem(49, nav(Material.BARRIER, "§c" + (core != null ? core.lang().raw(core.api().lang(viewer), "gui.close") : "Закрыть")));

        if (mode == Mode.CLAIMS) {
            var core2 = LoomCorePlugin.get();
            double payout = mgr.payoutOf(viewer);
            if (payout > 0) {
                inv.setItem(40, nav(Material.GOLD_INGOT, "§6" + core2.api().fmt(payout) + " монет",
                        "§7Нажмите, чтобы получить."));
            }
        }
        // филлер пустых ячеек
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, ru.loom.core.gui.GuiUtil.filler());
        }
        p.openInventory(inv);
    }

    private ItemStack decorateClaim(Lot lot) {
        ItemStack it = lot.item.clone();
        ItemMeta m = it.getItemMeta();
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        lore.add("§8──────────");
        lore.add("§aЛКМ — получить предмет");
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack decorate(Lot lot) {
        ItemStack it = lot.item.clone();
        ItemMeta m = it.getItemMeta();
        var core = LoomCorePlugin.get();
        var mgr = AuctionPlugin.get().auction();
        String lang = core != null ? core.api().lang(viewer) : "ru";
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        lore.add("§8──────────");
        lore.add("§7Продавец: §f" + lot.sellerName);
        if (lot.buyout > 0) lore.add("§7Выкуп: §6" + core.api().fmt(lot.buyout));
        if (lot.bidder == null) lore.add("§7Ставок нет §8— §7старт §6" + core.api().fmt(lot.startPrice));
        else {
            lore.add("§7Ставка: §6" + core.api().fmt(lot.currentBid) + " §8(" + lot.bidderName + ")");
            lore.add("§7Следующая: §6" + core.api().fmt(mgr.minNextBid(lot)));
        }
        lore.add("§7Окончание: §f" + lot.timeLeft(lang) + "§8" + (lot.extensions > 0 ? " §7(+" + lot.extensions + ")" : ""));
        lore.add("§8ЛКМ — ставка/выкуп");
        if (lot.seller.equals(viewer)) lore.add("§8ПКМ — отменить");
        Player vp = Bukkit.getPlayer(viewer);
        if (vp != null && vp.hasPermission("loom.admin")) {
            lore.add("§cПКМ (админ) — снять лот принудительно");
        }
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack nav(Material mat, String name, String... extra) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        if (extra.length > 0) m.setLore(List.of(extra));
        it.setItemMeta(m);
        return it;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
