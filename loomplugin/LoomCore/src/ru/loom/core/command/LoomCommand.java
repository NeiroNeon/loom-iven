package ru.loom.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.loom.core.LoomAPI;
import ru.loom.core.LoomCorePlugin;

import java.util.UUID;

/** /loom — админ-команда ядра. */
public final class LoomCommand implements CommandExecutor {

    private final LoomCorePlugin plugin;

    public LoomCommand(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (args.length == 0) {
            s.sendMessage("§6LoomCore §7— ядро Loom Iven §6" + plugin.getDescription().getVersion());
            s.sendMessage("§7/loom coins <give|take|set> <ник> <сумма> §f— экономика");
            s.sendMessage("§7/loom reload §f— перечитать конфиги");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!s.hasPermission("loom.admin")) { s.sendMessage("§cНедостаточно прав."); return true; }
                plugin.reloadConfig();
                s.sendMessage("§aКонфиги перечитаны.");
            }
            case "coins" -> {
                if (!s.hasPermission("loom.admin")) { s.sendMessage("§cНедостаточно прав."); return true; }
                if (args.length < 4) { s.sendMessage("§7/loom coins <give|take|set> <ник> <сумма>"); return true; }
                var target = Bukkit.getOfflinePlayer(args[2]);
                double amount;
                try { amount = Double.parseDouble(args[3]); }
                catch (NumberFormatException ex) { s.sendMessage("§cСумма — число."); return true; }
                UUID id = target.getUniqueId();
                String name = target.getName() != null ? target.getName() : args[2];
                switch (args[1].toLowerCase()) {
                    case "give" -> plugin.api().add(id, name, amount);
                    case "take" -> plugin.api().withdraw(id, name, amount);
                    case "set" -> plugin.api().set(id, name, amount);
                    default -> { s.sendMessage("§7give | take | set"); return true; }
                }
                s.sendMessage("§aГотово: " + name + " → " + LoomAPI.fmt(plugin.api().coins(id)));
            }
            default -> s.sendMessage("§7/loom coins | reload");
        }
        return true;
    }
}
