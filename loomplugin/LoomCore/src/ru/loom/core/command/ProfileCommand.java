package ru.loom.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.gui.ProfileGUI;

/** /профиль — профиль и статистика игрока (Java: сундук, Bedrock: форма). */
public final class ProfileCommand implements CommandExecutor {

    private final LoomCorePlugin plugin;

    public ProfileCommand(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Только для игроков.");
            return true;
        }
        ProfileGUI.open(p);
        return true;
    }
}
