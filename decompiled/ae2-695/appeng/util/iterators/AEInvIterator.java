/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.iterators;

import appeng.api.storage.data.IAEItemStack;
import appeng.tile.inventory.AppEngInternalAEInventory;
import java.util.Iterator;

public final class AEInvIterator
implements Iterator<IAEItemStack> {
    private final AppEngInternalAEInventory inventory;
    private final int size;
    private int counter = 0;

    public AEInvIterator(AppEngInternalAEInventory inventory) {
        this.inventory = inventory;
        this.size = this.inventory.getSizeInventory();
    }

    @Override
    public boolean hasNext() {
        return this.counter < this.size;
    }

    @Override
    public IAEItemStack next() {
        IAEItemStack result = this.inventory.getAEStackInSlot(this.counter);
        ++this.counter;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

