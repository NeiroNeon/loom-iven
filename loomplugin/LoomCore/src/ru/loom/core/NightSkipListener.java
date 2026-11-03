package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * Пропуск ночи Ветераном: игрок с косметическим рангом Ветеран+
 * ложится в кровать — ночь сменяется утром для всего мира.
 */
public final class NightSkipListener implements Listener {

    private final LoomCorePlugin plugin;

    public NightSkipListener(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBed(PlayerBedEnterEvent e) {
        if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        Player p = e.getPlayer();
        String cos = plugin.api().cosmeticRank(p.getUniqueId());
        int order = orderOf(cos);
        if (order < orderOf("veteran")) return;

        World w = p.getWorld();
        if (w.getTime() < 12500 || w.getTime() > 23450) return; // не ночь
        w.setTime(23450); // рассвет
        if (w.isThundering()) w.setThundering(false);
        w.setStorm(false);
        long mins = plugin.api().playtimeMinutes(p.getUniqueId()) / 60;
        plugin.lang().broadcast("nightskip.done",
                "%player%", p.getName(), "%rank%", plugin.lang().raw("ru", "cosmetic." + cos),
                "%hours%", String.valueOf(mins));
    }

    private int orderOf(String key) {
        return switch (key) {
            case "veteran" -> 1;
            case "elder" -> 2;
            case "virtuoso" -> 3;
            case "icon" -> 4;
            default -> 0;
        };
    }
}
