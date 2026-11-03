package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.loom.core.LoomCorePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Торговый квартал (v1.4, пункт 4.3): дома на спавне + участки на спавне.
 *  — /квартал build all — строит платформу и размечает участки вокруг координат из конфига;
 *  — /участок claim — покупка ближайшего свободного участка;
 *  — защита: в чужом участке строить нельзя (кроме владельца и админов).
 */
public final class QuarterManager implements Listener {

    public static final int PLOT_SIZE = 9;      // 9×9
    public static final int PLOT_MARGIN = 2;    // дорожки между участками

    private final ProgressPlugin plugin;
    private final Map<Integer, Plot> plots = new HashMap<>();

    public static final class Plot {
        public final int id;
        public final int minX, minZ, maxX, maxZ;
        Plot(int id, int minX, int minZ, int maxX, int maxZ) {
            this.id = id; this.minX = minX; this.minZ = minZ; this.maxX = maxX; this.maxZ = maxZ;
        }
        boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    public QuarterManager(ProgressPlugin plugin) {
        this.plugin = plugin;
        // участки создаются при /квартал build all либо берутся из конфига
        int count = plugin.getConfig().getInt("quarter.plots", 12);
        int cx = plugin.getConfig().getInt("quarter.center-x", 0);
        int cz = plugin.getConfig().getInt("quarter.center-z", 0);
        buildPlots(cx, cz, count);
    }

    public void buildPlots(int centerX, int centerZ, int count) {
        plots.clear();
        int id = 1;
        int row = 0, col = 0;
        int perRow = (int) Math.ceil(Math.sqrt(count));
        while (id <= count) {
            int bx = centerX + col * (PLOT_SIZE + PLOT_MARGIN);
            int bz = centerZ + row * (PLOT_SIZE + PLOT_MARGIN);
            plots.put(id, new Plot(id, bx, bz, bx + PLOT_SIZE - 1, bz + PLOT_SIZE - 1));
            id++;
            col++;
            if (col >= perRow) { col = 0; row++; }
        }
    }

    /** Построить платформу квартала и границы участков. */
    public String buildAll(Player admin) {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("quarter.enabled", false)) {
            return "§cКвартал отключён: quarter.enabled: false в конфиге LoomProgress.";
        }
        World w = Bukkit.getWorld(cfg.getString("quarter.world", "world"));
        if (w == null) return "§cМир не найден.";
        int y = cfg.getInt("quarter.y", -60);
        int cx = cfg.getInt("quarter.center-x", 0);
        int cz = cfg.getInt("quarter.center-z", 0);
        buildPlots(cx, cz, cfg.getInt("quarter.plots", 12));

        int minX = cx - PLOT_SIZE * 2, maxX = cx + PLOT_SIZE * 2 + PLOT_MARGIN * 2;
        int minZ = cz - PLOT_SIZE * 2, maxZ = cz + PLOT_SIZE * 2 + PLOT_MARGIN * 2;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block b = w.getBlockAt(x, y, z);
                b.setType(Material.SMOOTH_STONE_SLAB, false);
                w.getBlockAt(x, y - 1, z).setType(Material.STONE_BRICKS, false);
                // разметка границ участков
                for (Plot p : plots.values()) {
                    if ((x == p.minX || x == p.maxX) && z >= p.minZ && z <= p.maxZ
                            || (z == p.minZ || z == p.maxZ) && x >= p.minX && x <= p.maxX) {
                        b.setType(Material.STONE_BRICK_SLAB, false);
                    }
                }
            }
        }
        admin.sendMessage("§aКвартал построен: " + plots.size() + " участков. Разметка готова.");
        return null;
    }

    /** Купить ближайший свободный участок. */
    public String claim(Player p) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        // доступ: Хранитель+
        if (core.api().ladderOrder(p.getUniqueId())
                < core.ranks().ladderOrder(core.ranks().ladderThresholdMinutes("keeper"))) {
            return core.msg(p.getUniqueId(), "quarter.need-keeper");
        }
        boolean already = st.plotOwners.containsValue(p.getUniqueId());
        if (already) return core.msg(p.getUniqueId(), "quarter.already-owner");
        Plot nearest = nearestFree(p.getLocation());
        if (nearest == null) return core.msg(p.getUniqueId(), "quarter.no-free");
        double price = plugin.getConfig().getDouble("quarter.plot-price", 3000);
        if (!core.api().has(p.getUniqueId(), price)) return core.msg(p.getUniqueId(), "quarter.no-money");
        core.api().withdraw(p.getUniqueId(), p.getName(), price);
        st.plotOwners.put(nearest.id, p.getUniqueId());
        st.saveQuarter();
        core.lang().broadcast("quarter.claimed",
                "%player%", p.getName(), "%plot%", String.valueOf(nearest.id),
                "%price%", LoomCorePlugin.get().api().fmt(price));
        return null;
    }

    private Plot nearestFree(Location loc) {
        Plot best = null;
        double bestD = Double.MAX_VALUE;
        for (Plot p : plots.values()) {
            if (plugin.storage().plotOwners.containsKey(p.id)) continue;
            int cxp = (p.minX + p.maxX) / 2, czp = (p.minZ + p.maxZ) / 2;
            double d = loc.distanceSquared(new Location(loc.getWorld(), cxp, loc.getY(), czp));
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    public Plot plotAt(int x, int z) {
        for (Plot p : plots.values()) if (p.contains(x, z)) return p;
        return null;
    }

    public UUID ownerOf(Plot p) {
        return plugin.storage().plotOwners.get(p.id);
    }

    // ---------------- защита ----------------

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!allowed(e.getPlayer(), e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!allowed(e.getPlayer(), e.getBlock())) e.setCancelled(true);
    }

    private boolean allowed(Player p, Block b) {
        Plot plot = plotAt(b.getX(), b.getZ());
        if (plot == null) return true; // не квартал
        if (p.hasPermission("loom.admin")) return true;
        UUID owner = ownerOf(plot);
        return owner != null && owner.equals(p.getUniqueId());
    }
}
