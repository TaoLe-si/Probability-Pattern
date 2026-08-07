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

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.math.ProbabilitySizing;
import com.tz.statpatterns.math.ProbabilitySizingResult;

import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.PatternHelper;
import cpw.mods.fml.common.FMLLog;

/**
 * ICraftingPatternDetails implementation for a probability (statistical) pattern.
 * <p>
 * Extends AE2's {@link PatternHelper} so the standard per-attempt inputs/outputs are
 * decoded exactly like a normal processing pattern. On top of that it carries the
 * probability parameters (success probability p, significance alpha, alpha95 flag).
 * <p>
 * The number of attempts for a requested output amount N is computed by
 * {@link #plannedAttempts(long)} using {@link ProbabilitySizing}. The crafting
 * interception coremod injects a call to this method into
 * {@code appeng.crafting.CraftingTreeProcess.getTimes} so that the crafting tree
 * runs the machine enough times to guarantee P(produced >= N) >= 1 - alpha.
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

        // Diagnostic: shows where AE2 constructs our details (interface registration, crafting
        // job, tooltip rendering, ...) and that the per-attempt inputs/outputs decode correctly.
        try {
            final IAEItemStack[] ins = this.getInputs();
            final IAEItemStack[] outs = this.getOutputs();
            FMLLog.info(
                "[ProbabilityPattern] details constructed: in=%d out=%d outStack=%d p=%.3f alpha=%.3f",
                ins.length,
                outs.length,
                outs.length > 0 && outs[0] != null ? outs[0].getStackSize() : -1L,
                this.successProbability,
                this.alpha);
        } catch (final Throwable t) {
            FMLLog.info("[ProbabilityPattern] details constructed but getInputs/getOutputs threw: %s", t.toString());
        }
    }

    /**
     * @return true if this is a genuine probability pattern (p < 1.0). Deterministic
     *         patterns (p == 1.0) behave exactly like normal processing patterns and must NOT
     *         have their attempt count overridden.
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

    public int smallSampleLimit() {
        return this.smallSampleLimit;
    }

    /**
     * Compute the planned number of attempts needed to reach at least
     * {@code requiredSuccesses} successful attempts with confidence 1 - alpha.
     * <p>
     * This is the method invoked by the crafting-interception mixin
     * ({@code CraftingTreeProcess.getTimes}). The caller converts the requested output
     * amount into the required number of <em>successes</em> first
     * ({@code ceil(requestedOutput / outputPerAttempt)}), so the success target, not the
     * raw output count, is passed in here — mirroring the 1.21.1 original's
     * {@code StatisticalPatternDetails.sizing()}. Do NOT divide by the per-attempt output
     * amount again here, that would under-count the successes and fail the confidence.
     */
    public long plannedAttempts(final long requiredSuccesses) {
        final long successes = Math.max(1, requiredSuccesses);
        final ProbabilitySizingResult sizing = ProbabilitySizing
            .planAttempts(successes, this.successProbability, this.alpha, this.smallSampleLimit);
        return sizing.attempts();
    }

    /**
     * Create a probability pattern ItemStack from per-attempt inputs and a target output.
     */
    public static ItemStack encode(final List<ItemStack> inputsPerAttempt, final ItemStack output,
        final double successProbability, final double alpha, final boolean alpha95) {
        final EncodedStatisticalPattern encoded = new EncodedStatisticalPattern(
            inputsPerAttempt,
            output,
            successProbability,
            alpha,
            30,
            alpha95);
        final ItemStack stack = new ItemStack(ProbabilityPatternMod.probabilityPatternItem);
        final NBTTagCompound tag = new NBTTagCompound();
        encoded.writeToNBT(tag);
        stack.setTagCompound(tag);
        return stack;
    }
}
