package ru.loom.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Парсинг MiniMessage-строк (конфиг HUD, заголовки). */
public final class Mini {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Mini() { }

    public static Component of(String miniMessage) {
        try {
            return MM.deserialize(miniMessage == null ? "" : miniMessage);
        } catch (Exception ex) {
            return Component.text(miniMessage == null ? "" : miniMessage);
        }
    }
}
