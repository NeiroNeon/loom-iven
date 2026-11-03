package ru.loom.auction;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.loom.core.LoomCorePlugin;

/** /ah — аукцион: витрина, продажа, ставки, мои лоты, выдача. */
public final class AuctionCommand implements CommandExecutor {

    private final AuctionPlugin plugin;

    public AuctionCommand(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Только для игроков.");
            return true;
        }
        var core = LoomCorePlugin.get();
        var mgr = plugin.auction();

        // доступ с Постояльца
        if (core.api().ladderOrder(p.getUniqueId()) < mgr.accessOrder()) {
            p.sendMessage(core.msg(p.getUniqueId(), "auction.need-resident"));
            return true;
        }

        if (args.length == 0) {
            new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.MARKET, 0).open(p);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell", "продать" -> {
                if (args.length < 2) { p.sendMessage("§7/ah sell <цена> [цена выкупа]"); return true; }
                ItemStack item = p.getInventory().getItemInMainHand();
                if (item.getType().isAir()) {
                    p.sendMessage(core.msg(p.getUniqueId(), "auction.hold-item"));
                    return true;
                }
                double price, buyout = 0;
                try { price = Double.parseDouble(args[1]); }
                catch (NumberFormatException ex) { p.sendMessage("§cЦена — число."); return true; }
                if (args.length >= 3) {
                    try { buyout = Double.parseDouble(args[2]); }
                    catch (NumberFormatException ex) { p.sendMessage("§cЦена выкупа — число."); return true; }
                }
                String err = mgr.create(p.getUniqueId(), p.getName(), item, price, buyout);
                if (err != null) { p.sendMessage(err); return true; }
                p.getInventory().setItemInMainHand(null);
                p.sendMessage(core.msg(p.getUniqueId(), "auction.created",
                        "%price%", core.api().fmt(price), "%buyout%", buyout > 0 ? core.api().fmt(buyout) : "—"));
                core.lang().broadcast("auction.broadcast.new",
                        "%player%", p.getName(), "%price%", core.api().fmt(price));
            }
            case "my", "мои" -> new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.MINE, 0).open(p);
            case "claim", "выдать" -> {
                // выдача через GUI: предметы по клику, выплата кнопкой
                new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.CLAIMS, 0).open(p);
            }
            case "help", "помощь" -> {
                p.sendMessage("§6/ah §7— витрина (ЛКМ — ставка/выкуп)");
                p.sendMessage("§6/ah sell <цена> [выкуп] §7— выставить предмет из руки");
                p.sendMessage("§6/ah my §7— мои лоты (ПКМ — отмена)");
                p.sendMessage("§6/ah claim §7— получить продажи и покупки");
            }
            default -> new AuctionGUI(p.getUniqueId(), AuctionGUI.Mode.MARKET, 0).open(p);
        }
        return true;
    }
}
