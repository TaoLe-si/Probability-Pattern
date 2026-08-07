/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ISidedInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.util.inv;

import appeng.util.inv.IInventoryWrapper;
import appeng.util.inv.WrapperInventoryRange;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public class WrapperMCISidedInventory
extends WrapperInventoryRange
implements IInventoryWrapper {
    private final ISidedInventory side;
    private final ForgeDirection dir;

    public WrapperMCISidedInventory(ISidedInventory a, ForgeDirection d) {
        super((IInventory)a, a.getAccessibleSlotsFromSide(d.ordinal()), false);
        this.side = a;
        this.dir = d;
    }

    @Override
    public ItemStack decrStackSize(int var1, int var2) {
        if (this.canRemoveItemFromSlot(var1, this.getStackInSlot(var1))) {
            return super.decrStackSize(var1, var2);
        }
        return null;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        if (this.isIgnoreValidItems()) {
            return true;
        }
        if (this.side.isItemValidForSlot(this.getSlots()[i], itemstack)) {
            return this.side.canInsertItem(this.getSlots()[i], itemstack, this.dir.ordinal());
        }
        return false;
    }

    @Override
    public boolean canRemoveItemFromSlot(int i, ItemStack is) {
        if (is == null) {
            return false;
        }
        return this.side.canExtractItem(this.getSlots()[i], is, this.dir.ordinal());
    }
}

