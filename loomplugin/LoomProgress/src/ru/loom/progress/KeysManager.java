package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Косметика и ключи (v1.4).
 *  — Ключи: 1 ключ за каждые 20 ч наигранного времени ПОСЛЕ порога Легенды (360 ч).
 *  — Косметика: открывается ключами (редкости) и часами (Ветеран+ через магазин LoomCore).
 */
public final class KeysManager {

    /** Минут на ключ за часы выше Легенды. */
    public static final long MINUTES_PER_KEY = 20 * 60;

    private final ProgressPlugin plugin;

    public KeysManager(ProgressPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveKey(UUID id, int n) {
        plugin.storage().keys.merge(id, n, Integer::sum);
        var core = LoomCorePlugin.get();
        Player p = Bukkit.getPlayer(id);
        if (p != null) core.lang().send(p, "keys.gained", "%n%", String.valueOf(n));
    }

    public int keysOf(UUID id) {
        return plugin.storage().keys.getOrDefault(id, 0);
    }

    /** Открыть ключ: случайная косметика по редкости. */
    public String openKey(UUID id) {
        var st = plugin.storage();
        int k = keysOf(id);
        if (k <= 0) return null;
        st.keys.put(id, k - 1);
        // пул косметики
        List<String> pool = plugin.getConfig().getStringList("cosmetics.pool");
        if (pool.isEmpty()) pool = List.of("trail_flame", "trail_heart", "aura_gold", "hat_crown", "pet_parrot");
        String cosmetic = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
        List<String> owned = st.cosmeticsOwned.computeIfAbsent(id, x -> new ArrayList<>());
        if (owned.contains(cosmetic)) {
            // дубликат — компенсация монетами
            LoomCorePlugin.get().api().add(id, nameOf(id),
                    plugin.getConfig().getDouble("cosmetics.duplicate-coins", 250));
            LoomCorePlugin.get().lang().send(Bukkit.getPlayer(id), "keys.duplicate");
        } else {
            owned.add(cosmetic);
            LoomCorePlugin.get().lang().send(Bukkit.getPlayer(id), "keys.won", "%item%", cosmeticKey(cosmetic, id));
        }
        return cosmetic;
    }

    private String cosmeticKey(String id, UUID player) {
        var core = LoomCorePlugin.get();
        return core.msg(player, "cosmetics." + id, "%fallback%", id);
    }

    /** Ежеминутный тик: ключи за часы выше Легенды + еженедельный ключ донат-привилегии. */
    public void tick() {
        var core = LoomCorePlugin.get();
        if (core == null) return;
        long legendMinutes = core.ranks().ladderThresholdMinutes("legend");
        var st = plugin.storage();
        long epochDay = System.currentTimeMillis() / 86_400_000L;
        for (var op : Bukkit.getOnlinePlayers()) {
            UUID id = op.getUniqueId();
            // еженедельный ключ за донат-привилегию (Патриций/Фантом) — раз в 7 дней
            Long lastWeekly = st.weeklyKeyDay.get(id);
            if ((lastWeekly == null || epochDay - lastWeekly >= 7)
                    && op.hasPermission("loomprogress.keys.weekly")) {
                st.weeklyKeyDay.put(id, epochDay);
                giveKey(id, 1);
            }
            long minutes = core.api().playtimeMinutes(id);
            if (minutes <= legendMinutes) continue;
            long counted = minutes - legendMinutes;
            long last = st.hoursAtLastKey.getOrDefault(id, 0L);
            long due = counted / MINUTES_PER_KEY - last / MINUTES_PER_KEY;
            if (due > 0) {
                st.hoursAtLastKey.put(id, (last / MINUTES_PER_KEY + due) * MINUTES_PER_KEY);
                giveKey(id, (int) due);
            }
        }
    }

    public boolean owns(UUID id, String cosmetic) {
        List<String> o = plugin.storage().cosmeticsOwned.get(id);
        return o != null && o.contains(cosmetic);
    }

    public List<String> owned(UUID id) {
        return plugin.storage().cosmeticsOwned.getOrDefault(id, new ArrayList<>());
    }

    public void activate(UUID id, String cosmetic) {
        plugin.storage().activeCosmetic.put(id, cosmetic);
    }

    public String active(UUID id) {
        return plugin.storage().activeCosmetic.get(id);
    }

    private String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }
}
