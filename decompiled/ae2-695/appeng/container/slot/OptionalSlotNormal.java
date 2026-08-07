/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.slot;

import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import net.minecraft.inventory.IInventory;

public class OptionalSlotNormal
extends AppEngSlot {
    private final int groupNum;
    private final IOptionalSlotHost host;

    public OptionalSlotNormal(IInventory inv, IOptionalSlotHost containerBus, int slot, int xPos, int yPos, int groupNum) {
        super(inv, slot, xPos, yPos);
        this.groupNum = groupNum;
        this.host = containerBus;
    }

    @Override
    public boolean isEnabled() {
        if (this.host == null) {
            return false;
        }
        return this.host.isSlotEnabled(this.groupNum);
    }
}

