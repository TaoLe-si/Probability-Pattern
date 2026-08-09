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
package com.zincglux.statpatterns.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.zincglux.statpatterns.math.ProbabilitySizing;
import com.zincglux.statpatterns.math.ProbabilitySizingResult;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;

/**
 * ICraftingPatternDetails for a probability pattern (Spec v2.0 3.2.2).
 * <p>
 * Implemented from scratch against {@link ICraftingPatternDetails} — it does NOT
 * extend AE2's {@code PatternHelper}. It decodes the per-attempt inputs / outputs
 * from the pattern NBT itself (via {@link EncodedStatisticalPattern}), carries the
 * probability parameters (single-attempt success probability {@code p}, confidence
 * {@code alpha} and the {@code alpha95} flag) and computes the planned number of
 * attempts via {@link ProbabilitySizing}.
 * <p>
 * A probability pattern is always a processing pattern, so {@link #isCraftable()}
 * returns false and the crafting-specific methods ({@link #getOutput},
 * {@link #isValidItemForSlot}) are not supported.
 * <p>
 * The crafting mixin ({@code CraftingTreeProcess.getTimes}) calls
 * {@link #plannedAttempts(long)} so the crafting tree runs the machine enough times
 * to guarantee {@code P(produced >= requested) >= 1 - alpha}.
 */
public class StatisticalPatternDetails implements ICraftingPatternDetails {

    private final ItemStack patternItem;
    private final IAEItemStack[] inputs;
    private final IAEItemStack[] outputs;
    private final IAEItemStack[] condensedInputs;
    private final IAEItemStack[] condensedOutputs;
    private final boolean canSubstitute;
    private final boolean canBeSubstitute;
    private final double successProbability;
    private final double alpha;
    private final int smallSampleLimit;
    private int priority = 0;

    public StatisticalPatternDetails(final ItemStack is, final World w) {
        this.patternItem = is;

        final NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            throw new IllegalArgumentException("No pattern here!");
        }

        this.canSubstitute = tag.getBoolean(EncodedStatisticalPattern.TAG_SUBSTITUTE);
        this.canBeSubstitute = tag.getBoolean(EncodedStatisticalPattern.TAG_BE_SUBSTITUTE);
        this.successProbability = tag.getDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY);
        this.alpha = tag.getDouble(EncodedStatisticalPattern.TAG_ALPHA);
        this.smallSampleLimit = tag.hasKey(EncodedStatisticalPattern.TAG_SMALL_SAMPLE_LIMIT)
            ? tag.getInteger(EncodedStatisticalPattern.TAG_SMALL_SAMPLE_LIMIT)
            : EncodedStatisticalPattern.DEFAULT_SMALL_SAMPLE_LIMIT;

        this.inputs = toStackArray(EncodedStatisticalPattern.decodeInputs(tag), true);
        this.outputs = toStackArray(
            EncodedStatisticalPattern.decodeOutputs(tag)
                .toArray(new ItemStack[0]),
            false);
        this.condensedInputs = EncodedStatisticalPattern
            .toCondensedStackList(EncodedStatisticalPattern.decodeInputs(tag));
        this.condensedOutputs = EncodedStatisticalPattern
            .toCondensedStackList(EncodedStatisticalPattern.decodeOutputs(tag));

        // Mirror AE2's PatternHelper: a pattern with no inputs or no outputs is malformed and
        // must not reach the crafting tree (a null matchingOutput would NPE in the resolver).
        if (this.condensedInputs.length == 0 || this.condensedOutputs.length == 0) {
            throw new IllegalArgumentException("No pattern here!");
        }
    }

    /**
     * AE2 stores pattern details in hash maps (e.g. {@code CraftingGridCache.craftingMethods})
     * and relies on a stable identity based on the source item — mirror the behaviour of
     * AE2's {@code PatternHelper}, which keys equality on the pattern stack.
     */
    @Override
    public int hashCode() {
        return this.patternItem == null ? 0 : this.patternItem.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final StatisticalPatternDetails other = (StatisticalPatternDetails) obj;
        return this.patternItem != null && other.patternItem != null && this.patternItem.equals(other.patternItem);
    }

    /**
     * @return true — this is always a probability pattern (Spec v2.0 3.2.2). A
     *         deterministic encoding (p == 1.0) still goes through {@link #plannedAttempts},
     *         which degrades to {@code n == k} and behaves exactly like a normal pattern.
     */
    public boolean isProbabilityPattern() {
        return true;
    }

    /**
     * Planned number of attempts to reach at least {@code requiredSuccesses} successful
     * attempts with confidence {@code 1 - alpha}. The caller converts the requested output
     * amount into the required number of successes first ({@code ceil(requested / perAttempt)}).
     */
    public long plannedAttempts(final long requiredSuccesses) {
        final long successes = Math.max(1, requiredSuccesses);
        final ProbabilitySizingResult sizing = ProbabilitySizing
            .planAttempts(successes, this.successProbability, this.alpha, this.smallSampleLimit);
        return sizing.attempts();
    }

    // ---- ICraftingPatternDetails ----

    @Override
    public ItemStack getPattern() {
        return this.patternItem;
    }

    @Override
    public boolean isValidItemForSlot(final int slotIndex, final ItemStack itemStack, final World world) {
        // Processing pattern: no 3x3 crafting grid semantics.
        return false;
    }

    @Override
    public boolean isCraftable() {
        return false; // probability patterns are always processing patterns
    }

    @Override
    public IAEItemStack[] getInputs() {
        return this.inputs;
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return this.condensedInputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return this.condensedOutputs;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return this.outputs;
    }

    @Override
    public boolean canSubstitute() {
        return this.canSubstitute;
    }

    @Override
    public boolean canBeSubstitute() {
        return this.canBeSubstitute;
    }

    @Override
    public ItemStack getOutput(final InventoryCrafting craftingInv, final World world) {
        // Processing pattern: no crafting-table output.
        return null;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    /** Convert raw stacks to IAEItemStack[]. If includeNulls, nulls are preserved (for getInputs). */
    private static IAEItemStack[] toStackArray(final ItemStack[] stacks, final boolean includeNulls) {
        final IAEItemStack[] out = new IAEItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] != null) {
                out[i] = AEApi.instance()
                    .storage()
                    .createItemStack(stacks[i]);
            } else if (!includeNulls) {
                // skip; the array is already sized, nulls stay null but we compact below
            }
        }
        if (!includeNulls) {
            final List<IAEItemStack> compact = new ArrayList<IAEItemStack>();
            for (final IAEItemStack s : out) {
                if (s != null) {
                    compact.add(s);
                }
            }
            return compact.toArray(new IAEItemStack[0]);
        }
        return out;
    }
}
