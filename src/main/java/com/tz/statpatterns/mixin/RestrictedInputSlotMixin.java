/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.mixin;

import com.tz.statpatterns.core.definition.SPItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.menu.slot.RestrictedInputSlot;

@Mixin(RestrictedInputSlot.class)
public abstract class RestrictedInputSlotMixin {

    @Shadow
    @Final
    private RestrictedInputSlot.PlacableItemType which;

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void allowProbabilityPattern(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Also accept our probability pattern in blank pattern slots
        if (!cir.getReturnValue()
                && which == RestrictedInputSlot.PlacableItemType.BLANK_PATTERN
                && stack.is(SPItems.PROBABILITY_PATTERN.get())) {
            cir.setReturnValue(true);
        }
    }
}
