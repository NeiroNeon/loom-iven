package ru.loom.progress;

import org.bukkit.Bukkit;
import ru.loom.core.LoomCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Менторская система (v1.4).
 *  — Старожил+ (336 ч) создаёт персональный МНОГОРАЗОВЫЙ код: /ментор код;
 *  — код можно посмотреть повторно (и вывести на будущий сайт в профиле игрока);
 *  — новичок подтверждает наставничество: /ментор код <код>;
 *  — награды ментору за каждого ученика; данные (ментор/ученик/код) — для сайта.
 */
public final class MentorManager {

    private final ProgressPlugin plugin;

    public MentorManager(ProgressPlugin plugin) {
        this.plugin = plugin;
    }

    /** Может ли игрок быть ментором (Старожил+). */
    public boolean canMentor(UUID id) {
        var core = LoomCorePlugin.get();
        long need = core.ranks().cosmeticThresholdMinutes("elder");
        return core.api().playtimeMinutes(id) >= need;
    }

    /** Сгенерировать/показать персональный код. */
    public String myCode(UUID id) {
        var st = plugin.storage();
        for (Map.Entry<String, UUID> e : st.codes.entrySet()) {
            if (e.getValue().equals(id)) return e.getKey();
        }
        String code = "LOOM-" + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000));
        st.codes.put(code, id);
        st.saveMentors();
        return code;
    }

    public UUID mentorByCode(String code) {
        return plugin.storage().codes.get(code.toUpperCase());
    }

    /** Подтверждение наставничества учеником. Возвращает ошибку или null. */
    public String confirm(UUID student, String studentName, String code) {
        var st = plugin.storage();
        var core = LoomCorePlugin.get();
        if (st.mentorOf.containsKey(student)) return core.msg(student, "mentor.already-student");
        UUID mentor = mentorByCode(code);
        if (mentor == null) return core.msg(student, "mentor.bad-code");
        if (mentor.equals(student)) return core.msg(student, "mentor.self");
        st.mentorOf.put(student, mentor);
        st.students.computeIfAbsent(mentor, k -> new ArrayList<>()).add(student);
        st.saveMentors();
        // награды
        double coins = plugin.getConfig().getDouble("mentor.reward-coins", 500);
        int keys = plugin.getConfig().getInt("mentor.reward-keys", 1);
        core.api().add(mentor, nameOf(mentor), coins);
        if (keys > 0) plugin.keys().giveKey(mentor, keys);
        double studentCoins = plugin.getConfig().getDouble("mentor.student-coins", 250);
        core.api().add(student, studentName, studentCoins);
        core.lang().broadcast("mentor.confirmed",
                "%mentor%", nameOf(mentor), "%student%", studentName);
        return null;
    }

    public UUID mentorOf(UUID student) {
        return plugin.storage().mentorOf.get(student);
    }

    public List<UUID> studentsOf(UUID mentor) {
        return plugin.storage().students.getOrDefault(mentor, new ArrayList<>());
    }

    /** Данные для будущего сайта: ник ментора, код, список учеников. */
    public String profileLine(UUID mentor) {
        var core = LoomCorePlugin.get();
        String code = myCode(mentor);
        int n = studentsOf(mentor).size();
        return core.msg(mentor, "mentor.profile",
                "%code%", code, "%students%", String.valueOf(n));
    }

    private String nameOf(UUID id) {
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "???";
    }
}
