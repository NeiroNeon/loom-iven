package ru.loom.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Лот аукциона. */
public final class Lot {

    public enum State { ACTIVE, SOLD, ENDED, CLAIMED }

    public final UUID id;
    public final UUID seller;
    public String sellerName;
    public final ItemStack item;
    public final double startPrice;   // стартовая цена (= начальная ставка)
    public final double buyout;       // мгновенный выкуп (0 — без выкупа)
    public double currentBid;         // текущая ставка (== startPrice до первой ставки)
    public UUID bidder;               // кто держит текущую ставку
    public String bidderName;
    public long endsAt;               // epoch millis
    public int extensions;            // сколько раз продлевали (анти-снайпинг)
    public State state;
    /** Кому принадлежит купленный предмет до получения (выигравший). */
    public UUID claimTo;

    public Lot(UUID id, UUID seller, String sellerName, ItemStack item,
               double startPrice, double buyout, long durationMs) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.startPrice = startPrice;
        this.buyout = buyout;
        this.currentBid = startPrice;
        this.endsAt = System.currentTimeMillis() + durationMs;
        this.state = State.ACTIVE;
    }

    /** Восстановление из хранилища. */
    public Lot(UUID id, UUID seller, String sellerName, ItemStack item,
               double startPrice, double buyout, double currentBid,
               UUID bidder, String bidderName, long endsAt, int extensions, State state, UUID claimTo) {
        this.id = id; this.seller = seller; this.sellerName = sellerName; this.item = item;
        this.startPrice = startPrice; this.buyout = buyout; this.currentBid = currentBid;
        this.bidder = bidder; this.bidderName = bidderName; this.endsAt = endsAt;
        this.extensions = extensions; this.state = state; this.claimTo = claimTo;
    }

    public long secondsLeft() {
        return Math.max(0, (endsAt - System.currentTimeMillis()) / 1000L);
    }

    public String timeLeft(String lang) {
        long s = secondsLeft();
        long h = s / 3600, m = (s % 3600) / 60;
        if ("en".equals(lang)) return h > 0 ? h + "h " + m + "m" : m + "m";
        return h > 0 ? h + " ч " + m + " мин" : m + " мин";
    }
}
