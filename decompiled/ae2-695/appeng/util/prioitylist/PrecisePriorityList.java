/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.prioitylist;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.util.prioitylist.IPartitionList;

public class PrecisePriorityList<T extends IAEStack<T>>
implements IPartitionList<T> {
    private final IItemList<T> list;

    public PrecisePriorityList(IItemList<T> in) {
        this.list = in;
    }

    @Override
    public boolean isListed(T input) {
        return this.list.findPrecise(input) != null;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public Iterable<T> getItems() {
        return this.list;
    }
}

