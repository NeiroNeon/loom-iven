package ru.loom.core.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.AdminGUI;
import ru.loom.core.gui.BedrockCheck;
import ru.loom.core.gui.CommandPages;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.gui.ListMenu;
import ru.loom.core.gui.MenuGUI;
import ru.loom.core.gui.ProfileGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Пункты меню ядра: профиль, ранги, дом, спавн, команды, админ.
 * Плюс реестр команд (страницы «Команды») и админ-секции ядра
 * (привилегии, экономика).
 */
public final class CoreActions {

    private static final String[] LADDER = {"novice", "resident", "keeper", "legend"};

    private CoreActions() { }

    public static void register(LoomCorePlugin plugin) {
        // ---------- пункты главного меню ----------
        plugin.menus().register(new MenuAction() {
            @Override public String id() { return "profile"; }
            @Override public int order() { return 10; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.PLAYER_HEAD; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.menu.item.profile");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.profile-lore"));
            }
            @Override public void click(Player p) { ProfileGUI.open(p); }
            @Override public String usage() { return "/профиль"; }
        });

        plugin.menus().register(new MenuAction() {
            @Override public String id() { return "ranks"; }
            @Override public int order() { return 20; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.EXPERIENCE_BOTTLE; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.menu.item.ranks");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.ranks-lore"));
            }
            @Override public void click(Player p) { ranksGui(p); }
        });

        plugin.menus().register(new MenuAction() {
            @Override public String id() { return "home"; }
            @Override public int order() { return 30; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.ENDER_PEARL; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.menu.item.home");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.home-lore"));
            }
            @Override public void click(Player p) { p.performCommand("home"); }
        });

        plugin.menus().register(new MenuAction() {
            @Override public String id() { return "spawn"; }
            @Override public int order() { return 31; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.COMPASS; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.menu.item.spawn");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.spawn-lore"));
            }
            @Override public void click(Player p) { p.performCommand("spawn"); }
        });

        // «Команды» — страница категорий вместо текста в чате
        plugin.menus().register(new MenuAction() {
            @Override public String id() { return "help"; }
            @Override public int order() { return 90; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.BOOK; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.menu.item.help");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.help-lore"));
            }
            @Override public void click(Player p) { CommandPages.open(p); }
        });

        // Админ-панель (GUI) вместо текстового списка
        plugin.menus().register(new MenuAction() {
            @Override public String id() { return "admin"; }
            @Override public int order() { return 95; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.COMMAND_BLOCK; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.menu.item.admin");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.menu.item.admin-lore"));
            }
            @Override public void click(Player p) { AdminGUI.open(p); }
        });

        // ---------- страницы «Команды»: ядро ----------
        command(plugin, "cmd.menu", "core", 10, Material.CHEST,
                p -> MenuGUI.open(p), "/меню", null);
        command(plugin, "cmd.profile", "core", 11, Material.PLAYER_HEAD,
                p -> ProfileGUI.open(p), "/профиль", "gui.menu.item.profile");
        command(plugin, "cmd.lang", "core", 12, Material.REPEATING_COMMAND_BLOCK,
                p -> toggleLang(p), "/язык ru|en", null);
        command(plugin, "cmd.home", "core", 13, Material.ENDER_PEARL,
                p -> p.performCommand("home"), "/дом", null);
        command(plugin, "cmd.spawn", "core", 14, Material.COMPASS,
                p -> p.performCommand("spawn"), "/спавн", null);

        // ---------- страницы «Команды»: экономика ----------
        command(plugin, "cmd.coins-top", "economy", 20, Material.GOLD_INGOT,
                p -> ListMenu.open(p, "gui.cmd.coins-top-title", topCoins(p), null),
                "/монеты топ", null);
        command(plugin, "cmd.hours", "economy", 21, Material.CLOCK,
                p -> ListMenu.open(p, "gui.cmd.hours-title", hoursLines(p), null),
                "/часы", null);

        // ---------- админ-секции ядра ----------
        plugin.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.privileges"; }
            @Override public int order() { return 10; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.WRITABLE_BOOK; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.admin.priv");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.priv-lore"));
            }
            @Override public void click(Player p) { AdminGUI.privileges(p); }
        });

        plugin.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.economy"; }
            @Override public int order() { return 20; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.GOLD_NUGGET; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui.admin.economy");
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.economy-lore"));
            }
            @Override public void click(Player p) { ecoPlayers(p); }
        });
    }

    // ---------------- утилиты ----------------

    private interface Click { void run(Player p); }

    private static void command(LoomCorePlugin plugin, String id, String cat, int order,
                                Material icon, Click click) {
        plugin.menus().registerCommand(new MenuAction() {
            @Override public String id() { return id; }
            @Override public int order() { return order; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return icon; }
            @Override public String name(Player p, String lang) {
                return GuiUtil.t(p, lang, "gui." + id);
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui." + id + "-lore"));
            }
            @Override public void click(Player p) { click.run(p); }
            @Override public String category() { return cat; }
        });
    }

    private static void command(LoomCorePlugin plugin, String id, String cat, int order,
                                Material icon, Click click, String usage, String menuNameKey) {
        plugin.menus().registerCommand(new MenuAction() {
            @Override public String id() { return id; }
            @Override public int order() { return order; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return icon; }
            @Override public String name(Player p, String lang) {
                return menuNameKey != null ? GuiUtil.t(p, lang, menuNameKey)
                        : GuiUtil.t(p, lang, "gui." + id);
            }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui." + id + "-lore"));
            }
            @Override public void click(Player p) { click.run(p); }
            @Override public String category() { return cat; }
            @Override public String usage() { return usage; }
        });
    }

    private static void toggleLang(Player p) {
        var core = LoomCorePlugin.get();
        String next = "ru".equals(core.api().lang(p.getUniqueId())) ? "en" : "ru";
        core.api().setLang(p.getUniqueId(), next);
        p.sendMessage(core.msg(p.getUniqueId(), "lang.set", "%lang%",
                "ru".equals(next) ? "RU" : "EN"));
    }

    private static List<String> topCoins(Player p) {
        var core = LoomCorePlugin.get();
        List<String> out = new ArrayList<>();
        var top = core.economy().top(10);
        if (top.isEmpty()) {
            out.add(core.msg(p.getUniqueId(), "coins.top.empty"));
            return out;
        }
        int i = 1;
        for (var e : top.entrySet()) {
            String name = org.bukkit.Bukkit.getOfflinePlayer(e.getKey()).getName();
            out.add(core.msg(p.getUniqueId(), "coins.top.line",
                    "%pos%", String.valueOf(i++), "%name%", name == null ? "—" : name,
                    "%amount%", core.api().fmt(e.getValue())));
        }
        return out;
    }

    private static List<String> hoursLines(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        long min = core.api().playtimeMinutes(p.getUniqueId());
        List<String> out = new ArrayList<>();
        out.add(core.msg(p.getUniqueId(), "hours.self.hours", "%hours%", String.valueOf(min / 60),
                "%minutes%", String.valueOf(min % 60)));
        out.add("§7" + core.lang().raw(lang, "ladder." + core.api().ladderRank(p.getUniqueId())));
        return out;
    }

    private static void ecoPlayers(Player p) {
        if (BedrockCheck.isBedrock(p)) {
            p.sendMessage("§7Экономика: используйте консольные /loom give|take|set");
            return;
        }
        AdminGUI.privilegesEco(p);
    }

    /** GUI рангов: лестница активности + косметические ранги. */
    private static void ranksGui(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        long minutes = core.api().playtimeMinutes(p.getUniqueId());
        String current = core.api().ladderRank(p.getUniqueId());
        SimpleView view = new SimpleView(p);
        var inv = Bukkit.createInventory(view, 27, core.lang().raw(lang, "gui.menu.ranks-header"));
        int slot = 10;
        for (String k : LADDER) {
            long h = core.ranks().ladderThresholdMinutes(k) / 60;
            boolean cur = k.equals(current);
            List<String> lore = new ArrayList<>();
            lore.add("§7" + h + " ч");
            lore.add(cur ? "§a▶ " + core.lang().raw(lang, "gui.ranks.current")
                    : "§8• " + core.lang().raw(lang, "gui.ranks.locked"));
            inv.setItem(slot, GuiUtil.raw(
                    cur ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                    core.lang().raw(lang, "ladder." + k), lore));
            slot += 2;
        }
        int cs = 19;
        for (String k : new String[]{"veteran", "elder", "virtuoso", "icon"}) {
            long h = core.ranks().cosmeticThresholdMinutes(k) / 60;
            if (h == Long.MAX_VALUE / 60) continue;
            boolean reached = minutes >= core.ranks().cosmeticThresholdMinutes(k);
            inv.setItem(cs++, GuiUtil.raw(Material.ENDER_PEARL,
                    "§d" + core.lang().raw(lang, "cosmetic." + k),
                    List.of("§7" + h + " ч", reached
                            ? "§a✔ " + core.lang().raw(lang, "gui.ranks.reached")
                            : "§8• " + core.lang().raw(lang, "gui.ranks.locked"))));
        }
        inv.setItem(26, GuiUtil.raw(Material.ARROW,
                core.lang().raw(lang, "gui.menu.open"), List.of()));
        GuiUtil.fillAll(inv);
        view.show(inv);
    }

    /** Минимальный holder: просмотр без действий, любой клик закрывает. */
    private static final class SimpleView implements ru.loom.core.gui.LoomMenu {
        private final Player viewer;

        SimpleView(Player p) {
            this.viewer = p;
        }

        void show(org.bukkit.inventory.Inventory inv) {
            viewer.openInventory(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            return true; // любой клик закрывает
        }

        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
}
