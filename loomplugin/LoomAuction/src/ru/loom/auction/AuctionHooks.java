package ru.loom.auction;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.GuiUtil;
import ru.loom.core.menu.MenuAction;
import ru.loom.core.stats.StatsSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Интеграция LoomAuction с ядром: пункт меню «Аукцион» (открывает витрину,
 * у Bedrock витрина работает через Geyser) и секция статистики профиля.
 */
public final class AuctionHooks {

    private AuctionHooks() { }

    public static void register(AuctionPlugin plugin) {
        var core = LoomCorePlugin.get();
        if (core == null) return;

        core.menus().register(new MenuAction() {
            @Override public String id() { return "auction"; }
            @Override public int order() { return 42; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.GOLD_INGOT; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.menu.item.auction"); }
            @Override public List<String> lore(Player p, String lang) {
                List<String> out = new ArrayList<>(List.of(GuiUtil.t(p, lang, "gui.menu.item.auction-lore")));
                out.add("\n§8Активных лотов: §7" + plugin.auction().activeLots().size());
                double payout = plugin.auction().payoutOf(p.getUniqueId());
                if (payout > 0) out.add("§8К получению: §6" + LoomCorePlugin.get().api().fmt(payout));
                return out;
            }
            @Override public void click(Player p) {
                // гейт «Постоялец» — и через меню тоже
                if (LoomCorePlugin.get().api().ladderOrder(p.getUniqueId()) < plugin.auction().accessOrder()) {
                    p.sendMessage(LoomCorePlugin.get().msg(p.getUniqueId(), "auction.need-resident"));
                    return;
                }
                new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.MARKET, 0).open(p);
            }
        });

        // страницы «Команды»: аукцион
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.ah"; }
            @Override public int order() { return 30; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.GOLD_INGOT; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.ah"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.ah-lore"));
            }
            @Override public void click(Player p) { new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.MARKET, 0).open(p); }
            @Override public String category() { return "trade"; }
            @Override public String usage() { return "/ah"; }
        });
        core.menus().registerCommand(new MenuAction() {
            @Override public String id() { return "cmd.ah-claim"; }
            @Override public int order() { return 31; }
            @Override public boolean adminOnly() { return false; }
            @Override public Material icon(Player p) { return Material.CHEST; }
            @Override public String name(Player p, String lang) { return GuiUtil.t(p, lang, "gui.cmd.ah-claim"); }
            @Override public List<String> lore(Player p, String lang) {
                return List.of(GuiUtil.t(p, lang, "gui.cmd.ah-claim-lore"));
            }
            @Override public void click(Player p) { new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.CLAIMS, 0).open(p); }
            @Override public String category() { return "trade"; }
            @Override public String usage() { return "/ah claim"; }
        });
        AuctionAdminGUI.register(plugin);

        core.stats().register("auction", new StatsSection() {
            @Override public String title(Player p, String lang) {
                return core.lang().raw(lang, "gui.stats.auction-title");
            }
            @Override public List<String> lines(Player p, String lang) {
                List<String> out = new ArrayList<>();
                int mine = 0;
                for (Lot l : plugin.auction().activeLots()) {
                    if (l.seller.equals(p.getUniqueId())) mine++;
                }
                out.add(core.lang().raw(lang, "gui.stats.auction-lots", "%n%", String.valueOf(mine)));
                out.add(core.lang().raw(lang, "gui.stats.auction-claims", "%n%",
                        String.valueOf(plugin.auction().claimsOf(p.getUniqueId()).size())));
                double payout = plugin.auction().payoutOf(p.getUniqueId());
                out.add(core.lang().raw(lang, "gui.stats.auction-payout", "%amount%",
                        LoomCorePlugin.get().api().fmt(payout)));
                return out;
            }
        });
    }
}
