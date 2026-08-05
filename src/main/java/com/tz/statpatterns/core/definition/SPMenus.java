package com.tz.statpatterns.core.definition;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;


import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import org.jetbrains.annotations.Nullable;

public final class SPMenus {
    private static boolean registered = false;
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(Registries.MENU, ProbabilityPatternMod.MOD_ID);

    // Built via AE2's MenuTypeBuilder (registered under ae2: namespace)
    public static final MenuType<ProbabilityPatternTerminalMenu> PROBABILITY_PATTERN_TERMINAL =
            MenuTypeBuilder.create(
                    (containerId, playerInventory, host) -> new ProbabilityPatternTerminalMenu(containerId, playerInventory, host),
                    IPatternTerminalMenuHost.class
            ).build("probability_pattern_terminal");

    // Wireless variant
    @Nullable
    public static final MenuType<WirelessProbabilityPatternTerminalMenu> WIRELESS_PROBABILITY_PATTERN_TERMINAL;

    static {
        if (ModList.get().isLoaded("ae2wtlib")) {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL =
                    MenuTypeBuilder
                            .create((MenuFactory<WirelessProbabilityPatternTerminalMenu, IPatternTerminalMenuHost>)
                                            WirelessProbabilityPatternTerminalMenu::new,
                                    IPatternTerminalMenuHost.class)
                            .build("wireless_probability_pattern_terminal");
        } else {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL = null;
        }
    }

    private SPMenus() {
    }

    public static void register(IEventBus modEventBus) {
        if (registered) return;
        registered = true;
        MENUS.register(modEventBus);
    }
}
