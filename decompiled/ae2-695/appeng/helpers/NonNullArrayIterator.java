/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  scala.NotImplementedError
 */
package appeng.helpers;

import java.util.Iterator;
import scala.NotImplementedError;

public class NonNullArrayIterator<E>
implements Iterator<E> {
    private final E[] g;
    private int offset = 0;

    public NonNullArrayIterator(E[] o) {
        this.g = o;
    }

    @Override
    public boolean hasNext() {
        while (this.offset < this.g.length && this.g[this.offset] == null) {
            ++this.offset;
        }
        return this.offset != this.g.length;
    }

    @Override
    public E next() {
        E result = this.g[this.offset];
        ++this.offset;
        return result;
    }

    @Override
    public void remove() {
        throw new NotImplementedError();
    }
}

