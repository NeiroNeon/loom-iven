package ru.loom.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** PlaceholderAPI: %loom_coins%, %loom_rank%, %loom_cosmetic%, %loom_hours%, %loom_lang%. */
final class PlaceholderHook extends PlaceholderExpansion {

    private final LoomCorePlugin plugin;

    PlaceholderHook(LoomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "loom"; }

    @Override
    public @NotNull String getAuthor() { return "Loom Iven"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String params) {
        if (p == null) return "";
        return switch (params) {
            case "coins" -> LoomAPI.fmt(plugin.api().coins(p.getUniqueId()));
            case "rank" -> plugin.lang().raw(plugin.api().lang(p.getUniqueId()),
                    "ladder." + plugin.api().ladderRank(p.getUniqueId()));
            case "cosmetic" -> plugin.lang().raw(plugin.api().lang(p.getUniqueId()),
                    "cosmetic." + plugin.api().cosmeticRank(p.getUniqueId()));
            case "hours" -> String.valueOf(plugin.api().playtimeMinutes(p.getUniqueId()) / 60);
            case "lang" -> plugin.api().lang(p.getUniqueId());
            default -> null;
        };
    }
}
