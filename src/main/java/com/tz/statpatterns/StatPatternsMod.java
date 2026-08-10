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

import com.tz.statpatterns.api.ids.StatPatternsComponents;
import appeng.core.definitions.AEItems;
import appeng.api.upgrades.Upgrades;

import com.tz.statpatterns.core.definition.*;
import com.tz.statpatterns.integration.ae2wtlib.AE2WTLibIntegration;
import com.tz.statpatterns.integration.ae2wtlib.WirelessTerminalItemRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import appeng.api.features.GridLinkables;
import appeng.api.parts.PartModels;
import appeng.items.tools.powered.WirelessTerminalItem;

@Mod(StatPatternsMod.MOD_ID)
public final class StatPatternsMod {
    public static final String MOD_ID = "statpatterns";

    public StatPatternsMod(IEventBus modEventBus) {
        PartModels.registerModels(List.of(id("part/probability_pattern_terminal_off"), id("part/probability_pattern_terminal_on")));

        StatPatternsParts.init();
        StatPatternsComponents.DR.register(modEventBus);
        StatPatternsItems.DR.register(modEventBus);
        StatPatternsMenus.register(modEventBus);

        StatPatternsCreativeTabs.CREATIVE_TABS.register(modEventBus);

        // Register wireless terminal item at HIGH priority so it is available before
        // ae2wtlib_api's AddTerminalEvent.run() fires at NORMAL priority.
        // The actual registration logic lives in WirelessTerminalItemRegistrar — a
        // separate class that is ONLY loaded when ae2wtlib is present, so that
        // StatPatternsTerminalItem (→ ItemWT) class references never appear
        // in this class's constant pool, avoiding NoClassDefFoundError when
        // ae2wtlib is absent.
        if (ModList.get().isLoaded("ae2wtlib")) {
            WirelessTerminalItemRegistrar.register(modEventBus);
        }

        // Add wireless terminal to creative tab during registration (NORMAL priority).
        // At this point the holder has already been bound by the HIGH priority
        // listener in WirelessTerminalItemRegistrar, so def.asItem() is safe.
        modEventBus.addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.ITEM)) {
                var wirelessTerminal = StatPatternsItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
                if (wirelessTerminal != null) {
                    StatPatternsCreativeTabs.addRaw(wirelessTerminal.asItem());
                }
            }
        });

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
                var wirelessTerminal = StatPatternsItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
                if (wirelessTerminal != null) {
                    GridLinkables.register(wirelessTerminal, WirelessTerminalItem.LINKABLE_HANDLER);
                    Upgrades.add(AEItems.ENERGY_CARD, wirelessTerminal, 2);
                }

                AE2WTLibIntegration.registerUpgrades();
            });
        }
    }
}