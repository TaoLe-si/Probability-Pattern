/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package appeng.util;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class PatternMultiplierHelper {
    private static final int FINAL_BIT = 0x40000000;

    public static int getMaxBitMultiplier(ICraftingPatternDetails details) {
        int max;
        long size;
        int maxMulti = 30;
        for (IAEItemStack input : details.getInputs()) {
            if (input == null) continue;
            size = input.getStackSize();
            max = 0;
            while ((size & 0x40000000L) == 0L) {
                size <<= 1;
                ++max;
            }
            if (max >= maxMulti) continue;
            maxMulti = max;
        }
        for (IAEItemStack out : details.getOutputs()) {
            if (out == null) continue;
            size = out.getStackSize();
            max = 0;
            while ((size & 0x40000000L) == 0L) {
                size <<= 1;
                ++max;
            }
            if (max >= maxMulti) continue;
            maxMulti = max;
        }
        return maxMulti;
    }

    public static int getMaxBitDivider(ICraftingPatternDetails details) {
        int max;
        long size;
        int maxDiv = 30;
        for (IAEItemStack input : details.getInputs()) {
            if (input == null) continue;
            size = input.getStackSize();
            max = 0;
            while ((size & 1L) == 0L) {
                size >>= 1;
                ++max;
            }
            if (max >= maxDiv) continue;
            maxDiv = max;
        }
        for (IAEItemStack out : details.getOutputs()) {
            if (out == null) continue;
            size = out.getStackSize();
            max = 0;
            while ((size & 1L) == 0L) {
                size >>= 1;
                ++max;
            }
            if (max >= maxDiv) continue;
            maxDiv = max;
        }
        return maxDiv;
    }

    public static void applyModification(ItemStack stack, int bitMultiplier) {
        NBTTagCompound tag;
        int x;
        if (bitMultiplier == 0) {
            return;
        }
        boolean isDividing = false;
        if (bitMultiplier < 0) {
            isDividing = true;
            bitMultiplier = -bitMultiplier;
        }
        NBTTagCompound encodedValue = stack.stackTagCompound;
        NBTTagList inTag = encodedValue.getTagList("in", 10);
        NBTTagList outTag = encodedValue.getTagList("out", 10);
        for (x = 0; x < inTag.tagCount(); ++x) {
            tag = inTag.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;
            if (tag.hasKey("Count")) {
                tag.setInteger("Count", isDividing ? tag.getInteger("Count") >> bitMultiplier : tag.getInteger("Count") << bitMultiplier);
            }
            if (!tag.hasKey("Cnt", 4)) continue;
            tag.setLong("Cnt", isDividing ? tag.getLong("Cnt") >> bitMultiplier : tag.getLong("Cnt") << bitMultiplier);
        }
        for (x = 0; x < outTag.tagCount(); ++x) {
            tag = outTag.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;
            if (tag.hasKey("Count")) {
                tag.setInteger("Count", isDividing ? tag.getInteger("Count") >> bitMultiplier : tag.getInteger("Count") << bitMultiplier);
            }
            if (!tag.hasKey("Cnt", 4)) continue;
            tag.setLong("Cnt", isDividing ? tag.getLong("Cnt") >> bitMultiplier : tag.getLong("Cnt") << bitMultiplier);
        }
    }
}

