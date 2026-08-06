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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.util.Platform;

/**
 * Encoded statistical pattern data (1.7.10 port of the {@code record EncodedStatisticalPattern}
 * from the 1.21.1 version). In 1.7.10 the data is stored as plain NBT on the pattern ItemStack
 * rather than as a DataComponent + Codec.
 */
public final class EncodedStatisticalPattern {

    public static final String TAG_INPUTS = "in";
    public static final String TAG_OUTPUT = "out";
    public static final String TAG_SUCCESS_PROBABILITY = "sp_probability";
    public static final String TAG_ALPHA = "sp_alpha";
    public static final String TAG_ALPHA95 = "sp_alpha95";
    public static final String TAG_SMALL_SAMPLE_LIMIT = "sp_smallSampleLimit";
    public static final String TAG_CRAFTING = "crafting";
    public static final String TAG_SUBSTITUTE = "substitute";

    private final List<ItemStack> inputsPerAttempt;
    private final ItemStack output;
    private final double successProbability;
    private final double alpha;
    private final int smallSampleLimit;
    private final boolean isAlpha95;

    public EncodedStatisticalPattern(final List<ItemStack> inputsPerAttempt, final ItemStack output,
        final double successProbability, final double alpha, final int smallSampleLimit, final boolean isAlpha95) {
        if (inputsPerAttempt == null || inputsPerAttempt.isEmpty()) {
            throw new IllegalArgumentException("At least one input is required.");
        }
        for (ItemStack stack : inputsPerAttempt) {
            if (stack == null || stack.stackSize <= 0) {
                throw new IllegalArgumentException("Inputs must be non-null and positive.");
            }
        }
        if (output == null || output.stackSize <= 0) {
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

        this.inputsPerAttempt = Collections.unmodifiableList(new ArrayList<ItemStack>(inputsPerAttempt));
        this.output = output;
        this.successProbability = successProbability;
        this.alpha = alpha;
        this.smallSampleLimit = smallSampleLimit;
        this.isAlpha95 = isAlpha95;
    }

    public List<ItemStack> inputsPerAttempt() {
        return this.inputsPerAttempt;
    }

    public ItemStack output() {
        return this.output;
    }

    public double successProbability() {
        return this.successProbability;
    }

    public double alpha() {
        return this.alpha;
    }

    public int smallSampleLimit() {
        return this.smallSampleLimit;
    }

    public boolean isAlpha95() {
        return this.isAlpha95;
    }

    /**
     * Serialize to NBT. The standard AE2 pattern keys ("in" / "out" / "crafting" /
     * "substitute") are written so that {@link appeng.helpers.PatternHelper} can also
     * decode the inputs/outputs; the probability keys are namespaced with {@code sp_}.
     */
    public NBTTagCompound writeToNBT(final NBTTagCompound tag) {
        final NBTTagList tagIn = new NBTTagList();
        for (ItemStack in : this.inputsPerAttempt) {
            tagIn.appendTag(createItemTag(in));
        }
        tag.setTag(TAG_INPUTS, tagIn);

        final NBTTagList tagOut = new NBTTagList();
        tagOut.appendTag(createItemTag(this.output));
        tag.setTag(TAG_OUTPUT, tagOut);

        tag.setBoolean(TAG_CRAFTING, false);
        tag.setBoolean(TAG_SUBSTITUTE, false);

        tag.setDouble(TAG_SUCCESS_PROBABILITY, this.successProbability);
        tag.setDouble(TAG_ALPHA, this.alpha);
        tag.setBoolean(TAG_ALPHA95, this.isAlpha95);
        tag.setInteger(TAG_SMALL_SAMPLE_LIMIT, this.smallSampleLimit);
        return tag;
    }

    /**
     * Write the stack using AE2's canonical pattern NBT encoding
     * ({@link appeng.util.Platform#writeItemStackToNBT}). {@code PatternHelper} decodes
     * pattern slots through {@code Platform.loadItemStackFromNBT}, which overwrites
     * {@code stackSize} from the integer {@code Count} tag. Plain
     * {@code ItemStack.writeToNBT} stores {@code Count} as a byte, so the decoded amount
     * would be 0 and the pattern would be rejected as "No pattern here!".
     */
    private static NBTBase createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();
        Platform.writeItemStackToNBT(i, c);
        return c;
    }

    /**
     * Decode from an ItemStack's tag compound. Returns null if the probability keys are absent.
     */
    public static EncodedStatisticalPattern decode(final NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(TAG_SUCCESS_PROBABILITY)) {
            return null;
        }

        final List<ItemStack> inputs = new ArrayList<ItemStack>();
        final NBTTagList inTag = tag.getTagList(TAG_INPUTS, 10);
        for (int x = 0; x < inTag.tagCount(); x++) {
            final ItemStack gs = Platform.loadItemStackFromNBT(inTag.getCompoundTagAt(x));
            if (gs != null) {
                inputs.add(gs);
            }
        }

        ItemStack output = null;
        final NBTTagList outTag = tag.getTagList(TAG_OUTPUT, 10);
        for (int x = 0; x < outTag.tagCount(); x++) {
            final ItemStack gs = Platform.loadItemStackFromNBT(outTag.getCompoundTagAt(x));
            if (gs != null) {
                output = gs;
                break;
            }
        }

        if (inputs.isEmpty() || output == null) {
            return null;
        }

        return new EncodedStatisticalPattern(
            inputs,
            output,
            tag.getDouble(TAG_SUCCESS_PROBABILITY),
            tag.getDouble(TAG_ALPHA),
            tag.getInteger(TAG_SMALL_SAMPLE_LIMIT),
            tag.getBoolean(TAG_ALPHA95));
    }
}
