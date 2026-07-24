package com.tz.statpatterns.client;

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.client.gui.style.StyleManager;
import appeng.init.client.InitScreens;
import com.tz.statpatterns.ProbabilityPatternMod;

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

            var wirelessMenuType = SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
            if (wirelessMenuType != null) {
                InitScreens.<WirelessProbabilityPatternTerminalMenu, WirelessProbabilityPatternTerminalScreen>register(
                        wirelessMenuType,
                        WirelessProbabilityPatternTerminalScreen::new,
                        "/screens/terminals/wireless_probability_pattern_terminal.json");
            }
        });
    }
}
