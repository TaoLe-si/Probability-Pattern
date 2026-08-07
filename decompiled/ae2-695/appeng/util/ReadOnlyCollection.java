/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import appeng.api.util.IReadOnlyCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class ReadOnlyCollection<T>
implements IReadOnlyCollection<T> {
    private final Collection<T> c;

    public ReadOnlyCollection(Collection<? extends T> in) {
        this.c = in;
    }

    @SafeVarargs
    public ReadOnlyCollection(T ... in) {
        this.c = Arrays.asList(in);
    }

    @Override
    public Iterator<T> iterator() {
        return this.c.iterator();
    }

    @Override
    public int size() {
        return this.c.size();
    }

    @Override
    public boolean isEmpty() {
        return this.c.isEmpty();
    }

    @Override
    public boolean contains(Object node) {
        return this.c.contains(node);
    }
}

