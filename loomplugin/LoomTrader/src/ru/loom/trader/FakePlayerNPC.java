package ru.loom.trader;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Игрок-НПС «Ивлия» (v1.8): серверная невидимая стойка-хитбокс + клиентская
 * подмена на модель игрока со скином (ProtocolLib). Клик по НПС работает
 * как обычное взаимодействие с сущностью; Bedrock видит модель через Geyser.
 * Скин — подписанная текстура из конфига (npc.skin-value/signature).
 */
final class FakePlayerNPC {

    private final LoomTraderPlugin plugin;
    private final Set<UUID> applied = new HashSet<>();
    private BukkitTask task;
    private UUID npcUuid;
    private String profileName;

    FakePlayerNPC(LoomTraderPlugin plugin) {
        this.plugin = plugin;
    }

    boolean enabled() {
        return "player".equalsIgnoreCase(plugin.getConfig().getString("npc.type", "player"));
    }

    /** Спавн стойки-хитбокса и голограммы имени; рассылка подмены всем онлайн. */
    ArmorStand spawn(Location loc, UUID uuid) {
        this.npcUuid = uuid;
        this.profileName = plugin.getConfig().getString("npc.profile-name", "Ivliya");
        String name = plugin.getConfig().getString("npc.name", "§6Ивлия §7— странствующий торговец");

        // хитбокс: невидимая стойка (клиентскую подмену делаем пакетами)
        ArmorStand hitbox = loc.getWorld().spawn(loc, ArmorStand.class, st -> {
            st.setInvisible(true);
            st.setMarker(false);            // нормальный хитбокс для кликов
            st.setGravity(false);
            st.setInvulnerable(true);
            st.setSilent(true);
            st.setPersistent(true);
            st.setCollidable(false);
            st.setCanPickupItems(false);
            st.setCustomNameVisible(false);
        });

        // голограмма с именем чуть выше модели
        Location holo = loc.clone().add(0, 0.25, 0);
        loc.getWorld().spawn(holo, ArmorStand.class, st -> {
            st.setInvisible(true);
            st.setMarker(true);
            st.setGravity(false);
            st.setInvulnerable(true);
            st.setSilent(true);
            st.setPersistent(true);
            st.setCustomName(name);
            st.setCustomNameVisible(true);
        });

        applied.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendDisguise(p, loc, hitbox.getEntityId());
        }
        return hitbox;
    }

    /** Подмена для одного зрителя: ADD_PLAYER → спавн игрока → REMOVE из таба. */
    void sendDisguise(Player viewer, Location loc, int entityId) {
        if (!enabled() || npcUuid == null) return;
        String value = plugin.getConfig().getString("npc.skin-value", "");
        String signature = plugin.getConfig().getString("npc.skin-signature", "");
        if (value == null || value.isEmpty()) return; // без скина — не подменяем
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();

            // ADD_PLAYER в список игроков
            PacketContainer info = pm.createPacket(com.comphenix.protocol.PacketType.Play.Server.PLAYER_INFO);
            info.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
            WrappedGameProfile profile = new WrappedGameProfile(npcUuid, profileName);
            profile.getProperties().put("textures", new WrappedSignedProperty("textures", value, signature));
            PlayerInfoData data = new PlayerInfoData(profile, 30,
                    EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(profileName));
            info.getPlayerInfoDataLists().write(1, List.of(data));
            pm.sendServerPacket(viewer, info);

            // спавн «игрока» на месте стойки (тот же entity id → клиент заменяет сущность)
            PacketContainer spawn = pm.createPacket(com.comphenix.protocol.PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            spawn.getIntegers().write(0, entityId);
            spawn.getUUIDs().write(0, npcUuid);
            spawn.getDoubles().write(0, loc.getX());
            spawn.getDoubles().write(1, loc.getY());
            spawn.getDoubles().write(2, loc.getZ());
            spawn.getBytes().write(0, (byte) (int) (loc.getYaw() * 256f / 360f));
            spawn.getBytes().write(1, (byte) 0);
            pm.sendServerPacket(viewer, spawn);

            // убрать из таб-листа (модель остаётся)
            PacketContainer rm = pm.createPacket(com.comphenix.protocol.PacketType.Play.Server.PLAYER_INFO);
            rm.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));
            rm.getUUIDLists().write(0, List.of(npcUuid));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (viewer.isOnline()) pm.sendServerPacket(viewer, rm);
                } catch (Exception ignored) {
                    // игрок мог выйти
                }
            }, 10L);

            applied.add(viewer.getUniqueId());
        } catch (Exception ex) {
            plugin.getLogger().warning("Подмена НПС не удалась (" + viewer.getName() + "): " + ex.getMessage());
        }
    }

    /** Тик: подмена для новых зрителей (входы/смена мира). */
    void start() {
        if (!enabled()) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Entity e = findHitbox();
            if (e == null) return;
            Location loc = e.getLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!applied.contains(p.getUniqueId())) {
                    sendDisguise(p, loc, e.getEntityId());
                }
            }
        }, 40L, 40L);
    }

    void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    /** Найти стойку-хитбокс по UUID. */
    Entity findHitbox() {
        return plugin.npc().findEntity();
    }

    void clearApplied() {
        applied.clear();
    }
}
