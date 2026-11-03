package ru.loom.core.hud;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * HUD-скорборд (справа): логотип сервера в заголовке и полезные строки.
 * Строки — команды с префиксом/суффиксом (обновление без мерцания).
 * Bedrock через Geyser рендерит тот же sidebar автоматически.
 *
 * Переменные строк: %online% %coins% %hours% %rank% %cosmetic% — ядро;
 * другие плагины добавляют свои через registerVar("fund", ...).
 */
public final class HudService implements Listener {

    private static final int MAX_LINES = 15;

    private final LoomCorePlugin plugin;
    private final Map<UUID, Board> boards = new ConcurrentHashMap<>();
    private final Map<String, BiFunction<Player, String, String>> vars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public HudService(LoomCorePlugin plugin) {
        this.plugin = plugin;
        vars.put("online", (p, l) -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        vars.put("coins", (p, l) -> plugin.api().fmt(plugin.api().coins(p.getUniqueId())));
        vars.put("hours", (p, l) -> String.valueOf(plugin.api().playtimeMinutes(p.getUniqueId()) / 60));
        vars.put("rank", (p, l) -> plugin.lang().raw(l, "ladder." + plugin.api().ladderRank(p.getUniqueId())));
        vars.put("cosmetic", (p, l) -> {
            String key = plugin.api().cosmeticRank(p.getUniqueId());
            return "none".equals(key) ? plugin.lang().raw(l, "cosmetic.none")
                    : plugin.lang().raw(l, "cosmetic." + key);
        });
        // ---- окружение игрока (v1.8) ----
        vars.put("biome", (p, l) -> ru.loom.core.gui.GuiUtil.biome(
                p.getLocation().getBlock().getBiome(), l));
        vars.put("coords", (p, l) -> {
            var loc = p.getLocation();
            return (int) Math.floor(loc.getX()) + " " + (int) Math.floor(loc.getY())
                    + " " + (int) Math.floor(loc.getZ());
        });
        vars.put("dir", (p, l) -> ru.loom.core.gui.GuiUtil.dir(p.getLocation().getYaw(), l));
        vars.put("time", (p, l) -> ru.loom.core.gui.GuiUtil.clock(p.getWorld().getTime()));
        vars.put("weather", (p, l) -> p.getWorld().isThundering() ? plugin.lang().raw(l, "hud.thunder")
                : p.getWorld().hasStorm() ? plugin.lang().raw(l, "hud.rain")
                : plugin.lang().raw(l, "hud.clear"));
        vars.put("world", (p, l) -> ru.loom.core.gui.GuiUtil.worldName(p.getWorld().getName(), l));
        vars.put("ping", (p, l) -> p.getPing() + ("en".equals(l) ? "ms" : " мс"));
        vars.put("session", (p, l) -> plugin.afk().sessionActiveMinutes(p.getUniqueId())
                + ("en".equals(l) ? "m" : " мин"));
    }

    /** Регистрация переменной строк HUD (плагинами). */
    public void registerVar(String name, BiFunction<Player, String, String> value) {
        vars.put(name, value);
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("hud.enabled", true)) return;
        int seconds = Math.max(1, plugin.getConfig().getInt("hud.update-seconds", 2));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 20L, 20L * seconds);
        for (Player p : Bukkit.getOnlinePlayers()) attach(p);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        boards.clear();
    }

    // ---------------- события ----------------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("hud.enabled", true)) return;
        attach(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
    }

    // ---------------- сборка и обновление ----------------

    private void attach(Player p) {
        Board b = new Board();
        String title = plugin.getConfig().getString("hud.title",
                "<bold><color:#E8DED0>LOOM</color> <color:#D08A4A>IVEN</color></bold>");
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        b.obj = sb.registerNewObjective("loom_hud", Criteria.DUMMY,
                ru.loom.core.text.Mini.of(title), RenderType.INTEGER);
        b.obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = org.bukkit.ChatColor.values()[i].toString();
            b.entries[i] = entry;
            b.teams[i] = sb.registerNewTeam("h" + i);
            b.teams[i].addEntry(entry);
            b.obj.getScore(entry).setScore(MAX_LINES - i);
        }
        b.rendered = new String[MAX_LINES];
        boards.put(p.getUniqueId(), b);
        p.setScoreboard(sb);
        tick(p);
    }

    private void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) tick(p);
    }

    private void tick(Player p) {
        Board b = boards.get(p.getUniqueId());
        if (b == null) {
            attach(p);
            return;
        }
        String lang = plugin.api().lang(p.getUniqueId());
        List<String> lines = resolveLines(p, lang);
        for (int i = 0; i < MAX_LINES; i++) {
            String text = i < lines.size() ? lines.get(i) : "";
            if (text.equals(b.rendered[i])) continue;
            b.rendered[i] = text;
            if (text.isEmpty()) {
                b.teams[i].prefix(net.kyori.adventure.text.Component.empty());
                b.teams[i].suffix(net.kyori.adventure.text.Component.empty());
            } else {
                b.teams[i].prefix(ru.loom.core.text.Mini.of(text));
                b.teams[i].suffix(net.kyori.adventure.text.Component.empty());
            }
        }
    }

    private List<String> resolveLines(Player p, String lang) {
        String base = "en".equals(lang) ? "hud.lines-en" : "hud.lines-ru";
        List<String> cfg = plugin.getConfig().getStringList(base);
        if (cfg.isEmpty()) cfg = plugin.getConfig().getStringList("hud.lines-ru");
        List<String> out = new ArrayList<>();
        for (String raw : cfg) {
            if (out.size() >= MAX_LINES) break;
            out.add(resolve(raw, p, lang));
        }
        return out;
    }

    private String resolve(String raw, Player p, String lang) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            int open = raw.indexOf('%', i);
            if (open < 0) {
                sb.append(raw, i, raw.length());
                break;
            }
            int close = raw.indexOf('%', open + 1);
            if (close < 0) {
                sb.append(raw, i, raw.length());
                break;
            }
            String name = raw.substring(open + 1, close);
            var fn = vars.get(name);
            String v = "";
            if (fn != null) {
                try {
                    v = fn.apply(p, lang);
                } catch (Exception ex) {
                    v = "";
                }
                if (v == null) v = "";
            }
            sb.append(raw, i, open).append(v);
            i = close + 1;
        }
        return sb.toString();
    }

    /** Скорборд одного игрока (объект-держатель строк). */
    private static final class Board {
        Objective obj;
        final Team[] teams = new Team[MAX_LINES];
        final String[] entries = new String[MAX_LINES];
        String[] rendered;
    }
}
