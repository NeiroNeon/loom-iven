package ru.loom.core.bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Мост к нативным формам Floodgate (Bedrock через Geyser).
 * Класс сам не держит ссылок на классы Floodgate: вся работа — во вложенном
 * Impl, который загружается только при установленном плагине floodgate.
 * Ответы форм приходят из потока Geyser — всегда возвращаемся в главный поток.
 */
public final class BedrockUi {

    private BedrockUi() { }

    /** Установлен ли floodgate. */
    public static boolean available() {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    /** Bedrock-игрок? Если floodgate нет — false (используется сундук-GUI). */
    public static boolean isBedrock(UUID id) {
        return available() && Impl.isBedrock(id);
    }

    /** Простая форма со списком кнопок; onPick получает индекс нажатой кнопки. */
    public static void buttons(Player p, String title, String content,
                               List<String> buttons, IntConsumer onPick) {
        if (!available()) return;
        Impl.buttons(p, title, content, buttons, onPick);
    }

    /** Форма ввода одной строки; onValue получает введённый текст. */
    public static void input(Player p, String title, String label,
                             String placeholder, BiConsumer<Player, String> onValue) {
        if (!available()) return;
        Impl.input(p, title, label, placeholder, onValue);
    }

    private static final class Impl {
        static boolean isBedrock(UUID id) {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(id);
        }

        static void buttons(Player p, String title, String content,
                            List<String> buttons, IntConsumer onPick) {
            var b = org.geysermc.cumulus.form.SimpleForm.builder();
            b.title(title);
            if (content != null && !content.isEmpty()) b.content(content);
            for (String s : buttons) b.button(s);
            b.validResultHandler(response ->
                    back(p.getUniqueId(), pl -> onPick.accept(response.clickedButtonId())));
            org.geysermc.floodgate.api.FloodgateApi.getInstance().sendForm(p.getUniqueId(), b);
        }

        static void input(Player p, String title, String label,
                          String placeholder, BiConsumer<Player, String> onValue) {
            var b = org.geysermc.cumulus.form.CustomForm.builder();
            b.title(title);
            b.label(label);
            b.input(placeholder, "");
            b.validResultHandler(response ->
                    back(p.getUniqueId(), pl -> onValue.accept(pl, response.asInput(0))));
            org.geysermc.floodgate.api.FloodgateApi.getInstance().sendForm(p.getUniqueId(), b);
        }

        /** Возврат в главный поток: игрок мог уже выйти — подставляем актуальный Player. */
        private static void back(UUID id, Consumer<Player> action) {
            var core = ru.loom.core.LoomCorePlugin.get();
            if (core == null || !core.isEnabled()) return;
            Bukkit.getScheduler().runTask(core, () -> {
                Player pl = Bukkit.getPlayer(id);
                if (pl == null) return;
                try {
                    action.accept(pl);
                } catch (Exception ignored) {
                    // ответ формы не должен ронять сервер
                }
            });
        }
    }
}
