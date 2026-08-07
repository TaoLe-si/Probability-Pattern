/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.util.inv.WrapperChainedInventory;
import appeng.util.inv.WrapperInventoryRange;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class AdaptorPlayerInventory
implements IInventory {
    private final IInventory src;
    private final int min = 0;
    private final int size = 36;

    public AdaptorPlayerInventory(IInventory playerInv, boolean swap) {
        this.src = swap ? new WrapperChainedInventory(new WrapperInventoryRange(playerInv, 9, this.size - 9, false), new WrapperInventoryRange(playerInv, 0, 9, false)) : playerInv;
    }

    public int getSizeInventory() {
        return this.size;
    }

    public ItemStack getStackInSlot(int var1) {
        return this.src.getStackInSlot(var1 + this.min);
    }

    public ItemStack decrStackSize(int var1, int var2) {
        return this.src.decrStackSize(this.min + var1, var2);
    }

    public ItemStack getStackInSlotOnClosing(int var1) {
        return this.src.getStackInSlotOnClosing(this.min + var1);
    }

    public void setInventorySlotContents(int var1, ItemStack var2) {
        this.src.setInventorySlotContents(var1 + this.min, var2);
    }

    public String getInventoryName() {
        return this.src.getInventoryName();
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    public int getInventoryStackLimit() {
        return this.src.getInventoryStackLimit();
    }

    public void markDirty() {
        this.src.markDirty();
    }

    public boolean isUseableByPlayer(EntityPlayer var1) {
        return this.src.isUseableByPlayer(var1);
    }

    public void openInventory() {
        this.src.openInventory();
    }

    public void closeInventory() {
        this.src.closeInventory();
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return this.src.isItemValidForSlot(i, itemstack);
    }
}

