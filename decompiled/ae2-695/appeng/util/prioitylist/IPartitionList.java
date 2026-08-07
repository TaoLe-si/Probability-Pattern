/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.prioitylist;

import appeng.api.storage.data.IAEStack;

public interface IPartitionList<T extends IAEStack<T>> {
    public boolean isListed(T var1);

    public boolean isEmpty();

    public Iterable<T> getItems();
}

