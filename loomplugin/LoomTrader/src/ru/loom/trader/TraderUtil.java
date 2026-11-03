package ru.loom.trader;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Утилиты: формат чисел, названия предметов, проверка/изъятие предметов под заказ. */
final class TraderUtil {

    private static final DecimalFormat NUM = num();

    private static DecimalFormat num() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator(' ');
        return new DecimalFormat("#,###", s);
    }

    /** 2000000 → "2 000 000". */
    static String fmt(long v) { return NUM.format(v); }

    /** Доля в процентах: "12.5" / "7". */
    static String share(double pct) {
        if (Math.abs(pct - Math.rint(pct)) < 0.05) return String.valueOf((long) Math.rint(pct));
        return String.format(Locale.US, "%.1f", pct);
    }

    /** Локализованное название материала (ключ item.name.<MATERIAL>). */
    static String itemName(String lang, Material mat) {
        var core = ru.loom.core.LoomCorePlugin.get();
        if (core == null) return mat.name();
        return core.lang().raw(lang, "raw.item.name." + mat.name());
    }

    /** Название заказа: для зелий — potion.<key>, иначе item.name.<material>. */
    static String questName(String lang, QuestPool.Quest q) {
        var core = ru.loom.core.LoomCorePlugin.get();
        if (core == null) return q.potionKey() != null ? q.potionKey() : q.mat().name();
        String key = q.potionKey() != null ? "raw.potion." + q.potionKey() : "raw.item.name." + q.mat().name();
        return core.lang().raw(lang, key);
    }

    /** "1 д 5 ч" / "5 ч 20 мин" / "45 мин". */
    static String timeLeft(long ms) {
        long min = Math.max(0, ms / 60_000L);
        long days = min / 1440;
        long hours = (min % 1440) / 60;
        long mins = min % 60;
        if (days > 0) return days + " д " + hours + " ч";
        if (hours > 0) return hours + " ч " + mins + " мин";
        return mins + " мин";
    }

    /** Подходит ли предмет под заказ (материал / тип зелья / зачарованная книга). */
    static boolean matches(ItemStack it, QuestPool.Quest q) {
        if (it == null || !it.hasItemMeta()) {
            return it != null && q.potionKey() == null && !"ench_book".equals(q.id())
                    && it.getType() == q.mat();
        }
        if (q.potionKey() != null) {
            if (it.getType() != Material.POTION || !(it.getItemMeta() instanceof PotionMeta pm)) return false;
            var base = pm.getBasePotionType();
            return base != null && base.getKey().getKey().equalsIgnoreCase(q.potionKey());
        }
        if ("ench_book".equals(q.id())) {
            return it.getType() == Material.ENCHANTED_BOOK
                    && it.getItemMeta() instanceof EnchantmentStorageMeta esm
                    && esm.hasStoredEnchants();
        }
        return it.getType() == q.mat();
    }

    /** Сколько подходящих предметов в инвентаре. */
    static int count(org.bukkit.entity.Player p, QuestPool.Quest q) {
        int n = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) {
            if (matches(it, q)) n += it.getAmount();
        }
        return n;
    }

    /** Изъять до want штук; вернуть сколько изъято. */
    static int take(org.bukkit.entity.Player p, QuestPool.Quest q, int want) {
        int removed = 0;
        var inv = p.getInventory();
        ItemStack[] content = inv.getStorageContents();
        for (int i = 0; i < content.length && removed < want; i++) {
            ItemStack it = content[i];
            if (!matches(it, q)) continue;
            int need = want - removed;
            int take = Math.min(need, it.getAmount());
            if (take >= it.getAmount()) inv.setItem(i, null);
            else it.setAmount(it.getAmount() - take);
            removed += take;
        }
        return removed;
    }

    private TraderUtil() { }
}
