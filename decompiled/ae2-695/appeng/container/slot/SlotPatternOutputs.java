/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.slot;

import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import net.minecraft.inventory.IInventory;

public class SlotPatternOutputs
extends OptionalSlotFake {
    public SlotPatternOutputs(IInventory inv, IOptionalSlotHost containerBus, int idx, int x, int y, int offX, int offY, int groupNum) {
        super(inv, containerBus, idx, x, y, offX, offY, groupNum);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean shouldDisplay() {
        return super.isEnabled();
    }
}

