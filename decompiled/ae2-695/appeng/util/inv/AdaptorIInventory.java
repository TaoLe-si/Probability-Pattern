/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.IInventoryWrapper;
import appeng.util.inv.ItemSlot;
import java.util.Iterator;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class AdaptorIInventory
extends InventoryAdaptor {
    private final IInventory i;
    private final boolean wrapperEnabled;
    private boolean skipStackSizeCheck;

    public AdaptorIInventory(IInventory s) {
        this.i = s;
        this.skipStackSizeCheck = this.i.getClass().toString().equals("class wanion.avaritiaddons.block.chest.infinity.TileEntityInfinityChest");
        this.wrapperEnabled = s instanceof IInventoryWrapper;
    }

    public AdaptorIInventory(IInventory s, int maxStack) {
        this(s);
        this.skipStackSizeCheck = this.skipStackSizeCheck || maxStack == Integer.MAX_VALUE;
    }

    @Override
    public ItemStack removeItems(int amount, ItemStack filter, IInventoryDestination destination) {
        int s = this.i.getSizeInventory();
        ItemStack rv = null;
        for (int x = 0; x < s && amount > 0; ++x) {
            ItemStack is = this.i.getStackInSlot(x);
            if (is == null || !this.canRemoveStackFromSlot(x, is) || filter != null && !Platform.isSameItemPrecise(is, filter)) continue;
            int boundAmounts = amount;
            if (boundAmounts > is.stackSize) {
                boundAmounts = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                boundAmounts = 0;
            }
            if (boundAmounts <= 0) continue;
            if (rv == null) {
                filter = rv = is.copy();
                rv.stackSize = boundAmounts;
                amount -= boundAmounts;
            } else {
                rv.stackSize += boundAmounts;
                amount -= boundAmounts;
            }
            if (is.stackSize == boundAmounts) {
                this.i.setInventorySlotContents(x, null);
                this.i.markDirty();
                continue;
            }
            ItemStack po = is.copy();
            po.stackSize -= boundAmounts;
            this.i.setInventorySlotContents(x, po);
            this.i.markDirty();
        }
        return rv;
    }

    @Override
    public ItemStack simulateRemove(int amount, ItemStack filter, IInventoryDestination destination) {
        int s = this.i.getSizeInventory();
        ItemStack rv = null;
        for (int x = 0; x < s && amount > 0; ++x) {
            ItemStack is = this.i.getStackInSlot(x);
            if (is == null || !this.canRemoveStackFromSlot(x, is) || filter != null && !Platform.isSameItemPrecise(is, filter)) continue;
            int boundAmount = amount;
            if (boundAmount > is.stackSize) {
                boundAmount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                boundAmount = 0;
            }
            if (boundAmount <= 0) continue;
            if (rv == null) {
                rv = is.copy();
                rv.stackSize = boundAmount;
                amount -= boundAmount;
                continue;
            }
            rv.stackSize += boundAmount;
            amount -= boundAmount;
        }
        return rv;
    }

    @Override
    public ItemStack removeSimilarItems(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        int s = this.i.getSizeInventory();
        for (int x = 0; x < s; ++x) {
            ItemStack is = this.i.getStackInSlot(x);
            if (is == null || !this.canRemoveStackFromSlot(x, is) || filter != null && !Platform.isSameItemFuzzy(is, filter, fuzzyMode)) continue;
            int newAmount = amount;
            if (newAmount > is.stackSize) {
                newAmount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                newAmount = 0;
            }
            ItemStack rv = null;
            if (newAmount > 0) {
                rv = is.copy();
                rv.stackSize = newAmount;
                if (is.stackSize == rv.stackSize) {
                    this.i.setInventorySlotContents(x, null);
                    this.i.markDirty();
                } else {
                    ItemStack po = is.copy();
                    po.stackSize -= rv.stackSize;
                    this.i.setInventorySlotContents(x, po);
                    this.i.markDirty();
                }
            }
            if (rv == null) continue;
            return rv;
        }
        return null;
    }

    @Override
    public ItemStack simulateSimilarRemove(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        int s = this.i.getSizeInventory();
        for (int x = 0; x < s; ++x) {
            ItemStack is = this.i.getStackInSlot(x);
            if (is == null || !this.canRemoveStackFromSlot(x, is) || filter != null && !Platform.isSameItemFuzzy(is, filter, fuzzyMode)) continue;
            int boundAmount = amount;
            if (boundAmount > is.stackSize) {
                boundAmount = is.stackSize;
            }
            if (destination != null && !destination.canInsert(is)) {
                boundAmount = 0;
            }
            if (boundAmount <= 0) continue;
            ItemStack rv = is.copy();
            rv.stackSize = boundAmount;
            return rv;
        }
        return null;
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded) {
        return this.addItems(toBeAdded, true, InsertionMode.DEFAULT);
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded, InsertionMode insertionMode) {
        return this.addItems(toBeAdded, true, insertionMode);
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated) {
        return this.addItems(toBeSimulated, false, InsertionMode.DEFAULT);
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated, InsertionMode insertionMode) {
        return this.addItems(toBeSimulated, false, insertionMode);
    }

    @Override
    public boolean containsItems() {
        int s = this.i.getSizeInventory();
        for (int x = 0; x < s; ++x) {
            if (this.i.getStackInSlot(x) == null) continue;
            return true;
        }
        return false;
    }

    private ItemStack addItems(ItemStack itemsToAdd, boolean modulate, InsertionMode insertionMode) {
        ItemStack next;
        int slot;
        if (itemsToAdd == null || itemsToAdd.stackSize == 0) {
            return null;
        }
        ItemStack left = itemsToAdd.copy();
        int invLimit = this.i.getInventoryStackLimit();
        int perOperationLimit = this.skipStackSizeCheck ? invLimit : Math.min(invLimit, itemsToAdd.getMaxStackSize());
        int inventorySize = this.i.getSizeInventory();
        if (insertionMode != InsertionMode.DEFAULT) {
            for (slot = 0; slot < inventorySize; ++slot) {
                next = left.copy();
                next.stackSize = Math.min(perOperationLimit, next.stackSize);
                if (!this.i.isItemValidForSlot(slot, next) || this.i.getStackInSlot(slot) != null) continue;
                if (modulate) {
                    this.i.setInventorySlotContents(slot, next);
                    this.i.markDirty();
                }
                left.stackSize -= next.stackSize;
                if (left.stackSize > 0) continue;
                return null;
            }
        }
        if (insertionMode == InsertionMode.ONLY_EMPTY) {
            return left;
        }
        for (slot = 0; slot < inventorySize; ++slot) {
            next = left.copy();
            next.stackSize = Math.min(perOperationLimit, next.stackSize);
            if (!this.i.isItemValidForSlot(slot, next)) continue;
            ItemStack is = this.i.getStackInSlot(slot);
            if (is == null) {
                left.stackSize -= next.stackSize;
                if (modulate) {
                    this.i.setInventorySlotContents(slot, next);
                    this.i.markDirty();
                }
                if (left.stackSize > 0) continue;
                return null;
            }
            if (!Platform.isSameItemPrecise(is, left) || is.stackSize >= perOperationLimit) continue;
            int room = perOperationLimit - is.stackSize;
            int used = Math.min(left.stackSize, room);
            if (modulate) {
                is.stackSize += used;
                this.i.setInventorySlotContents(slot, is);
                this.i.markDirty();
            }
            left.stackSize -= used;
            if (left.stackSize > 0) continue;
            return null;
        }
        return left;
    }

    private boolean canRemoveStackFromSlot(int x, ItemStack is) {
        if (this.wrapperEnabled) {
            return ((IInventoryWrapper)this.i).canRemoveItemFromSlot(x, is);
        }
        return true;
    }

    @Override
    public Iterator<ItemSlot> iterator() {
        return new InvIterator();
    }

    private class InvIterator
    implements Iterator<ItemSlot> {
        private final ItemSlot is = new ItemSlot();
        private int x = 0;

        private InvIterator() {
        }

        @Override
        public boolean hasNext() {
            return this.x < AdaptorIInventory.this.i.getSizeInventory();
        }

        @Override
        public ItemSlot next() {
            ItemStack iss = AdaptorIInventory.this.i.getStackInSlot(this.x);
            this.is.setExtractable(AdaptorIInventory.this.canRemoveStackFromSlot(this.x, iss));
            this.is.setItemStack(iss);
            this.is.setSlot(this.x);
            ++this.x;
            return this.is;
        }

        @Override
        public void remove() {
        }
    }
}

