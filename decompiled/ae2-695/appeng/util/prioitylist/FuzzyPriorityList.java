/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.prioitylist;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.util.prioitylist.IPartitionList;
import java.util.Collection;

public class FuzzyPriorityList<T extends IAEStack<T>>
implements IPartitionList<T> {
    private final IItemList<T> list;
    private final FuzzyMode mode;

    public FuzzyPriorityList(IItemList<T> in, FuzzyMode mode) {
        this.list = in;
        this.mode = mode;
    }

    @Override
    public boolean isListed(T input) {
        Collection<T> out = this.list.findFuzzy(input, this.mode);
        return out != null && !out.isEmpty();
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

