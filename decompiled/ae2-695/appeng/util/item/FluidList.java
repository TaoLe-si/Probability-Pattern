/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.MeaningfulFluidIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public final class FluidList
implements IItemList<IAEFluidStack> {
    private final ObjectOpenHashSet<IAEFluidStack> records = new ObjectOpenHashSet();

    @Override
    public void add(IAEFluidStack option) {
        if (option == null) {
            return;
        }
        IAEFluidStack st = this.getFluidRecord(option);
        if (st != null) {
            st.add(option);
            return;
        }
        IAEFluidStack opt = option.copy();
        this.putFluidRecord(opt);
    }

    @Override
    public IAEFluidStack findPrecise(IAEFluidStack fluidStack) {
        if (fluidStack == null) {
            return null;
        }
        return this.getFluidRecord(fluidStack);
    }

    @Override
    public Collection<IAEFluidStack> findFuzzy(IAEFluidStack filter, FuzzyMode fuzzy) {
        if (filter == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(this.findPrecise(filter));
    }

    @Override
    public boolean isEmpty() {
        return !this.iterator().hasNext();
    }

    @Override
    public void addStorage(IAEFluidStack option) {
        if (option == null) {
            return;
        }
        IAEFluidStack st = this.getFluidRecord(option);
        if (st != null) {
            st.incStackSize(option.getStackSize());
            return;
        }
        IAEFluidStack opt = option.copy();
        this.putFluidRecord(opt);
    }

    @Override
    public void addCrafting(IAEFluidStack option) {
        if (option == null) {
            return;
        }
        IAEFluidStack st = this.getFluidRecord(option);
        if (st != null) {
            st.setCraftable(true);
            return;
        }
        IAEFluidStack opt = option.copy();
        opt.setStackSize(0L);
        opt.setCraftable(true);
        this.putFluidRecord(opt);
    }

    @Override
    public void addRequestable(IAEFluidStack option) {
        if (option == null) {
            return;
        }
        IAEFluidStack st = this.getFluidRecord(option);
        if (st != null) {
            st.setCountRequestable(st.getCountRequestable() + option.getCountRequestable());
            return;
        }
        IAEFluidStack opt = option.copy();
        opt.setStackSize(0L);
        opt.setCraftable(false);
        opt.setCountRequestable(option.getCountRequestable());
        this.putFluidRecord(opt);
    }

    @Override
    public IAEFluidStack getFirstItem() {
        Iterator<IAEFluidStack> iterator = this.iterator();
        if (iterator.hasNext()) {
            IAEFluidStack stackType = iterator.next();
            return stackType;
        }
        return null;
    }

    @Override
    public int size() {
        return this.records.size();
    }

    @Override
    public Iterator<IAEFluidStack> iterator() {
        return new MeaningfulFluidIterator<IAEFluidStack>((Iterator<IAEFluidStack>)this.records.iterator());
    }

    @Override
    public void resetStatus() {
        for (IAEFluidStack i : this) {
            i.reset();
        }
    }

    private IAEFluidStack getFluidRecord(IAEFluidStack fluid) {
        return (IAEFluidStack)this.records.get((Object)fluid);
    }

    private void putFluidRecord(IAEFluidStack fluid) {
        this.records.add((Object)fluid);
    }
}

