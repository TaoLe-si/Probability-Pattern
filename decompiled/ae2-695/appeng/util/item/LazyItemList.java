/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LazyItemList<StackType extends IAEStack<StackType>>
implements IItemList<StackType> {
    private volatile IItemList<StackType> cache = null;
    private final Supplier<IItemList<StackType>> computeFn;

    public LazyItemList(Supplier<IItemList<StackType>> computeFn) {
        this.computeFn = computeFn;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private IItemList<StackType> getCachedOrCompute() {
        if (this.cache == null) {
            LazyItemList lazyItemList = this;
            synchronized (lazyItemList) {
                if (this.cache == null) {
                    this.cache = Objects.requireNonNull(this.computeFn.get());
                }
            }
        }
        return this.cache;
    }

    @Override
    public void add(StackType option) {
        this.getCachedOrCompute().add(option);
    }

    @Override
    public StackType findPrecise(StackType i) {
        return this.getCachedOrCompute().findPrecise(i);
    }

    @Override
    public Collection<StackType> findFuzzy(StackType input, FuzzyMode fuzzy) {
        return this.getCachedOrCompute().findFuzzy(input, fuzzy);
    }

    @Override
    public boolean isEmpty() {
        return this.getCachedOrCompute().isEmpty();
    }

    @Override
    public void addStorage(StackType option) {
        this.getCachedOrCompute().addStorage(option);
    }

    @Override
    public void addCrafting(StackType option) {
        this.getCachedOrCompute().addCrafting(option);
    }

    @Override
    public void addRequestable(StackType option) {
        this.getCachedOrCompute().addRequestable(option);
    }

    @Override
    public StackType getFirstItem() {
        return this.getCachedOrCompute().getFirstItem();
    }

    @Override
    public int size() {
        return this.getCachedOrCompute().size();
    }

    @Override
    public Iterator<StackType> iterator() {
        return this.getCachedOrCompute().iterator();
    }

    @Override
    public void resetStatus() {
        this.getCachedOrCompute().resetStatus();
    }

    @Override
    public StackType[] toArray(StackType[] zeroSizedArray) {
        return this.getCachedOrCompute().toArray((IAEStack[])zeroSizedArray);
    }

    @Override
    public void forEach(Consumer<? super StackType> action) {
        this.getCachedOrCompute().forEach(action);
    }

    @Override
    public Spliterator<StackType> spliterator() {
        return this.getCachedOrCompute().spliterator();
    }
}

