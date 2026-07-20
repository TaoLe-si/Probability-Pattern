package com.tz.statpatterns;

import java.util.List;

import com.tz.statpatterns.api.ids.Components;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.core.definitions.AEItems;
import appeng.api.upgrades.Upgrades;
import com.tz.statpatterns.crafting.ProbabilityPatternDecoder;

import com.tz.statpatterns.core.definition.*;
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
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                GridLinkables.register(SPItems.PROBABILITY_PATTERN_HANDHELD_TERMINAL,
                    WirelessTerminalItem.LINKABLE_HANDLER);

                // Register upgrade card support for the portable probability pattern terminal.
                // AE2 only registers energy cards for its own wireless terminals by exact item identity,
                // so we must register our custom terminal separately.
                Upgrades.add(AEItems.ENERGY_CARD, SPItems.PROBABILITY_PATTERN_HANDHELD_TERMINAL, 2);
            });
        }
    }
}