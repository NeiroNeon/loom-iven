package ru.loom.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.loom.core.perm.Lp;

/**
 * Формат чата Loom: плашка-привилегия (LuckPerms prefix) + белый ник +
 * серое двоеточие + сообщение. Работает и для Bedrock (Geyser передаёт чат).
 */
public final class ChatFormat implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent e) {
        e.renderer((source, displayName, message, viewer) -> {
            String prefix = Lp.prefix(source.getUniqueId());
            Component comp = Component.empty();
            if (prefix != null && !prefix.isBlank()) {
                comp = comp.append(LEGACY.deserialize(prefix));
            }
            return comp
                    .append(Component.text(source.getName(), NamedTextColor.WHITE))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(message.colorIfAbsent(NamedTextColor.WHITE));
        });
    }
}
