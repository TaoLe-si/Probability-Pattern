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
package com.zincglux.statpatterns.mixin;

import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.zincglux.statpatterns.client.ClientProbabilityPatternTextures;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.client.texture.CableBusTextures;

/**
 * Gives the in-world probability terminal part its own front-face textures.
 * <p>
 * AE2 renders every reporting part's front face from {@code getFrontBright/Colored/
 * Dark()} which return {@link CableBusTextures} enum values (fixed entries, hard-coded
 * to the {@code appliedenergistics2:} namespace), so a custom texture cannot be added
 * by overriding those getters. Instead this mixin redirects the {@code getIcon()} call
 * inside {@code AbstractPartDisplay.renderStatic/renderInventory}: for our
 * {@link ProbabilityPatternTerminalPart} the vanilla Pattern-Terminal icons are swapped
 * for the mod's own {@code part_probability_*} textures (registered in
 * {@link ClientProbabilityPatternTextures}). All other parts / textures pass through.
 * <p>
 * remap = false: AbstractPartDisplay is a mod class (not obfuscated).
 */
@Mixin(value = appeng.parts.reporting.AbstractPartDisplay.class, remap = false)
public abstract class PartDisplayTextureMixin {

    @Redirect(
        method = "renderStatic",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/texture/CableBusTextures;getIcon()Lnet/minecraft/util/IIcon;"))
    private IIcon statProbabilityFrontIcon(final CableBusTextures tex) {
        return probabilityFrontIcon(tex);
    }

    @Redirect(
        method = "renderInventory",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/texture/CableBusTextures;getIcon()Lnet/minecraft/util/IIcon;"))
    private IIcon invProbabilityFrontIcon(final CableBusTextures tex) {
        return probabilityFrontIcon(tex);
    }

    private IIcon probabilityFrontIcon(final CableBusTextures tex) {
        if ((Object) this instanceof ProbabilityPatternTerminalPart) {
            if (tex == CableBusTextures.PartPatternTerm_Bright && ClientProbabilityPatternTextures.brightIcon != null) {
                return ClientProbabilityPatternTextures.brightIcon;
            }
            if (tex == CableBusTextures.PartPatternTerm_Colored
                && ClientProbabilityPatternTextures.coloredIcon != null) {
                return ClientProbabilityPatternTextures.coloredIcon;
            }
            if (tex == CableBusTextures.PartPatternTerm_Dark && ClientProbabilityPatternTextures.darkIcon != null) {
                return ClientProbabilityPatternTextures.darkIcon;
            }
        }
        return tex.getIcon();
    }
}
