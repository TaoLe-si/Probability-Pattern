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
 * Algorithm used to size the number of attempts for a probability pattern.
 * <p>
 * Spec v2.0 4.6: {@code mode} of a {@link ProbabilitySizingResult} is one of
 * {@code BINOMIAL} (exact binomial distribution, small samples) or
 * {@code NORMAL_APPROXIMATION} (normal approximation, large samples).
 */
public enum DistributionMode {

    BINOMIAL("binomial"),
    NORMAL_APPROXIMATION("normal_approximation");

    private final String serializedName;

    DistributionMode(final String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }
}
