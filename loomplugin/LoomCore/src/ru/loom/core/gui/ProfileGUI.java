package ru.loom.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.stats.StatsSection;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Профиль игрока (/профиль): ранг, часы, монеты, косметика, серверные данные
 * и секции статистики, зарегистрированные другими плагинами.
 */
public final class ProfileGUI implements LoomMenu {

    private static final String[] LADDER = {"novice", "resident", "keeper", "legend"};

    static final int SLOT_BACK = 46;
    static final int SLOT_CLOSE = 49;
    static final int[] SECTION_SLOTS = {27, 28, 29, 30, 31, 32};

    private final UUID viewer;
    private final Inventory inv;

    public ProfileGUI(Player p) {
        var core = LoomCorePlugin.get();
        this.viewer = p.getUniqueId();
        String lang = core.api().lang(viewer);
        this.inv = Bukkit.createInventory(this, 54, GuiUtil.t(p, lang, "gui.profile.title"));
        redraw(p);
    }

    public static void open(Player p) {
        if (BedrockCheck.isBedrock(p)) {
            MenuForms.profileForm(p);
            return;
        }
        p.openInventory(new ProfileGUI(p).inv);
    }

    private void redraw(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(viewer);
        inv.clear();

        inv.setItem(13, head(p, lang));
        inv.setItem(20, GuiUtil.raw(Material.GOLD_NUGGET, coinName(p, lang), coinLore(p, lang)));
        inv.setItem(21, GuiUtil.raw(Material.CLOCK, timeName(p, lang), timeLore(p, lang)));
        inv.setItem(22, GuiUtil.raw(Material.EXPERIENCE_BOTTLE, rankName(p, lang), rankLore(p, lang)));
        inv.setItem(23, GuiUtil.raw(Material.ELYTRA, cosmeticName(p, lang), cosmeticLore(p, lang)));
        inv.setItem(24, GuiUtil.raw(Material.SPYGLASS, serverName(p, lang), serverLore(p, lang)));

        List<StatsSection> sections = core.stats().all();
        for (int i = 0; i < SECTION_SLOTS.length && i < sections.size(); i++) {
            StatsSection s = sections.get(i);
            List<String> lore = new ArrayList<>(s.lines(p, lang));
            inv.setItem(SECTION_SLOTS[i], GuiUtil.raw(Material.PAPER, s.title(p, lang), lore));
        }

        inv.setItem(SLOT_BACK, GuiUtil.item(p, Material.ARROW, lang, "gui.menu.open"));
        inv.setItem(SLOT_CLOSE, GuiUtil.item(p, Material.BARRIER, lang, "gui.close"));
    }

    private ItemStack head(Player p, String lang) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta m = it.getItemMeta();
        if (m instanceof SkullMeta sm) sm.setOwningPlayer(p);
        if (m != null) {
            m.setDisplayName(GuiUtil.t(p, lang, "gui.profile.head-name", "%player%", p.getName()));
            m.setLore(headLore(p, lang));
            it.setItemMeta(m);
        }
        return it;
    }

    // ---------------- текстовые блоки (общие для GUI и Bedrock-формы) ----------------

    private static String coinName(Player p, String lang) {
        return GuiUtil.t(p, lang, "gui.profile.coins-name");
    }

    private static List<String> coinLore(Player p, String lang) {
        var core = LoomCorePlugin.get();
        double c = core.api().coins(p.getUniqueId());
        return List.of(GuiUtil.t(p, lang, "gui.profile.coins-value", "%amount%", LoomCorePlugin.get().api().fmt(c)));
    }

    private static String timeName(Player p, String lang) {
        return GuiUtil.t(p, lang, "gui.profile.time-name");
    }

    private static List<String> timeLore(Player p, String lang) {
        var core = LoomCorePlugin.get();
        long hours = core.api().playtimeMinutes(p.getUniqueId()) / 60;
        long session = core.afk().sessionActiveMinutes(p.getUniqueId());
        return List.of(
                GuiUtil.t(p, lang, "gui.profile.time-hours", "%hours%", String.valueOf(hours)),
                GuiUtil.t(p, lang, "gui.profile.time-session", "%minutes%", String.valueOf(session)));
    }

    private static String rankName(Player p, String lang) {
        var core = LoomCorePlugin.get();
        String key = core.api().ladderRank(p.getUniqueId());
        return GuiUtil.t(p, lang, "gui.profile.rank-name", "%rank%", core.lang().raw(lang, "ladder." + key));
    }

    private static List<String> rankLore(Player p, String lang) {
        var core = LoomCorePlugin.get();
        long minutes = core.api().playtimeMinutes(p.getUniqueId());
        String key = core.api().ladderRank(p.getUniqueId());
        int idx = index(key);
        List<String> out = new ArrayList<>();
        if (idx >= LADDER.length - 1) {
            out.add(GuiUtil.t(p, lang, "gui.profile.rank-max"));
        } else {
            String next = LADDER[idx + 1];
            long nextMin = core.ranks().ladderThresholdMinutes(next);
            long left = Math.max(0, nextMin - minutes);
            double ratio = nextMin <= 0 ? 1 : (double) minutes / nextMin;
            out.add(GuiUtil.t(p, lang, "gui.profile.rank-next",
                    "%rank%", core.lang().raw(lang, "ladder." + next),
                    "%left%", String.valueOf(left / 60)));
            out.add("§8" + GuiUtil.bar(ratio, 20));
            out.add(GuiUtil.t(p, lang, "gui.profile.rank-pct", "%pct%", String.valueOf((int) Math.round(ratio * 100))));
        }
        return out;
    }

    private static int index(String key) {
        for (int i = 0; i < LADDER.length; i++) if (LADDER[i].equals(key)) return i;
        return 0;
    }

    private static String cosmeticName(Player p, String lang) {
        return GuiUtil.t(p, lang, "gui.profile.cosmetic-name");
    }

    private static List<String> cosmeticLore(Player p, String lang) {
        var core = LoomCorePlugin.get();
        String key = core.api().cosmeticRank(p.getUniqueId());
        String name = "none".equals(key)
                ? GuiUtil.t(p, lang, "cosmetic.none")
                : core.lang().raw(lang, "cosmetic." + key);
        return List.of(GuiUtil.t(p, lang, "gui.profile.cosmetic-value", "%cosmetic%", name));
    }

    private static String serverName(Player p, String lang) {
        return GuiUtil.t(p, lang, "gui.profile.server-name");
    }

    private static List<String> serverLore(Player p, String lang) {
        var core = LoomCorePlugin.get();
        String first = new SimpleDateFormat("dd.MM.yyyy").format(new Date(p.getFirstPlayed()));
        return List.of(
                GuiUtil.t(p, lang, "gui.profile.server-online", "%online%", String.valueOf(Bukkit.getOnlinePlayers().size())),
                GuiUtil.t(p, lang, "gui.profile.server-ping", "%ping%", String.valueOf(p.getPing())),
                GuiUtil.t(p, lang, "gui.profile.server-world", "%world%", p.getWorld().getName()),
                GuiUtil.t(p, lang, "gui.profile.server-first", "%date%", first));
    }

    private static List<String> headLore(Player p, String lang) {
        List<String> out = new ArrayList<>();
        out.addAll(timeLore(p, lang));
        out.add(GuiUtil.t(p, lang, "gui.profile.head-hint"));
        return out;
    }

    /** Все строки профиля текстом (для Bedrock-формы). */
    static List<String> textLines(Player p, String lang) {
        List<String> out = new ArrayList<>();
        out.add(GuiUtil.t(p, lang, "gui.profile.head-name", "%player%", p.getName()));
        out.addAll(coinLore(p, lang));
        out.addAll(timeLore(p, lang));
        out.add(rankName(p, lang));
        out.addAll(rankLore(p, lang));
        out.addAll(cosmeticLore(p, lang));
        out.addAll(serverLore(p, lang));
        for (StatsSection s : LoomCorePlugin.get().stats().all()) {
            out.add("");
            out.add(s.title(p, lang));
            out.addAll(s.lines(p, lang));
        }
        return out;
    }

    @Override
    public boolean handleClick(int slot, Player p) {
        if (slot == SLOT_CLOSE) return true;
        if (slot == SLOT_BACK) {
            MenuGUI.open(p);
        }
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
