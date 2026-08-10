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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;


import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import org.jetbrains.annotations.Nullable;

public final class StatPatternsMenus {
    private static boolean registered = false;
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(Registries.MENU, StatPatternsMod.MOD_ID);

    // Built via AE2's MenuTypeBuilder (registered under ae2: namespace)
    public static final MenuType<StatPatternsTerminalMenu> STAT_PATTERN_TERMINAL =
            MenuTypeBuilder.create(
                    (containerId, playerInventory, host) -> new StatPatternsTerminalMenu(containerId, playerInventory, host),
                    IPatternTerminalMenuHost.class
            ).build("stat_pattern_terminal");

    // Wireless variant
    @Nullable
    public static final MenuType<WirelessStatPatternsTerminalMenu> WIRELESS_STAT_PATTERN_TERMINAL;

    static {
        if (ModList.get().isLoaded("ae2wtlib")) {
            WIRELESS_STAT_PATTERN_TERMINAL =
                    MenuTypeBuilder
                            .create((MenuFactory<WirelessStatPatternsTerminalMenu, IPatternTerminalMenuHost>)
                                            WirelessStatPatternsTerminalMenu::new,
                                    IPatternTerminalMenuHost.class)
                            .build("wireless_stat_pattern_terminal");
        } else {
            WIRELESS_STAT_PATTERN_TERMINAL = null;
        }
    }

    private StatPatternsMenus() {
    }

    public static void register(IEventBus modEventBus) {
        if (registered) return;
        registered = true;
        MENUS.register(modEventBus);
    }
}
