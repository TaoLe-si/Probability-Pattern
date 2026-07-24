package com.tz.statpatterns.api.ids;

import com.tz.statpatterns.core.SP;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;

public final class SPCreativeTabIds {
    private SPCreativeTabIds() {
    }

    public static final ResourceKey<CreativeModeTab> MAIN = create("main");

    private static ResourceKey<CreativeModeTab> create(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, SP.makeId(path));
    }
}
