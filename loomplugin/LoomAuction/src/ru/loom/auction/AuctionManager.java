package ru.loom.auction;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loom.core.LoomCorePlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Ядро аукциона: хранение лотов, ставки, анти-снайпинг, комиссия, финализация.
 */
public final class AuctionManager {

    private final AuctionPlugin plugin;
    private final File file;
    private final Map<UUID, Lot> lots = new ConcurrentHashMap<>();
    /** Ожидающие выдачи предметов: uuid игрока → список лотов. */
    private final Map<UUID, List<UUID>> claims = new ConcurrentHashMap<>();
    /** Незавершённые сделки (продавец → сумма к выплате после комиссии). */
    private final Map<UUID, Double> payouts = new ConcurrentHashMap<>();

    public AuctionManager(AuctionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/auction.yml");
        load();
    }

    /** Пауза аукциона (админ): новые ставки/выкупы/лоты запрещены. */
    private volatile boolean frozen = false;

    public boolean isFrozen() { return frozen; }

    public void setFrozen(boolean f) { this.frozen = f; }

    public int accessOrder() { // минимум Постоялец
        return 1;
    }

    /** Лимит лотов игрока: донат-привилегия loomauction.maxlots.<n> перекрывает конфиг. */
    public int maxLotsFor(UUID seller) {
        Player pl = Bukkit.getPlayer(seller);
        int def = plugin.getConfig().getInt("auction.max-lots-per-player", 5);
        if (pl == null) return def;
        int best = def;
        for (int v : new int[]{15, 12, 10, 9, 8, 7, 6}) {
            if (v > best && pl.hasPermission("loomauction.maxlots." + v)) best = v;
        }
        return best;
    }

    /** Процент комиссии продавца: донат-привилегия loomauction.commission.<pct>. */
    public double commissionPercentFor(UUID seller) {
        Player pl = Bukkit.getPlayer(seller);
        double def = plugin.getConfig().getDouble("auction.commission-percent", 5.0);
        if (pl == null) return def;
        for (double v : new double[]{0, 1, 2, 3, 4}) {
            if (pl.hasPermission("loomauction.commission." + (int) v)) return v;
        }
        return def;
    }

    // ---------------- операции ----------------

    /** Выставить лот. Возвращает ошибку или null. */
    public String create(UUID seller, String sellerName, ItemStack item, double price, double buyout) {
        var core = LoomCorePlugin.get();
        if (core == null) return "§cLoomCore недоступен.";
        if (frozen) return core.msg(seller, "auction.frozen");
        if (core.api().ladderOrder(seller) < accessOrder())
            return core.msg(seller, "auction.need-resident");
        long active = lots.values().stream()
                .filter(l -> l.state == Lot.State.ACTIVE && l.seller.equals(seller)).count();
        int maxSlots = maxLotsFor(seller);
        if (active >= maxSlots) return core.msg(seller, "auction.too-many", "%max%", String.valueOf(maxSlots));
        double min = plugin.getConfig().getDouble("auction.min-price", 10);
        if (price < min) return core.msg(seller, "auction.min-price", "%min%", LoomCorePlugin.get().api().fmt(min));
        if (buyout > 0 && buyout < price) return "§cЦена выкупа не может быть меньше стартовой.";
        Lot lot = new Lot(UUID.randomUUID(), seller, sellerName, item, price, buyout,
                plugin.getConfig().getLong("auction.duration-hours", 48) * 3600_000L);
        lots.put(lot.id, lot);
        saveAsync();
        return null;
    }

    /** Минимальная следующая ставка. */
    public double minNextBid(Lot lot) {
        double stepPercent = plugin.getConfig().getDouble("auction.bid-step-percent", 5.0);
        double stepMin = plugin.getConfig().getDouble("auction.bid-step-min", 5);
        double step = Math.max(stepMin, lot.currentBid * stepPercent / 100.0);
        return Math.round((lot.currentBid + step) * 100.0) / 100.0;
    }

    /** Сделать ставку. Возвращает сообщение-ошибку или null. */
    public String bid(UUID bidder, String bidderName, Lot lot, double amount) {
        var core = LoomCorePlugin.get();
        if (core == null) return "§cLoomCore недоступен.";
        if (frozen) return core.msg(bidder, "auction.frozen");
        if (lot.state != Lot.State.ACTIVE) return core.msg(bidder, "auction.ended");
        if (lot.seller.equals(bidder)) return core.msg(bidder, "auction.own-lot");
        if (core.api().ladderOrder(bidder) < accessOrder())
            return core.msg(bidder, "auction.need-resident");
        double minNext = minNextBid(lot);
        if (amount < minNext)
            return core.msg(bidder, "auction.bid-too-low", "%min%", core.api().fmt(minNext));
        if (!core.api().has(bidder, amount)) return core.msg(bidder, "auction.no-money");

        // деньги: удерживаем новую ставку, мгновенно возвращаем предыдущему лидеру
        core.api().withdraw(bidder, bidderName, amount);
        if (lot.bidder != null && !lot.bidder.equals(bidder)) {
            core.api().add(lot.bidder, lot.bidderName, lot.currentBid);
        }
        lot.currentBid = amount;
        lot.bidder = bidder;
        lot.bidderName = bidderName;

        // анти-снайпинг: ставка в последние N секунд продлевает лот
        // v1.8: ключи переименованы (anti-… вместо опечатки anty-…), старое написание — fallback
        long antiSnipeMs = plugin.getConfig().getLong("auction.anti-snipe-extend-seconds",
                plugin.getConfig().getLong("auction.anty-snipe-extend-seconds", 300)) * 1000L;
        int maxExt = plugin.getConfig().getInt("auction.anti-snipe-max",
                plugin.getConfig().getInt("auction.anty-snipe-max", 3));
        if (lot.secondsLeft() * 1000L < antiSnipeMs && lot.extensions < maxExt) {
            lot.endsAt += antiSnipeMs;
            lot.extensions++;
            Player bp = Bukkit.getPlayer(bidder);
            if (bp != null) core.lang().send(bp, "auction.extended", "%sec%", String.valueOf(antiSnipeMs / 1000));
        }
        Player sellerP = Bukkit.getPlayer(lot.seller);
        if (sellerP != null) core.lang().send(sellerP, "auction.outbid-you",
                "%player%", bidderName, "%amount%", core.api().fmt(amount));
        saveAsync();
        return null;
    }

    /** Мгновенный выкуп. */
    public String buyout(UUID buyer, String buyerName, Lot lot) {
        if (lot.buyout <= 0 || lot.state != Lot.State.ACTIVE) return "§cВыкуп недоступен.";
        var core = LoomCorePlugin.get();
        if (frozen) return core.msg(buyer, "auction.frozen");
        if (lot.seller.equals(buyer)) return core.msg(buyer, "auction.own-lot");
        if (core.api().ladderOrder(buyer) < accessOrder())
            return core.msg(buyer, "auction.need-resident");
        if (!core.api().has(buyer, lot.buyout)) return core.msg(buyer, "auction.no-money");
        // вернуть текущую ставку
        if (lot.bidder != null) core.api().add(lot.bidder, lot.bidderName, lot.currentBid);
        core.api().withdraw(buyer, buyerName, lot.buyout);
        finalizeSale(lot, buyer, buyerName, lot.buyout);
        return null;
    }

    /** Отмена лота продавцом (без ставок — бесплатно). */
    public String cancel(UUID seller, Lot lot) {
        if (!lot.seller.equals(seller) || lot.state != Lot.State.ACTIVE) return "§cНельзя отменить.";
        var core = LoomCorePlugin.get();
        if (lot.bidder != null) {
            // ставки есть — отменять нельзя (честность торгов)
            return core.msg(seller, "auction.cancel-with-bids");
        }
        lot.state = Lot.State.CLAIMED;
        lot.claimTo = seller;
        claims.computeIfAbsent(seller, k -> new ArrayList<>()).add(lot.id);
        detached.put(lot.id, lot);
        lots.remove(lot.id);
        saveAsync();
        return null;
    }

    /** Проверка и финализация завершённых лотов (вызывается планировщиком). */
    public void tick() {
        long now = System.currentTimeMillis();
        for (Lot lot : lots.values().toArray(new Lot[0])) {
            if (lot.state != Lot.State.ACTIVE) continue;
            if (now < lot.endsAt) continue;
            if (lot.bidder == null) {
                // ставок нет — предмет возвращается продавцу
                lot.state = Lot.State.CLAIMED;
                lot.claimTo = lot.seller;
                claims.computeIfAbsent(lot.seller, k -> new ArrayList<>()).add(lot.id);
                detached.put(lot.id, lot);
                lots.remove(lot.id);
                Player sp = Bukkit.getPlayer(lot.seller);
                if (sp != null) LoomCorePlugin.get().lang().send(sp, "auction.expired-no-bids");
            } else {
                finalizeSale(lot, lot.bidder, lot.bidderName, lot.currentBid);
            }
        }
    }

    /** Завершение продажи: комиссия продавцу, предмет покупателю. */
    private void finalizeSale(Lot lot, UUID buyer, String buyerName, double amount) {
        var core = LoomCorePlugin.get();
        double commPercent = commissionPercentFor(lot.seller);
        double commMin = plugin.getConfig().getDouble("auction.commission-min", 10);
        double commission = commPercent <= 0 ? 0 : Math.max(commMin, amount * commPercent / 100.0);
        double net = Math.max(0, amount - commission);

        lot.state = Lot.State.SOLD;
        payouts.merge(lot.seller, net, Double::sum);
        lot.claimTo = buyer;
        claims.computeIfAbsent(buyer, k -> new ArrayList<>()).add(lot.id);
        detached.put(lot.id, lot);
        lots.remove(lot.id);

        Player sellerP = Bukkit.getPlayer(lot.seller);
        if (sellerP != null) {
            core.lang().send(sellerP, "auction.sold", "%buyer%", buyerName,
                    "%amount%", core.api().fmt(amount), "%net%", core.api().fmt(net),
                    "%commission%", core.api().fmt(commission));
        }
        Player buyerP = Bukkit.getPlayer(buyer);
        if (buyerP != null) {
            core.lang().send(buyerP, "auction.won", "%amount%", core.api().fmt(amount));
        }
        core.lang().broadcast("auction.broadcast.sold",
                "%seller%", lot.sellerName, "%buyer%", buyerName,
                "%amount%", core.api().fmt(amount));
        saveAsync();
    }

    /** Забрать предметы (выплата за продажи + покупки). */
    public List<ItemStack> takeClaims(UUID player) {
        List<ItemStack> out = new ArrayList<>();
        List<UUID> ids = claims.remove(player);
        if (ids != null) {
            for (UUID lid : ids) {
                Lot l = findAny(lid);
                if (l != null) {
                    out.add(l.item.clone());
                    detached.remove(lid);
                }
            }
        }
        saveAsync();
        return out;
    }

    /** Забрать один конкретный лот из выдачи (GUI). Возвращает предмет или null. */
    public ItemStack claimOne(UUID player, UUID lotId) {
        List<UUID> ids = claims.get(player);
        if (ids == null || !ids.remove(lotId)) return null;
        Lot l = findAny(lotId);
        ItemStack item = l == null ? null : l.item.clone();
        if (l != null) detached.remove(lotId);
        saveAsync();
        return item;
    }

    /** Админ: принудительное снятие лота — ставка возвращается, предмет продавцу. */
    public String adminRemove(UUID lotId) {
        Lot lot = lots.get(lotId);
        if (lot == null) return "§cЛот не найден среди активных.";
        var core = LoomCorePlugin.get();
        if (lot.bidder != null) {
            core.api().add(lot.bidder, lot.bidderName, lot.currentBid);
            Player bp = Bukkit.getPlayer(lot.bidder);
            if (bp != null) bp.sendMessage("§eСтавка возвращена — лот снят модератором.");
        }
        lot.state = Lot.State.CLAIMED;
        lot.claimTo = lot.seller;
        lot.bidder = null;
        lot.bidderName = null;
        claims.computeIfAbsent(lot.seller, k -> new ArrayList<>()).add(lot.id);
        detached.put(lot.id, lot);
        lots.remove(lot.id);
        Player sp = Bukkit.getPlayer(lot.seller);
        if (sp != null) sp.sendMessage("§eВаш лот снят модератором — предмет вернётся через §f/ah claim§e.");
        saveAsync();
        return null;
    }

    /** Статистика для админ-GUI. */
    public List<String> adminStats() {
        List<String> out = new ArrayList<>();
        long active = lots.values().stream().filter(l -> l.state == Lot.State.ACTIVE).count();
        double bids = lots.values().stream().filter(l -> l.bidder != null).mapToDouble(l -> l.currentBid).sum();
        double payoutsSum = payouts.values().stream().mapToDouble(Double::doubleValue).sum();
        int claimsTotal = claims.values().stream().mapToInt(List::size).sum();
        out.add("§7Активных лотов: §f" + active);
        out.add("§7Сумма удержанных ставок: §6" + LoomCorePlugin.get().api().fmt(bids));
        out.add("§7Ожидают выплаты продавцам: §6" + LoomCorePlugin.get().api().fmt(payoutsSum));
        out.add("§7Предметов к выдаче: §f" + claimsTotal);
        out.add("§7Пауза аукциона: " + (frozen ? "§cвключена" : "§aвыключена"));
        return out;
    }

    public double takePayout(UUID player) {
        Double v = payouts.remove(player);
        saveAsync();
        return v == null ? 0 : v;
    }

    /** Публичный поиск лота (активный или в выдаче) для GUI. */
    public Lot findLot(UUID id) {
        return findAny(id);
    }

    private Lot findAny(UUID id) {
        for (Lot l : lots.values()) if (l.id.equals(id)) return l;
        // поиск в хранилище выгруженных лотов
        Lot l = detached.get(id);
        return l;
    }

    private final Map<UUID, Lot> detached = new ConcurrentHashMap<>();

    public Lot active(UUID id) {
        return lots.get(id);
    }

    public List<Lot> activeLots() {
        return lots.values().stream()
                .filter(l -> l.state == Lot.State.ACTIVE)
                .sorted((a, b) -> Long.compare(a.endsAt, b.endsAt))
                .collect(Collectors.toList());
    }

    public List<UUID> claimsOf(UUID player) {
        return claims.getOrDefault(player, new ArrayList<>());
    }

    public double payoutOf(UUID player) {
        return payouts.getOrDefault(player, 0.0);
    }

    // ---------------- хранилище ----------------

    private void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        int i = 0;
        for (Lot l : lots.values()) writeLot(y, "lots." + (i++), l);
        for (Lot l : detached.values()) writeLot(y, "detached." + (i++), l);
        for (Map.Entry<UUID, List<UUID>> e : claims.entrySet()) {
            y.set("claims." + e.getKey(), e.getValue().stream().map(UUID::toString).collect(Collectors.toList()));
        }
        for (Map.Entry<UUID, Double> e : payouts.entrySet()) y.set("payouts." + e.getKey(), e.getValue());
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("save auction.yml: " + ex.getMessage());
        }
    }

    private void writeLot(YamlConfiguration y, String base, Lot l) {
        y.set(base + ".id", l.id.toString());
        y.set(base + ".seller", l.seller.toString());
        y.set(base + ".sellerName", l.sellerName);
        y.set(base + ".item", encode(l.item));
        y.set(base + ".start", l.startPrice);
        y.set(base + ".buyout", l.buyout);
        y.set(base + ".bid", l.currentBid);
        y.set(base + ".bidder", l.bidder == null ? null : l.bidder.toString());
        y.set(base + ".bidderName", l.bidderName);
        y.set(base + ".endsAt", l.endsAt);
        y.set(base + ".ext", l.extensions);
        y.set(base + ".state", l.state.name());
        y.set(base + ".claimTo", l.claimTo == null ? null : l.claimTo.toString());
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String sec : new String[]{"lots", "detached"}) {
            ConfigurationSection root = y.getConfigurationSection(sec);
            if (root == null) continue;
            for (String key : root.getKeys(false)) {
                ConfigurationSection c = root.getConfigurationSection(key);
                if (c == null) continue;
                try {
                    Lot l = new Lot(
                            UUID.fromString(c.getString("id")),
                            UUID.fromString(c.getString("seller")),
                            c.getString("sellerName", "?"),
                            decode(c.getString("item")),
                            c.getDouble("start"), c.getDouble("buyout"), c.getDouble("bid"),
                            c.getString("bidder") == null ? null : UUID.fromString(c.getString("bidder")),
                            c.getString("bidderName"),
                            c.getLong("endsAt"), c.getInt("ext"),
                            Lot.State.valueOf(c.getString("state", "ACTIVE")),
                            c.getString("claimTo") == null ? null : UUID.fromString(c.getString("claimTo")));
                    if (l.state == Lot.State.ACTIVE) lots.put(l.id, l);
                    else detached.put(l.id, l);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Битый лот пропущен: " + ex.getMessage());
                }
            }
        }
        ConfigurationSection cl = y.getConfigurationSection("claims");
        if (cl != null) {
            for (String k : cl.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(k);
                    List<UUID> list = new ArrayList<>();
                    for (String s : cl.getStringList(k)) list.add(UUID.fromString(s));
                    claims.put(id, list);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        ConfigurationSection po = y.getConfigurationSection("payouts");
        if (po != null) {
            for (String k : po.getKeys(false)) {
                try { payouts.put(UUID.fromString(k), po.getDouble(k)); }
                catch (IllegalArgumentException ignored) { }
            }
        }
    }

    private String encode(ItemStack item) {
        try {
            return Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (Throwable t) {
            return "";
        }
    }

    private ItemStack decode(String b64) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
        } catch (Throwable t) {
            return new ItemStack(org.bukkit.Material.STONE);
        }
    }
}
