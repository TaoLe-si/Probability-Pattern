/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;

public class ItemSlot {
    private int slot;
    private boolean isExtractable;
    private IAEItemStack aeItemStack;
    private ItemStack itemStack;

    public ItemStack getItemStack() {
        return this.itemStack == null ? (this.aeItemStack == null ? null : (this.itemStack = this.aeItemStack.getItemStack())) : this.itemStack;
    }

    public void setItemStack(ItemStack is) {
        this.aeItemStack = null;
        this.itemStack = is;
    }

    public IAEItemStack getAEItemStack() {
        return this.aeItemStack == null ? (this.itemStack == null ? null : (this.aeItemStack = AEItemStack.create(this.itemStack))) : this.aeItemStack;
    }

    void setAEItemStack(IAEItemStack is) {
        this.aeItemStack = is;
        this.itemStack = null;
    }

    public boolean isExtractable() {
        return this.isExtractable;
    }

    void setExtractable(boolean isExtractable) {
        this.isExtractable = isExtractable;
    }

    public int getSlot() {
        return this.slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }
}

