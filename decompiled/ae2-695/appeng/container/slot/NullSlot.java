/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class NullSlot
extends Slot {
    public NullSlot() {
        super(null, 0, 0, 0);
    }

    public void onSlotChange(ItemStack par1ItemStack, ItemStack par2ItemStack) {
    }

    public void onPickupFromSlot(EntityPlayer par1EntityPlayer, ItemStack par2ItemStack) {
    }

    public boolean isItemValid(ItemStack par1ItemStack) {
        return false;
    }

    public ItemStack getStack() {
        return null;
    }

    public void putStack(ItemStack par1ItemStack) {
    }

    public void onSlotChanged() {
    }

    public int getSlotStackLimit() {
        return 0;
    }

    public ItemStack decrStackSize(int par1) {
        return null;
    }

    public boolean isSlotInInventory(IInventory par1IInventory, int par2) {
        return false;
    }

    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        return false;
    }

    public int getSlotIndex() {
        return 0;
    }
}

