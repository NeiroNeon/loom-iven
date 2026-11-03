package ru.loom.core.menu;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Реестр пунктов главного меню + страницы «Команды» + админ-секции плагинов.
 * Всё описывается одним интерфейсом MenuAction (категория — доп. поле).
 */
public final class MenuRegistry {

    private final List<MenuAction> actions = new CopyOnWriteArrayList<>();
    private final List<MenuAction> commands = new CopyOnWriteArrayList<>();
    private final List<MenuAction> adminSections = new CopyOnWriteArrayList<>();

    public void register(MenuAction action) {
        actions.removeIf(a -> a.id().equals(action.id()));
        actions.add(action);
    }

    /** Регистрация команды для страницы «Команды» (описания в лорах). */
    public void registerCommand(MenuAction entry) {
        commands.removeIf(a -> a.id().equals(entry.id()));
        commands.add(entry);
    }

    /** Регистрация админ-секции для админ-панели (GUI/форма секции открывает click()). */
    public void registerAdmin(MenuAction section) {
        adminSections.removeIf(a -> a.id().equals(section.id()));
        adminSections.add(section);
    }

    /** Видимые игроку пункты меню, отсортированные по order. */
    public List<MenuAction> visible(Player p) {
        List<MenuAction> out = new ArrayList<>();
        for (MenuAction a : actions) {
            if (!a.adminOnly() || p.hasPermission("loom.admin")) out.add(a);
        }
        out.sort(Comparator.comparingInt(MenuAction::order));
        return out;
    }

    /** Видимые команды категории (для страницы «Команды»). */
    public List<MenuAction> visibleCommands(Player p, String category) {
        List<MenuAction> out = new ArrayList<>();
        for (MenuAction a : commands) {
            if (!a.category().equals(category)) continue;
            if (a.adminOnly() && !p.hasPermission("loom.admin")) continue;
            out.add(a);
        }
        out.sort(Comparator.comparingInt(MenuAction::order));
        return out;
    }

    /** Все категории команд, видимые игроку (по убыванию наличия команд). */
    public List<String> commandCategories(Player p) {
        List<String> out = new ArrayList<>();
        for (MenuAction a : commands) {
            if (a.adminOnly() && !p.hasPermission("loom.admin")) continue;
            if (!out.contains(a.category())) out.add(a.category());
        }
        return out;
    }

    /** Видимые админ-секции (только loom.admin). */
    public List<MenuAction> visibleAdmin(Player p) {
        List<MenuAction> out = new ArrayList<>();
        for (MenuAction a : adminSections) {
            if (!a.adminOnly() || p.hasPermission("loom.admin")) out.add(a);
        }
        out.sort(Comparator.comparingInt(MenuAction::order));
        return out;
    }
}
