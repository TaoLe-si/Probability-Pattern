/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.inv;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import java.util.Collection;
import java.util.Iterator;

public class ItemListIgnoreCrafting<T extends IAEStack>
implements IItemList<T> {
    private final IItemList<T> target;

    public ItemListIgnoreCrafting(IItemList<T> cla) {
        this.target = cla;
    }

    @Override
    public void add(T option) {
        if (option != null && option.isCraftable()) {
            option = option.copy();
            option.setCraftable(false);
        }
        this.target.add(option);
    }

    @Override
    public T findPrecise(T i) {
        return this.target.findPrecise(i);
    }

    @Override
    public Collection<T> findFuzzy(T input, FuzzyMode fuzzy) {
        return this.target.findFuzzy(input, fuzzy);
    }

    @Override
    public boolean isEmpty() {
        return this.target.isEmpty();
    }

    @Override
    public void addStorage(T option) {
        this.target.addStorage(option);
    }

    @Override
    public void addCrafting(T option) {
    }

    @Override
    public void addRequestable(T option) {
        this.target.addRequestable(option);
    }

    @Override
    public T getFirstItem() {
        return this.target.getFirstItem();
    }

    @Override
    public int size() {
        return this.target.size();
    }

    @Override
    public Iterator<T> iterator() {
        return this.target.iterator();
    }

    @Override
    public void resetStatus() {
        this.target.resetStatus();
    }
}

