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
package com.tz.statpatterns.client;

import appeng.init.client.InitScreens;

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;

/**
 * Registers the wireless probability pattern terminal screen.
 * <p>
 * This deliberately lives in its own class that is <b>not</b> annotated with
 * {@code @Mod.EventBusSubscriber}: Forge reflects over subscriber classes during mod
 * construction, and references here put {@link WirelessProbabilityPatternTerminalMenu}
 * / {@link WirelessProbabilityPatternTerminalScreen} into the class signature,
 * forcing ae2wtlib classes to load even when ae2wtlib is absent.
 * <p>
 * This class is only loaded when {@link #register()} is actually invoked, which
 * only happens when ae2wtlib is present — keeping the mod loadable without it.
 */
public final class WirelessProbabilityPatternTerminalScreenRegistration {
    private WirelessProbabilityPatternTerminalScreenRegistration() {
    }

    public static void register() {
        var wirelessMenuType = SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
        if (wirelessMenuType == null) {
            return;
        }
        InitScreens.<WirelessProbabilityPatternTerminalMenu, WirelessProbabilityPatternTerminalScreen>register(
                wirelessMenuType,
                WirelessProbabilityPatternTerminalScreen::new,
                "/screens/terminals/wireless_probability_pattern_terminal.json");
    }
}
