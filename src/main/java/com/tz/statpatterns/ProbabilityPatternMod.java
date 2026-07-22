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
package com.tz.statpatterns;

import java.util.List;

import com.tz.statpatterns.api.ids.Components;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.core.definitions.AEItems;
import appeng.api.upgrades.Upgrades;
import com.tz.statpatterns.crafting.ProbabilityPatternDecoder;

import com.tz.statpatterns.core.definition.*;
import com.tz.statpatterns.integration.ae2wtlib.AE2WTLibIntegration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import appeng.api.features.GridLinkables;
import appeng.api.parts.PartModels;
import appeng.items.tools.powered.WirelessTerminalItem;

@Mod(ProbabilityPatternMod.MOD_ID)
public final class ProbabilityPatternMod {
    public static final String MOD_ID = "probabilitypattern";

    public ProbabilityPatternMod(IEventBus modEventBus) {
        PartModels.registerModels(List.of(id("part/probability_pattern_terminal_off"), id("part/probability_pattern_terminal_on")));

        SPParts.init();
        Components.DR.register(modEventBus);
        SPItems.DR.register(modEventBus);
        SPMenus.register(modEventBus);

        SPCreativeTabs.CREATIVE_TABS.register(modEventBus);

        AE2WTLibIntegration.registerTerminal();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                GridLinkables.register(SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL, WirelessTerminalItem.LINKABLE_HANDLER);

                Upgrades.add(AEItems.ENERGY_CARD, SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL, 2);

                AE2WTLibIntegration.registerUpgrades();
            });
        }
    }
}