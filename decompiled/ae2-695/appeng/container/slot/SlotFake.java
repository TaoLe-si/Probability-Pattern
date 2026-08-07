/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.slot.AppEngSlot;
import appeng.util.item.AEItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotFake
extends AppEngSlot {
    private IAEItemStack aeStack;

    public SlotFake(IInventory inv, int idx, int x, int y) {
        super(inv, idx, x, y);
    }

    public void onPickupFromSlot(EntityPlayer par1EntityPlayer, ItemStack par2ItemStack) {
    }

    public ItemStack decrStackSize(int par1) {
        return null;
    }

    @Override
    public boolean isItemValid(ItemStack par1ItemStack) {
        return false;
    }

    @Override
    public void putStack(ItemStack is) {
        if (is != null) {
            is = is.copy();
        }
        this.aeStack = AEItemStack.create(is);
        super.putStack(is);
    }

    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        return false;
    }

    public IAEItemStack getAEStack() {
        return this.aeStack;
    }
}

