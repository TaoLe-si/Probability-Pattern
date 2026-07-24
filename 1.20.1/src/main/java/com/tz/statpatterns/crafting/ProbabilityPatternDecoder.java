package com.tz.statpatterns.crafting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;

public enum ProbabilityPatternDecoder implements IPatternDetailsDecoder {
    INSTANCE;

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        return stack.getItem() instanceof ProbabilityPatternItem;
    }

    @Nullable
    @Override
    public IPatternDetails decodePattern(AEItemKey what, Level level) {
        if (level == null || what == null || !(what.getItem() instanceof ProbabilityPatternItem item)) {
            return null;
        }
        return item.decode(what, level);
    }

    @Nullable
    @Override
    public IPatternDetails decodePattern(ItemStack stack, Level level, boolean describeErrors) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ProbabilityPatternItem)) {
            return null;
        }
        var key = AEItemKey.of(stack);
        if (key == null) return null;
        return StatisticalPatternDetails.decode(key, level);
    }
}
