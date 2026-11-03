package ru.loom.core.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Хелперы сборки предметов GUI (имя + лор) и прогресс-бара. */
public final class GuiUtil {

    private GuiUtil() { }

    /** Предмет GUI: имя и лор без чат-префикса (ключи gui.*). */
    public static ItemStack item(Player p, Material mat, String lang, String nameKey, String... loreKeys) {
        return item(p, mat, lang, nameKey, List.of(loreKeys));
    }

    public static ItemStack item(Player p, Material mat, String lang, String nameKey, List<String> loreKeys) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m == null) return it;
        m.setDisplayName(t(p, lang, nameKey));
        List<String> lore = new ArrayList<>();
        for (String k : loreKeys) lore.add(t(p, lang, k));
        if (!lore.isEmpty()) m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    /** Сырой предмет: имя из ключа + готовый лор. */
    public static ItemStack raw(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m == null) return it;
        m.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    /** Строка из языкового файла (ключи gui./raw./ladder./cosmetic. — без префикса). */
    public static String t(Player p, String lang, String key, String... kv) {
        var core = LoomCorePlugin.get();
        if (core == null) return key;
        return core.lang().raw(lang, key, kv);
    }

    /** Прогресс-бар 0..1 из символов █ и ░. */
    public static String bar(double ratio, int width) {
        if (ratio < 0) ratio = 0;
        if (ratio > 1) ratio = 1;
        int filled = (int) Math.round(ratio * width);
        return "█".repeat(filled) + "░".repeat(Math.max(0, width - filled));
    }

    /** Локализованное имя материала (для подсказок), en — англ. каталог. */
    public static String materialName(Material mat, String lang) {
        String s = mat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return "en".equals(lang) ? s : s;
    }

    // ---------------- v1.8: филлер, навигация, время, окружение ----------------

    /** Права администрации/владельца проекта. */
    public static final String ADMIN_PERM = "loom.admin";
    public static final String OWNER_PERM = "loom.owner";

    /** Филлер пустых ячеек: тонированное стекло (TINTED_GLASS) без имени — клики отменяются GuiListener'ом. */
    public static ItemStack filler() {
        ItemStack it = new ItemStack(Material.TINTED_GLASS);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            it.setItemMeta(m);
        }
        return it;
    }

    /** Заполнить все пустые слоты диапазона филлером. */
    public static void fillEmpty(Inventory inv, int from, int toExcl) {
        ItemStack f = filler();
        for (int i = from; i < toExcl && i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, f);
        }
    }

    /** Заполнить весь инвентарь филлером (после расстановки предметов). */
    public static void fillAll(Inventory inv) {
        fillEmpty(inv, 0, inv.getSize());
    }

    /** Длительность человекочитаемо: «2д 5ч 3м» / «2d 5h 3m». */
    public static String dur(long ms, String lang) {
        if (ms < 0) ms = 0;
        long totalMin = ms / 60000L;
        long d = totalMin / 1440, h = (totalMin % 1440) / 60, m = totalMin % 60;
        boolean en = "en".equals(lang);
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append(en ? "d " : "д ");
        if (h > 0) sb.append(h).append(en ? "h " : "ч ");
        if (sb.length() == 0 || m > 0) sb.append(m).append(en ? "m" : "м");
        return sb.toString().trim();
    }

    /** Внутриигровое время мира: тики → ЧЧ:ММ. */
    public static String clock(long time) {
        long hours = (time / 1000L + 6) % 24;
        long minutes = (time % 1000L) * 60L / 1000L;
        return String.format("%02d:%02d", hours, minutes);
    }

    /** Направление взгляда: С/В/Ю/З со стрелкой. */
    public static String dir(float yaw, String lang) {
        float a = (yaw % 360 + 360) % 360;
        if (a >= 315 || a < 45) return "en".equals(lang) ? "S ▼" : "Ю ▼";
        if (a < 135) return "en".equals(lang) ? "W ◀" : "З ◀";
        if (a < 225) return "en".equals(lang) ? "N ▲" : "С ▲";
        return "en".equals(lang) ? "E ▶" : "В ▶";
    }

    private static final String[][] BIOMES_RU = {
            {"plains", "Равнина"}, {"forest", "Лес"}, {"birch_forest", "Берёзовая роща"},
            {"dark_forest", "Тёмный лес"}, {"flower_forest", "Цветущий лес"}, {"old_growth_birch_forest", "Древняя роща"},
            {"taiga", "Тайга"}, {"snowy_taiga", "Снежная тайга"}, {"desert", "Пустыня"},
            {"savanna", "Саванна"}, {"jungle", "Джунгли"}, {"swamp", "Болото"},
            {"mangrove_swamp", "Мангровые заросли"}, {"badlands", "Бедленды"}, {"ocean", "Океан"},
            {"deep_ocean", "Глубокий океан"}, {"warm_ocean", "Тёплый океан"}, {"frozen_ocean", "Замёрзший океан"},
            {"river", "Река"}, {"beach", "Пляж"}, {"stony_shore", "Каменистый берег"},
            {"mountains", "Горы"}, {"windswept_hills", "Ветреные холмы"}, {"snowy_plains", "Снежная равнина"},
            {"ice_spikes", "Ледяные пики"}, {"sunflower_plains", "Подсолнуховая равнина"}, {"mushroom_fields", "Грибные поля"},
            {"cherry_grove", "Вишнёвая роща"}, {"pale_garden", "Бледный сад"}, {"the_nether", "Незер"},
            {"crimson_forest", "Багровый лес"}, {"warped_forest", "Искажённый лес"}, {"soul_sand_valley", "Долина песка душ"},
            {"basalt_deltas", "Базальтовые дельты"}, {"the_end", "Край"}, {"lush_caves", "Пышные пещеры"},
            {"dripstone_caves", "Пещеры сталактитов"}, {"deep_dark", "Глубокая тьма"}
    };

    /** Красивое имя биома (RU-словарь для популярных, иначе англ. каталог). */
    public static String biome(org.bukkit.block.Biome b, String lang) {
        if (b == null) return "—";
        String key = b.getKey().getKey();
        if (!"en".equals(lang)) {
            for (String[] e : BIOMES_RU) if (e[0].equals(key)) return e[1];
        }
        String s = key.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Красивое имя мира. */
    public static String worldName(String name, String lang) {
        boolean en = "en".equals(lang);
        return switch (name) {
            case "world" -> en ? "Overworld" : "Верхний мир";
            case "world_nether" -> en ? "Nether" : "Незер";
            case "world_the_end" -> en ? "The End" : "Край";
            default -> name;
        };
    }
}
