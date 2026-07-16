package com.tz.statpatterns.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.core.definition.SPItems;
import com.tz.statpatterns.math.ProbabilitySizing;
import com.tz.statpatterns.math.ProbabilitySizingResult;
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
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedProcessingPattern;

public final class StatisticalPatternDetails extends AEProcessingPattern {
    private final EncodedStatisticalPattern encoded;
    @Nullable
    private final Long requestedOutputAmount;

    private StatisticalPatternDetails(AEItemKey definition, EncodedStatisticalPattern encoded) {
        this(definition, encoded, null);
    }

    private StatisticalPatternDetails(AEItemKey definition, EncodedStatisticalPattern encoded,
                                      @Nullable Long requestedOutputAmount) {
        super(definition);
        this.encoded = Objects.requireNonNull(encoded, "encoded");
        this.requestedOutputAmount = requestedOutputAmount;
    }

    @Nullable
    public static StatisticalPatternDetails decode(AEItemKey what, Level level) {
        if (what == null || what.getItem() != SPItems.PROBABILITY_PATTERN.get()) {
            return null;
        }
        var encoded = what.get(Components.ENCODED_STATISTICAL_PATTERN.get());
        if (encoded == null) {
            throw new IllegalArgumentException("Missing statistical pattern component.");
        }
        if (what.get(AEComponents.ENCODED_PROCESSING_PATTERN) == null) {
            throw new IllegalArgumentException("Missing AE2 processing pattern component.");
        }
        return new StatisticalPatternDetails(what, encoded);
    }

    public static ItemStack encode(List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs,
                                   double successProbability, double alpha) {
        var output = sparseOutputs.stream().filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("At least one output is required."));
        var compactInputs = sparseInputs.stream().filter(Objects::nonNull).toList();
        if (compactInputs.isEmpty()) {
            throw new IllegalArgumentException("At least one input is required.");
        }

        var stack = new ItemStack(SPItems.PROBABILITY_PATTERN.get());
        stack.set(AEComponents.ENCODED_PROCESSING_PATTERN, new EncodedProcessingPattern(sparseInputs, sparseOutputs));
        stack.set(Components.ENCODED_STATISTICAL_PATTERN.get(),
                new EncodedStatisticalPattern(compactInputs, output, successProbability, alpha, 30));
        return stack;
    }

    public static ItemStack encode(List<GenericStack> inputsPerAttempt, GenericStack output,
                                   double successProbability, double alpha) {
        return encode(new ArrayList<>(inputsPerAttempt), List.of(output), successProbability, alpha);
    }

    public static PatternDetailsTooltip getInvalidPatternTooltip(ItemStack stack, Level level,
                                                                 @Nullable Exception cause, TooltipFlag flags) {
        var tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_PRODUCES);
        var encoded = stack.get(Components.ENCODED_STATISTICAL_PATTERN.get());
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
            var amount = Math.multiplyExact(input.amount(), sizing().attempts());
            var available = allInputs.get(key);
            if (available < amount) {
                throw new RuntimeException(
                        "Expected at least %d of %s when pushing probability pattern, but only %d available"
                                .formatted(amount, key, available));
            }
            inputSink.pushInput(key, amount);
            allInputs.remove(key, amount);
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
            tooltip.addProperty(
                    Component.translatable("probabilitypattern.tooltip.success_probability"),
                    Component.literal("%.0f%%".formatted(encoded.successProbability() * 100.0)));
        }
        return tooltip;
    }

    public ProbabilitySizingResult sizing() {
        var targetOutput = requestedOutputAmount != null ? requestedOutputAmount : encoded.output().amount();
        var successes = Math.max(1, ceilDiv(targetOutput, encoded.output().amount()));
        return ProbabilitySizing.planAttempts(
                successes,
                encoded.successProbability(),
                encoded.alpha(),
                encoded.smallSampleLimit());
    }

    public double successProbability() {
        return encoded.successProbability();
    }

    public StatisticalPatternDetails forRequest(long requestedOutputAmount) {
        return new StatisticalPatternDetails((AEItemKey) getDefinition(), encoded, Math.max(1, requestedOutputAmount));
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
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
