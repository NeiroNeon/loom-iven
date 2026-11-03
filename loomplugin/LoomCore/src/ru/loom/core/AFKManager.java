package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Анти-АФК: считаем только активное время.
 *  - 10 минут бездействия → игрок «расслаблен» (не получает награды ивентов);
 *  - 6 часов полного бездействия → учёт времени останавливается (жёсткий порог).
 */
public final class AFKManager implements Listener {

    private final LoomCorePlugin plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pausedAt = new ConcurrentHashMap<>();

    public AFKManager(LoomCorePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60, 20L * 60); // раз в минуту
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        // учитываем только существенные перемещения и повороты головы
        if (e.hasChangedBlock() || e.hasChangedOrientation()) touch(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        touch(e.getPlayer());
        sessionStart.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastActivity.remove(id);
        sessionStart.remove(id);
        pausedAt.remove(id);
    }

    public void touch(Player p) {
        lastActivity.put(p.getUniqueId(), System.currentTimeMillis());
        if (pausedAt.containsKey(p.getUniqueId())) {
            pausedAt.remove(p.getUniqueId());
            plugin.lang().send(p, "afk.back");
        }
    }

    private void tick() {
        long idleSoft = plugin.getConfig().getLong("afk.soft-minutes", 10) * 60_000L;
        long idleHard = plugin.getConfig().getLong("afk.hard-hours", 6) * 3600_000L;
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            long last = lastActivity.getOrDefault(p.getUniqueId(), now);
            long idle = now - last;
            if (idle >= idleHard && !pausedAt.containsKey(p.getUniqueId())) {
                pausedAt.put(p.getUniqueId(), now);
                plugin.lang().send(p, "afk.paused");
            } else if (idle >= idleSoft && !pausedAt.containsKey(p.getUniqueId())) {
                plugin.lang().send(p, "afk.idle");
            }
        }
    }

    public boolean isIdle(Player p) {
        long idleSoft = plugin.getConfig().getLong("afk.soft-minutes", 10) * 60_000L;
        long last = lastActivity.getOrDefault(p.getUniqueId(), System.currentTimeMillis());
        return System.currentTimeMillis() - last >= idleSoft;
    }

    /** Активные минуты текущей сессии (без учёта паузы АФК) — фолбэк-оценка часов. */
    public long sessionActiveMinutes(UUID id) {
        long start = sessionStart.getOrDefault(id, 0L);
        if (start == 0) return 0;
        long paused = pausedAt.getOrDefault(id, 0L);
        if (paused > 0) return Math.max(0, (paused - start) / 60_000L);
        return Math.max(0, (System.currentTimeMillis() - start) / 60_000L);
    }
}
