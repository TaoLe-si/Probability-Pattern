package com.tz.statpatterns.core;

import net.minecraft.resources.ResourceLocation;

import static com.tz.statpatterns.ProbabilityPatternMod.MOD_ID;

public interface SP {
    static ResourceLocation makeId(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
