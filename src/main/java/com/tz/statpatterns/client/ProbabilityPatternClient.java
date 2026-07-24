
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

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import appeng.client.gui.style.StyleManager;

import com.tz.statpatterns.ProbabilityPatternMod;

@EventBusSubscriber(modid = ProbabilityPatternMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ProbabilityPatternClient {
    private ProbabilityPatternClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Wired terminal (no upgrade slots)
        event.<ProbabilityPatternTerminalMenu, ProbabilityPatternTerminalScreen<ProbabilityPatternTerminalMenu>>register(
                SPMenus.PROBABILITY_PATTERN_TERMINAL.get(),
                (menu, playerInventory, title) -> new ProbabilityPatternTerminalScreen<>(
                        menu,
                        playerInventory,
                        title,
                        StyleManager.loadStyleDoc("/screens/terminals/probability_pattern_encoding_terminal.json")));

        // Wireless terminal (with upgrade slots and ae2wtlib support) — only when ae2wtlib is present
        var wirelessMenuType = SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
        if (wirelessMenuType != null) {
            event.<WirelessProbabilityPatternTerminalMenu, WirelessProbabilityPatternTerminalScreen>register(
                    wirelessMenuType.get(),
                    (menu, playerInventory, title) -> new WirelessProbabilityPatternTerminalScreen(
                            menu,
                            playerInventory,
                            title,
                            StyleManager.loadStyleDoc("/screens/terminals/wireless_probability_pattern_terminal.json")));
        }
    }
}

