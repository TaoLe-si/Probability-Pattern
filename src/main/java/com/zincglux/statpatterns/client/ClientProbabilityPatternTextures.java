/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.zincglux.statpatterns.client;

import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Registers the in-world probability terminal part front-face textures into the block
 * texture atlas and exposes them for {@code PartDisplayTextureMixin}'s render swap.
 * <p>
 * Registered on {@code MinecraftForge.EVENT_BUS} (client side only, from
 * {@code ProbabilityPatternMod.postInit}). The class itself is intentionally NOT
 * {@code @SideOnly} so the mixin can reference it without server class-loading issues.
 */
public final class ClientProbabilityPatternTextures {

    public static IIcon brightIcon;
    public static IIcon coloredIcon;
    public static IIcon darkIcon;

    public ClientProbabilityPatternTextures() {}

    @SubscribeEvent
    public void onTextureStitch(final TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() == 0) { // 0 = block texture atlas
            brightIcon = event.map.registerIcon("statpatterns:part_probability_bright");
            coloredIcon = event.map.registerIcon("statpatterns:part_probability_colored");
            darkIcon = event.map.registerIcon("statpatterns:part_probability_dark");
        }
    }
}
