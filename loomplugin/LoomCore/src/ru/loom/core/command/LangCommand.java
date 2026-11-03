package ru.loom.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;

/** /язык (lang) — переключение личного языка RU/EN. */
public final class LangCommand implements CommandExecutor {

    private final LoomCorePlugin plugin;

    public LangCommand(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (!(s instanceof Player p)) return true;
        String code = args.length >= 1 ? args[0] : "ru";
        if (!code.equalsIgnoreCase("ru") && !code.equalsIgnoreCase("en")) {
            p.sendMessage("§7RU или EN: §f/язык ru §7| §f/lang en");
            return true;
        }
        plugin.api().setLang(p.getUniqueId(), code);
        p.sendMessage(plugin.lang().raw(code, "lang.set", "%lang%", code.toUpperCase()));
        return true;
    }
}
