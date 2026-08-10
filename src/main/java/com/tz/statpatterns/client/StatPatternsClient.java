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
import com.tz.statpatterns.terminal.WirelessStatPatternsTerminalMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.init.client.InitScreens;
import com.tz.statpatterns.StatPatternsMod;

@Mod.EventBusSubscriber(modid = StatPatternsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StatPatternsClient {
    private StatPatternsClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            InitScreens.<StatPatternsTerminalMenu, StatPatternsTerminalScreen<StatPatternsTerminalMenu>>register(
                    StatPatternsMenus.STAT_PATTERN_TERMINAL,
                    StatPatternsTerminalScreen::new,
                    "/screens/terminals/stat_pattern_encoding_terminal.json");

            var wirelessMenuType = StatPatternsMenus.WIRELESS_STAT_PATTERN_TERMINAL;
            if (wirelessMenuType != null) {
                InitScreens.<WirelessStatPatternsTerminalMenu, WirelessStatPatternsTerminalScreen>register(
                        wirelessMenuType,
                        WirelessStatPatternsTerminalScreen::new,
                        "/screens/terminals/wireless_stat_pattern_terminal.json");
            }
        });
    }
}
