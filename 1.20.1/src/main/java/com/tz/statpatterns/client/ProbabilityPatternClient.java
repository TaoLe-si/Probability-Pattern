package com.tz.statpatterns.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.init.client.InitScreens;
import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;

@Mod.EventBusSubscriber(modid = ProbabilityPatternMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ProbabilityPatternClient {
    private ProbabilityPatternClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            InitScreens.<ProbabilityPatternTerminalMenu, ProbabilityPatternTerminalScreen<ProbabilityPatternTerminalMenu>>register(
                    SPMenus.PROBABILITY_PATTERN_TERMINAL,
                    ProbabilityPatternTerminalScreen::new,
                    "/screens/terminals/probability_pattern_encoding_terminal.json");

            // Wireless terminal (with upgrade slots and ae2wtlib support) — only when ae2wtlib is present.
            // The actual registration lives in a separate class (not an @EventBusSubscriber) so that
            // reflecting this class never loads ae2wtlib classes when ae2wtlib is absent.
            if (ModList.get().isLoaded("ae2wtlib")) {
                WirelessProbabilityPatternTerminalScreenRegistration.register();
            }
        });
    }
}
