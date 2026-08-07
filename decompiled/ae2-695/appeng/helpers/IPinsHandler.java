/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.helpers;

import appeng.api.config.PinsState;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;

public interface IPinsHandler {
    default public int getPinCount() {
        return PinsState.getPinsCount();
    }

    default public void setPin(ItemStack is, int idx) {
        throw new UnsupportedOperationException("setPin is not supported by this handler");
    }

    default public void setAEPins(IAEItemStack[] pins) {
        throw new UnsupportedOperationException("setAEPins is not supported by this handler");
    }

    default public ItemStack getPin(int idx) {
        throw new UnsupportedOperationException("getPin is not supported by this handler");
    }

    default public IAEItemStack getAEPin(int idx) {
        throw new UnsupportedOperationException("getAEPin is not supported by this handler");
    }

    default public PinsState getPinsState() {
        return PinsState.DISABLED;
    }

    default public void setPinsState(PinsState state) {
        throw new UnsupportedOperationException("setPinsState is not supported by this handler");
    }
}

