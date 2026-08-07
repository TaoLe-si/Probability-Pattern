/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class OptionalSlotFakeTypeOnly
extends OptionalSlotFake {
    public OptionalSlotFakeTypeOnly(IInventory inv, IOptionalSlotHost containerBus, int idx, int x, int y, int offX, int offY, int groupNum) {
        super(inv, containerBus, idx, x, y, offX, offY, groupNum);
    }

    @Override
    public void putStack(ItemStack is) {
        if (is != null) {
            is = is.copy();
            if (is.stackSize > 1) {
                is.stackSize = 1;
            } else if (is.stackSize < -1) {
                is.stackSize = -1;
            }
        }
        super.putStack(is);
    }
}

