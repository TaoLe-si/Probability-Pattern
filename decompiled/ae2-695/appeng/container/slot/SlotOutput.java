/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.slot.AppEngSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotOutput
extends AppEngSlot {
    public SlotOutput(IInventory a, int b, int c, int d, int i) {
        super(a, b, c, d);
        this.setIIcon(i);
    }

    @Override
    public boolean isItemValid(ItemStack i) {
        return false;
    }
}

