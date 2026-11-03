package ru.loom.core.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Кнопка главного меню (/меню). Плагины регистрируют свои пункты при onEnable;
 * порядок задаётся полем order, Bedrock-игрокам кнопка показывается формой.
 */
public interface MenuAction {

    String id();

    /** Порядок сортировки (меньше — выше). */
    int order();

    /** true — показывать только с правом loom.admin. */
    boolean adminOnly();

    Material icon(Player p);

    /** Локализованное название (без префикса чата). */
    String name(Player p, String lang);

    /** Локализованный лор (описание/статистика/подсказка по клику). */
    List<String> lore(Player p, String lang);

    /** Действие по клику. Вызывается в главный поток на тик позже клика. */
    void click(Player p);

    /** Категория для страницы «Команды»: core/economy/progress/trade/admin. */
    default String category() { return "core"; }

    /** Краткое использование для лора команды («/меню» и т.п.); пусто — не показывать. */
    default String usage() { return ""; }
}
