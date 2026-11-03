package ru.loom.auction;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LoomAuction 1.4.0 — аукцион по §2 дизайн-дока:
 *  • доступ с Постояльца;
 *  • ставки с минимальным шагом (% от текущей + минимум);
 *  • окончание: таймер 48 ч + анти-снайпинг (продление при ставке в конце);
 *  • комиссия с победителя (процент + минимум), проигравшим возврат мгновенно.
 */
public final class AuctionPlugin extends JavaPlugin {

    private static AuctionPlugin instance;
    private AuctionManager auction;

    public static AuctionPlugin get() {
        return instance;
    }

    public AuctionManager auction() {
        return auction;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        auction = new AuctionManager(this);

        var ah = getCommand("ah");
        if (ah != null) ah.setExecutor(new AuctionCommand(this));

        Bukkit.getPluginManager().registerEvents(new AuctionListeners(this), this);

        // финализация лотов каждые 30 секунд
        Bukkit.getScheduler().runTaskTimer(this, auction::tick, 20L * 30, 20L * 30);
        // сохранение каждые 5 минут
        Bukkit.getScheduler().runTaskTimer(this, auction::save, 20L * 300, 20L * 300);

        AuctionHooks.register(this);

        getLogger().info("LoomAuction 1.4.1 включён (§2: ставки/окончание/комиссия; GUI-меню).");
    }

    @Override
    public void onDisable() {
        if (auction != null) auction.save();
        instance = null;
    }
}
