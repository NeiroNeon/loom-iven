package ru.loom.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.bedrock.BedrockUi;
import ru.loom.core.perm.Lp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Админ-панель (/меню → Админ): секции плагинов + привилегии и экономика ядра.
 * Привилегии: онлайн-игроки → карточка → выбор группы (ЛКМ навсегда, ПКМ на
 * срок) → выдача через консольные команды LuckPerms. Без права loom.admin
 * панель недоступна (кнопка в меню тоже скрыта).
 */
public final class AdminGUI implements LoomMenu {

    /** Группы для выдачи: id → (иконка, тип: LADDER/ OVERLAY). */
    record Group(String id, Material icon, boolean ladder, String prefixView) { }

    static final Group[] GROUPS = {
            new Group("default", Material.GRAY_DYE, true, "&8[&7Новичок&8]"),
            new Group("resident", Material.LIGHT_BLUE_DYE, true, "&8[&bПостоялец&8]"),
            new Group("keeper", Material.ORANGE_DYE, true, "&8[&6Хранитель&8]"),
            new Group("legend", Material.MAGENTA_DYE, true, "&8[&dЛегенда&8]"),
            new Group("sortannik", Material.LIME_DYE, false, "&8[&2Соратник&8]"),
            new Group("mecenat", Material.CYAN_DYE, false, "&8[&3Меценат&8]"),
            new Group("patricij", Material.BLUE_DYE, false, "&8[&9Патриций&8]"),
            new Group("fantom", Material.PURPLE_DYE, false, "&8[&5Фантом&8]"),
            new Group("mentor", Material.GREEN_DYE, false, "&8[&2Ментор&8]"),
            new Group("events", Material.PINK_DYE, false, "&8[&5Ивенты&8]"),
            new Group("moderator", Material.LIGHT_GRAY_DYE, false, "&8[&9Модер&8]"),
            new Group("admin", Material.RED_DYE, false, "&8[&cАдмин&8]"),
            new Group("owner", Material.NETHERITE_INGOT, false, "&8[&4Владелец&8]"),
    };

    private final Inventory inv;
    private final UUID target; // null — список игроков
    private final boolean eco; // true — клик по игроку открывает экономику

    private AdminGUI(Player p, UUID target, boolean eco) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        this.target = target;
        this.eco = eco;
        this.inv = Bukkit.createInventory(this, 54,
                target == null ? GuiUtil.t(p, lang, "gui.admin.players.title")
                        : GuiUtil.t(p, lang, "gui.admin.card.title", "%player%", nameOf(target)));
        redraw(p, lang);
    }

    private AdminGUI(Player p, UUID target) {
        this(p, target, false);
    }

    /** Экономика: выбор игрока → пресеты выдачи/снятия/обнуления. */
    public static void privilegesEco(Player p) {
        if (!p.hasPermission(GuiUtil.ADMIN_PERM)) return;
        if (BedrockCheck.isBedrock(p)) {
            p.sendMessage("§7Экономика: используйте консольные /loom give|take|set");
            return;
        }
        p.openInventory(new AdminGUI(p, null, true).inv);
    }

    /** Вход в панель привилегий (ядро). */
    public static void privileges(Player p) {
        if (BedrockCheck.isBedrock(p)) {
            formPlayers(p);
            return;
        }
        p.openInventory(new AdminGUI(p, null).inv);
    }

    /** Вход в админ-панель (хаб секций). Скрыта без loom.admin. */
    public static void open(Player p) {
        if (!p.hasPermission(GuiUtil.ADMIN_PERM)) return;
        if (BedrockCheck.isBedrock(p)) {
            formHub(p);
            return;
        }
        p.openInventory(new Hub(p).inv);
    }

    /** Хаб: секции админ-панели, зарегистрированные плагинами. */
    private static final class Hub implements LoomMenu {
        private final Inventory inv;

        Hub(Player p) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(p.getUniqueId());
            this.inv = Bukkit.createInventory(this, 54, GuiUtil.t(p, lang, "gui.admin.title"));
            int slot = 0;
            for (var s : core.menus().visibleAdmin(p)) {
                if (slot >= 45) break;
                inv.setItem(slot++, GuiUtil.raw(s.icon(p), s.name(p, lang), s.lore(p, lang)));
            }
            inv.setItem(49, GuiUtil.item(p, Material.ARROW, lang, "gui.menu.open"));
            inv.setItem(52, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
            GuiUtil.fillAll(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            var core = LoomCorePlugin.get();
            if (slot == 52) return true;
            if (slot == 49) {
                MenuGUI.open(p);
                return false;
            }
            if (slot < 45) {
                List<ru.loom.core.menu.MenuAction> sections = core.menus().visibleAdmin(p);
                if (slot < sections.size()) {
                    var s = sections.get(slot);
                    p.closeInventory();
                    Bukkit.getScheduler().runTask(core, () -> {
                        try {
                            s.click(p);
                        } catch (Exception ex) {
                            core.getLogger().warning("Админ: ошибка секции " + s.id() + ": " + ex);
                        }
                    });
                }
            }
            return false;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    /** Хаб для Bedrock. */
    private static void formHub(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        List<ru.loom.core.menu.MenuAction> sections = core.menus().visibleAdmin(p);
        List<String> buttons = new ArrayList<>();
        for (var s : sections) buttons.add(s.name(p, lang));
        BedrockUi.buttons(p, GuiUtil.t(p, lang, "gui.admin.title"), "", buttons, idx -> {
            if (idx >= 0 && idx < sections.size()) {
                var s = sections.get(idx);
                Bukkit.getScheduler().runTask(core, () -> {
                    try {
                        s.click(p);
                    } catch (Exception ignored) {
                        // ответ формы не должен ронять сервер
                    }
                });
            }
        });
    }

    private static String nameOf(UUID id) {
        Player pl = Bukkit.getPlayer(id);
        return pl != null ? pl.getName() : id.toString().substring(0, 8);
    }

    private void redraw(Player p, String lang) {
        inv.clear();
        if (target == null) {
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            online.sort(Comparator.comparing(Player::getName));
            int slot = 0;
            for (Player t : online) {
                if (slot >= 45) break;
                List<String> lore = List.of(
                        "§7" + GuiUtil.t(p, lang, "gui.admin.group.current") + ": §f"
                                + GuiUtil.t(p, lang, "group." + Lp.primaryGroup(t.getUniqueId())),
                        "§a" + GuiUtil.t(p, lang, "gui.cmd.open-hint"));
                var head = GuiUtil.raw(Material.PLAYER_HEAD, "§f" + t.getName(), lore);
                var meta = head.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
                    sm.setOwningPlayer(t);
                    head.setItemMeta(meta = sm);
                }
                inv.setItem(slot++, head);
            }
            inv.setItem(49, GuiUtil.raw(Material.ARROW, GuiUtil.t(p, lang, "gui.admin.back-panel"), List.of()));
        } else {
            Player t = Bukkit.getPlayer(target);
            String group = Lp.primaryGroup(target);
            List<String> card = new ArrayList<>();
            card.add("§7" + GuiUtil.t(p, lang, "gui.admin.group.current") + ": §f"
                    + GuiUtil.t(p, lang, "group." + group));
            if (t != null) {
                var core = LoomCorePlugin.get();
                card.add("§e" + GuiUtil.t(p, lang, "gui.profile.time-hours") + " §f"
                        + (core.api().playtimeMinutes(target) / 60) + " ч");
                card.add("§6" + GuiUtil.t(p, lang, "gui.profile.coins") + " §f"
                        + core.api().fmt(core.api().coins(target)));
            }
            inv.setItem(4, GuiUtil.raw(Material.PLAYER_HEAD, "§f" + nameOf(target), card));
            inv.setItem(11, GuiUtil.item(p, Material.WRITABLE_BOOK, lang,
                    "gui.admin.change-group", "gui.admin.change-group-lore"));
            inv.setItem(13, GuiUtil.item(p, Material.SUNFLOWER, lang, "gui.admin.refresh"));
            inv.setItem(15, GuiUtil.item(p, Material.ARROW, lang, "gui.admin.back-list"));
        }
        GuiUtil.fillAll(inv);
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        var core = LoomCorePlugin.get();
        if (slot == 49 && target == null) {
            AdminGUI.open(p); // назад в панель секций
            return false;
        }
        if (target == null) {
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            online.sort(Comparator.comparing(Player::getName));
            if (slot < online.size()) {
                p.openInventory(eco
                        ? new EcoGUI(p, online.get(slot).getUniqueId()).inv
                        : new AdminGUI(p, online.get(slot).getUniqueId()).inv);
            }
            return false;
        }
        if (slot == 13) {
            p.openInventory(new AdminGUI(p, target).inv);
            return false;
        }
        if (slot == 15) {
            p.openInventory(new AdminGUI(p, null).inv);
            return false;
        }
        if (slot == 11) {
            p.openInventory(new GroupGUI(p, target, 0).inv);
        }
        return false;
    }

    /** Выбор группы. */
    private static final class GroupGUI implements LoomMenu {
        private final UUID target;
        private final Inventory inv;

        GroupGUI(Player p, UUID target, int pageUnused) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(p.getUniqueId());
            this.target = target;
            this.inv = Bukkit.createInventory(this, 54,
                    GuiUtil.t(p, lang, "gui.admin.groups.title", "%player%", nameOf(target)));
            int slot = 0;
            for (Group g : GROUPS) {
                if (slot >= 45) break;
                List<String> lore = List.of(
                        "§7" + GuiUtil.t(p, lang, "gui.admin.prefix-preview", "%prefix%", g.prefixView()),
                        g.ladder() ? "§a" + GuiUtil.t(p, lang, "gui.admin.group.set-ladder")
                                : "§a" + GuiUtil.t(p, lang, "gui.admin.group.set-overlay"),
                        "§e" + GuiUtil.t(p, lang, "gui.admin.group.temp"),
                        "§8" + GuiUtil.t(p, lang, "gui.admin.group.id", "%id%", g.id()));
                inv.setItem(slot++, GuiUtil.raw(g.icon(), GuiUtil.t(p, lang, "group." + g.id()), lore));
            }
            inv.setItem(49, GuiUtil.raw(Material.SPECTRAL_ARROW, GuiUtil.t(p, lang, "gui.back"), List.of()));
            GuiUtil.fillAll(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            return handleClick(slot, p, org.bukkit.event.inventory.ClickType.LEFT);
        }

        @Override
        public boolean handleClick(int slot, Player p, org.bukkit.event.inventory.ClickType type) {
            if (slot == 49) {
                p.openInventory(new AdminGUI(p, target).inv);
                return false;
            }
            if (slot < GROUPS.length) {
                Group g = GROUPS[slot];
                if (type.isRightClick()) {
                    p.openInventory(new DurationGUI(p, target, g).inv);
                } else {
                    dispatch(p, target, g, 0);
                    p.openInventory(new AdminGUI(p, target).inv);
                }
            }
            return false;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    /** Срок выдачи: навсегда / 7 дней / 30 дней. */
    private static final class DurationGUI implements LoomMenu {
        private final UUID target;
        private final Group group;
        private final Inventory inv;

        DurationGUI(Player p, UUID target, Group group) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(p.getUniqueId());
            this.target = target;
            this.group = group;
            this.inv = Bukkit.createInventory(this, 27, GuiUtil.t(p, lang, "gui.admin.duration.title"));
            inv.setItem(10, GuiUtil.item(p, Material.BOOK, lang, "gui.admin.duration.perm"));
            inv.setItem(12, GuiUtil.item(p, Material.CLOCK, lang, "gui.admin.duration.7"));
            inv.setItem(14, GuiUtil.item(p, Material.CLOCK, lang, "gui.admin.duration.30"));
            inv.setItem(16, GuiUtil.raw(Material.BARRIER,
                    GuiUtil.t(p, lang, "gui.admin.group.remove"), List.of()));
            inv.setItem(22, GuiUtil.raw(Material.SPECTRAL_ARROW, GuiUtil.t(p, lang, "gui.back"), List.of()));
            GuiUtil.fillAll(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            if (slot == 22) {
                p.openInventory(new GroupGUI(p, target, 0).inv);
                return false;
            }
            if (slot == 10) dispatch(p, target, group, 0);
            if (slot == 12) dispatch(p, target, group, 7);
            if (slot == 14) dispatch(p, target, group, 30);
            if (slot == 16) remove(p, target, group);
            if (slot == 10 || slot == 12 || slot == 14 || slot == 16) {
                p.openInventory(new AdminGUI(p, target).inv);
            }
            return false;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static void dispatch(Player p, UUID target, Group g, int days) {
        String name = nameOf(target);
        String cmd;
        if (days <= 0) {
            cmd = g.ladder()
                    ? "lp user " + name + " parent set " + g.id()
                    : "lp user " + name + " parent add " + g.id();
        } else {
            cmd = "lp user " + name + " parent addtemp " + g.id() + " " + days + "d";
        }
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        p.sendMessage(ok ? core.lang().raw(lang, "gui.admin.done", "%cmd%", cmd)
                : "§cLuckPerms: команда не выполнена");
    }

    private static void remove(Player p, UUID target, Group g) {
        String name = nameOf(target);
        String cmd = "lp user " + name + " parent remove " + g.id();
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        p.sendMessage(ok ? core.lang().raw(lang, "gui.admin.done", "%cmd%", cmd)
                : "§cLuckPerms: команда не выполнена");
    }

    /** Экономика: пресеты для цели. */
    private static final class EcoGUI implements LoomMenu {
        private final UUID target;
        private final Inventory inv;

        EcoGUI(Player p, UUID target) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(p.getUniqueId());
            this.target = target;
            this.inv = Bukkit.createInventory(this, 27,
                    GuiUtil.t(p, lang, "gui.admin.eco.title", "%player%", nameOf(target)));
            inv.setItem(10, GuiUtil.item(p, Material.EMERALD, lang, "gui.admin.eco.give100"));
            inv.setItem(11, GuiUtil.item(p, Material.EMERALD_BLOCK, lang, "gui.admin.eco.give1k"));
            inv.setItem(12, GuiUtil.item(p, Material.BEACON, lang, "gui.admin.eco.give10k"));
            inv.setItem(14, GuiUtil.item(p, Material.RED_DYE, lang, "gui.admin.eco.take100"));
            inv.setItem(15, GuiUtil.item(p, Material.REDSTONE, lang, "gui.admin.eco.take1k"));
            inv.setItem(16, GuiUtil.item(p, Material.REDSTONE_BLOCK, lang, "gui.admin.eco.take10k"));
            inv.setItem(22, GuiUtil.item(p, Material.BARRIER, lang, "gui.admin.eco.zero"));
            inv.setItem(26, GuiUtil.raw(Material.SPECTRAL_ARROW, GuiUtil.t(p, lang, "gui.back"), List.of()));
            GuiUtil.fillAll(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            var core = LoomCorePlugin.get();
            if (slot == 26) {
                p.openInventory(new AdminGUI(p, null, true).inv);
                return false;
            }
            double amount = switch (slot) {
                case 10 -> 100;
                case 11 -> 1000;
                case 12 -> 10000;
                case 14 -> -100;
                case 15 -> -1000;
                case 16 -> -10000;
                default -> 0;
            };
            if (slot == 22) {
                core.api().set(target, nameOf(target), 0);
            } else if (amount > 0) {
                core.api().add(target, nameOf(target), amount);
            } else if (amount < 0) {
                core.api().withdraw(target, nameOf(target), -amount);
            } else {
                return false;
            }
            String lang = core.api().lang(p.getUniqueId());
            p.sendMessage("§a" + nameOf(target) + " §7→ §f"
                    + core.api().fmt(core.api().coins(target)) + " §e◆");
            return false;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    // ---------------- Bedrock ----------------

    private static void formPlayers(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<String> buttons = new ArrayList<>();
        for (Player t : online) buttons.add(t.getName());
        buttons.add(GuiUtil.t(p, lang, "gui.admin.back-panel"));
        BedrockUi.buttons(p, GuiUtil.t(p, lang, "gui.admin.priv"), "", buttons, idx -> {
            if (idx >= 0 && idx < online.size()) {
                Bukkit.getScheduler().runTask(core, () -> formGroups(p, online.get(idx).getUniqueId()));
            }
        });
    }

    private static void formGroups(Player p, UUID target) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        List<String> buttons = new ArrayList<>();
        for (Group g : GROUPS) buttons.add(GuiUtil.t(p, lang, "group." + g.id()));
        buttons.add(GuiUtil.t(p, lang, "gui.back"));
        BedrockUi.buttons(p, GuiUtil.t(p, lang, "gui.admin.groups.title",
                "%player%", nameOf(target)), "", buttons, idx -> {
            if (idx >= 0 && idx < GROUPS.length) {
                // навсегда: лестница set, оверлеи add
                Group g = GROUPS[idx];
                String cmd = g.ladder()
                        ? "lp user " + nameOf(target) + " parent set " + g.id()
                        : "lp user " + nameOf(target) + " parent add " + g.id();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                p.sendMessage(core.lang().raw(lang, "gui.admin.done", "%cmd%", cmd));
            }
        });
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
