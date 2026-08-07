/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.inv;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.inv.IMEAdaptor;
import appeng.util.inv.ItemSlot;
import java.util.Iterator;

public final class IMEAdaptorIterator
implements Iterator<ItemSlot> {
    private final Iterator<IAEItemStack> stack;
    private final ItemSlot slot = new ItemSlot();
    private final IMEAdaptor parent;
    private final int containerSize;
    private int offset = 0;
    private boolean hasNext;

    public IMEAdaptorIterator(IMEAdaptor parent, IItemList<IAEItemStack> availableItems) {
        this.stack = availableItems.iterator();
        this.containerSize = parent.getMaxSlots();
        this.parent = parent;
    }

    @Override
    public boolean hasNext() {
        this.hasNext = this.stack.hasNext();
        return this.offset < this.containerSize || this.hasNext;
    }

    @Override
    public ItemSlot next() {
        this.slot.setSlot(this.offset);
        ++this.offset;
        this.slot.setExtractable(true);
        if (this.parent.getMaxSlots() < this.offset) {
            this.parent.setMaxSlots(this.offset);
        }
        if (this.hasNext) {
            IAEItemStack item = this.stack.next();
            this.slot.setAEItemStack(item);
            return this.slot;
        }
        this.slot.setItemStack(null);
        return this.slot;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

