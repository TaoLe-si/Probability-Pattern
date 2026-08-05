package com.tz.statpatterns.crafting;

import java.util.Collections;
import java.util.List;

import appeng.api.stacks.GenericStack;

/**
 * Record holding the encoded statistical pattern data.
 * In 1.20.1, serialization is handled via NBT in Components class
 * (no DataComponent/Codec/StreamCodec in Forge 1.20.1).
 */
public record EncodedStatisticalPattern(
        List<GenericStack> inputsPerAttempt,
        GenericStack output,
        double successProbability,
        double alpha,
        int smallSampleLimit,
        boolean isAlpha95) {
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
}
