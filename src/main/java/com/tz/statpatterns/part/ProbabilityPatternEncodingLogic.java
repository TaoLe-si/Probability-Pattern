package com.tz.statpatterns.part;

import appeng.api.ids.AEComponents;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.inv.AppEngInternalInventory;
import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class ProbabilityPatternEncodingLogic extends PatternEncodingLogic {
    private double probability = 0.8;
    private boolean alpha95 = true;

    public ProbabilityPatternEncodingLogic(IPatternTerminalLogicHost host) {
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
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        super.onChangeInventory(inv, slot);

        if (inv == getEncodedPatternInv()) {
            var pattern = getEncodedPatternInv().getStackInSlot(0);
            if (!pattern.isEmpty() && pattern.getItem() instanceof ProbabilityPatternItem) {
                loadProbabilityPattern(pattern);
            }
        }
    }

    private void loadProbabilityPattern(net.minecraft.world.item.ItemStack pattern) {
        var encoded = pattern.get(AEComponents.ENCODED_PROCESSING_PATTERN);
        if (encoded != null) {
            setMode(EncodingMode.PROCESSING);

            var inputs = encoded.sparseInputs();
            var outputs = encoded.sparseOutputs();

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
                    outputInv.setStack(i, i < outputs.size() ? outputs.get(i) : null);
                }
            } finally {
                outputInv.endBatch();
            }

            saveChanges();
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider provider) {
        super.readFromNBT(tag, provider);
        if (tag.contains("probability")) {
            this.probability = Mth.clamp(tag.getDouble("probability"), 0.01, 0.9999);
        }
        if (tag.contains("alpha95")) {
            this.alpha95 = tag.getBoolean("alpha95");
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeToNBT(tag, provider);
        tag.putDouble("probability", this.probability);
        tag.putBoolean("alpha95", this.alpha95);
    }
}
