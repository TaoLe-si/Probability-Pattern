/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.prioitylist;

import appeng.api.storage.data.IAEStack;
import appeng.util.prioitylist.IPartitionList;
import java.util.ArrayList;
import java.util.List;

public class DefaultPriorityList<T extends IAEStack<T>>
implements IPartitionList<T> {
    private static final List NULL_LIST = new ArrayList();

    @Override
    public boolean isListed(T input) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Iterable<T> getItems() {
        return NULL_LIST;
    }
}

