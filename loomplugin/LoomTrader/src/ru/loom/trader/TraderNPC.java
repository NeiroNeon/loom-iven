package ru.loom.trader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

/**
 * НПС-торговец «Ивлия» (v1.8). Два вида:
 *  — npc.type: player (по умолчанию) — игрок-НПС со скином (FakePlayerNPC,
 *    серверная стойка-хитбокс + клиентская подмена через ProtocolLib);
 *  — npc.type: villager — прежний ванильный житель.
 * Правый клик — открыть доску заказов. Позиция и UUID — в data/trader.yml.
 */
final class TraderNPC {

    private final LoomTraderPlugin plugin;
    private final FakePlayerNPC fake;

    TraderNPC(LoomTraderPlugin plugin) {
        this.plugin = plugin;
        this.fake = new FakePlayerNPC(plugin);
    }

    FakePlayerNPC fake() {
        return fake;
    }

    boolean playerMode() {
        return fake.enabled();
    }

    /** Найти живую сущность НПС по сохранённому UUID (если чанк загружен). */
    Entity findEntity() {
        var st = plugin.storage();
        if (st.npcUuid == null) return null;
        java.util.UUID id;
        try { id = java.util.UUID.fromString(st.npcUuid); } catch (IllegalArgumentException ex) { return null; }
        for (World w : Bukkit.getWorlds()) {
            Entity e = w.getEntity(id);
            if (e != null && e.isValid()) return e;
        }
        return null;
    }

    /** Гарантировать наличие НПС (старт + ретраи). Возвращает true, если есть. */
    boolean ensure() {
        if (findEntity() != null) return true;
        Location loc = plugin.storage().npcLocation();
        if (loc == null) return false;          // y: -1 — автоспавн отключён
        if (!loc.isWorldLoaded() || !loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }
        return spawn(loc) != null;
    }

    /** Спавн в заданной точке (сохраняет UUID и координаты). */
    Entity spawn(Location loc) {
        var st = plugin.storage();
        Entity e;
        if (playerMode()) {
            e = fake.spawn(loc, java.util.UUID.randomUUID());
        } else {
            String name = plugin.getConfig().getString("npc.name", "§6Ивлия");
            e = loc.getWorld().spawn(loc, Villager.class, villager -> {
                villager.setProfession(Villager.Profession.NONE);
                villager.setAI(false);
                villager.setSilent(true);
                villager.setPersistent(true);
                villager.setRemoveWhenFarAway(false);
                villager.setInvulnerable(true);
                villager.setCanPickupItems(false);
                villager.setCollidable(false);
                villager.setCustomName(name);
                villager.setCustomNameVisible(true);
            });
        }
        st.npcUuid = e.getUniqueId().toString();
        st.npcWorld = loc.getWorld().getName();
        st.npcX = loc.getX();
        st.npcY = loc.getY();
        st.npcZ = loc.getZ();
        st.npcYaw = loc.getYaw();
        st.saveTrader();
        return e;
    }

    void remove() {
        if (playerMode()) fake.clearApplied();
        Entity e = findEntity();
        if (e != null) {
            e.remove();
            // голограмма имени (рядом, marker-стойка с тем же именем) — удалить тоже
            for (Entity near : e.getNearbyEntities(1, 2, 1)) {
                if (near instanceof ArmorStand as && as.isMarker() && as.isInvisible()) near.remove();
            }
        }
        var st = plugin.storage();
        st.npcUuid = null;
        st.npcWorld = null;
        st.npcY = -1;
        st.saveTrader();
    }

    /** Проверка: это наш НПС? */
    boolean isNPC(Entity e) {
        var st = plugin.storage();
        return st.npcUuid != null && e.getUniqueId().toString().equals(st.npcUuid);
    }

    /** Открыть доску игроку (клик по НПС). */
    void open(Player p) {
        var st = plugin.storage();
        if (st.dailyIds.isEmpty()) plugin.daily().rollIfNeeded();
        plugin.openBoard(p);
    }
}
