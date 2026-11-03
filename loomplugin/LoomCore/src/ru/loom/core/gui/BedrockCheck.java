package ru.loom.core.gui;

import org.bukkit.entity.Player;
import ru.loom.core.bedrock.BedrockUi;

/** Единая точка решения: Bedrock-игроку показываем форму, Java — сундук. */
public final class BedrockCheck {

    private BedrockCheck() { }

    public static boolean isBedrock(Player p) {
        return BedrockUi.available() && BedrockUi.isBedrock(p.getUniqueId());
    }
}
