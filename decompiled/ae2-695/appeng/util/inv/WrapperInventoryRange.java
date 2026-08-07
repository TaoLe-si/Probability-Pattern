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

public class WrapperInventoryRange
implements IInventory {
    private final IInventory src;
    private boolean ignoreValidItems = false;
    private int[] slots;

    public WrapperInventoryRange(IInventory a, int[] s, boolean ignoreValid) {
        this.src = a;
        this.setSlots(s);
        if (this.getSlots() == null) {
            this.setSlots(new int[0]);
        }
        this.setIgnoreValidItems(ignoreValid);
    }

    public WrapperInventoryRange(IInventory a, int min, int size, boolean ignoreValid) {
        this.src = a;
        this.setSlots(new int[size]);
        for (int x = 0; x < size; ++x) {
            this.getSlots()[x] = min + x;
        }
        this.setIgnoreValidItems(ignoreValid);
    }

    public static String concatLines(int[] s, String separator) {
        if (s.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int value : s) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(value);
            }
            return sb.toString();
        }
        return "";
    }

    public int getSizeInventory() {
        return this.getSlots().length;
    }

    public ItemStack getStackInSlot(int var1) {
        return this.src.getStackInSlot(this.getSlots()[var1]);
    }

    public ItemStack decrStackSize(int var1, int var2) {
        return this.src.decrStackSize(this.getSlots()[var1], var2);
    }

    public ItemStack getStackInSlotOnClosing(int var1) {
        return this.src.getStackInSlotOnClosing(this.getSlots()[var1]);
    }

    public void setInventorySlotContents(int var1, ItemStack var2) {
        this.src.setInventorySlotContents(this.getSlots()[var1], var2);
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
        if (this.isIgnoreValidItems()) {
            return true;
        }
        return this.src.isItemValidForSlot(this.getSlots()[i], itemstack);
    }

    boolean isIgnoreValidItems() {
        return this.ignoreValidItems;
    }

    private void setIgnoreValidItems(boolean ignoreValidItems) {
        this.ignoreValidItems = ignoreValidItems;
    }

    int[] getSlots() {
        return this.slots;
    }

    private void setSlots(int[] slots) {
        this.slots = slots;
    }
}

