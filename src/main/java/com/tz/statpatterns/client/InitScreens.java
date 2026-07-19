package com.tz.statpatterns.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.menu.AEBaseMenu;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class InitScreens {
    public static void init(RegisterMenuScreensEvent event) {
        InitScreens.<ProbabilityPatternTerminalMenu, ProbabilityPatternTerminalScreen<ProbabilityPatternTerminalMenu>>register(event,
                SPMenus.PROBABILITY_PATTERN_TERMINAL.get(),
                ProbabilityPatternTerminalScreen::new,
                "/screens/terminals/probability_pattern_encoding_terminal.json");
    }
    public static <M extends AEBaseMenu, U extends AEBaseScreen<M>> void register(RegisterMenuScreensEvent event,
                                                                                  MenuType<M> type,
                                                                                  StyledScreenFactory<M, U> factory,
                                                                                  String stylePath) {
        event.<M, U>register(type, (menu, playerInv, title) -> {
            var style = StyleManager.loadStyleDoc(stylePath);

            return factory.create(menu, playerInv, title, style);
        });
    }

    /**
     * A type definition that matches the constructors of our screens, which take an additional {@link ScreenStyle}
     * argument.
     */
    @FunctionalInterface
    public interface StyledScreenFactory<T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> {
        U create(T t, Inventory pi, Component title, ScreenStyle style);
    }
}
