/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.api.config.FuzzyMode;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import appeng.util.iterators.StackToSlotIterator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.item.ItemStack;

public class AdaptorList
extends InventoryAdaptor {
    private final List<ItemStack> i;

    public AdaptorList(List<ItemStack> s) {
        this.i = s;
    }

    @Override
    public ItemStack removeItems(int amount, ItemStack filter, IInventoryDestination destination) {
        int s = this.i.size();
        for (int x = 0; x < s; ++x) {
            ItemStack is = this.i.get(x);
            if (is == null || filter != null && !Platform.isSameItemPrecise(is, filter)) continue;
            if (amount > is.stackSize) {
                amount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                amount = 0;
            }
            if (amount <= 0) continue;
            ItemStack rv = is.copy();
            rv.stackSize = amount;
            is.stackSize -= amount;
            if (is.stackSize <= 0) {
                this.i.remove(x);
            }
            return rv;
        }
        return null;
    }

    @Override
    public ItemStack simulateRemove(int amount, ItemStack filter, IInventoryDestination destination) {
        for (ItemStack is : this.i) {
            if (is == null || filter != null && !Platform.isSameItemPrecise(is, filter)) continue;
            if (amount > is.stackSize) {
                amount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                amount = 0;
            }
            if (amount <= 0) continue;
            ItemStack rv = is.copy();
            rv.stackSize = amount;
            return rv;
        }
        return null;
    }

    @Override
    public ItemStack removeSimilarItems(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        int s = this.i.size();
        for (int x = 0; x < s; ++x) {
            ItemStack is = this.i.get(x);
            if (is == null || filter != null && !Platform.isSameItemFuzzy(is, filter, fuzzyMode)) continue;
            if (amount > is.stackSize) {
                amount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                amount = 0;
            }
            if (amount <= 0) continue;
            ItemStack rv = is.copy();
            rv.stackSize = amount;
            is.stackSize -= amount;
            if (is.stackSize <= 0) {
                this.i.remove(x);
            }
            return rv;
        }
        return null;
    }

    @Override
    public ItemStack simulateSimilarRemove(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        for (ItemStack is : this.i) {
            if (is == null || filter != null && !Platform.isSameItemFuzzy(is, filter, fuzzyMode)) continue;
            if (amount > is.stackSize) {
                amount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                amount = 0;
            }
            if (amount <= 0) continue;
            ItemStack rv = is.copy();
            rv.stackSize = amount;
            return rv;
        }
        return null;
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded) {
        if (toBeAdded == null) {
            return null;
        }
        if (toBeAdded.stackSize == 0) {
            return null;
        }
        ItemStack left = toBeAdded.copy();
        for (ItemStack is : this.i) {
            if (!Platform.isSameItem(is, left)) continue;
            is.stackSize += left.stackSize;
            return null;
        }
        this.i.add(left);
        return null;
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated) {
        return null;
    }

    @Override
    public boolean containsItems() {
        for (ItemStack is : this.i) {
            if (is == null) continue;
            return true;
        }
        return false;
    }

    @Override
    public Iterator<ItemSlot> iterator() {
        return new StackToSlotIterator(this.i.iterator());
    }
}

