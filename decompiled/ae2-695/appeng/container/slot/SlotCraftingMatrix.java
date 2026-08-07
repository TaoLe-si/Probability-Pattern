/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.slot.AppEngSlot;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotCraftingMatrix
extends AppEngSlot {
    private final Container c;

    public SlotCraftingMatrix(Container c, IInventory par1iInventory, int par2, int par3, int par4) {
        super(par1iInventory, par2, par3, par4);
        this.c = c;
    }

    @Override
    public void clearStack() {
        super.clearStack();
        this.c.onCraftMatrixChanged(this.inventory);
    }

    @Override
    public void putStack(ItemStack par1ItemStack) {
        super.putStack(par1ItemStack);
        this.c.onCraftMatrixChanged(this.inventory);
    }

    @Override
    public boolean isPlayerSide() {
        return true;
    }

    public ItemStack decrStackSize(int par1) {
        ItemStack is = super.decrStackSize(par1);
        this.c.onCraftMatrixChanged(this.inventory);
        return is;
    }
}

