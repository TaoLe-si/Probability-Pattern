/*
 * Decompiled with CFR 0.152.
 */
package appeng.me.cache.helpers;

import appeng.parts.p2p.PartP2PTunnel;
import java.util.Collection;
import java.util.Iterator;

public class TunnelIterator<T extends PartP2PTunnel>
implements Iterator<T> {
    private final Iterator<T> wrapped;
    private final Class targetType;
    private T Next;

    public TunnelIterator(Collection<T> tunnelSources, Class clz) {
        this.wrapped = tunnelSources.iterator();
        this.targetType = clz;
        this.findNext();
    }

    private void findNext() {
        while (this.Next == null && this.wrapped.hasNext()) {
            this.Next = (PartP2PTunnel)this.wrapped.next();
            if (this.targetType.isInstance(this.Next)) continue;
            this.Next = null;
        }
    }

    @Override
    public boolean hasNext() {
        this.findNext();
        return this.Next != null;
    }

    @Override
    public T next() {
        T tmp = this.Next;
        this.Next = null;
        return tmp;
    }

    @Override
    public void remove() {
    }
}

