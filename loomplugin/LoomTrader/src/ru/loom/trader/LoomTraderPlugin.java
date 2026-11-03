package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LoomTrader 1.0.0 — НПС-торговщица Ивлия.
 *
 *  • Дейлики: раз в сутки доска получает до 10 заказов из пула 59 (ресурсы, зелья,
 *    ценные предметы). Награда = рыночная цена × бонус 1.25–1.4 → заказ выгоднее
 *    обычной продажи. Сдача — клик по заказу в GUI у НПС или /квесты сдать <id>,
 *    один заказ один раз в день на игрока (максимум 10/день).
 *
 *  • Глобальный недельный заказ: весь сервер приносит один предмет на общую цель
 *    (например, 2 000 000 пшеницы). Призовой фонд (монеты) делится пропорционально
 *    доле вклада: вклад 20 000 пшеницы из 2 000 000 → 1% фонда. Цель достигнута —
 *    заказ закрывается сразу; срок вышел без цели — фонд всё равно делится по долям.
 *    Прогресс: boss-bar всем игрокам + вехи 25/50/75%.
 *
 * Команды: /квесты (/quests), /заказ (/order), /торговец (/trader, админ).
 */
public final class LoomTraderPlugin extends JavaPlugin {

    private static LoomTraderPlugin instance;

    private TraderStorage storage;
    private DailyManager daily;
    private OrderManager order;
    private TraderNPC npc;
    private TraderGUI gui; // только как фабрика

    public static LoomTraderPlugin get() { return instance; }

    public TraderStorage storage() { return storage; }
    public DailyManager daily() { return daily; }
    public OrderManager order() { return order; }
    public TraderNPC npc() { return npc; }

    int dailyCount() {
        return Math.max(1, Math.min(QuestPool.POOL.size(),
                getConfig().getInt("daily.count", 10)));
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        storage = new TraderStorage(this);
        daily = new DailyManager(this);
        order = new OrderManager(this);
        npc = new TraderNPC(this);

        Bukkit.getPluginManager().registerEvents(new TraderListeners(this), this);
        var exec = new TraderCommands(this);
        for (String name : new String[]{"quests", "order", "trader"}) {
            var cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(exec);
                cmd.setTabCompleter(exec);
            }
        }

        // доска на сегодня (после рестарта в тот же день остаётся той же)
        daily.rollIfNeeded();
        // недельный заказ: активный восстановится в конструкторе OrderManager, нет — автостарт по ротации
        order.ensureWeekly();
        npc.ensure();
        npc.fake().start(); // подмена НПС-игрока для новых зрителей (v1.8)

        // минутный тик: смена дня (новая доска), дедлайн заказа, восстановление НПС
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            daily.rollIfNeeded();
            order.tick();
            npc.ensure();
        }, 20L * 30, 20L * 60);
        // автосохранение
        Bukkit.getScheduler().runTaskTimer(this, storage::saveAll, 20L * 300, 20L * 300);

        TraderHooks.register(this);

        getLogger().info("LoomTrader 1.8.0 включён (НПС: " + getConfig().getString("npc.type", "player") + "): заказов в сутки — " + dailyCount()
                + ", НПС " + (storage.npcUuid != null ? "на месте" : "не поставлен (/торговец spawn)"));
    }

    /** Открыть доску игроку: Bedrock — нативная форма Floodgate, Java — сундук-GUI. */
    void openBoard(Player p) {
        if (ru.loom.core.bedrock.BedrockUi.isBedrock(p.getUniqueId())) {
            TraderForms.board(p);
            return;
        }
        new TraderGUI(p.getUniqueId()).open(p);
    }

    @Override
    public void onDisable() {
        if (npc != null) npc.fake().stop();
        if (storage != null) storage.saveAll();
    }
}
