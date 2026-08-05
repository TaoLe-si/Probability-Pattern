package com.tz.statpatterns.crafting;

import appeng.api.crafting.*;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.tz.statpatterns.api.ids.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Custom EncodedPatternItem that handles probability pattern decoding.
 * Adapted for 1.20.1 Forge (EncodedPatternItem is not generic).
 */
public class ProbabilityPatternItem extends EncodedPatternItem {

    public ProbabilityPatternItem(Properties properties) {
        super(properties);
        PatternDetailsHelper.registerDecoder(ProbabilityPatternDecoder.INSTANCE);
    }

    @Override
    public @Nullable IPatternDetails decode(AEItemKey what, Level level) {
        return StatisticalPatternDetails.decode(what, level);
    }

    @Override
    public @Nullable IPatternDetails decode(ItemStack stack, Level level, boolean describeErrors) {
        if (stack.isEmpty()) return null;
        var key = AEItemKey.of(stack);
        if (key == null) return null;
        return decode(key, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flags) {
        // Skip pattern tooltip for blank (unencoded) patterns
        var encoded = Components.readStatisticalPattern(stack);
        if (encoded == null) {
            return; // Blank pattern - just show item name, no pattern tooltip
        }
        // Add probability info before the default AE2 pattern tooltip
        if (encoded.successProbability() < 1.0) {
            lines.add(Component.translatable("probabilitypattern.tooltip.success_probability")
                    .append(": ")
                    .append(Component.literal("%.0f%%".formatted(encoded.successProbability() * 100.0))
                            .withStyle(ChatFormatting.GOLD)));
            lines.add(Component.translatable("probabilitypattern.tooltip.isalpha95")
                    .append(": ")
                    .append(Component.literal("%.0f%%".formatted((1.0 - encoded.alpha()) * 100.0))
                            .withStyle(ChatFormatting.GOLD)));
        }
        super.appendHoverText(stack, level, lines, flags);
    }
}
