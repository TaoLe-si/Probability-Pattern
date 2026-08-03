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

@Mod(ProbabilityPatternMod.MOD_ID)
public final class ProbabilityPatternMod {
    public static final String MOD_ID = "probabilitypattern";

    public ProbabilityPatternMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        PartModels.registerModels(List.of(id("part/probability_pattern_terminal_off"), id("part/probability_pattern_terminal_on")));

        SPParts.init();
        SPItems.DR.register(modEventBus);
        SPMenus.register(modEventBus);
        SPCreativeTabs.CREATIVE_TABS.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            // WUTHandler.addTerminal() can NEVER be called in 1.20.1:
            // Hotkeys.finalize() runs during AE2 mod construction (before any event).
            // WUT merge is handled exclusively by the ae2wtlib:upgrade recipe JSON.
            event.enqueueWork(() -> {
                var wirelessTerminal = SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
                if (wirelessTerminal != null) {
                    GridLinkables.register(wirelessTerminal, WirelessTerminalItem.LINKABLE_HANDLER);
                    Upgrades.add(AEItems.ENERGY_CARD, wirelessTerminal, 2, GuiText.WirelessTerminals.getTranslationKey());
                }
                AE2WTLibIntegration.registerUpgrades();
            });
        }
    }
}
