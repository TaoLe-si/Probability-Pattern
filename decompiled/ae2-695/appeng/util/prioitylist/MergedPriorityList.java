/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.prioitylist;

import appeng.api.storage.data.IAEStack;
import appeng.util.prioitylist.IPartitionList;
import java.util.ArrayList;
import java.util.Collection;

public final class MergedPriorityList<T extends IAEStack<T>>
implements IPartitionList<T> {
    private final Collection<IPartitionList<T>> positive = new ArrayList<IPartitionList<T>>();
    private final Collection<IPartitionList<T>> negative = new ArrayList<IPartitionList<T>>();

    public void addNewList(IPartitionList<T> list, boolean isWhitelist) {
        if (isWhitelist) {
            this.positive.add(list);
        } else {
            this.negative.add(list);
        }
    }

    @Override
    public boolean isListed(T input) {
        for (IPartitionList<T> l : this.negative) {
            if (!l.isListed(input)) continue;
            return false;
        }
        if (!this.positive.isEmpty()) {
            for (IPartitionList<T> l : this.positive) {
                if (!l.isListed(input)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return this.positive.isEmpty() && this.negative.isEmpty();
    }

    @Override
    public Iterable<T> getItems() {
        throw new UnsupportedOperationException();
    }
}

