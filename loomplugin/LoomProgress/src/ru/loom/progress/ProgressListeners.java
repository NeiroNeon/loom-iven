package ru.loom.progress;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import ru.loom.core.LoomCorePlugin;

import java.util.Map;
import java.util.UUID;

/**
 * Счётчики для ачивок и ивентов: добыча, стройка, охота, рыбалка, жатва.
 * Также: день сезона по входу, двойной дроп новичкам, анти-АФК-фильтр.
 */
public final class ProgressListeners implements Listener {

    private final ProgressPlugin plugin;

    public ProgressListeners(ProgressPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean idle(Player p) {
        var core = LoomCorePlugin.get();
        return core != null && core.afk().isIdle(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.season().touchDay(e.getPlayer().getUniqueId());
    }

    // ---------- добыча блоков ----------

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (idle(p)) return;
        UUID id = p.getUniqueId();
        Material m = e.getBlock().getType();

        // общий счётчик добычи
        plugin.achievements().addProgress(id, "mine_total", 1);

        // руды (ивент «сбор железа» и ачивки)
        if (m.name().endsWith("_ORE") || m.name().equals("ANCIENT_DEBRIS")) {
            plugin.achievements().addProgress(id, "mine_ores", 1);
            var ev = plugin.events().current();
            if (ev != null && ev.kind == EventsManager.Kind.ORE_COLLECT
                    && plugin.events().activeFor(id)) {
                ev.points.merge(id, 1L, Long::sum);
            }
        }
        // дерево
        if (m.name().endsWith("_LOG")) plugin.achievements().addProgress(id, "mine_logs", 1);
        // камень
        if (m.name().endsWith("STONE") || m.name().endsWith("COBBLESTONE") || m.name().equals("DEEPSLATE"))
            plugin.achievements().addProgress(id, "mine_stone", 1);

        // двойной дроп новичкам во вторник
        var ev = plugin.events().current();
        if (ev != null && ev.kind == EventsManager.Kind.DOUBLE_DROP && plugin.events().activeFor(id)) {
            if (!m.name().endsWith("_ORE")) { // руды не дублируем (баланс)
                Block b = e.getBlock();
                for (ItemStack drop : b.getDrops(p.getInventory().getItemInMainHand(), p)) {
                    if (drop.getType().isAir()) continue;
                    p.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), drop);
                }
                ev.points.merge(id, 1L, Long::sum);
            } else {
                ev.points.merge(id, 1L, Long::sum);
            }
        }
    }

    // ---------- стройка ----------

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (idle(e.getPlayer())) return;
        plugin.achievements().addProgress(e.getPlayer().getUniqueId(), "build_total", 1);
    }

    // ---------- охота ----------

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || idle(killer)) return;
        UUID id = killer.getUniqueId();
        plugin.achievements().addProgress(id, "hunt_total", 1);

        var ev = plugin.events().current();
        boolean veteranEvent = ev != null && ev.veterans && plugin.events().activeFor(id);
        if (e.getEntityType() == EntityType.ZOMBIE || e.getEntityType() == EntityType.SKELETON
                || e.getEntityType() == EntityType.SPIDER || e.getEntityType() == EntityType.CREEPER) {
            plugin.achievements().addProgress(id, "hunt_monsters", 1);
            if (veteranEvent && ev.kind == EventsManager.Kind.HUNT) ev.points.merge(id, 1L, Long::sum);
        }
        if (e.getEntityType() == EntityType.ENDER_DRAGON || e.getEntityType() == EntityType.WITHER
                || e.getEntityType() == EntityType.WARDEN) {
            plugin.achievements().addProgress(id, "hunt_bosses", 1);
            if (veteranEvent && ev.kind == EventsManager.Kind.BOSS_BATTLE) ev.points.merge(id, 10L, Long::sum);
        }
        if (e.getEntityType() == EntityType.COW || e.getEntityType() == EntityType.SHEEP
                || e.getEntityType() == EntityType.PIG || e.getEntityType() == EntityType.CHICKEN) {
            plugin.achievements().addProgress(id, "hunt_animals", 1);
        }
    }

    // ---------- рыбалка ----------

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (idle(e.getPlayer())) return;
        plugin.achievements().addProgress(e.getPlayer().getUniqueId(), "fish_total", 1);
    }

    // ---------- жатва (вс) ----------

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Material m = e.getBlock().getType();
        if (!m.name().equals("WHEAT") && !m.name().equals("POTATOES")
                && !m.name().equals("CARROTS") && !m.name().equals("BEETROOTS")
                && !m.name().equals("NETHER_WART") && !m.name().equals("COCOA")) return;
        if (idle(p)) return;
        plugin.achievements().addProgress(p.getUniqueId(), "farm_harvest", 1);
        var ev = plugin.events().current();
        if (ev != null && ev.kind == EventsManager.Kind.HARVEST && plugin.events().activeFor(p.getUniqueId())) {
            ev.points.merge(p.getUniqueId(), 1L, Long::sum);
        }
    }
}
