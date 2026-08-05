package com.tz.statpatterns.api.ids;

import net.minecraft.resources.ResourceLocation;

import static com.tz.statpatterns.ProbabilityPatternMod.MOD_ID;

public class ItemIds {
    public static final ResourceLocation PROBABILITY_PATTERN_TERMINAL = id("probability_pattern_terminal");
    public static final ResourceLocation PROBABILITY_PATTERN = id("probability_pattern");
    public static final ResourceLocation WIRELESS_PROBABILITY_PATTERN_TERMINAL = id("wireless_probability_pattern_terminal");

    private static ResourceLocation id(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
