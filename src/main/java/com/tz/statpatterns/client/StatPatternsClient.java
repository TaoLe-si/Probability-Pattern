
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

import com.tz.statpatterns.core.definition.StatPatternsMenus;
import com.tz.statpatterns.terminal.StatPatternsTerminalMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import guideme.Guide;

import appeng.client.gui.style.StyleManager;

import com.tz.statpatterns.StatPatternsMod;

@EventBusSubscriber(modid = StatPatternsMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StatPatternsClient {
    private StatPatternsClient() {
    }

    static {
        // Register the GuideME guide book (content lives in assets/probabilitypattern/ae2guide)
        Guide.builder(StatPatternsMod.id("guide"))
                .folder("ae2guide")
                .build();
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Wired terminal (no upgrade slots)
        event.<StatPatternsTerminalMenu, StatPatternsTerminalScreen<StatPatternsTerminalMenu>>register(
                StatPatternsMenus.PROBABILITY_PATTERN_TERMINAL.get(),
                (menu, playerInventory, title) -> new StatPatternsTerminalScreen<>(
                        menu,
                        playerInventory,
                        title,
                        StyleManager.loadStyleDoc("/screens/terminals/probability_pattern_encoding_terminal.json")));

        // Wireless terminal (with upgrade slots and ae2wtlib support) — only when ae2wtlib is present.
        // The actual registration lives in a separate class (not an @EventBusSubscriber) so that
        // reflecting this class never loads ae2wtlib classes when ae2wtlib is absent.
        if (ModList.get().isLoaded("ae2wtlib")) {
            WirelessStatPatternsTerminalScreenRegistration.register(event);
        }
    }
}

