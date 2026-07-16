package com.tz.statpatterns.core.definition;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;


public final class SPMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(Registries.MENU, ProbabilityPatternMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ProbabilityPatternTerminalMenu>> PROBABILITY_PATTERN_TERMINAL =
            MENUS.register("probability_pattern_terminal",
                    () -> MenuTypeBuilder
                            .create((MenuFactory<ProbabilityPatternTerminalMenu, IPatternTerminalMenuHost>)
                                    ProbabilityPatternTerminalMenu::new,
                                    IPatternTerminalMenuHost.class)
                            .build(ProbabilityPatternMod.id("probability_pattern_terminal")));

    private SPMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
