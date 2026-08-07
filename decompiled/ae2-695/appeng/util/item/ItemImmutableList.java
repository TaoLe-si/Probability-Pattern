/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public final class ItemImmutableList
implements IItemList<IAEItemStack> {
    private final IItemList<IAEItemStack>[] lists;

    @SafeVarargs
    public ItemImmutableList(IItemList<IAEItemStack> ... itemLists) {
        if (itemLists == null || itemLists.length == 0) {
            throw new IllegalArgumentException("ItemImmutableList must be initialized with at least one list");
        }
        this.lists = Arrays.copyOf(itemLists, itemLists.length);
    }

    @Override
    public void addStorage(IAEItemStack option) {
        throw new UnsupportedOperationException("Cannot add to immutable list");
    }

    @Override
    public void addCrafting(IAEItemStack option) {
        throw new UnsupportedOperationException("Cannot add to immutable list");
    }

    @Override
    public void addRequestable(IAEItemStack option) {
        throw new UnsupportedOperationException("Cannot add to immutable list");
    }

    @Override
    public IAEItemStack getFirstItem() {
        for (IItemList<IAEItemStack> list : this.lists) {
            if (list.isEmpty()) continue;
            return list.getFirstItem();
        }
        return null;
    }

    @Override
    public int size() {
        int totalSize = 0;
        for (IItemList<IAEItemStack> list : this.lists) {
            totalSize += list.size();
        }
        return totalSize;
    }

    @Override
    public Iterator<IAEItemStack> iterator() {
        return new Iterator<IAEItemStack>(){
            private int currentListIndex = 0;
            private Iterator<IAEItemStack> currentIterator = ItemImmutableList.access$000(ItemImmutableList.this)[this.currentListIndex].iterator();

            @Override
            public boolean hasNext() {
                if (this.currentIterator.hasNext()) {
                    return true;
                }
                if (this.currentListIndex < ItemImmutableList.this.lists.length - 1) {
                    ++this.currentListIndex;
                    this.currentIterator = ItemImmutableList.this.lists[this.currentListIndex].iterator();
                    return this.hasNext();
                }
                return false;
            }

            @Override
            public IAEItemStack next() {
                return this.currentIterator.next();
            }
        };
    }

    @Override
    public void resetStatus() {
        throw new UnsupportedOperationException("Cannot reset immutable list");
    }

    @Override
    public void add(IAEItemStack option) {
        throw new UnsupportedOperationException("Cannot add to immutable list");
    }

    @Override
    public IAEItemStack findPrecise(IAEItemStack i) {
        IAEItemStack result = null;
        for (IItemList<IAEItemStack> list : this.lists) {
            IAEItemStack found = list.findPrecise(i);
            if (found == null) continue;
            if (result == null) {
                result = found.copy();
                continue;
            }
            result.setStackSize(result.getStackSize() + found.getStackSize());
        }
        return result;
    }

    @Override
    public Collection<IAEItemStack> findFuzzy(IAEItemStack input, FuzzyMode fuzzy) {
        ArrayList<IAEItemStack> found = new ArrayList<IAEItemStack>(this.lists[0].findFuzzy(input, fuzzy));
        for (int i = 1; i < this.lists.length; ++i) {
            IItemList<IAEItemStack> itemsList = this.lists[i];
            block1: for (IAEItemStack item : itemsList.findFuzzy(input, fuzzy)) {
                for (IAEItemStack existing : found) {
                    if (!existing.isSameType(item)) continue;
                    existing.setStackSize(existing.getStackSize() + item.getStackSize());
                    continue block1;
                }
                found.add(item.copy());
            }
        }
        return found;
    }

    @Override
    public boolean isEmpty() {
        for (IItemList<IAEItemStack> itemsList : this.lists) {
            if (itemsList.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean hasWriteAccess() {
        return false;
    }
}

