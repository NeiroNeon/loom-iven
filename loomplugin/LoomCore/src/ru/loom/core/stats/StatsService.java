package ru.loom.core.stats;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Реестр секций статистики профиля. Плагины регистрируются при своём onEnable. */
public final class StatsService {

    private final Map<String, StatsSection> sections = new LinkedHashMap<>();

    /** Регистрация/замена секции по id. */
    public void register(String id, StatsSection section) {
        sections.put(id, section);
    }

    public StatsSection section(String id) {
        return sections.get(id);
    }

    public List<StatsSection> all() {
        return new ArrayList<>(sections.values());
    }
}
