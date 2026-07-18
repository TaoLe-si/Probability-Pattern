package com.tz.statpatterns.crafting;

import appeng.api.crafting.*;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.tz.statpatterns.api.ids.Components;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Custom EncodedPatternItem that skips pattern tooltip for blank (unencoded) patterns.
 */
public class ProbabilityPatternItem extends EncodedPatternItem<StatisticalPatternDetails> {

    public ProbabilityPatternItem(Properties properties, EncodedPatternDecoder<StatisticalPatternDetails> decoder, @Nullable InvalidPatternTooltipStrategy invalidPatternTooltip) {
        super(properties, decoder, invalidPatternTooltip);
        PatternDetailsHelper.registerDecoder(ProbabilityPatternDecoder.INSTANCE);
        PatternDetailsHelper.encodedPatternItemBuilder(decoder);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        // Skip pattern tooltip for blank (unencoded) patterns
        var encoded = stack.get(Components.ENCODED_STATISTICAL_PATTERN);
        if (encoded == null) {
            return; // Blank pattern - just show item name, no pattern tooltip
        }
        super.appendHoverText(stack, context, lines, flags);
    }
}
