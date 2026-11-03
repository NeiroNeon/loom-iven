package ru.loom.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.loom.core.LoomCorePlugin;
import ru.loom.core.bedrock.BedrockUi;
import ru.loom.core.menu.MenuAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Bedrock-формы (Floodgate) главного меню и профиля.
 * Кнопки формы соответствуют кнопкам сундук-GUI.
 */
final class MenuForms {

    private MenuForms() { }

    /** Хаб-меню для Bedrock: кнопки = пункты реестра. */
    static void hubForm(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());
        List<MenuAction> actions = core.menus().visible(p);

        List<String> buttons = new ArrayList<>();
        for (MenuAction a : actions) buttons.add(a.name(p, lang));

        String content = core.lang().raw(lang, "gui.menu.form-content");
        BedrockUi.buttons(p, core.lang().raw(lang, "gui.menu.title"), content, buttons, idx -> {
            if (idx < 0 || idx >= actions.size()) return;
            try {
                actions.get(idx).click(p);
            } catch (Exception ex) {
                core.getLogger().warning("Меню (форма): ошибка действия: " + ex);
            }
        });
    }

    /** Профиль для Bedrock: текстовая форма со всеми строками. */
    static void profileForm(Player p) {
        var core = LoomCorePlugin.get();
        String lang = core.api().lang(p.getUniqueId());

        StringBuilder sb = new StringBuilder();
        for (String line : ProfileGUI.textLines(p, lang)) sb.append(line).append("\n");
        sb.append("\n").append(core.lang().raw(lang, "gui.profile.footer"));

        List<String> buttons = List.of(
                core.lang().raw(lang, "gui.profile.update"),
                core.lang().raw(lang, "gui.menu.open"),
                core.lang().raw(lang, "gui.close"));
        BedrockUi.buttons(p, core.lang().raw(lang, "gui.profile.title"), sb.toString(), buttons, idx -> {
            if (idx == 0) Bukkit.getScheduler().runTask(core, () -> profileForm(p));
            else if (idx == 1) Bukkit.getScheduler().runTask(core, () -> hubForm(p));
        });
    }
}
