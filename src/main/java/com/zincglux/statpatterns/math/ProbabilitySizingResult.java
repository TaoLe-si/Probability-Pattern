/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.zincglux.statpatterns.math;

/**
 * Result of a probability sizing calculation.
 * <p>
 * Spec v2.0 4.6: carries the target number of successes, the computed number of
 * attempts, the single-attempt success probability, the significance level, the
 * algorithm used and the actual underproduction risk P(X &lt; k).
 */
public final class ProbabilitySizingResult {

    /** Target number of successes k (requested output / output per attempt). */
    private final long targetSuccesses;

    /** Computed number of attempts n. */
    private final long attempts;

    /** Single-attempt success probability p. */
    private final double successProbability;

    /** Significance level alpha. */
    private final double alpha;

    /** Algorithm used (BINOMIAL or NORMAL_APPROXIMATION). */
    private final DistributionMode mode;

    /** Actual underproduction risk P(X &lt; k) for the returned attempt count. */
    private final double actualRisk;

    public ProbabilitySizingResult(final long targetSuccesses, final long attempts, final double successProbability,
        final double alpha, final DistributionMode mode, final double actualRisk) {
        this.targetSuccesses = targetSuccesses;
        this.attempts = attempts;
        this.successProbability = successProbability;
        this.alpha = alpha;
        this.mode = mode;
        this.actualRisk = actualRisk;
    }

    public long targetSuccesses() {
        return this.targetSuccesses;
    }

    public long attempts() {
        return this.attempts;
    }

    public double successProbability() {
        return this.successProbability;
    }

    public double alpha() {
        return this.alpha;
    }

    public DistributionMode mode() {
        return this.mode;
    }

    public double actualRisk() {
        return this.actualRisk;
    }
}
