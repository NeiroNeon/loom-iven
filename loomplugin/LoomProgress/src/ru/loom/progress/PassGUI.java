package ru.loom.progress;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.bedrock.BedrockUi;
import ru.loom.core.gui.BedrockCheck;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.gui.LoomMenu;
import ru.loom.core.gui.MenuGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Полный GUI сезонного пропуска (v1.8): все 30 дней обеих веток на экране,
 * лор каждого дня = награда, статус, окно и оставшееся время. Сгоревший день
 * восстанавливается хроно-предметом кликом. Bedrock — форма со списком дней.
 */
public final class PassGUI implements LoomMenu {

    private final UUID viewer;
    private final boolean donateView;
    private final Inventory inv;

    private PassGUI(Player p, boolean donateView) {
        var core = LoomCorePlugin.get();
        this.viewer = p.getUniqueId();
        this.donateView = donateView;
        String lang = core.api().lang(viewer);
        this.inv = Bukkit.createInventory(this, 54, GuiUtil.t(p, lang, "gui.pass.title",
                "%season%", ProgressPlugin.get().storage().seasonId));
        redraw(p);
    }

    public static void open(Player p) {
        if (BedrockCheck.isBedrock(p)) {
            form(p);
            return;
        }
        p.openInventory(new PassGUI(p, false).inv);
    }

    private void redraw(Player p) {
        inv.clear();
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        var sp = plugin.season();
        String lang = core.api().lang(viewer);
        boolean donate = sp.hasDonateTrack(viewer);

        // дни 1..30 в слотах 0..29
        for (int day = 1; day <= SeasonPassManager.SEASON_DAYS; day++) {
            inv.setItem(day - 1, dayItem(p, day, donateView));
        }
        // тумблер ветки / покупка
        if (donateView && donate) {
            inv.setItem(48, GuiUtil.raw(Material.GOLD_INGOT,
                    GuiUtil.t(p, lang, "gui.pass.track-donate"),
                    List.of(GuiUtil.t(p, lang, "gui.pass.viewing-donate"))));
        } else if (donateView) {
            inv.setItem(48, GuiUtil.raw(Material.GOLD_INGOT,
                    GuiUtil.t(p, lang, "gui.pass.track-donate"),
                    List.of(GuiUtil.t(p, lang, "gui.pass.need-buy"))));
        } else {
            inv.setItem(48, GuiUtil.raw(Material.EMERALD,
                    GuiUtil.t(p, lang, "gui.pass.track-free"),
                    List.of(GuiUtil.t(p, lang, "gui.pass.viewing-free"))));
        }
        // покупка донат-ветки
        if (!donate) {
            inv.setItem(50, GuiUtil.raw(Material.AMETHYST_SHARD,
                    GuiUtil.t(p, lang, "gui.panel.pass.buy"),
                    List.of(GuiUtil.t(p, lang, "gui.panel.pass.buy-lore", "%price%",
                            core.api().fmt(plugin.getConfig().getDouble("season.donate-price-coins", 25000))))));
        }
        // инфо: сезон/день/хроно
        int day = sp.dayOf(viewer);
        List<String> info = new ArrayList<>();
        info.add(GuiUtil.t(p, lang, "gui.panel.pass.day", "%day%", String.valueOf(day)));
        info.add(GuiUtil.t(p, lang, "gui.pass.chrono", "%n%", String.valueOf(sp.chronoOf(viewer))));
        info.add(GuiUtil.t(p, lang, "gui.pass.rules"));
        inv.setItem(49, GuiUtil.raw(Material.NETHER_STAR,
                GuiUtil.t(p, lang, "gui.pass.info"), info));
        inv.setItem(45, GuiUtil.raw(Material.ARROW, GuiUtil.t(p, lang, "gui.menu.open"), List.of()));
        inv.setItem(52, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        if (donate) {
            inv.setItem(53, GuiUtil.raw(Material.GOLD_BLOCK,
                    GuiUtil.t(p, lang, "gui.pass.switch-donate"),
                    List.of(GuiUtil.t(p, lang, "gui.pass.switch-hint"))));
        }
        GuiUtil.fillAll(inv);
    }

    private org.bukkit.inventory.ItemStack dayItem(Player p, int day, boolean donate) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        var sp = plugin.season();
        String lang = core.api().lang(viewer);
        String rid = (donate ? "d" : "f") + day;
        var state = sp.stateOf(viewer, day, donate);

        Material icon;
        String statusLine;
        List<String> lore = new ArrayList<>();
        double coins = plugin.getConfig().getDouble("season.rewards.coins-per-day", 300);
        lore.add("§7" + GuiUtil.t(p, lang, "gui.pass.reward-coins", "%amount%", core.api().fmt(coins)));
        if (day % 7 == 0) lore.add("§d" + GuiUtil.t(p, lang, "gui.pass.reward-key"));
        if (donate && day >= 15) lore.add("§5" + GuiUtil.t(p, lang, "gui.pass.reward-cosmetic"));
        if (donate && sp.isChronoDay(day)) lore.add("§e" + GuiUtil.t(p, lang, "gui.pass.reward-chrono"));

        switch (state) {
            case CLAIMED -> {
                icon = Material.LIME_STAINED_GLASS_PANE;
                statusLine = "§2✔ " + GuiUtil.t(p, lang, "gui.pass.claimed");
            }
            case OPEN -> {
                long left = sp.windowEnd(viewer, day) - System.currentTimeMillis();
                icon = donate ? Material.GOLD_INGOT : Material.LIME_DYE;
                statusLine = "§a" + GuiUtil.t(p, lang, "gui.pass.open")
                        + " §7(" + GuiUtil.t(p, lang, "gui.pass.left", "%time%",
                        ru.loom.core.gui.GuiUtil.dur(left, lang)) + ")";
                lore.add("§a" + GuiUtil.t(p, lang, "gui.pass.click-claim"));
            }
            case MISSED -> {
                icon = Material.GRAY_DYE;
                statusLine = "§c" + GuiUtil.t(p, lang, "gui.pass.missed");
                int chrono = sp.chronoOf(viewer);
                if (chrono > 0) {
                    lore.add("§e" + GuiUtil.t(p, lang, "gui.pass.restore-hint",
                            "%n%", String.valueOf(chrono)));
                } else {
                    lore.add("§8" + GuiUtil.t(p, lang, "gui.pass.restore-none"));
                }
            }
            default -> {
                long in = sp.windowStart(viewer, day) - System.currentTimeMillis();
                icon = Material.LIGHT_GRAY_DYE;
                statusLine = "§8" + GuiUtil.t(p, lang, "gui.pass.future",
                        "%time%", ru.loom.core.gui.GuiUtil.dur(Math.max(0, in), lang));
            }
        }
        lore.add(1, statusLine); // статус вторым элементом (после разделителя-заголовка нет)
        lore.add(0, "§f" + GuiUtil.t(p, lang, "gui.pass.day-n", "%n%", String.valueOf(day)));
        return GuiUtil.raw(icon, GuiUtil.t(p, lang, "gui.pass.day-name",
                "%n%", String.valueOf(day)), lore);
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        var sp = plugin.season();
        String lang = core.api().lang(p.getUniqueId());
        if (slot == 52) return true;
        if (slot == 45) {
            MenuGUI.open(p);
            return false;
        }
        if (slot == 48) {
            // тумблер: просмотр донат-ветки (только владельцам; иначе — подсказка о покупке)
            if (!donateView && sp.hasDonateTrack(viewer)) {
                p.openInventory(new PassGUI(p, true).inv);
            } else if (donateView) {
                p.openInventory(new PassGUI(p, false).inv);
            }
            return false;
        }
        if (slot == 53 && sp.hasDonateTrack(viewer)) {
            p.openInventory(new PassGUI(p, true).inv);
            return false;
        }
        if (slot == 50 && !sp.hasDonateTrack(viewer)) {
            if (sp.buyDonateTrack(viewer, p.getName())) {
                p.sendMessage(core.msg(viewer, "pass.bought-you"));
            } else {
                p.sendMessage(core.msg(viewer, "pass.buy-fail"));
            }
            p.openInventory(new PassGUI(p, false).inv);
            return false;
        }
        if (slot >= 0 && slot < SeasonPassManager.SEASON_DAYS) {
            int day = slot + 1;
            String rid = (donateView ? "d" : "f") + day;
            var state = sp.stateOf(viewer, day, donateView);
            switch (state) {
                case OPEN -> {
                    String err = sp.claim(viewer, rid);
                    p.sendMessage(err != null ? err : core.msg(viewer, "pass.claimed"));
                }
                case MISSED -> {
                    String err = sp.restore(viewer, rid);
                    p.sendMessage(err != null ? err : core.msg(viewer, "pass.restored"));
                }
                case FUTURE -> p.sendMessage(core.msg(viewer, "pass.day-not-reached",
                        "%day%", String.valueOf(day), "%time%", "—"));
                case CLAIMED -> p.sendMessage(core.msg(viewer, "pass.already"));
            }
            p.openInventory(new PassGUI(p, donateView).inv);
        }
        return false;
    }

    // ---------------- Bedrock ----------------

    private static void form(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        var sp = plugin.season();
        String lang = core.api().lang(p.getUniqueId());
        boolean donate = sp.hasDonateTrack(p.getUniqueId());
        StringBuilder sb = new StringBuilder();
        sb.append(core.lang().raw(lang, "gui.panel.pass.day",
                "%day%", String.valueOf(sp.dayOf(p.getUniqueId())))).append('\n');
        sb.append(core.lang().raw(lang, "gui.pass.chrono",
                "%n%", String.valueOf(sp.chronoOf(p.getUniqueId())))).append("\n\n");
        List<String> buttons = new ArrayList<>();
        // открытые дни обеих веток
        for (int day = 1; day <= SeasonPassManager.SEASON_DAYS; day++) {
            if (sp.stateOf(p.getUniqueId(), day, false) == SeasonPassManager.DayState.OPEN) {
                buttons.add("§a[" + day + "] " + core.lang().raw(lang, "gui.pass.open"));
            }
            if (donate && sp.stateOf(p.getUniqueId(), day, true) == SeasonPassManager.DayState.OPEN) {
                buttons.add("§6[" + day + "•] " + core.lang().raw(lang, "gui.pass.open"));
            }
        }
        if (!donate) {
            buttons.add(core.lang().raw(lang, "gui.panel.pass.buy") + " — "
                    + core.api().fmt(plugin.getConfig().getDouble("season.donate-price-coins", 25000)));
        }
        buttons.add(core.lang().raw(lang, "gui.close"));
        BedrockUi.buttons(p, core.lang().raw(lang, "gui.pass.title",
                        "%season%", plugin.storage().seasonId),
                sb.toString(), buttons, idx -> {
                    // разбор: сначала открытые f-дни, затем d-дни, затем покупка
                    List<String> actions = new ArrayList<>();
                    for (int day = 1; day <= SeasonPassManager.SEASON_DAYS; day++) {
                        if (sp.stateOf(p.getUniqueId(), day, false) == SeasonPassManager.DayState.OPEN) {
                            actions.add("f" + day);
                        }
                        if (donate && sp.stateOf(p.getUniqueId(), day, true) == SeasonPassManager.DayState.OPEN) {
                            actions.add("d" + day);
                        }
                    }
                    if (idx < actions.size()) {
                        String rid = actions.get(idx);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            String err = sp.claim(p.getUniqueId(), rid);
                            p.sendMessage(err != null ? err : core.msg(p.getUniqueId(), "pass.claimed"));
                        });
                    } else if (!donate && idx == actions.size()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (sp.buyDonateTrack(p.getUniqueId(), p.getName())) {
                                p.sendMessage(core.msg(p.getUniqueId(), "pass.bought-you"));
                            } else {
                                p.sendMessage(core.msg(p.getUniqueId(), "pass.buy-fail"));
                            }
                        });
                    }
                });
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
