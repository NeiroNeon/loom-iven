package ru.loom.progress;

import java.util.Map;
import org.bukkit.Bukkit;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сезонный пропуск v1.8 — реальные сутки.
 *
 *  — Активация: первый вход игрока после старта сезона запускает личный
 *    30-дневный цикл (passStart). Перезаходы день НЕ двигают.
 *  — День N открыт ровно в своём окне [N-1·24ч, N·24ч) от активации;
 *    награда дня забирается только внутри его окна.
 *  — Пропустил окно (не забрал за сутки) — день сгорает: задним числом
 *    забрать нельзя. Хроно-предмет (донат-ветка, дни 5/10/15/20/25/30)
 *    восстанавливает один сгоревший день с выдачей награды.
 *  — Донат-ветка: 25 000 монет; двойные монеты; уникальная косметика с 15-го
 *    дня; ключи на днях кратных 7 — только в бесплатной ветке (без дублей).
 *  — Миграция: косметика сезона прошлого сезона доступна в бесплатной ветке
 *    с 25-го дня следующего.
 */
public final class SeasonPassManager {

    public static final int SEASON_DAYS = 30;
    public static final long DAY_MS = 86_400_000L;
    /** Донат-дни, дающие хроно-предмет (восстановление сгоревшего дня). */
    public static final int[] CHRONO_DAYS = {5, 10, 15, 20, 25, 30};

    private final ProgressPlugin plugin;

    public enum DayState { FUTURE, OPEN, MISSED, CLAIMED }

    public SeasonPassManager(ProgressPlugin plugin) {
        this.plugin = plugin;
        var st = plugin.storage();
        if (st.seasonStart == 0L) {
            st.seasonStart = System.currentTimeMillis();
            st.seasonId = "S1";
            st.saveSeason();
        }
    }

    // ---------------- цикл игрока ----------------

    /** День сезона 1..30, или 0 — цикл ещё не активирован. */
    public int dayOf(UUID id) {
        long start = plugin.storage().passStart.getOrDefault(id, 0L);
        if (start == 0L) return 0;
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed < 0) elapsed = 0;
        return Math.min(SEASON_DAYS, (int) (elapsed / DAY_MS) + 1);
    }

    /** Активация цикла при первом входе (перезаходы день не двигают). */
    public void touchDay(UUID id) {
        var st = plugin.storage();
        if (!st.passStart.containsKey(id)) {
            st.passStart.put(id, System.currentTimeMillis());
            st.saveSeason();
        }
    }

    /** Окно дня N: [начало, конец) в epoch ms. */
    public long windowStart(UUID id, int day) {
        return plugin.storage().passStart.getOrDefault(id, 0L) + (day - 1L) * DAY_MS;
    }

    public long windowEnd(UUID id, int day) {
        return plugin.storage().passStart.getOrDefault(id, 0L) + (long) day * DAY_MS;
    }

    public boolean isChronoDay(int day) {
        for (int d : CHRONO_DAYS) if (d == day) return true;
        return false;
    }

    /** Состояние дня в конкретной ветке. */
    public DayState stateOf(UUID id, int day, boolean donate) {
        String rid = (donate ? "d" : "f") + day;
        if (plugin.storage().passClaimed.getOrDefault(id, List.of()).contains(rid)) {
            return DayState.CLAIMED;
        }
        long now = System.currentTimeMillis();
        if (now < windowStart(id, day)) return DayState.FUTURE;
        if (now >= windowEnd(id, day)) return DayState.MISSED;
        return DayState.OPEN;
    }

    public int chronoOf(UUID id) {
        return plugin.storage().passChrono.getOrDefault(id, 0);
    }

    public boolean hasDonateTrack(UUID id) {
        return plugin.storage().passDonate.getOrDefault(id, false);
    }

    public boolean buyDonateTrack(UUID id, String name) {
        var core = LoomCorePlugin.get();
        double price = plugin.getConfig().getDouble("season.donate-price-coins", 25000);
        if (hasDonateTrack(id)) return false;
        if (!core.api().withdraw(id, name, price)) return false;
        plugin.storage().passDonate.put(id, true);
        plugin.storage().saveSeason();
        core.lang().broadcast("pass.bought", "%player%", name);
        return true;
    }

    // ---------------- клейм и восстановление ----------------

    /** id награды: "d{день}" — донат, "f{день}" — бесплатная. Ошибка или null. */
    public String claim(UUID id, String rewardId) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        boolean isDonateReward = rewardId.startsWith("d");
        int dayNum;
        try { dayNum = Integer.parseInt(rewardId.substring(1)); }
        catch (NumberFormatException ex) { return "§cНекорректная награда."; }
        if (dayNum < 1 || dayNum > SEASON_DAYS) return "§cНекорректная награда.";

        if (isDonateReward && !hasDonateTrack(id)) return core.msg(id, "pass.need-donate");

        DayState state = stateOf(id, dayNum, isDonateReward);
        switch (state) {
            case CLAIMED -> { return core.msg(id, "pass.already"); }
            case FUTURE -> {
                long ms = windowStart(id, dayNum) - System.currentTimeMillis();
                return core.msg(id, "pass.day-not-reached", "%day%", String.valueOf(dayNum),
                        "%time%", ru.loom.core.gui.GuiUtil.dur(ms, core.api().lang(id)));
            }
            case MISSED -> {
                return core.msg(id, "pass.day-missed", "%day%", String.valueOf(dayNum));
            }
            case OPEN -> { /* забираем */ }
        }

        List<String> claimed = st.passClaimed.computeIfAbsent(id, k -> new ArrayList<>());
        claimed.add(rewardId);
        st.saveSeason();
        reward(id, dayNum, isDonateReward);
        return null;
    }

    /** Восстановить сгоревший день хроно-предметом и сразу выдать награду. */
    public String restore(UUID id, String rewardId) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        int dayNum;
        try { dayNum = Integer.parseInt(rewardId.substring(1)); }
        catch (NumberFormatException ex) { return "§cНекорректная награда."; }
        boolean isDonateReward = rewardId.startsWith("d");
        if (stateOf(id, dayNum, isDonateReward) != DayState.MISSED) {
            return core.msg(id, "pass.restore-not-needed");
        }
        int chrono = chronoOf(id);
        if (chrono <= 0) return core.msg(id, "pass.no-chrono");
        st.passChrono.put(id, chrono - 1);
        st.saveSeason();
        return forceClaimMissed(id, rewardId);
    }

    /** Награда дня (вызывается после успешного claim/restore). */
    private void grantReward(UUID id, int day, boolean donate) {
        var core = LoomCorePlugin.get();
        double coins = plugin.getConfig().getDouble("season.rewards.coins-per-day", 300);
        core.api().add(id, nameOf(id), coins);

        // миграция: бесплатная ветка 25+ прошлого сезона
        var st = plugin.storage();
        String migrated = st.prevSeasonCosmetic.get(id);
        if (!donate && day >= 25 && migrated != null) {
            var owned = st.cosmeticsOwned.computeIfAbsent(id, k -> new ArrayList<>());
            if (!owned.contains(migrated)) {
                owned.add(migrated);
                core.lang().send(Bukkit.getPlayer(id), "pass.cosmetic", "%item%", migrated);
            }
        }
        if (donate && day >= 15) {
            // уникальная косметика сезона с 15-го дня донат-ветки
            String cosmetic = "season_" + st.seasonId.toLowerCase() + "_cosmetic";
            var owned = st.cosmeticsOwned.computeIfAbsent(id, k -> new ArrayList<>());
            if (!owned.contains(cosmetic)) {
                owned.add(cosmetic);
                core.lang().send(Bukkit.getPlayer(id), "pass.cosmetic", "%item%", cosmetic);
            }
        }
        if (donate && isChronoDay(day)) {
            st.passChrono.merge(id, 1, Integer::sum);
            st.saveSeason();
            core.lang().send(Bukkit.getPlayer(id), "pass.chrono-gained");
        }
        // ключ на днях кратных 7 — только в бесплатной ветке (фикс двойной выдачи)
        if (!donate && day % 7 == 0) plugin.keys().giveKey(id, 1);
    }

    /** Обёртка для restore: клейм сгоревшего дня напрямую (минуя state-проверку). */
    public String forceClaimMissed(UUID id, String rewardId) {
        var st = plugin.storage();
        List<String> claimed = st.passClaimed.computeIfAbsent(id, k -> new ArrayList<>());
        if (claimed.contains(rewardId)) return LoomCorePlugin.get().msg(id, "pass.already");
        int dayNum;
        try { dayNum = Integer.parseInt(rewardId.substring(1)); }
        catch (NumberFormatException ex) { return "§cНекорректная награда."; }
        boolean donate = rewardId.startsWith("d");
        if (donate && !hasDonateTrack(id)) return LoomCorePlugin.get().msg(id, "pass.need-donate");
        claimed.add(rewardId);
        st.saveSeason();
        grantReward(id, dayNum, donate);
        return null;
    }

    /** Совместимость: claim без окна (для старых вызовов). */
    private void reward(UUID id, int day, boolean donate) {
        grantReward(id, day, donate);
    }

    // ---------------- смена сезона ----------------

    /** Смена сезона: косметика донат-ветки прошлого сезона → бесплатная ветка 25+. */
    public void tick() {
        var st = plugin.storage();
        long startedMs = st.seasonStart;
        long daysElapsed = (System.currentTimeMillis() - startedMs) / (SEASON_DAYS * DAY_MS);
        if (daysElapsed >= 1) {
            String oldSeason = st.seasonId;
            int num = Integer.parseInt(st.seasonId.substring(1)) + 1;
            st.seasonId = "S" + num;
            st.seasonStart = startedMs + SEASON_DAYS * DAY_MS;
            // миграция: кто владеет косметикой прошлого сезона (донат 15+) — получит её в f25+
            String oldCosmetic = "season_" + oldSeason.toLowerCase() + "_cosmetic";
            for (Map.Entry<UUID, List<String>> e : st.cosmeticsOwned.entrySet()) {
                if (e.getValue().contains(oldCosmetic)) {
                    st.prevSeasonCosmetic.put(e.getKey(), oldCosmetic);
                }
            }
            st.passStart.clear();
            st.passChrono.clear();
            st.passDonate.clear();
            st.passClaimed.clear();
            st.saveSeason();
            var core = LoomCorePlugin.get();
            core.lang().broadcast("pass.new-season", "%old%", oldSeason, "%new%", st.seasonId);
            core.lang().broadcastDual(
                    "§7Косметика прошлого сезона теперь в бесплатной ветке с §f25-го дня§7.",
                    "§7Last season's cosmetics are now in the free track from §fday 25§7.");
        }
    }

    // ---------------- админ-операции (GUI) ----------------

    public void adminGrantChrono(UUID id, int n) {
        plugin.storage().passChrono.merge(id, n, Integer::sum);
        plugin.storage().saveSeason();
    }

    public void adminGrantDonate(UUID id) {
        plugin.storage().passDonate.put(id, true);
        plugin.storage().saveSeason();
    }

    /** Полный сброс цикла: игрок заново активирует пропуск при следующем входе. */
    public void adminReset(UUID id) {
        var st = plugin.storage();
        st.passStart.remove(id);
        st.passDonate.remove(id);
        st.passClaimed.remove(id);
        st.passChrono.remove(id);
        st.saveSeason();
    }

    /** Принудительный переход к новому сезону (владелец). */
    public void adminForceSeason() {
        var st = plugin.storage();
        st.seasonStart = 0; // tick() пересоздаст с новым id? — нет, просто сдвинем
        st.seasonStart = System.currentTimeMillis();
        st.saveSeason();
    }

    private String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }
}
