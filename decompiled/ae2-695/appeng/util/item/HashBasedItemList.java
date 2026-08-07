/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.MeaningfulItemIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Iterator;

public final class HashBasedItemList
implements IItemList<IAEItemStack> {
    private final ObjectOpenHashSet<IAEItemStack> records = new ObjectOpenHashSet();

    @Override
    public void add(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st != null) {
            st.add(option);
            return;
        }
        IAEItemStack opt = option.copy();
        this.putItemRecord(opt);
    }

    @Override
    public IAEItemStack findPrecise(IAEItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return (IAEItemStack)this.records.get((Object)itemStack);
    }

    @Override
    public Collection<IAEItemStack> findFuzzy(IAEItemStack filter, FuzzyMode fuzzy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return !this.iterator().hasNext();
    }

    @Override
    public void addStorage(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st != null) {
            st.incStackSize(option.getStackSize());
            return;
        }
        IAEItemStack opt = option.copy();
        this.putItemRecord(opt);
    }

    @Override
    public void addCrafting(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st != null) {
            st.setCraftable(true);
            return;
        }
        IAEItemStack opt = option.copy();
        opt.setStackSize(0L);
        opt.setCraftable(true);
        this.putItemRecord(opt);
    }

    @Override
    public void addRequestable(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st != null) {
            st.setCountRequestable(st.getCountRequestable() + option.getCountRequestable());
            return;
        }
        IAEItemStack opt = option.copy();
        opt.setStackSize(0L);
        opt.setCraftable(false);
        opt.setCountRequestable(option.getCountRequestable());
        this.putItemRecord(opt);
    }

    @Override
    public IAEItemStack getFirstItem() {
        Iterator<IAEItemStack> iterator = this.iterator();
        if (iterator.hasNext()) {
            IAEItemStack stackType = iterator.next();
            return stackType;
        }
        return null;
    }

    @Override
    public int size() {
        return this.records.size();
    }

    @Override
    public Iterator<IAEItemStack> iterator() {
        return new MeaningfulItemIterator<IAEItemStack>((Iterator<IAEItemStack>)this.records.iterator());
    }

    @Override
    public void resetStatus() {
        for (IAEItemStack i : this) {
            i.reset();
        }
    }

    private void putItemRecord(IAEItemStack itemStack) {
        this.records.add((Object)itemStack);
    }
}

