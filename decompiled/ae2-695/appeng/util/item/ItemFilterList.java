/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  javax.annotation.Nonnull
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nonnull;

public class ItemFilterList
implements IItemList<IAEItemStack> {
    private final ObjectOpenHashSet<IAEItemStack> records = new ObjectOpenHashSet();

    @Override
    public void add(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st == null) {
            this.putItemRecord(option.copy());
        }
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
        if (st == null) {
            this.putItemRecord(option.copy());
        }
    }

    @Override
    public void addCrafting(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st == null) {
            this.putItemRecord(option.copy());
        }
    }

    @Override
    public void addRequestable(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.records.get((Object)option);
        if (st == null) {
            this.putItemRecord(option.copy());
        }
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
    @Nonnull
    public Iterator<IAEItemStack> iterator() {
        return this.records.iterator();
    }

    @Override
    public void resetStatus() {
        for (IAEItemStack i : this) {
            i.reset();
        }
    }

    public void clear() {
        this.records.clear();
    }

    private void putItemRecord(IAEItemStack itemStack) {
        ((IAEItemStack)((IAEItemStack)itemStack.setStackSize(1L)).setCraftable(false)).setCountRequestable(0L);
        this.records.add((Object)itemStack);
    }
}

