package com.tz.statpatterns.mixin;

import appeng.core.definitions.AEItems;
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
    @Shadow @Final
    private RestrictedInputSlot.PlacableItemType which;

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void allowProbabilityPattern(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()
                && which == RestrictedInputSlot.PlacableItemType.BLANK_PATTERN
                && stack.is(SPItems.PROBABILITY_PATTERN.stack().getItem())) {
            cir.setReturnValue(true);
        }
        if (cir.getReturnValue()
                && (which == RestrictedInputSlot.PlacableItemType.BLANK_PATTERN
                    || which == RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN)
                && isSingularity(stack)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isSingularity(ItemStack stack) {
        return stack.is(AEItems.QUANTUM_ENTANGLED_SINGULARITY.stack().getItem())
                || stack.is(AEItems.SINGULARITY.stack().getItem());
    }
}
