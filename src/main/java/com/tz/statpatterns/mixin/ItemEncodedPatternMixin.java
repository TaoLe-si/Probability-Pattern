/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
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
package com.tz.statpatterns.mixin;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tz.statpatterns.crafting.EncodedStatisticalPattern;

import appeng.items.misc.ItemEncodedPattern;

/**
 * Appends the probability / confidence lines to the tooltip of an encoded pattern that
 * carries the {@code sp_*} probability tags, while keeping AE2 GTNH's vanilla tooltip
 * (inputs/outputs) intact.
 * <p>
 * AE2's tooltip entry point for patterns is
 * {@code ItemEncodedPattern.addCheckedInformation(ItemStack, EntityPlayer, List, boolean)}
 * (invoked from {@code AEBaseItem.addInformation}), so this mixin injects at its RETURN.
 * <p>
 * remap = false: ItemEncodedPattern is a mod class (not obfuscated).
 */
@Mixin(value = ItemEncodedPattern.class, remap = false)
public abstract class ItemEncodedPatternMixin {

    @Inject(method = "addCheckedInformation", at = @At("RETURN"))
    private void probabilitypattern_appendProbabilityTooltip(final ItemStack stack, final EntityPlayer player,
        final List<String> lines, final boolean displayMoreInfo, final CallbackInfo ci) {
        final NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY)) {
            return; // not a probability pattern
        }
        final double p = tag.getDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "probabilitypattern.tooltip.success_probability",
                String.format("%.0f%%", p * 100.0)));
        final double alpha = tag.getDouble(EncodedStatisticalPattern.TAG_ALPHA);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "probabilitypattern.tooltip.alpha",
                String.format("%.0f%%", (1.0 - alpha) * 100.0)));
    }
}
