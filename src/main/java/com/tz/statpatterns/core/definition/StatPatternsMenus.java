
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

import com.tz.statpatterns.StatPatternsMod;
import com.tz.statpatterns.terminal.StatPatternsTerminalMenu;
import com.tz.statpatterns.terminal.WirelessStatPatternsTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import org.jetbrains.annotations.Nullable;


public final class StatPatternsMenus {
    private static boolean registered = false;
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(Registries.MENU, StatPatternsMod.MOD_ID);

    // Pre-create the MenuType so it's available for ae2wtlib registration
    public static final MenuType<StatPatternsTerminalMenu> PROBABILITY_PATTERN_TERMINAL_TYPE =
            MenuTypeBuilder.create(
                            (containerId, playerInventory, host) -> new StatPatternsTerminalMenu(containerId, playerInventory, host),
                            IPatternTerminalMenuHost.class
                    )
                    .buildUnregistered(StatPatternsMod.id("probability_pattern_terminal"));

    public static final DeferredHolder<MenuType<?>, MenuType<StatPatternsTerminalMenu>> PROBABILITY_PATTERN_TERMINAL =
            MENUS.register("probability_pattern_terminal", () -> PROBABILITY_PATTERN_TERMINAL_TYPE);

    // Wireless variant — only available when ae2wtlib is present
    @Nullable
    public static final MenuType<WirelessStatPatternsTerminalMenu> WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE;

    @Nullable
    public static final DeferredHolder<MenuType<?>, MenuType<WirelessStatPatternsTerminalMenu>> WIRELESS_PROBABILITY_PATTERN_TERMINAL;

    static {
        if (ModList.get().isLoaded("ae2wtlib")) {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE =
                    MenuTypeBuilder
                            .create((MenuFactory<WirelessStatPatternsTerminalMenu, IPatternTerminalMenuHost>)
                                            WirelessStatPatternsTerminalMenu::new,
                                    IPatternTerminalMenuHost.class)
                            .buildUnregistered(StatPatternsMod.id("wireless_probability_pattern_terminal"));
            WIRELESS_PROBABILITY_PATTERN_TERMINAL =
                    MENUS.register("wireless_probability_pattern_terminal", () -> WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE);
        } else {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE = null;
            WIRELESS_PROBABILITY_PATTERN_TERMINAL = null;
        }
    }

    private StatPatternsMenus() {
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
