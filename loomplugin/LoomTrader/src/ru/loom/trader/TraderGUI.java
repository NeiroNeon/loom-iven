package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.GuiUtil;

import java.util.List;
import java.util.UUID;

/**
 * GUI доски Ивлия (54 слота): 10 заказов на фиксированных слотах,
 * книга-инструкция (49), барьер-закрыть (53). Клик по заказу — мгновенная сдача.
 */
final class TraderGUI implements InventoryHolder {

    /** Слоты доски: 10,11,12,13,14,15,16 (верх) + 19,20,21 (низ). */
    static final int[] QUEST_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
    static final int SLOT_INFO = 49;
    static final int SLOT_CLOSE = 53;

    private final UUID viewer;
    private final Inventory inv;

    TraderGUI(UUID viewer) {
        this.viewer = viewer;
        var core = LoomCorePlugin.get();
        int done = LoomTraderPlugin.get().daily().doneToday(viewer);
        String title = core != null
                ? core.lang().raw(core.api().lang(viewer), "raw.trader.title",
                        "%done%", String.valueOf(done),
                        "%count%", String.valueOf(LoomTraderPlugin.get().dailyCount()))
                : "Доска заказов";
        this.inv = Bukkit.createInventory(this, 54, title);
    }

    @Override
    public Inventory getInventory() { return inv; }

    void open(Player p) {
        redraw();
        GuiUtil.fillAll(inv);
        p.openInventory(inv);
    }

    private void redraw() {
        inv.clear();
        var core = LoomCorePlugin.get();
        var trader = LoomTraderPlugin.get();
        String lang = core != null ? core.api().lang(viewer) : "ru";
        List<QuestPool.Quest> quests = trader.daily().today();
        var done = trader.storage().doneToday(viewer);

        for (int i = 0; i < QUEST_SLOTS.length && i < quests.size(); i++) {
            QuestPool.Quest q = quests.get(i);
            boolean isDone = done.contains(q.id());
            inv.setItem(QUEST_SLOTS[i], questIcon(q, lang, isDone));
        }
        inv.setItem(SLOT_INFO, nav(Material.BOOK,
                core != null ? core.lang().raw(lang, "raw.trader.info-name") : "Как сдавать",
                core != null ? core.lang().raw(lang, "raw.trader.info-lore").split("\n") : new String[0]));
        inv.setItem(SLOT_CLOSE, nav(Material.BARRIER,
                core != null ? core.lang().raw(lang, "raw.trader.close") : "Закрыть", new String[0]));
    }

    private ItemStack questIcon(QuestPool.Quest q, String lang, boolean isDone) {
        var core = LoomCorePlugin.get();
        ItemStack it = new ItemStack(q.mat());
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        String name = TraderUtil.questName(lang, q);
        meta.setDisplayName(core != null
                ? core.lang().raw(lang, "raw.trader.quest-name",
                        "%quest%", name, "%amount%", String.valueOf(q.amount()))
                : name);
        String cat = core != null ? core.lang().raw(lang, "raw.trader.cat." + q.cat()) : q.cat();
        Player viewerPlayer = Bukkit.getPlayer(viewer);
        String have = viewerPlayer != null
                ? String.valueOf(TraderUtil.count(viewerPlayer, q)) : "0";
        meta.setLore(List.of(
                core != null ? core.lang().raw(lang, "raw.trader.lore-need",
                        "%amount%", String.valueOf(q.amount()), "%item%", name) : "",
                core != null ? core.lang().raw(lang, "raw.trader.lore-have",
                        "%have%", have, "%amount%", String.valueOf(q.amount())) : "",
                core != null ? core.lang().raw(lang, "raw.trader.lore-reward",
                        "%reward%", TraderUtil.fmt(LoomTraderPlugin.get().daily().rewardOf(q))) : "",
                core != null ? core.lang().raw(lang, "raw.trader.lore-category", "%category%", cat) : "",
                isDone
                        ? core.lang().raw(lang, "raw.trader.lore-done")
                        : core.lang().raw(lang, "raw.trader.lore-click")));
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack nav(Material mat, String name, String[] lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(List.of(lore));
            it.setItemMeta(meta);
        }
        return it;
    }

    /** Заказ по слоту клика (или null). */
    QuestPool.Quest questAt(int slot) {
        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            if (QUEST_SLOTS[i] == slot) {
                List<QuestPool.Quest> list = LoomTraderPlugin.get().daily().today();
                return i < list.size() ? list.get(i) : null;
            }
        }
        return null;
    }
}
