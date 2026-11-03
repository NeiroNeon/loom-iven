package ru.loom.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loom.core.command.CoinsCommand;
import ru.loom.core.command.LangCommand;
import ru.loom.core.command.LoomCommand;
import ru.loom.core.command.MenuCommand;
import ru.loom.core.command.ProfileCommand;
import ru.loom.core.gui.GuiListener;
import ru.loom.core.hud.HudService;
import ru.loom.core.menu.CoreActions;
import ru.loom.core.menu.MenuRegistry;
import ru.loom.core.stats.StatsService;

import java.util.UUID;

/**
 * LoomCore 1.5 — ядро сервера Loom Iven.
 * Экономика (монеты), локализация RU/EN на игрока, лестница активности,
 * косметические ранги за часы, анти-АФК, пропуск ночи Ветераном, API для плагинов,
 * GUI-хаб (/меню), профиль (/профиль), HUD-скорборд с логотипом сервера.
 */
public final class LoomCorePlugin extends JavaPlugin {

    private static LoomCorePlugin instance;

    private EconomyManager economy;
    private LangManager lang;
    private RankManager ranks;
    private AFKManager afk;
    private LoomAPI api;
    private MenuRegistry menus;
    private StatsService stats;
    private HudService hud;

    public static LoomCorePlugin get() {
        return instance;
    }

    public LoomAPI api() {
        return api;
    }

    /** Удобный доступ к локализации: msg(uuid, ключ, подстановки...). */
    public String msg(UUID id, String key, String... kv) {
        return api.msg(id, key, kv);
    }

    /** Локализация по коду языка. */
    public String msg(String code, String key, String... kv) {
        return api.msg(code, key, kv);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        economy = new EconomyManager(this);
        lang = new LangManager(this);
        ranks = new RankManager(this);
        afk = new AFKManager(this);

        api = new LoomAPI(this);

        menus = new MenuRegistry();
        stats = new StatsService();
        hud = new HudService(this);

        CoreActions.register(this);

        Bukkit.getPluginManager().registerEvents(afk, this);
        Bukkit.getPluginManager().registerEvents(new NightSkipListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new ru.loom.core.ChatFormat(), this);
        if (ru.loom.core.perm.Lp.available()) {
            getLogger().info("LuckPerms подключён: плашки-префиксы в чате.");
        }

        var cmd = getCommand("loom");
        if (cmd != null) cmd.setExecutor(new LoomCommand(this));
        var coins = getCommand("coins");
        if (coins != null) {
            CoinsCommand cc = new CoinsCommand(this);
            coins.setExecutor(cc);
            coins.setTabCompleter(cc);
        }
        var langCmd = getCommand("lang");
        if (langCmd != null) langCmd.setExecutor(new LangCommand(this));
        var menuCmd = getCommand("menu");
        if (menuCmd != null) menuCmd.setExecutor(new MenuCommand(this));
        var profileCmd = getCommand("profile");
        if (profileCmd != null) profileCmd.setExecutor(new ProfileCommand(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI подключён: %loom_*%");
        }

        // Автосохранение каждые 5 минут
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            economy.save();
            lang.save();
        }, 20L * 300, 20L * 300);

        // HUD-скорборд: старт на следующем тике (после включения зависимых плагинов)
        Bukkit.getScheduler().runTask(this, () -> hud.start());

        getLogger().info("LoomCore включён: экономика, RU/EN, лестница, анти-АФК, меню, профиль, HUD.");
    }

    @Override
    public void onDisable() {
        if (hud != null) hud.stop();
        economy.save();
        lang.save();
        instance = null;
    }

    public EconomyManager economy() { return economy; }
    public LangManager lang() { return lang; }
    public RankManager ranks() { return ranks; }
    public AFKManager afk() { return afk; }
    public MenuRegistry menus() { return menus; }
    public StatsService stats() { return stats; }
    public HudService hud() { return hud; }
}
