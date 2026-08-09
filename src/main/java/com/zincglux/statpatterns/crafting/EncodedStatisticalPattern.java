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

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;

/**
 * Independent codec for an encoded probability pattern (Spec v2.0 3.2.1).
 * <p>
 * The NBT layout matches a vanilla GTNH 695 processing pattern so AE2's crafting
 * grid, ME interfaces and molecular assembler all recognise it:
 * <ul>
 * <li>{@code in}: 9-slot list of per-attempt inputs (empty tags for empty slots),
 * stacks written with {@link Platform#writeItemStackToNBT} so {@code Count}
 * is an int.</li>
 * <li>{@code out}: list of outputs.</li>
 * <li>{@code crafting}/{@code substitute}/{@code beSubstitute}/{@code author}:
 * vanilla pattern flags.</li>
 * <li>{@code sp_probability}/{@code sp_alpha}/{@code sp_alpha95}/{@code sp_smallSampleLimit}:
 * the probability parameters.</li>
 * </ul>
 * This class owns both {@link #encode} (used by the terminal container) and the
 * decode helpers (used by {@link StatisticalPatternDetails} via
 * {@code getPatternForItem}), so the on-wire format and the parsing logic live in
 * one place.
 */
public final class EncodedStatisticalPattern {

    public static final String TAG_INPUTS = "in";
    public static final String TAG_OUTPUT = "out";
    public static final String TAG_CRAFTING = "crafting";
    public static final String TAG_SUBSTITUTE = "substitute";
    public static final String TAG_BE_SUBSTITUTE = "beSubstitute";
    public static final String TAG_AUTHOR = "author";
    public static final String TAG_SUCCESS_PROBABILITY = "sp_probability";
    public static final String TAG_ALPHA = "sp_alpha";
    public static final String TAG_ALPHA95 = "sp_alpha95";
    public static final String TAG_SMALL_SAMPLE_LIMIT = "sp_smallSampleLimit";

    /** Small-sample threshold below which the exact binomial algorithm is used. */
    public static final int DEFAULT_SMALL_SAMPLE_LIMIT = 30;

    private EncodedStatisticalPattern() {}

    /**
     * Build the NBT of an encoded probability pattern.
     *
     * @param inputs             9 per-attempt input stacks (nulls allowed for empty slots)
     * @param outputs            output stacks (non-null)
     * @param substitute         allow substitution for inputs
     * @param beSubstitute       allow this pattern to be used as a substitute
     * @param author             player name stored as the pattern author
     * @param successProbability single-attempt success probability p
     * @param alpha              significance level (0.05 = 95%, 0.01 = 99%)
     * @param alpha95            true if 95% confidence was selected
     * @return the encoded tag compound
     */
    public static NBTTagCompound encode(final ItemStack[] inputs, final List<ItemStack> outputs,
        final boolean substitute, final boolean beSubstitute, final String author, final double successProbability,
        final double alpha, final boolean alpha95) {
        final NBTTagCompound tag = new NBTTagCompound();

        final NBTTagList in = new NBTTagList();
        for (final ItemStack stack : inputs) {
            in.appendTag(writeStack(stack));
        }
        final NBTTagList out = new NBTTagList();
        for (final ItemStack stack : outputs) {
            out.appendTag(writeStack(stack));
        }

        tag.setTag(TAG_INPUTS, in);
        tag.setTag(TAG_OUTPUT, out);
        tag.setBoolean(TAG_CRAFTING, false);
        tag.setBoolean(TAG_SUBSTITUTE, substitute);
        tag.setBoolean(TAG_BE_SUBSTITUTE, beSubstitute);
        tag.setString(TAG_AUTHOR, author);

        tag.setDouble(TAG_SUCCESS_PROBABILITY, clampProbability(successProbability));
        tag.setDouble(TAG_ALPHA, alpha);
        tag.setBoolean(TAG_ALPHA95, alpha95);
        tag.setInteger(TAG_SMALL_SAMPLE_LIMIT, DEFAULT_SMALL_SAMPLE_LIMIT);
        return tag;
    }

    /**
     * Read the 9 per-attempt input stacks from an encoded pattern NBT.
     * Returns a fixed-length array; empty slots come back as null.
     */
    public static ItemStack[] decodeInputs(final NBTTagCompound tag) {
        final ItemStack[] inputs = new ItemStack[9];
        if (tag == null || !tag.hasKey(TAG_INPUTS, 9)) {
            return inputs;
        }
        final NBTTagList in = tag.getTagList(TAG_INPUTS, 10);
        for (int i = 0; i < Math.min(in.tagCount(), inputs.length); i++) {
            inputs[i] = Platform.loadItemStackFromNBT(in.getCompoundTagAt(i));
        }
        return inputs;
    }

    /**
     * Read the output stacks from an encoded pattern NBT. Null entries are skipped.
     */
    public static List<ItemStack> decodeOutputs(final NBTTagCompound tag) {
        final List<ItemStack> outputs = new ArrayList<ItemStack>();
        if (tag == null || !tag.hasKey(TAG_OUTPUT, 9)) {
            return outputs;
        }
        final NBTTagList out = tag.getTagList(TAG_OUTPUT, 10);
        for (int i = 0; i < out.tagCount(); i++) {
            final ItemStack stack = Platform.loadItemStackFromNBT(out.getCompoundTagAt(i));
            if (stack != null) {
                outputs.add(stack);
            }
        }
        return outputs;
    }

    /**
     * @return true if the given pattern tag carries the probability parameters, i.e. it is
     *         an encoded probability pattern and not a blank pattern or a vanilla pattern.
     */
    public static boolean isProbabilityPattern(final NBTTagCompound tag) {
        return tag != null && tag.hasKey(TAG_SUCCESS_PROBABILITY);
    }

    /** Convert a raw input list to condensed {@link IAEItemStack}s (deduplicated, summed). */
    public static IAEItemStack[] toCondensedStackList(final ItemStack[] stacks) {
        final List<IAEItemStack> condensed = new ArrayList<IAEItemStack>();
        for (final ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }
            final IAEItemStack entry = AEApi.instance()
                .storage()
                .createItemStack(stack);
            boolean merged = false;
            for (final IAEItemStack existing : condensed) {
                if (existing.equals(entry)) {
                    existing.add(entry);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                condensed.add(entry);
            }
        }
        return condensed.toArray(new IAEItemStack[0]);
    }

    /** Convert a raw output list to condensed {@link IAEItemStack}s. */
    public static IAEItemStack[] toCondensedStackList(final List<ItemStack> stacks) {
        return toCondensedStackList(stacks.toArray(new ItemStack[0]));
    }

    private static NBTTagCompound writeStack(final ItemStack stack) {
        final NBTTagCompound tag = new NBTTagCompound();
        if (stack != null) {
            // Platform.writeItemStackToNBT stores Count as an int; PatternHelper /
            // Platform.loadItemStackFromNBT reads it back correctly. Plain
            // ItemStack.writeToNBT stores a byte Count -> decodes as stackSize 0.
            Platform.writeItemStackToNBT(stack, tag);
        }
        return tag;
    }

    private static double clampProbability(final double p) {
        return Math.max(0.01, Math.min(0.9999, p));
    }
}
