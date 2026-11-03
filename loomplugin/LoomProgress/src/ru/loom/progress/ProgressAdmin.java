package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.BedrockCheck;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.gui.LoomMenu;
import ru.loom.core.gui.MenuGUI;
import ru.loom.core.menu.MenuAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Админ-секции LoomProgress (админ-панель ядра): пропуск, ключи, сбор,
 * ивенты, квартал. Выбор игрока — AdminPick; для Bedrock — формы.
 */
public final class ProgressAdmin {

    private ProgressAdmin() { }

    // ================= выбор игрока =================

    /** Универсальный выбор онлайн-игрока (сундук-GUI / форма Bedrock). */
    public static void pickPlayer(Player admin, String titleKey, Consumer<UUID> then) {
        if (BedrockCheck.isBedrock(admin)) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(admin.getUniqueId());
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            List<String> buttons = new ArrayList<>();
            for (Player t : online) buttons.add(t.getName());
            buttons.add(core.lang().raw(lang, "gui.close"));
            ru.loom.core.bedrock.BedrockUi.buttons(admin,
                    GuiUtil.t(admin, lang, titleKey), "", buttons, idx -> {
                        if (idx >= 0 && idx < online.size()) {
                            UUID id = online.get(idx).getUniqueId();
                            Bukkit.getScheduler().runTask(ProgressPlugin.get(), () -> then.accept(id));
                        }
                    });
            return;
        }
        new Pick(admin, titleKey, then).show();
    }

    private static final class Pick implements LoomMenu {
        private final Inventory inv;
        private final Consumer<UUID> then;
        private final Player admin;

        Pick(Player admin, String titleKey, Consumer<UUID> then) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(admin.getUniqueId());
            this.admin = admin;
            this.then = then;
            this.inv = Bukkit.createInventory(this, 54, GuiUtil.t(admin, lang, titleKey));
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            online.sort(Comparator.comparing(Player::getName));
            int slot = 0;
            for (Player t : online) {
                if (slot >= 45) break;
                var head = GuiUtil.raw(Material.PLAYER_HEAD, "§f" + t.getName(), List.of());
                var meta = head.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
                    sm.setOwningPlayer(t);
                    head.setItemMeta(meta = sm);
                }
                inv.setItem(slot++, head);
            }
            inv.setItem(52, GuiUtil.item(admin, Material.BARRIER, lang, "gui.close"));
            GuiUtil.fillAll(inv);
        }

        void show() {
            admin.openInventory(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            if (slot == 52) return true;
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            online.sort(Comparator.comparing(Player::getName));
            if (slot < online.size()) {
                UUID id = online.get(slot).getUniqueId();
                p.closeInventory();
                Bukkit.getScheduler().runTask(ProgressPlugin.get(), () -> then.accept(id));
            }
            return false;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    // ================= пропуск =================

    public static void passAdmin(Player admin) {
        pickPlayer(admin, "gui.admin.progress.pass-title", target -> {
            if (BedrockCheck.isBedrock(admin)) {
                formActions(admin, target, List.of(
                        "§eХроно-предмет ×1", "§eХроно-предмет ×5",
                        "§6Выдать донат-ветку", "§cСбросить пропуск"), i -> passAction(admin, target, i));
                return;
            }
            new SimpleActions(admin, "gui.admin.progress.pass-title", List.of(
                    new Act(Material.CLOCK, "§eХроно-предмет ×1", a -> passAction(admin, target, 0)),
                    new Act(Material.CLOCK, "§eХроно-предмет ×5", a -> passAction(admin, target, 1)),
                    new Act(Material.AMETHYST_SHARD, "§6Выдать донат-ветку", a -> passAction(admin, target, 2)),
                    new Act(Material.BARRIER, "§cСбросить пропуск", a -> passAction(admin, target, 3))
            ), () -> passAdmin(admin)).show();
        });
    }

    private static void passAction(Player admin, UUID target, int i) {
        var core = LoomCorePlugin.get();
        var sp = ProgressPlugin.get().season();
        switch (i) {
            case 0 -> sp.adminGrantChrono(target, 1);
            case 1 -> sp.adminGrantChrono(target, 5);
            case 2 -> sp.adminGrantDonate(target);
            case 3 -> sp.adminReset(target);
        }
        admin.sendMessage("§aГотово (" + nameOf(target) + ").");
    }

    // ================= ключи =================

    public static void keysAdmin(Player admin) {
        pickPlayer(admin, "gui.admin.progress.keys-title", target -> {
            if (BedrockCheck.isBedrock(admin)) {
                formActions(admin, target, List.of("§dКлюч ×1", "§dКлюч ×5", "§dКлюч ×20"),
                        i -> ProgressPlugin.get().keys().giveKey(target, new int[]{1, 5, 20}[i]));
                return;
            }
            new SimpleActions(admin, "gui.admin.progress.keys-title", List.of(
                    new Act(Material.TRIPWIRE_HOOK, "§dКлюч ×1", a -> ProgressPlugin.get().keys().giveKey(target, 1)),
                    new Act(Material.TRIPWIRE_HOOK, "§dКлюч ×5", a -> ProgressPlugin.get().keys().giveKey(target, 5)),
                    new Act(Material.TRIPWIRE_HOOK, "§dКлюч ×20", a -> ProgressPlugin.get().keys().giveKey(target, 20))
            ), () -> keysAdmin(admin)).show();
        });
    }

    // ================= сбор =================

    public static void fundAdmin(Player admin) {
        if (BedrockCheck.isBedrock(admin)) {
            formActions(admin, null, List.of(
                    "§eПродлить +24 ч", "§eПродлить +72 ч",
                    "§aФорс-финиш с выплатами", "§cОтмена с возвратом"), i -> fundAction(admin, i));
            return;
        }
        new SimpleActions(admin, "gui.admin.progress.fund-title", List.of(
                new Act(Material.CLOCK, "§eПродлить дедлайн +24 ч", a -> fundAction(admin, 0)),
                new Act(Material.CLOCK, "§eПродлить дедлайн +72 ч", a -> fundAction(admin, 1)),
                new Act(Material.EMERALD, "§aФорс-финиш с выплатами", a -> fundAction(admin, 2)),
                new Act(Material.BARRIER, "§cОтмена с полным возвратом", a -> fundAction(admin, 3))
        ), () -> fundAdmin(admin)).show();
    }

    private static void fundAction(Player admin, int i) {
        var fm = ProgressPlugin.get().fund();
        switch (i) {
            case 0 -> fm.adminExtend(24);
            case 1 -> fm.adminExtend(72);
            case 2 -> fm.finish();
            case 3 -> fm.cancel(admin.getUniqueId(), admin.getName());
        }
        admin.sendMessage("§aГотово.");
    }

    // ================= ивенты =================

    public static void eventsAdmin(Player admin) {
        if (BedrockCheck.isBedrock(admin)) {
            formActions(admin, null, List.of(
                    "§aФорс-старт: двойной дроп", "§aФорс-старт: сбор руды",
                    "§aФорс-старт: жатва", "§6Форс-старт: охота",
                    "§6Форс-старт: босс-битва", "§eФорс-финиш"), i -> eventAction(admin, i));
            return;
        }
        new SimpleActions(admin, "gui.admin.progress.events-title", List.of(
                new Act(Material.WHEAT, "§aФорс-старт: двойной дроп", a -> eventAction(admin, 0)),
                new Act(Material.IRON_PICKAXE, "§aФорс-старт: сбор руды", a -> eventAction(admin, 1)),
                new Act(Material.BREAD, "§aФорс-старт: жатва", a -> eventAction(admin, 2)),
                new Act(Material.IRON_SWORD, "§6Форс-старт: охота", a -> eventAction(admin, 3)),
                new Act(Material.WITHER_SKELETON_SKULL, "§6Форс-старт: босс-битва", a -> eventAction(admin, 4)),
                new Act(Material.EMERALD, "§eФорс-финиш с выплатами", a -> eventAction(admin, 5))
        ), () -> eventsAdmin(admin)).show();
    }

    private static void eventAction(Player admin, int i) {
        var ev = ProgressPlugin.get().events();
        String[] kinds = {"double_drop", "ore_collect", "harvest", "hunt", "boss_battle"};
        if (i < 5) {
            if (ev.forceStart(kinds[i], 3)) admin.sendMessage("§aИвент запущен: " + kinds[i]);
            else admin.sendMessage("§cНеизвестный вид ивента.");
        } else {
            ev.forceFinish();
            admin.sendMessage("§aИвент завершён, награды выданы.");
        }
    }

    // ================= квартал =================

    public static void quarterAdmin(Player admin) {
        if (BedrockCheck.isBedrock(admin)) {
            admin.sendMessage("§7Квартал: управление участками — в Java-клиенте (/меню → Админ).");
            return;
        }
        new Plots(admin).show();
    }

    private static final class Plots implements LoomMenu {
        private final Inventory inv;
        private final List<Integer> ids = new ArrayList<>();
        private final Player admin;

        Plots(Player admin) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(admin.getUniqueId());
            this.admin = admin;
            this.inv = Bukkit.createInventory(this, 54,
                    GuiUtil.t(admin, lang, "gui.admin.progress.quarter-title"));
            var owners = ProgressPlugin.get().storage().plotOwners;
            int slot = 0;
            for (int i = 0; i < 60 && slot < 45; i++) {
                UUID owner = owners.get(i);
                if (owner == null) continue;
                ids.add(i);
                inv.setItem(slot++, GuiUtil.raw(Material.GOLD_BLOCK,
                        "§fУчасток §e#" + (i + 1),
                        List.of("§7Владелец: §f" + nameOf(owner),
                                "§cПКМ — снять владельца")));
            }
            if (ids.isEmpty()) {
                inv.setItem(22, GuiUtil.raw(Material.BARRIER, "§7Участки пока не куплены", List.of()));
            }
            inv.setItem(52, GuiUtil.item(admin, Material.BARRIER, lang, "gui.close"));
            GuiUtil.fillAll(inv);
        }

        void show() {
            admin.openInventory(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p, ClickType type) {
            if (slot == 52) return true;
            if (slot < ids.size() && type.isRightClick()) {
                int plotId = ids.get(slot);
                ProgressPlugin.get().storage().plotOwners.remove(plotId);
                ProgressPlugin.get().storage().saveQuarter();
                p.sendMessage("§aВладелец участка #" + (plotId + 1) + " снят.");
                quarterAdmin(p);
            }
            return false;
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            return handleClick(slot, p, ClickType.LEFT);
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    // ================= утилиты =================

    private record Act(Material icon, String name, Consumer<Player> run) { }

    /** Маленькое меню действий (27 слотов). */
    private static final class SimpleActions implements LoomMenu {
        private final Inventory inv;
        private final List<Act> acts;
        private final Runnable back;
        private final Player admin;

        SimpleActions(Player admin, String titleKey, List<Act> acts, Runnable back) {
            var core = LoomCorePlugin.get();
            String lang = core.api().lang(admin.getUniqueId());
            this.admin = admin;
            this.acts = acts;
            this.back = back;
            this.inv = Bukkit.createInventory(this, 27, GuiUtil.t(admin, lang, titleKey));
            int slot = 10;
            for (Act a : acts) {
                inv.setItem(slot, GuiUtil.raw(a.icon(), a.name(), List.of("§aКлик — выполнить")));
                slot += 2;
            }
            inv.setItem(22, GuiUtil.raw(Material.SPECTRAL_ARROW, GuiUtil.t(admin, lang, "gui.back"), List.of()));
            inv.setItem(26, GuiUtil.item(admin, Material.BARRIER, lang, "gui.close"));
            GuiUtil.fillAll(inv);
        }

        void show() {
            admin.openInventory(inv);
        }

        @Override
        public boolean handleClick(int slot, Player p) {
            if (slot == 26) return true;
            if (slot == 22) {
                back.run();
                return false;
            }
            int idx = (slot - 10) / 2;
            if (slot >= 10 && (slot - 10) % 2 == 0 && idx >= 0 && idx < acts.size()) {
                acts.get(idx).run().accept(p);
                p.closeInventory();
            }
            return false;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static void formActions(Player admin, UUID target, List<String> labels, Consumer<Integer> run) {
        List<String> buttons = new ArrayList<>(labels);
        buttons.add("§7← Назад");
        ru.loom.core.bedrock.BedrockUi.buttons(admin,
                labels.get(0).replaceAll("§.", "").split(" ")[0], "", buttons, idx -> {
                    if (idx >= 0 && idx < labels.size()) {
                        Bukkit.getScheduler().runTask(ProgressPlugin.get(), () -> run.accept(idx));
                    }
                });
    }

    private static String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }

    // ================= регистрация =================

    public static void register(ProgressPlugin plugin) {
        var core = LoomCorePlugin.get();
        if (core == null) return;

        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.pass"; }
            @Override public int order() { return 30; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.NETHER_STAR; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.admin.progress.pass"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.progress.pass-lore"));
            }
            @Override public void click(Player p) { passAdmin(p); }
        });

        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.keys"; }
            @Override public int order() { return 31; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.TRIPWIRE_HOOK; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.admin.progress.keys"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.progress.keys-lore"));
            }
            @Override public void click(Player p) { keysAdmin(p); }
        });

        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.fund"; }
            @Override public int order() { return 32; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.GOLDEN_CHESTPLATE; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.admin.progress.fund"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.progress.fund-lore"));
            }
            @Override public void click(Player p) { fundAdmin(p); }
        });

        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.events"; }
            @Override public int order() { return 33; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.WHEAT; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.admin.progress.events"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.progress.events-lore"));
            }
            @Override public void click(Player p) { eventsAdmin(p); }
        });

        core.menus().registerAdmin(new MenuAction() {
            @Override public String id() { return "admin.quarter"; }
            @Override public int order() { return 34; }
            @Override public boolean adminOnly() { return true; }
            @Override public Material icon(Player p) { return Material.GOLD_BLOCK; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.admin.progress.quarter"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.admin.progress.quarter-lore"));
            }
            @Override public void click(Player p) { quarterAdmin(p); }
        });
    }
}
