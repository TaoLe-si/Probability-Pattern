package com.tz.statpatterns.crafting;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import appeng.api.stacks.GenericStack;

public record EncodedStatisticalPattern(
        List<GenericStack> inputsPerAttempt,
        GenericStack output,
        double successProbability,
        double alpha,
        int smallSampleLimit) {
    public EncodedStatisticalPattern {
        inputsPerAttempt = Collections.unmodifiableList(inputsPerAttempt);
        if (inputsPerAttempt.isEmpty()) {
            throw new IllegalArgumentException("At least one input is required.");
        }
        if (inputsPerAttempt.stream().anyMatch(stack -> stack == null || stack.amount() <= 0)) {
            throw new IllegalArgumentException("Inputs must be non-null and positive.");
        }
        if (output.amount() <= 0) {
            throw new IllegalArgumentException("Output amount must be positive.");
        }
        if (!(successProbability > 0.0 && successProbability <= 1.0)) {
            throw new IllegalArgumentException("Success probability must be in (0, 1].");
        }
        if (!(alpha > 0.0 && alpha < 1.0)) {
            throw new IllegalArgumentException("Alpha must be in (0, 1).");
        }
        if (smallSampleLimit < 1) {
            throw new IllegalArgumentException("Small sample limit must be positive.");
        }
    }

    public static final Codec<EncodedStatisticalPattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("inputsPerAttempt")
                            .forGetter(EncodedStatisticalPattern::inputsPerAttempt),
                    GenericStack.CODEC.fieldOf("output")
                            .forGetter(EncodedStatisticalPattern::output),
                    Codec.DOUBLE.fieldOf("successProbability")
                            .forGetter(EncodedStatisticalPattern::successProbability),
                    Codec.DOUBLE.optionalFieldOf("alpha", 0.05)
                            .forGetter(EncodedStatisticalPattern::alpha),
                    Codec.INT.optionalFieldOf("smallSampleLimit", 30)
                            .forGetter(EncodedStatisticalPattern::smallSampleLimit))
            .apply(builder, EncodedStatisticalPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedStatisticalPattern> STREAM_CODEC = StreamCodec
            .composite(
                    GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    EncodedStatisticalPattern::inputsPerAttempt,
                    GenericStack.STREAM_CODEC,
                    EncodedStatisticalPattern::output,
                    ByteBufCodecs.DOUBLE,
                    EncodedStatisticalPattern::successProbability,
                    ByteBufCodecs.DOUBLE,
                    EncodedStatisticalPattern::alpha,
                    ByteBufCodecs.VAR_INT,
                    EncodedStatisticalPattern::smallSampleLimit,
                    EncodedStatisticalPattern::new);
}