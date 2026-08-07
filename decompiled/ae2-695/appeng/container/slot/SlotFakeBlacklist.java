/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.slot;

import appeng.container.slot.SlotFakeTypeOnly;
import net.minecraft.inventory.IInventory;

public class SlotFakeBlacklist
extends SlotFakeTypeOnly {
    public SlotFakeBlacklist(IInventory inv, int idx, int x, int y) {
        super(inv, idx, x, y);
    }

    @Override
    public float getOpacityOfIcon() {
        return 0.8f;
    }

    @Override
    public boolean renderIconWithItem() {
        return true;
    }

    @Override
    public int getIcon() {
        if (this.getHasStack()) {
            return this.getStack().stackSize > 0 ? 30 : 14;
        }
        return -1;
    }
}

