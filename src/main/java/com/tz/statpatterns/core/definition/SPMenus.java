
/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.core.definition;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;


public final class SPMenus {
    private static boolean registered = false;
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(Registries.MENU, ProbabilityPatternMod.MOD_ID);

    // Pre-create the MenuType so it's available for ae2wtlib registration
    public static final MenuType<ProbabilityPatternTerminalMenu> PROBABILITY_PATTERN_TERMINAL_TYPE =
            MenuTypeBuilder.create(
                            (containerId, playerInventory, host) -> new ProbabilityPatternTerminalMenu(containerId, playerInventory, host),
                            IPatternTerminalMenuHost.class
                    )
                    .buildUnregistered(ProbabilityPatternMod.id("probability_pattern_terminal"));

    public static final DeferredHolder<MenuType<?>, MenuType<ProbabilityPatternTerminalMenu>> PROBABILITY_PATTERN_TERMINAL =
            MENUS.register("probability_pattern_terminal", () -> PROBABILITY_PATTERN_TERMINAL_TYPE);

    // Wireless variant with upgrade slots and singularity slot
    public static final MenuType<WirelessProbabilityPatternTerminalMenu> WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE =
            MenuTypeBuilder
                    .create((MenuFactory<WirelessProbabilityPatternTerminalMenu, IPatternTerminalMenuHost>)
                                    WirelessProbabilityPatternTerminalMenu::new,
                            IPatternTerminalMenuHost.class)
                    .buildUnregistered(ProbabilityPatternMod.id("wireless_probability_pattern_terminal"));

    public static final DeferredHolder<MenuType<?>, MenuType<WirelessProbabilityPatternTerminalMenu>> WIRELESS_PROBABILITY_PATTERN_TERMINAL =
            MENUS.register("wireless_probability_pattern_terminal", () -> WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE);

    private SPMenus() {
    }

    public static void register(IEventBus modEventBus) {
        if (registered) {
            // 如果已经注册过，直接返回，避免添加重复监听器
            return;
        }
        registered = true;
        MENUS.register(modEventBus);
    }
}
