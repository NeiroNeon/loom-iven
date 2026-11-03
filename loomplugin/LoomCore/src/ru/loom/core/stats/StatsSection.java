package ru.loom.core.stats;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Секция статистики профиля, которую дописывает любой плагин (Auction, Progress,
 * Trader...) при включении. Рендерится в /профиль и (опционально) в HUD.
 */
public interface StatsSection {

    /** Заголовок секции (локализованный). */
    String title(Player p, String lang);

    /** Строки значений (локализованные, без чат-префикса). */
    List<String> lines(Player p, String lang);
}
