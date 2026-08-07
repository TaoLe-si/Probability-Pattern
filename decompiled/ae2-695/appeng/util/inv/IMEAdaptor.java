/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.InventoryAdaptor;
import appeng.util.IterationCounter;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.IMEAdaptorIterator;
import appeng.util.inv.ItemSlot;
import appeng.util.item.AEItemStack;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import net.minecraft.item.ItemStack;

public class IMEAdaptor
extends InventoryAdaptor {
    private final IMEInventory<IAEItemStack> target;
    private final BaseActionSource src;
    private int maxSlots = 0;

    public IMEAdaptor(IMEInventory<IAEItemStack> input, BaseActionSource src) {
        this.target = input;
        this.src = src;
    }

    @Override
    public Iterator<ItemSlot> iterator() {
        return new IMEAdaptorIterator(this, this.getList());
    }

    private IItemList<IAEItemStack> getList() {
        return this.target.getAvailableItems(AEApi.instance().storage().createItemList(), IterationCounter.fetchNewId());
    }

    public IItemList getAvailableItems(IItemList out, int iteration) {
        return this.target.getAvailableItems(out, iteration);
    }

    @Override
    public ItemStack removeItems(int amount, ItemStack filter, IInventoryDestination destination) {
        return this.doRemoveItems(amount, filter, destination, Actionable.MODULATE);
    }

    private ItemStack doRemoveItems(int amount, ItemStack filter, IInventoryDestination destination, Actionable type) {
        IAEItemStack req = null;
        if (filter == null) {
            IItemList<IAEItemStack> list = this.getList();
            if (!list.isEmpty()) {
                req = list.getFirstItem();
            }
        } else {
            req = AEItemStack.create(filter);
        }
        IAEItemStack out = null;
        if (req != null) {
            req.setStackSize(amount);
            out = this.target.extractItems(req, type, this.src);
        }
        if (out != null) {
            return out.getItemStack();
        }
        return null;
    }

    @Override
    public ItemStack simulateRemove(int amount, ItemStack filter, IInventoryDestination destination) {
        return this.doRemoveItems(amount, filter, destination, Actionable.SIMULATE);
    }

    @Override
    public ItemStack removeSimilarItems(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        if (filter == null) {
            return this.doRemoveItems(amount, null, destination, Actionable.MODULATE);
        }
        return this.doRemoveItemsFuzzy(amount, filter, destination, Actionable.MODULATE, fuzzyMode);
    }

    private ItemStack doRemoveItemsFuzzy(int amount, ItemStack filter, IInventoryDestination destination, Actionable type, FuzzyMode fuzzyMode) {
        AEItemStack reqFilter = AEItemStack.create(filter);
        if (reqFilter == null) {
            return null;
        }
        IAEItemStack out = null;
        for (IAEItemStack req : ImmutableList.copyOf(this.getList().findFuzzy(reqFilter, fuzzyMode))) {
            if (req == null) continue;
            req.setStackSize(amount);
            out = this.target.extractItems(req, type, this.src);
            if (out == null) continue;
            return out.getItemStack();
        }
        return null;
    }

    @Override
    public ItemStack simulateSimilarRemove(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        if (filter == null) {
            return this.doRemoveItems(amount, null, destination, Actionable.SIMULATE);
        }
        return this.doRemoveItemsFuzzy(amount, filter, destination, Actionable.SIMULATE, fuzzyMode);
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded) {
        IAEItemStack out;
        AEItemStack in = AEItemStack.create(toBeAdded);
        if (in != null && (out = this.target.injectItems(in, Actionable.MODULATE, this.src)) != null) {
            return out.getItemStack();
        }
        return null;
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated) {
        IAEItemStack out;
        AEItemStack in = AEItemStack.create(toBeSimulated);
        if (in != null && (out = this.target.injectItems(in, Actionable.SIMULATE, this.src)) != null) {
            return out.getItemStack();
        }
        return null;
    }

    @Override
    public boolean containsItems() {
        return !this.getList().isEmpty();
    }

    int getMaxSlots() {
        return this.maxSlots;
    }

    void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }
}

