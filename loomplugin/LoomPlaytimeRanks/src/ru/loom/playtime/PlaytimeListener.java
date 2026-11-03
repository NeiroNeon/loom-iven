package ru.loom.playtime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Записываем имена при входе (для офлайн-топов). */
final class PlaytimeListener implements Listener {

    private final PlaytimePlugin plugin;

    PlaytimeListener(PlaytimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.addMinutes(e.getPlayer().getUniqueId(), 0); // инициализация записи
    }
}
