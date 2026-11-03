package ru.loom.progress;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.loom.core.LoomCorePlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Ввод произвольной суммы вклада в чат (Java-игроки): после клика «Другая
 * сумма» следующий чат-месседж трактуется как число. «отмена»/«cancel» — отмена.
 */
public final class ChatInputs implements Listener {

    private static final long TIMEOUT_MS = 30_000L;

    private record Pending(long at, Consumer<String> callback) { }

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInputs(ProgressPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p, String promptKey, Consumer<String> onValue) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        pending.put(p.getUniqueId(), new Pending(System.currentTimeMillis(), onValue));
        p.closeInventory();
        p.sendMessage(core.lang().raw(lang, promptKey));
        p.sendMessage(core.lang().raw(lang, "gui.chatinput.cancel-hint"));
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Pending pd = pending.remove(e.getPlayer().getUniqueId());
        if (pd == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(LoomCorePlugin.get(), () -> {
            if ("отмена".equalsIgnoreCase(text) || "cancel".equalsIgnoreCase(text)) return;
            if (System.currentTimeMillis() - pd.at() > TIMEOUT_MS) return;
            try {
                pd.callback().accept(text);
            } catch (Exception ignored) {
            }
        });
    }
}
