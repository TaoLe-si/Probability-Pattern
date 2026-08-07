/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.util.iterators;

import appeng.util.inv.ItemSlot;
import java.util.Iterator;
import net.minecraft.item.ItemStack;

public class StackToSlotIterator
implements Iterator<ItemSlot> {
    private final ItemSlot iss = new ItemSlot();
    private final Iterator<ItemStack> is;
    private int x = 0;

    public StackToSlotIterator(Iterator<ItemStack> is) {
        this.is = is;
    }

    @Override
    public boolean hasNext() {
        return this.is.hasNext();
    }

    @Override
    public ItemSlot next() {
        this.iss.setSlot(this.x);
        ++this.x;
        this.iss.setItemStack(this.is.next());
        return this.iss;
    }

    @Override
    public void remove() {
    }
}

