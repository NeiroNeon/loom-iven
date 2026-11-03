package ru.loom.progress;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LoomProgress 1.7.0 — прогресс, ивенты и сезонный контент Loom Iven.
 *
 * v1.4 (перенос): ивенты новичок/Хранитель+ по расписанию, 36 ачивок в 10 категориях,
 * сезонный пропуск (30 дней, донат-косметика мигрирует в бесплатную ветку на 25+ день
 * следующего сезона), ключи за часы выше Легенды, менторы с многоразовыми кодами,
 * зал славы, торговый квартал с участками.
 *
 * v1.5–1.6: «Общий сбор» — событие с целевой суммой и вкладами; правило
 * «Безубыточного участника»: фонд выплат 120% собранного, выплата каждого
 * зажата в 105–150% вклада, недобор <50% цели — полный возврат.
 *
 * v1.7 (новое): GUI-интеграция с ядром — панель сбора (вклад пресетами и
 * произвольной суммой), панели достижений/пропуска/косметики, секция профиля,
 * переменная HUD; Java — сундуки, Bedrock — формы Floodgate.
 */
public final class ProgressPlugin extends JavaPlugin {

    private ProgressCommands commands;

    private static ProgressPlugin instance;

    private Storage storage;
    private EventsManager events;
    private AchievementsManager achievements;
    private SeasonPassManager season;
    private KeysManager keys;
    private MentorManager mentors;
    private HallOfFame fame;
    private QuarterManager quarter;
    private FundManager fund;
    private ChatInputs chats;

    public static ProgressPlugin get() { return instance; }

    public Storage storage() { return storage; }
    public EventsManager events() { return events; }
    public AchievementsManager achievements() { return achievements; }
    public SeasonPassManager season() { return season; }
    public KeysManager keys() { return keys; }
    public MentorManager mentors() { return mentors; }
    public HallOfFame fame() { return fame; }
    public QuarterManager quarter() { return quarter; }
    public FundManager fund() { return fund; }
    public ChatInputs chats() { return chats; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        storage = new Storage(this);
        keys = new KeysManager(this);
        achievements = new AchievementsManager(this);
        events = new EventsManager(this);
        season = new SeasonPassManager(this);
        mentors = new MentorManager(this);
        fame = new HallOfFame(this);
        quarter = new QuarterManager(this);
        fund = new FundManager(this);
        chats = new ChatInputs(this);
        ProgressHooks.register(this);

        Bukkit.getPluginManager().registerEvents(new ProgressListeners(this), this);
        Bukkit.getPluginManager().registerEvents(quarter, this);
        Bukkit.getPluginManager().registerEvents(fund, this);

        var exec = new ProgressCommands(this);
        this.commands = exec;
        for (String name : new String[]{"events", "ach", "pass", "keys", "mentor", "fame", "quarter", "plot", "fund"}) {
            var cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(exec);
                cmd.setTabCompleter(exec);
            }
        }

        // минутный тик: расписание ивентов и сезона, ключи за часы, общий сбор
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            events.tick();
            season.tick();
            keys.tick();
            fund.tick();
        }, 20L * 20, 20L * 60);
        // автосохранение
        Bukkit.getScheduler().runTaskTimer(this, storage::saveAll, 20L * 300, 20L * 300);

        getLogger().info("LoomProgress 1.8.0 включён: ивенты, ачивки, пропуск (реальные сутки), ключи, менторы, слава, квартал, общий сбор, GUI.");
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.saveAll();
        instance = null;
    }

    public ProgressCommands commands() { return commands; }

    /** Статус ивентов как GUI-список (страница «Команды»). */
    public void eventsCommandGui(Player p) {
        commands().eventsGui(p);
    }

    /** Зал славы как GUI-список. */
    public void fameCommandGui(Player p) {
        commands().fameGui(p);
    }
}
