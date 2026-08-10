
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
package com.tz.statpatterns.crafting;

import java.util.List;
import java.util.Objects;

import appeng.api.crafting.PatternDetailsHelper;
import com.tz.statpatterns.api.ids.StatPatternsComponents;
import com.tz.statpatterns.core.definition.StatPatternsItems;
import com.tz.statpatterns.math.StatPatternsSizing;
import com.tz.statpatterns.math.StatPatternsSizingResult;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.EncodedProcessingPattern;

public final class StatPatternsDetails implements IPatternDetails {

    private final AEItemKey definition;
    private final EncodedStatPatterns encoded;
    @Nullable
    private final Long requestedOutputAmount;

    private StatPatternsDetails(AEItemKey definition, EncodedStatPatterns encoded) {
        this(definition, encoded, null);
    }

    private StatPatternsDetails(AEItemKey definition, EncodedStatPatterns encoded, @Nullable Long requestedOutputAmount) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.encoded = Objects.requireNonNull(encoded, "encoded");
        this.requestedOutputAmount = requestedOutputAmount;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass()
                && ((StatPatternsDetails) obj).definition.equals(definition);
    }

    @Nullable
    public static StatPatternsDetails decode(AEItemKey what, Level level) {
        if (what == null || what.getItem() != StatPatternsItems.PROBABILITY_PATTERN.get()) {
            return null;
        }
        var encoded = what.get(StatPatternsComponents.ENCODED_STATISTICAL_PATTERN);
        if (encoded == null) {
            return null;
        }
        return new StatPatternsDetails(what, encoded);
    }

    public static ItemStack encode(List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs, double successProbability, double alpha, boolean alpha95) {
        var output = sparseOutputs.stream().filter(Objects::nonNull).findFirst().orElseThrow(() -> new IllegalArgumentException("At least one output is required."));
        var compactInputs = sparseInputs.stream().filter(Objects::nonNull).toList();
        if (compactInputs.isEmpty()) {
            throw new IllegalArgumentException("At least one input is required.");
        }

        var stack = new ItemStack(StatPatternsItems.PROBABILITY_PATTERN.get());
        stack.set(AEComponents.ENCODED_PROCESSING_PATTERN, new EncodedProcessingPattern(sparseInputs, sparseOutputs));
        stack.set(StatPatternsComponents.ENCODED_STATISTICAL_PATTERN, new EncodedStatPatterns(compactInputs, output, successProbability, alpha, 30, alpha95));
        return stack;
    }

    public static PatternDetailsTooltip getInvalidPatternTooltip(ItemStack stack, Level level, @Nullable Exception cause, TooltipFlag flags) {
        var tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_PRODUCES);
        var encoded = stack.get(StatPatternsComponents.ENCODED_STATISTICAL_PATTERN);
        if (encoded != null) {
            encoded.inputsPerAttempt().forEach(tooltip::addInput);
            tooltip.addOutput(encoded.output());
        }
        return tooltip;
    }

    @Override
    public IInput[] getInputs() {
        if (requestedOutputAmount != null) {
            var sizing = sizing();
            return encoded.inputsPerAttempt().stream()
                    .map(input -> new Input(input.what(), Math.multiplyExact(input.amount(), sizing.attempts())))
                    .toArray(IInput[]::new);
        }
        return encoded.inputsPerAttempt().stream()
                .map(input -> new Input(input.what(), input.amount()))
                .toArray(IInput[]::new);
    }

    @Override
    public List<GenericStack> getOutputs() {
        if (requestedOutputAmount != null) {
            return List.of(new GenericStack(encoded.output().what(), requestedOutputAmount));
        }
        return List.of(encoded.output());
    }

    @Override
    public void pushInputsToExternalInventory(KeyCounter[] inputHolder, PatternInputSink inputSink) {
        var allInputs = new KeyCounter();
        for (var counter : inputHolder) {
            allInputs.addAll(counter);
        }

        for (var input : encoded.inputsPerAttempt()) {
            var key = input.what();
            var amount = allInputs.get(key);
            if (amount > 0) {
                inputSink.pushInput(key, amount);
            }
        }
    }

    @Override
    public PatternDetailsTooltip getTooltip(Level level, TooltipFlag flags) {
        var tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_PRODUCES);
        for (var input : encoded.inputsPerAttempt()) {
            tooltip.addInput(input);
        }
        tooltip.addOutput(encoded.output());
        if (encoded.successProbability() < 1.0) {
            tooltip.addProperty(Component.translatable("probabilitypattern.tooltip.success_probability"), Component.literal("%.0f%%".formatted(encoded.successProbability() * 100.0)));
            tooltip.addProperty(Component.translatable("probabilitypattern.tooltip.isalpha95"), Component.literal("%.0f%%".formatted((1.0 - encoded.alpha()) * 100.0)));
        }
        return tooltip;
    }

    public StatPatternsSizingResult sizing() {
        var targetOutput = requestedOutputAmount != null ? requestedOutputAmount : encoded.output().amount();
        // Use the actual number of items needed as targetSuccesses so that the binomial model
        // correctly ensures P(total_items_produced >= targetOutput) >= 1 - alpha.
        // Using ceilDiv(targetOutput, output.amount()) would under-count when output.amount() > 1,
        // leading to insufficient confidence for the actual item count needed.
        var successes = Math.max(1, targetOutput);
        return StatPatternsSizing.planAttempts(successes, encoded.successProbability(), encoded.alpha(), encoded.smallSampleLimit());
    }

    public double successProbability() {
        return encoded.successProbability();
    }

    /**
     * Get the alpha (significance level) encoded in this pattern.
     * alpha = 0.05 corresponds to 95% confidence, alpha = 0.01 to 99%.
     */
    public double alpha() {
        return encoded.alpha();
    }

    public StatPatternsDetails forRequest(long requestedOutputAmount) {
        return new StatPatternsDetails((AEItemKey) getDefinition(), encoded, Math.max(1, requestedOutputAmount));
    }

    private static final class Input implements IPatternDetails.IInput {
        private final GenericStack[] template;
        private final long multiplier;

        private Input(AEKey key, long amount) {
            this.template = new GenericStack[] { new GenericStack(key, 1) };
            this.multiplier = amount;
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return template;
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input.matches(template[0]);
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
