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
 * Result of a probability sizing calculation (Spec v2.0 4.6): the computed number
 * of attempts {@code n} such that {@code P(X >= k) >= 1 - alpha} for a binomial
 * process with {@code k} required successes.
 */
public final class ProbabilitySizingResult {

    /** Computed number of attempts n. */
    private final long attempts;

    public ProbabilitySizingResult(final long attempts) {
        this.attempts = attempts;
    }

    public long attempts() {
        return this.attempts;
    }
}
