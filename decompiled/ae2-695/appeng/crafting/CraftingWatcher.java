/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package appeng.crafting;

import appeng.api.networking.crafting.ICraftingWatcher;
import appeng.api.networking.crafting.ICraftingWatcherHost;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.CraftingGridCache;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import javax.annotation.Nonnull;

public class CraftingWatcher
implements ICraftingWatcher {
    private final CraftingGridCache gsc;
    private final ICraftingWatcherHost host;
    private final HashSet<IAEStack> myInterests = new HashSet();

    public CraftingWatcher(CraftingGridCache cache, ICraftingWatcherHost host) {
        this.gsc = cache;
        this.host = host;
    }

    public ICraftingWatcherHost getHost() {
        return this.host;
    }

    @Override
    public int size() {
        return this.myInterests.size();
    }

    @Override
    public boolean isEmpty() {
        return this.myInterests.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.myInterests.contains(o);
    }

    @Override
    @Nonnull
    public Iterator<IAEStack> iterator() {
        return new ItemWatcherIterator(this, this.myInterests.iterator());
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        return this.myInterests.toArray();
    }

    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] a) {
        return this.myInterests.toArray(a);
    }

    @Override
    public boolean add(IAEStack e) {
        if (this.myInterests.contains(e)) {
            return false;
        }
        return this.myInterests.add((IAEStack)e.copy()) && this.gsc.getInterestManager().put(e, this);
    }

    @Override
    public boolean remove(Object o) {
        return this.myInterests.remove(o) && this.gsc.getInterestManager().remove((IAEStack)o, this);
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return this.myInterests.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends IAEStack> c) {
        boolean didChange = false;
        for (IAEStack iAEStack : c) {
            didChange = this.add(iAEStack) || didChange;
        }
        return didChange;
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        boolean didSomething = false;
        for (Object o : c) {
            didSomething = this.remove(o) || didSomething;
        }
        return didSomething;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        boolean changed = false;
        Iterator<IAEStack> i = this.iterator();
        while (i.hasNext()) {
            if (c.contains(i.next())) continue;
            i.remove();
            changed = true;
        }
        return changed;
    }

    @Override
    public void clear() {
        Iterator<IAEStack> i = this.myInterests.iterator();
        while (i.hasNext()) {
            this.gsc.getInterestManager().remove(i.next(), this);
            i.remove();
        }
    }

    private class ItemWatcherIterator
    implements Iterator<IAEStack> {
        private final CraftingWatcher watcher;
        private final Iterator<IAEStack> interestIterator;
        private IAEStack myLast;

        public ItemWatcherIterator(CraftingWatcher parent, Iterator<IAEStack> i) {
            this.watcher = parent;
            this.interestIterator = i;
        }

        @Override
        public boolean hasNext() {
            return this.interestIterator.hasNext();
        }

        @Override
        public IAEStack next() {
            this.myLast = this.interestIterator.next();
            return this.myLast;
        }

        @Override
        public void remove() {
            CraftingWatcher.this.gsc.getInterestManager().remove(this.myLast, this.watcher);
            this.interestIterator.remove();
        }
    }
}

