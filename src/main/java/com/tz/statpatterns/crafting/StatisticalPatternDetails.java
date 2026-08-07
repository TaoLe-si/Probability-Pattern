/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
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
package com.tz.statpatterns.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.tz.statpatterns.math.ProbabilitySizing;
import com.tz.statpatterns.math.ProbabilitySizingResult;

import appeng.helpers.PatternHelper;

/**
 * ICraftingPatternDetails for a probability pattern.
 * <p>
 * Extends AE2 GTNH's {@link PatternHelper}, so the per-attempt inputs / outputs are
 * decoded exactly like a vanilla processing pattern. On top of that it carries the
 * probability parameters (success probability p, confidence alpha, alpha95 flag) and
 * computes the planned number of attempts via {@link ProbabilitySizing}.
 * <p>
 * The crafting mixin ({@code CraftingTreeProcess.getTimes}) calls
 * {@link #plannedAttempts(long)} so the crafting tree runs the machine enough times to
 * guarantee P(produced &gt;= requested) &gt;= 1 - alpha.
 */
public class StatisticalPatternDetails extends PatternHelper {

    private final double successProbability;
    private final double alpha;
    private final boolean isAlpha95;
    private final int smallSampleLimit;

    public StatisticalPatternDetails(final ItemStack is, final World w) {
        super(is, w);

        final NBTTagCompound tag = is.getTagCompound();
        this.successProbability = tag.getDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY);
        this.alpha = tag.getDouble(EncodedStatisticalPattern.TAG_ALPHA);
        this.isAlpha95 = tag.getBoolean(EncodedStatisticalPattern.TAG_ALPHA95);
        this.smallSampleLimit = tag.hasKey(EncodedStatisticalPattern.TAG_SMALL_SAMPLE_LIMIT)
            ? tag.getInteger(EncodedStatisticalPattern.TAG_SMALL_SAMPLE_LIMIT)
            : 30;
    }

    /**
     * @return true if this is a genuine probability pattern (p &lt; 1.0). Deterministic
     *         patterns (p == 1.0) behave exactly like normal processing patterns.
     */
    public boolean isProbabilityPattern() {
        return this.successProbability < 1.0;
    }

    public double successProbability() {
        return this.successProbability;
    }

    public double alpha() {
        return this.alpha;
    }

    public boolean isAlpha95() {
        return this.isAlpha95;
    }

    /**
     * Planned number of attempts to reach at least {@code requiredSuccesses} successful
     * attempts with confidence 1 - alpha. The caller converts the requested output amount
     * into the required number of successes first ({@code ceil(requested / perAttempt)}).
     */
    public long plannedAttempts(final long requiredSuccesses) {
        final long successes = Math.max(1, requiredSuccesses);
        final ProbabilitySizingResult sizing = ProbabilitySizing
            .planAttempts(successes, this.successProbability, this.alpha, this.smallSampleLimit);
        return sizing.attempts();
    }
}
