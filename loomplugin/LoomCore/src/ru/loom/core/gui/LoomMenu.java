package ru.loom.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

/** GUI-инвентарь с обработчиком кликов (клики внутри отменяются). */
public interface LoomMenu extends InventoryHolder {

    /** Клик по слоту верхней панели. Вернуть true — закрыть GUI. */
    boolean handleClick(int slot, Player p);

    /** Клик с типом нажатия (ЛКМ/ПКМ/Shift...). По умолчанию — без учёта типа. */
    default boolean handleClick(int slot, Player p, org.bukkit.event.inventory.ClickType type) {
        return handleClick(slot, p);
    }
}

