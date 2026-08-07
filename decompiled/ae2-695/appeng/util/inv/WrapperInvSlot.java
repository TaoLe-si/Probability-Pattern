/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class WrapperInvSlot {
    private final IInventory inv;

    public WrapperInvSlot(IInventory inv) {
        this.inv = inv;
    }

    public IInventory getWrapper(int slot) {
        return new InternalInterfaceWrapper(this.inv, slot);
    }

    protected boolean isItemValid(ItemStack itemstack) {
        return true;
    }

    private class InternalInterfaceWrapper
    implements IInventory {
        private final IInventory inv;
        private final int slot;

        public InternalInterfaceWrapper(IInventory target, int slot) {
            this.inv = target;
            this.slot = slot;
        }

        public int getSizeInventory() {
            return 1;
        }

        public ItemStack getStackInSlot(int i) {
            return this.inv.getStackInSlot(this.slot);
        }

        public ItemStack decrStackSize(int i, int num) {
            return this.inv.decrStackSize(this.slot, num);
        }

        public ItemStack getStackInSlotOnClosing(int i) {
            return this.inv.getStackInSlotOnClosing(this.slot);
        }

        public void setInventorySlotContents(int i, ItemStack itemstack) {
            this.inv.setInventorySlotContents(this.slot, itemstack);
        }

        public String getInventoryName() {
            return this.inv.getInventoryName();
        }

        public boolean hasCustomInventoryName() {
            return this.inv.hasCustomInventoryName();
        }

        public int getInventoryStackLimit() {
            return this.inv.getInventoryStackLimit();
        }

        public void markDirty() {
            this.inv.markDirty();
        }

        public boolean isUseableByPlayer(EntityPlayer entityplayer) {
            return this.inv.isUseableByPlayer(entityplayer);
        }

        public void openInventory() {
            this.inv.openInventory();
        }

        public void closeInventory() {
            this.inv.closeInventory();
        }

        public boolean isItemValidForSlot(int i, ItemStack itemstack) {
            return WrapperInvSlot.this.isItemValid(itemstack) && this.inv.isItemValidForSlot(this.slot, itemstack);
        }
    }
}

