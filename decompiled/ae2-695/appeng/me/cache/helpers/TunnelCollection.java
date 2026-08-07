/*
 * Decompiled with CFR 0.152.
 */
package appeng.me.cache.helpers;

import appeng.me.cache.helpers.TunnelIterator;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.util.iterators.NullIterator;
import java.util.Collection;
import java.util.Iterator;

public class TunnelCollection<T extends PartP2PTunnel>
implements Iterable<T> {
    private final Class clz;
    private Collection<T> tunnelSources;

    public TunnelCollection(Collection<T> src, Class c) {
        this.tunnelSources = src;
        this.clz = c;
    }

    public void setSource(Collection<T> c) {
        this.tunnelSources = c;
    }

    public boolean isEmpty() {
        return !this.iterator().hasNext();
    }

    @Override
    public Iterator<T> iterator() {
        if (this.tunnelSources == null) {
            return new NullIterator();
        }
        return new TunnelIterator<T>(this.tunnelSources, this.clz);
    }

    public boolean matches(Class<? extends PartP2PTunnel> c) {
        return this.clz == c;
    }

    public Class<? extends PartP2PTunnel> getClz() {
        return this.clz;
    }
}

