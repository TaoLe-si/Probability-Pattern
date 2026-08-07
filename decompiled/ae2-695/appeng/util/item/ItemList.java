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
import appeng.util.item.AEItemStack;
import appeng.util.item.MeaningfulItemIterator;
import appeng.util.item.OreReference;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public final class ItemList
implements IItemList<IAEItemStack> {
    private final NavigableMap<IAEItemStack, IAEItemStack> records = new ConcurrentSkipListMap<IAEItemStack, IAEItemStack>();
    private final ObjectOpenHashSet<IAEItemStack> setRecords = new ObjectOpenHashSet();

    @Override
    public void add(IAEItemStack option) {
        if (option == null) {
            return;
        }
        IAEItemStack st = (IAEItemStack)this.setRecords.get((Object)option);
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
        return (IAEItemStack)this.setRecords.get((Object)itemStack);
    }

    @Override
    public Collection<IAEItemStack> findFuzzy(IAEItemStack filter, FuzzyMode fuzzy) {
        if (filter == null) {
            return Collections.emptyList();
        }
        AEItemStack ais = (AEItemStack)filter;
        if (ais.isOre()) {
            OreReference or = ais.getDefinition().getIsOre();
            if (or.getAEEquivalents().size() == 1) {
                IAEItemStack is = or.getAEEquivalents().get(0);
                return this.findFuzzyDamage((AEItemStack)is, fuzzy, is.getItemDamage() == Short.MAX_VALUE);
            }
            LinkedList<IAEItemStack> output = new LinkedList<IAEItemStack>();
            for (IAEItemStack is : or.getAEEquivalents()) {
                output.addAll(this.findFuzzyDamage((AEItemStack)is, fuzzy, is.getItemDamage() == Short.MAX_VALUE));
            }
            return output;
        }
        return this.findFuzzyDamage(ais, fuzzy, false);
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
        IAEItemStack st = (IAEItemStack)this.setRecords.get((Object)option);
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
        IAEItemStack st = (IAEItemStack)this.setRecords.get((Object)option);
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
        IAEItemStack st = (IAEItemStack)this.setRecords.get((Object)option);
        if (st != null) {
            st.setCountRequestable(st.getCountRequestable() + option.getCountRequestable());
            st.setCountRequestableCrafts(st.getCountRequestableCrafts() + option.getCountRequestableCrafts());
            return;
        }
        IAEItemStack opt = option.copy();
        opt.setStackSize(0L);
        opt.setCraftable(false);
        opt.setCountRequestable(option.getCountRequestable());
        opt.setCountRequestableCrafts(option.getCountRequestableCrafts());
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
        return this.setRecords.size();
    }

    @Override
    public Iterator<IAEItemStack> iterator() {
        return new MeaningfulItemIterator<IAEItemStack>(new Iterator<IAEItemStack>(){
            private final Iterator<IAEItemStack> i;
            private IAEItemStack next;
            {
                this.i = ItemList.this.records.values().iterator();
                this.next = null;
            }

            @Override
            public boolean hasNext() {
                return this.i.hasNext();
            }

            @Override
            public IAEItemStack next() {
                this.next = this.i.next();
                return this.next;
            }

            @Override
            public void remove() {
                this.i.remove();
                ItemList.this.setRecords.remove((Object)this.next);
            }
        });
    }

    @Override
    public void resetStatus() {
        for (IAEItemStack i : this) {
            i.reset();
        }
    }

    public void clear() {
        this.setRecords.clear();
        this.records.clear();
    }

    private void putItemRecord(IAEItemStack itemStack) {
        this.setRecords.add((Object)itemStack);
        this.records.put(itemStack, itemStack);
    }

    private Collection<IAEItemStack> findFuzzyDamage(AEItemStack filter, FuzzyMode fuzzy, boolean ignoreMeta) {
        IAEItemStack low = filter.getLow(fuzzy, ignoreMeta);
        IAEItemStack high = filter.getHigh(fuzzy, ignoreMeta);
        return this.records.subMap(low, true, high, true).descendingMap().values();
    }
}

