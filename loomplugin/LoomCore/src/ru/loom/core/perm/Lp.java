package ru.loom.core.perm;

import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * Мост к LuckPerms: префикс-плашка и основная группа игрока.
 * Классы LuckPerms живут во вложенном Impl и загружаются только при
 * установленном плагине (same pattern, что и BedrockUi).
 */
public final class Lp {

    private Lp() { }

    public static boolean available() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    /** Плашка-префикс игрока (legacy &-коды) или "" — для чата и GUI. */
    public static String prefix(UUID id) {
        if (!available()) return "";
        return Impl.prefix(id);
    }

    /** Основная группа LP или "default". */
    public static String primaryGroup(UUID id) {
        if (!available()) return "default";
        return Impl.group(id);
    }

    private static final class Impl {
        static String prefix(UUID id) {
            try {
                var lp = net.luckperms.api.LuckPermsProvider.get();
                var user = lp.getUserManager().getUser(id);
                if (user == null) return "";
                var md = user.getCachedData()
                        .getMetaData(net.luckperms.api.query.QueryOptions.nonContextual());
                String prefix = md.getPrefix();
                return prefix == null ? "" : prefix;
            } catch (Throwable t) {
                return "";
            }
        }

        static String group(UUID id) {
            try {
                var lp = net.luckperms.api.LuckPermsProvider.get();
                var user = lp.getUserManager().getUser(id);
                return user == null ? "default" : user.getPrimaryGroup();
            } catch (Throwable t) {
                return "default";
            }
        }
    }
}
