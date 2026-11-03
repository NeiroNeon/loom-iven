package ru.loom.trader;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Пул заказов Ивлия (59 позиций, 3 категории).
 *
 * Баланс: награда = кол-во × базовая_цена × бонус(1.25–1.4), округлённая до 5.
 * Базовая цена — «рыночная» стоимость единицы; бонус сверху делает квест
 * выгоднее обычной продажи, чтобы игрок ХОТЕЛ брать заказ.
 *
 * Категории: resources (ресурсы), potions (зелья), items (предметы).
 * potionKey — для зелий (ключ типа зелья из реестра, напр. "strong_healing").
 * id "ench_book" — любая зачарованная книга (hasStoredEnchants).
 */
public final class QuestPool {

    public record Quest(String id, String cat, Material mat, int amount, long reward, String potionKey) { }

    public static final List<Quest> POOL = new ArrayList<>();

    private static void add(String id, String cat, Material mat, int amount, long reward) {
        POOL.add(new Quest(id, cat, mat, amount, reward, null));
    }

    private static void addPotion(String id, String potionKey, int amount, long reward) {
        POOL.add(new Quest(id, "potions", Material.POTION, amount, reward, potionKey));
    }

    static {
        // ---------- фермерство и еда ----------
        add("wheat",     "resources", Material.WHEAT,        1024, 110);
        add("carrot",    "resources", Material.CARROT,       768,  90);
        add("potato",    "resources", Material.POTATO,       768,  90);
        add("beetroot",  "resources", Material.BEETROOT,     512,  70);
        add("pumpkin",   "resources", Material.PUMPKIN,      320,  75);
        add("melon",     "resources", Material.MELON_SLICE,  576,  90);
        add("cane",      "resources", Material.SUGAR_CANE,   512,  95);
        add("egg",       "resources", Material.EGG,          256,  85);
        add("honeycomb", "resources", Material.HONEYCOMB,    128,  90);
        // ---------- добыча и руды ----------
        add("iron",      "resources", Material.IRON_INGOT,   192,  840);
        add("copper",    "resources", Material.COPPER_INGOT, 384,  450);
        add("gold",      "resources", Material.GOLD_INGOT,   96,   720);
        add("redstone",  "resources", Material.REDSTONE,     384,  385);
        add("lapis",     "resources", Material.LAPIS_LAZULI, 256,  385);
        add("coal",      "resources", Material.COAL,         512,  450);
        add("diamond",   "resources", Material.DIAMOND,      32,   1165);
        add("emerald",   "resources", Material.EMERALD,      64,   750);
        add("quartz",    "resources", Material.QUARTZ,       192,  530);
        add("obsidian",  "resources", Material.OBSIDIAN,     96,   560);
        add("amethyst",  "resources", Material.AMETHYST_SHARD, 128, 415);
        // ---------- мобы и опасности ----------
        add("string",    "resources", Material.STRING,         256, 200);
        add("leather",   "resources", Material.LEATHER,        192, 250);
        add("bone",      "resources", Material.BONE,           384, 190);
        add("pearl",     "resources", Material.ENDER_PEARL,    32,  500);
        add("rod",       "resources", Material.BLAZE_ROD,      24,  500);
        add("gunpowder", "resources", Material.GUNPOWDER,      192, 400);
        add("slime",     "resources", Material.SLIME_BALL,     128, 365);
        add("magma",     "resources", Material.MAGMA_CREAM,    96,  400);
        add("ghast",     "resources", Material.GHAST_TEAR,     16,  475);
        add("membrane",  "resources", Material.PHANTOM_MEMBRANE, 24, 470);
        // ---------- прочие ресурсы ----------
        add("ink",       "resources", Material.INK_SAC,        192, 275);
        add("glowink",   "resources", Material.GLOW_INK_SAC,   96,  375);
        add("snowball",  "resources", Material.SNOWBALL,       1024, 65);
        add("clay",       "resources", Material.CLAY_BALL,     256, 115);
        // ---------- зельеварение ----------
        addPotion("pot_heal2",    "strong_healing",      6,  325);
        addPotion("pot_str2",     "strong_strength",     6,  350);
        addPotion("pot_fire",     "fire_resistance",     8,  380);
        addPotion("pot_night",    "night_vision",        12, 320);
        addPotion("pot_speed2",   "strong_swiftness",    8,  330);
        addPotion("pot_reg2",     "strong_regeneration", 6,  360);
        addPotion("pot_invis",    "invisibility",        4,  300);
        addPotion("pot_slowfall", "slow_falling",        4,  320);
        // ---------- ценные предметы ----------
        add("ench_book",  "items", Material.ENCHANTED_BOOK,    4,  600);
        add("gapple",     "items", Material.GOLDEN_APPLE,      16, 730);
        add("gcarrot",    "items", Material.GOLDEN_CARROT,     128, 915);
        add("cake",       "items", Material.CAKE,              16, 375);
        add("xp_bottle",  "items", Material.EXPERIENCE_BOTTLE, 64, 540);
        add("nametag",    "items", Material.NAME_TAG,          4,  470);
        add("arrow",      "items", Material.ARROW,             512, 235);
        add("tnt",        "items", Material.TNT,               48, 310);
        add("eye",        "items", Material.ENDER_EYE,         12, 435);
        add("bookshelf",  "items", Material.BOOKSHELF,         32, 330);
        add("beehive",    "items", Material.BEEHIVE,           8,  415);
        add("lodestone",  "items", Material.LODESTONE,         4,  470);
        add("saddle",     "items", Material.SADDLE,            3,  430);
        add("heart_sea",  "items", Material.HEART_OF_THE_SEA,  2,  810);
        add("shell",      "items", Material.NAUTILUS_SHELL,    12, 470);
        add("sponge",     "items", Material.SPONGE,            16, 520);
        add("rocket",     "items", Material.FIREWORK_ROCKET,   64, 150);
    }

    public static Quest byId(String id) {
        for (Quest q : POOL) if (q.id().equalsIgnoreCase(id)) return q;
        return null;
    }

    private QuestPool() { }
}
