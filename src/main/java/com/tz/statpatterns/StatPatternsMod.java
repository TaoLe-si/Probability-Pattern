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

import com.tz.statpatterns.core.definition.*;
import com.tz.statpatterns.integration.ae2wtlib.AE2WTLibIntegration;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import appeng.api.features.GridLinkables;
import appeng.api.parts.PartModels;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.items.tools.powered.WirelessTerminalItem;

@Mod(StatPatternsMod.MOD_ID)
public final class StatPatternsMod {
    public static final String MOD_ID = "statpatterns";

    public StatPatternsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        PartModels.registerModels(List.of(id("part/stat_pattern_terminal_off"), id("part/stat_pattern_terminal_on")));

        StatPatternsParts.init();
        StatPatternsItems.DR.register(modEventBus);
        StatPatternsMenus.register(modEventBus);
        StatPatternsCreativeTabs.CREATIVE_TABS.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                var wirelessTerminal = StatPatternsItems.WIRELESS_STAT_PATTERN_TERMINAL;
                if (wirelessTerminal != null) {
                    GridLinkables.register(wirelessTerminal, WirelessTerminalItem.LINKABLE_HANDLER);
                    Upgrades.add(AEItems.ENERGY_CARD, wirelessTerminal, 2, GuiText.WirelessTerminals.getTranslationKey());
                }
                AE2WTLibIntegration.registerUpgrades();
                // Register the wireless terminal with ae2wtlib's WUT so it can be
                // crafted into a Wireless Universal Terminal and used from there.
                // Safe at this point: AE2 finalizes hotkeys only at RegisterKeyMappingsEvent,
                // which fires after FMLCommonSetupEvent.
                AE2WTLibIntegration.registerWithWUT();
            });
        }
    }
}
