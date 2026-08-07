/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.api.config.Actionable;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.inv.IInventoryDestination;
import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;

public class IMEInventoryDestination
implements IInventoryDestination {
    private final IMEInventory<IAEItemStack> me;

    public IMEInventoryDestination(IMEInventory<IAEItemStack> o) {
        this.me = o;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        IAEItemStack failed = this.me.injectItems(AEItemStack.create(stack), Actionable.SIMULATE, null);
        if (failed == null) {
            return true;
        }
        return failed.getStackSize() != (long)stack.stackSize;
    }
}

