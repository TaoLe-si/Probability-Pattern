/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tz.statpatterns.part;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.inv.AppEngInternalInventory;
import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.crafting.EncodedStatisticalPattern;
import com.tz.statpatterns.crafting.StatPatternsPatternItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class StatPatternsEncodingLogic extends PatternEncodingLogic {
    private double probability = 0.8;
    private boolean alpha95 = true;

    public StatPatternsEncodingLogic(IPatternTerminalLogicHost host) {
        super(host);
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = Mth.clamp(probability, 0.01, 0.9999);
        saveChanges();
    }

    public boolean isAlpha95() {
        return alpha95;
    }

    public void setAlpha95(boolean alpha95) {
        this.alpha95 = alpha95;
        saveChanges();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        super.onChangeInventory(inv, slot);

        if (inv == getEncodedPatternInv()) {
            var pattern = getEncodedPatternInv().getStackInSlot(0);
            if (!pattern.isEmpty() && pattern.getItem() instanceof StatPatternsPatternItem) {
                loadStatPattern(pattern);
            }
        }
    }

    private void loadStatPattern(ItemStack pattern) {
        var encoded = Components.readStatisticalPattern(pattern);
        if (encoded != null) {
            setMode(EncodingMode.PROCESSING);

            var inputs = encoded.inputsPerAttempt();
            var output = encoded.output();

            var inputInv = getEncodedInputInv();
            var outputInv = getEncodedOutputInv();

            inputInv.beginBatch();
            try {
                for (int i = 0; i < inputInv.size(); i++) {
                    inputInv.setStack(i, i < inputs.size() ? inputs.get(i) : null);
                }
            } finally {
                inputInv.endBatch();
            }

            outputInv.beginBatch();
            try {
                for (int i = 0; i < outputInv.size(); i++) {
                    outputInv.setStack(i, i == 0 ? output : null);
                }
            } finally {
                outputInv.endBatch();
            }

            saveChanges();
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        if (tag.contains("probability")) {
            this.probability = Mth.clamp(tag.getDouble("probability"), 0.01, 0.9999);
        }
        if (tag.contains("alpha95")) {
            this.alpha95 = tag.getBoolean("alpha95");
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putDouble("probability", this.probability);
        tag.putBoolean("alpha95", this.alpha95);
    }
}
