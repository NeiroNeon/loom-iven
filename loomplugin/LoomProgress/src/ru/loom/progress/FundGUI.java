package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.gui.LoomMenu;

import java.util.List;
import java.util.UUID;

/**
 * Панель «Общий сбор» (GUI для Java, форма для Bedrock): статус, вклад
 * пресетами, произвольная сумма, топ вкладчиков. Правила — «Безубыточный
 * участник»: фонд 120%, выплата каждого 105–150% вклада, недобор <50% — возврат.
 */
public final class FundGUI implements LoomMenu {

    static final int[] PRESET_SLOTS = {10, 11, 12, 13};
    static final int[] PRESETS = {100, 500, 1000, 5000};
    static final int SLOT_CUSTOM = 14;
    static final int SLOT_TOP = 15;
    static final int SLOT_PRESET10K = 16;
    static final int SLOT_INFO = 4;
    static final int SLOT_CLOSE = 22;

    private final UUID viewer;
    private final Inventory inv;

    public FundGUI(Player p) {
        var core = LoomCorePlugin.get();
        this.viewer = p.getUniqueId();
        String lang = core.api().lang(viewer);
        this.inv = Bukkit.createInventory(this, 27, GuiUtil.t(p, lang, "gui.fund.title"));
        redraw(p);
    }

    public static void open(Player p) {
        if (ru.loom.core.bedrock.BedrockUi.available()
                && ru.loom.core.bedrock.BedrockUi.isBedrock(p.getUniqueId())) {
            form(p);
            return;
        }
        p.openInventory(new FundGUI(p).inv);
    }

    private void redraw(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(viewer);
        var st = plugin.storage();
        inv.clear();

        if (!st.fundActive) {
            inv.setItem(SLOT_INFO, GuiUtil.raw(Material.BOOK,
                    GuiUtil.t(p, lang, "gui.fund.inactive-name"),
                    List.of(GuiUtil.t(p, lang, "gui.fund.inactive-lore"))));
            inv.setItem(SLOT_CLOSE, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
            GuiUtil.fillAll(inv);
            return;
        }

        inv.setItem(SLOT_INFO, GuiUtil.raw(Material.BOOK,
                GuiUtil.t(p, lang, "gui.fund.info-name"), infoLore(p, lang)));

        for (int i = 0; i < PRESET_SLOTS.length; i++) {
            int v = PRESETS[i];
            inv.setItem(PRESET_SLOTS[i], GuiUtil.raw(Material.GOLD_INGOT,
                    GuiUtil.t(p, lang, "gui.fund.preset-name", "%amount%", String.valueOf(v)),
                    List.of(GuiUtil.t(p, lang, "gui.fund.preset-lore"))));
        }
        inv.setItem(SLOT_CUSTOM, GuiUtil.item(p, Material.PAPER, lang,
                "gui.fund.custom", "gui.fund.custom-lore"));
        inv.setItem(SLOT_TOP, GuiUtil.item(p, Material.EMERALD, lang,
                "gui.fund.top", "gui.fund.top-lore"));
        // донат-пресет 10 000 (Патриций/Фантом)
        if (p.hasPermission("loomprogress.fund.preset10k")) {
            inv.setItem(SLOT_PRESET10K, GuiUtil.raw(Material.EMERALD_BLOCK,
                    GuiUtil.t(p, lang, "gui.fund.preset-name", "%amount%", "10000"),
                    List.of(GuiUtil.t(p, lang, "gui.fund.preset-lore"),
                            GuiUtil.t(p, lang, "gui.fund.preset-donor"))));
        }
        inv.setItem(SLOT_CLOSE, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
        GuiUtil.fillAll(inv);
    }

    private List<String> infoLore(Player p, String lang) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        var st = plugin.storage();
        long hours = Math.max(0, (st.fundEndsAt - System.currentTimeMillis()) / 3600_000L);
        return List.of(
                GuiUtil.t(p, lang, "gui.fund.info-progress",
                        "%collected%", FundManager.fmt(st.fundCollected),
                        "%target%", FundManager.fmt(st.fundTarget),
                        "%pct%", String.valueOf(plugin.fund().pct())),
                GuiUtil.t(p, lang, "gui.fund.info-left", "%hours%", String.valueOf(hours)),
                GuiUtil.t(p, lang, "gui.fund.info-my", "%amount%",
                        FundManager.fmt(st.fundContrib.getOrDefault(viewer, 0.0))),
                "§8──────────",
                GuiUtil.t(p, lang, "gui.fund.rule-rate"),
                GuiUtil.t(p, lang, "gui.fund.rule-clamp"),
                GuiUtil.t(p, lang, "gui.fund.rule-refund"));
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        if (slot == SLOT_CLOSE) return true;
        if (!plugin.storage().fundActive) return false;

        for (int i = 0; i < PRESET_SLOTS.length; i++) {
            if (slot == PRESET_SLOTS[i]) {
                String err = plugin.fund().contribute(p.getUniqueId(), p.getName(), PRESETS[i]);
                if (err != null) p.sendMessage(err);
                redraw(p);
                return false;
            }
        }
        if (slot == SLOT_CUSTOM) {
            if (ru.loom.core.bedrock.BedrockUi.isBedrock(p.getUniqueId())) {
                String lang = core.api().lang(p.getUniqueId());
                ru.loom.core.bedrock.BedrockUi.input(p,
                        core.lang().raw(lang, "gui.fund.custom-form-title"),
                        core.lang().raw(lang, "gui.fund.custom-form-label"),
                        "", (pl, value) -> contributeCustom(pl, value));
            } else {
                plugin.chats().open(p, "gui.fund.custom-prompt",
                        value -> contributeCustom(p, value));
            }
            return false;
        }
        if (slot == SLOT_PRESET10K && p.hasPermission("loomprogress.fund.preset10k")) {
            String err = plugin.fund().contribute(p.getUniqueId(), p.getName(), 10000);
            if (err != null) p.sendMessage(err);
            redraw(p);
            return false;
        }
        if (slot == SLOT_TOP) {
            openTop(p);
            return false;
        }
        return false;
    }

    static void contributeCustom(Player p, String value) {
        try {
            double amount = Double.parseDouble(value.replace(',', '.').trim());
            if (amount <= 0) return;
            String err = ProgressPlugin.get().fund().contribute(p.getUniqueId(), p.getName(), amount);
            if (err != null) p.sendMessage(err);
        } catch (NumberFormatException ex) {
            var core = LoomCorePlugin.get();
            p.sendMessage(core.msg(p.getUniqueId(), "gui.fund.bad-number"));
        }
    }

    private void openTop(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        java.util.List<String> lines = new java.util.ArrayList<>();
        var top = plugin.fund().top(10);
        if (top.isEmpty()) {
            lines.add(core.lang().raw(lang, "fund.top-empty"));
        }
        int pos = 1;
        for (var e : top.entrySet()) {
            String name = plugin.storage().fundNames.getOrDefault(e.getKey(), "?");
            lines.add(core.lang().raw(lang, "fund.top-line",
                    "%pos%", String.valueOf(pos++),
                    "%player%", name, "%amount%", FundManager.fmt(e.getValue())));
        }
        ru.loom.core.gui.ListMenu.open(p, "gui.fund.top-title", lines, pl -> open(pl));
    }

    /** Bedrock-форма панели сбора. */
    static void form(Player p) {
        var core = LoomCorePlugin.get();
        var plugin = ProgressPlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        var st = plugin.storage();

        StringBuilder content = new StringBuilder();
        if (!st.fundActive) {
            content.append(core.lang().raw(lang, "gui.fund.inactive-lore"));
        } else {
            long hours = Math.max(0, (st.fundEndsAt - System.currentTimeMillis()) / 3600_000L);
            content.append(core.lang().raw(lang, "gui.fund.info-progress",
                    "%collected%", FundManager.fmt(st.fundCollected),
                    "%target%", FundManager.fmt(st.fundTarget),
                    "%pct%", String.valueOf(plugin.fund().pct()))).append("\n");
            content.append(core.lang().raw(lang, "gui.fund.info-left", "%hours%", String.valueOf(hours))).append("\n");
            content.append(core.lang().raw(lang, "gui.fund.info-my", "%amount%",
                    FundManager.fmt(st.fundContrib.getOrDefault(p.getUniqueId(), 0.0)))).append("\n");
            content.append("\n").append(core.lang().raw(lang, "gui.fund.rule-rate")).append("\n");
            content.append(core.lang().raw(lang, "gui.fund.rule-clamp")).append("\n");
            content.append(core.lang().raw(lang, "gui.fund.rule-refund"));
        }

        List<String> buttons = new java.util.ArrayList<>();
        if (st.fundActive) {
            for (int v : PRESETS) {
                buttons.add(core.lang().raw(lang, "gui.fund.preset-name", "%amount%", String.valueOf(v)));
            }
            if (p.hasPermission("loomprogress.fund.preset10k")) {
                buttons.add(core.lang().raw(lang, "gui.fund.preset-name", "%amount%", "10000"));
            }
            buttons.add(core.lang().raw(lang, "gui.fund.custom"));
            buttons.add(core.lang().raw(lang, "gui.fund.top"));
        } else {
            buttons.add(core.lang().raw(lang, "gui.close"));
        }
        ru.loom.core.bedrock.BedrockUi.buttons(p,
                core.lang().raw(lang, "gui.fund.title"), content.toString(), buttons, idx -> {
                    if (!plugin.storage().fundActive) return;
                    if (idx >= 0 && idx < PRESETS.length) {
                        String err = plugin.fund().contribute(p.getUniqueId(), p.getName(), PRESETS[idx]);
                        if (err != null) p.sendMessage(err);
                    } else if (p.hasPermission("loomprogress.fund.preset10k") && idx == PRESETS.length) {
                        String err = plugin.fund().contribute(p.getUniqueId(), p.getName(), 10000);
                        if (err != null) p.sendMessage(err);
                    } else if (idx == PRESETS.length + (p.hasPermission("loomprogress.fund.preset10k") ? 1 : 0)) {
                        ru.loom.core.bedrock.BedrockUi.input(p,
                                core.lang().raw(lang, "gui.fund.custom-form-title"),
                                core.lang().raw(lang, "gui.fund.custom-form-label"),
                                "", FundGUI::contributeCustom);
                    } else if (idx == PRESETS.length + (p.hasPermission("loomprogress.fund.preset10k") ? 1 : 0) + 1) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                new FundGUI(p).openTop(p));
                    }
                });
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
