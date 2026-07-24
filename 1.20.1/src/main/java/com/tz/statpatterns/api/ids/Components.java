package com.tz.statpatterns.api.ids;

import com.tz.statpatterns.crafting.EncodedStatisticalPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.GenericStack;

import java.util.ArrayList;
import java.util.List;

/**
 * NBT-based replacement for DataComponents in 1.20.1 Forge.
 * Provides helper methods to read/write EncodedStatisticalPattern from/to ItemStack NBT.
 */
public class Components {

    private static final String KEY_STAT_PATTERN = "sp_statistical_pattern";
    private static final String KEY_PATTERN_LOGIC = "sp_pattern_logic";

    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_SUCCESS_PROB = "successProbability";
    private static final String KEY_ALPHA = "alpha";
    private static final String KEY_SMALL_SAMPLE = "smallSampleLimit";
    private static final String KEY_IS_ALPHA95 = "isAlpha95";

    /**
     * Read the EncodedStatisticalPattern from an ItemStack's NBT tag.
     */
    public static EncodedStatisticalPattern readStatisticalPattern(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_STAT_PATTERN)) {
            return null;
        }
        return readFromNBT(tag.getCompound(KEY_STAT_PATTERN));
    }

    /**
     * Write an EncodedStatisticalPattern to an ItemStack's NBT tag.
     */
    public static void writeStatisticalPattern(ItemStack stack, EncodedStatisticalPattern pattern) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(KEY_STAT_PATTERN, writeToNBT(pattern));
    }

    /**
     * Read the pattern logic state (CompoundTag) from an ItemStack.
     */
    public static CompoundTag readPatternLogicState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_PATTERN_LOGIC)) {
            return null;
        }
        return tag.getCompound(KEY_PATTERN_LOGIC);
    }

    /**
     * Write the pattern logic state (CompoundTag) to an ItemStack.
     */
    public static void writePatternLogicState(ItemStack stack, CompoundTag state) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(KEY_PATTERN_LOGIC, state);
    }

    private static EncodedStatisticalPattern readFromNBT(CompoundTag tag) {
        ListTag inputsList = tag.getList(KEY_INPUTS, Tag.TAG_COMPOUND);
        List<GenericStack> inputs = new ArrayList<>();
        for (int i = 0; i < inputsList.size(); i++) {
            GenericStack stack = GenericStack.readTag(inputsList.getCompound(i));
            if (stack != null) {
                inputs.add(stack);
            }
        }

        GenericStack output = GenericStack.readTag(tag.getCompound(KEY_OUTPUT));
        if (output == null || inputs.isEmpty()) {
            return null;
        }

        double successProbability = tag.getDouble(KEY_SUCCESS_PROB);
        double alpha = tag.contains(KEY_ALPHA) ? tag.getDouble(KEY_ALPHA) : 0.05;
        int smallSampleLimit = tag.contains(KEY_SMALL_SAMPLE) ? tag.getInt(KEY_SMALL_SAMPLE) : 30;
        boolean isAlpha95 = !tag.contains(KEY_IS_ALPHA95) || tag.getBoolean(KEY_IS_ALPHA95);

        return new EncodedStatisticalPattern(inputs, output, successProbability, alpha, smallSampleLimit, isAlpha95);
    }

    private static CompoundTag writeToNBT(EncodedStatisticalPattern pattern) {
        CompoundTag tag = new CompoundTag();

        ListTag inputsList = new ListTag();
        for (GenericStack input : pattern.inputsPerAttempt()) {
            CompoundTag inputTag = GenericStack.writeTag(input);
            if (inputTag != null) {
                inputsList.add(inputTag);
            }
        }
        tag.put(KEY_INPUTS, inputsList);

        CompoundTag outputTag = GenericStack.writeTag(pattern.output());
        if (outputTag != null) {
            tag.put(KEY_OUTPUT, outputTag);
        }

        tag.putDouble(KEY_SUCCESS_PROB, pattern.successProbability());
        tag.putDouble(KEY_ALPHA, pattern.alpha());
        tag.putInt(KEY_SMALL_SAMPLE, pattern.smallSampleLimit());
        tag.putBoolean(KEY_IS_ALPHA95, pattern.isAlpha95());

        return tag;
    }
}
