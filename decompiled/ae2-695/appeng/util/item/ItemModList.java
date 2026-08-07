/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.item;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemContainer;
import java.util.Collection;

public class ItemModList
implements IItemContainer<IAEItemStack> {
    private final IItemContainer<IAEItemStack> backingStore;
    private final IItemContainer<IAEItemStack> overrides = AEApi.instance().storage().createItemList();

    public ItemModList(IItemContainer<IAEItemStack> backend) {
        this.backingStore = backend;
    }

    @Override
    public void add(IAEItemStack option) {
        IAEItemStack over = this.overrides.findPrecise(option);
        if (over == null) {
            over = this.backingStore.findPrecise(option);
            if (over == null) {
                this.overrides.add(option);
            } else {
                option.add(over);
                this.overrides.add(option);
            }
        } else {
            this.overrides.add(option);
        }
    }

    @Override
    public IAEItemStack findPrecise(IAEItemStack i) {
        IAEItemStack over = this.overrides.findPrecise(i);
        if (over == null) {
            return this.backingStore.findPrecise(i);
        }
        return over;
    }

    @Override
    public Collection<IAEItemStack> findFuzzy(IAEItemStack input, FuzzyMode fuzzy) {
        return this.overrides.findFuzzy(input, fuzzy);
    }

    @Override
    public boolean isEmpty() {
        return this.overrides.isEmpty() && this.backingStore.isEmpty();
    }
}

