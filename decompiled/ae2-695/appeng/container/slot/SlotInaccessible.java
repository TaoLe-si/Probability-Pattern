/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.slot.AppEngSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotInaccessible
extends AppEngSlot {
    private ItemStack dspStack = null;

    public SlotInaccessible(IInventory i, int slotIdx, int x, int y) {
        super(i, slotIdx, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack i) {
        return false;
    }

    @Override
    public void onSlotChanged() {
        super.onSlotChanged();
        this.dspStack = null;
    }

    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        return false;
    }

    @Override
    public ItemStack getDisplayStack() {
        ItemStack dsp;
        if (this.dspStack == null && (dsp = super.getDisplayStack()) != null) {
            this.dspStack = dsp.copy();
        }
        return this.dspStack;
    }
}

