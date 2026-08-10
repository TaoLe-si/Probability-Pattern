
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

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import appeng.api.stacks.GenericStack;

public record EncodedStatPatterns(
        List<GenericStack> inputsPerAttempt,
        GenericStack output,
        double successProbability,
        double alpha,
        int smallSampleLimit,
        boolean isAlpha95) {
    public EncodedStatPatterns {
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

    public static final Codec<EncodedStatPatterns> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("inputsPerAttempt")
                            .forGetter(EncodedStatPatterns::inputsPerAttempt),
                    GenericStack.CODEC.fieldOf("output")
                            .forGetter(EncodedStatPatterns::output),
                    Codec.DOUBLE.fieldOf("successProbability")
                            .forGetter(EncodedStatPatterns::successProbability),
                    Codec.DOUBLE.optionalFieldOf("alpha", 0.05)
                            .forGetter(EncodedStatPatterns::alpha),
                    Codec.INT.optionalFieldOf("smallSampleLimit", 30)
                            .forGetter(EncodedStatPatterns::smallSampleLimit),
                    Codec.BOOL.optionalFieldOf("isAlpha95", true)
                            .forGetter(EncodedStatPatterns::isAlpha95))
            .apply(builder, EncodedStatPatterns::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedStatPatterns> STREAM_CODEC = StreamCodec
            .composite(
                    GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    EncodedStatPatterns::inputsPerAttempt,
                    GenericStack.STREAM_CODEC,
                    EncodedStatPatterns::output,
                    ByteBufCodecs.DOUBLE,
                    EncodedStatPatterns::successProbability,
                    ByteBufCodecs.DOUBLE,
                    EncodedStatPatterns::alpha,
                    ByteBufCodecs.VAR_INT,
                    EncodedStatPatterns::smallSampleLimit,
                    ByteBufCodecs.BOOL,
                    EncodedStatPatterns::isAlpha95,
                    EncodedStatPatterns::new);
}