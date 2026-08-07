/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.item;

import appeng.api.storage.data.IAEItemStack;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MeaningfulItemIterator<T extends IAEItemStack>
implements Iterator<T> {
    private final Iterator<T> parent;
    private T next;

    public MeaningfulItemIterator(Iterator<T> iterator) {
        this.parent = iterator;
    }

    @Override
    public boolean hasNext() {
        while (this.parent.hasNext()) {
            this.next = (IAEItemStack)this.parent.next();
            if (this.next.isMeaningful()) {
                return true;
            }
            this.parent.remove();
        }
        this.next = null;
        return false;
    }

    @Override
    public T next() {
        if (this.next == null) {
            throw new NoSuchElementException();
        }
        return this.next;
    }

    @Override
    public void remove() {
        this.parent.remove();
    }
}

