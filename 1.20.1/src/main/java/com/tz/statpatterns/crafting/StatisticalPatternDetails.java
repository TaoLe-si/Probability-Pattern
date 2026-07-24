package com.tz.statpatterns.crafting;

import java.util.List;
import java.util.Objects;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.core.definition.SPItems;
import com.tz.statpatterns.math.ProbabilitySizing;
import com.tz.statpatterns.math.ProbabilitySizingResult;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.PatternInputSink;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

public final class StatisticalPatternDetails implements IPatternDetails {

    private final AEItemKey definition;
    private final EncodedStatisticalPattern encoded;
    @Nullable
    private final Long requestedOutputAmount;

    private StatisticalPatternDetails(AEItemKey definition, EncodedStatisticalPattern encoded) {
        this(definition, encoded, null);
    }

    private StatisticalPatternDetails(AEItemKey definition, EncodedStatisticalPattern encoded, @Nullable Long requestedOutputAmount) {
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
                && ((StatisticalPatternDetails) obj).definition.equals(definition);
    }

    @Nullable
    public static StatisticalPatternDetails decode(AEItemKey what, Level level) {
        if (what == null || what.getItem() != SPItems.PROBABILITY_PATTERN.stack().getItem()) {
            return null;
        }
        CompoundTag tag = what.getTag();
        if (tag == null) {
            return null;
        }
        EncodedStatisticalPattern encoded = readPatternFromTag(tag);
        if (encoded == null) {
            return null;
        }
        return new StatisticalPatternDetails(what, encoded);
    }

    @Nullable
    private static EncodedStatisticalPattern readPatternFromTag(CompoundTag tag) {
        if (!tag.contains("sp_statistical_pattern")) {
            return null;
        }
        ItemStack tempStack = new ItemStack(SPItems.PROBABILITY_PATTERN.stack().getItem());
        tempStack.setTag(tag);
        return Components.readStatisticalPattern(tempStack);
    }

    public static ItemStack encode(List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs, double successProbability, double alpha, boolean alpha95) {
        var output = sparseOutputs.stream().filter(Objects::nonNull).findFirst().orElseThrow(() -> new IllegalArgumentException("At least one output is required."));
        var compactInputs = sparseInputs.stream().filter(Objects::nonNull).toList();
        if (compactInputs.isEmpty()) {
            throw new IllegalArgumentException("At least one input is required.");
        }

        var stack = new ItemStack(SPItems.PROBABILITY_PATTERN.stack().getItem());
        Components.writeStatisticalPattern(stack, new EncodedStatisticalPattern(compactInputs, output, successProbability, alpha, 30, alpha95));
        return stack;
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
    public GenericStack[] getOutputs() {
        if (requestedOutputAmount != null) {
            return new GenericStack[] { new GenericStack(encoded.output().what(), requestedOutputAmount) };
        }
        return new GenericStack[] { encoded.output() };
    }

    public ProbabilitySizingResult sizing() {
        var targetOutput = requestedOutputAmount != null ? requestedOutputAmount : encoded.output().amount();
        var successes = Math.max(1, targetOutput);
        return ProbabilitySizing.planAttempts(successes, encoded.successProbability(), encoded.alpha(), encoded.smallSampleLimit());
    }

    public double successProbability() {
        return encoded.successProbability();
    }

    public double alpha() {
        return encoded.alpha();
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return true;
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

    public StatisticalPatternDetails forRequest(long requestedOutputAmount) {
        return new StatisticalPatternDetails(definition, encoded, Math.max(1, requestedOutputAmount));
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
