/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  javax.annotation.Nonnegative
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package appeng.me.cache;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IMENetworkInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cache.GridStorageCache;
import appeng.me.storage.ItemWatcher;
import appeng.util.IterationCounter;
import appeng.util.item.LazyItemList;
import appeng.util.item.NetworkItemList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkMonitor<T extends IAEStack<T>>
implements IMEMonitor<T> {
    @Nonnull
    private static final Deque<NetworkMonitor<?>> GLOBAL_DEPTH = Lists.newLinkedList();
    @Nonnull
    private final GridStorageCache myGridCache;
    @Nonnull
    private final StorageChannel myChannel;
    @Nonnull
    private final IItemList<T> cachedList;
    @Nonnull
    private final Map<IMEMonitorHandlerReceiver<T>, Object> listeners;
    private boolean sendEvent = false;
    private boolean hasChanged = false;
    @Nonnegative
    private int localDepthSemaphore = 0;

    public NetworkMonitor(GridStorageCache cache, StorageChannel chan) {
        this.myGridCache = cache;
        this.myChannel = chan;
        this.cachedList = chan.createList();
        this.listeners = new HashMap<IMEMonitorHandlerReceiver<T>, Object>();
    }

    @Override
    public void addListener(IMEMonitorHandlerReceiver<T> l, Object verificationToken) {
        this.listeners.put(l, verificationToken);
    }

    @Override
    public boolean canAccept(T input) {
        return this.getHandler().canAccept(input);
    }

    @Override
    public T extractItems(T request, Actionable mode, BaseActionSource src) {
        if (mode == Actionable.SIMULATE) {
            return this.getHandler().extractItems(request, mode, src);
        }
        ++this.localDepthSemaphore;
        T leftover = this.getHandler().extractItems(request, mode, src);
        --this.localDepthSemaphore;
        if (this.localDepthSemaphore == 0) {
            this.monitorDifference((IAEStack)request.copy(), leftover, true, src);
        }
        return leftover;
    }

    @Override
    public AccessRestriction getAccess() {
        return this.getHandler().getAccess();
    }

    @Override
    public IItemList<T> getAvailableItems(IItemList out, int iteration) {
        return this.getHandler().getAvailableItems(out, iteration);
    }

    @Override
    public StorageChannel getChannel() {
        return this.getHandler().getChannel();
    }

    @Override
    public int getPriority() {
        return this.getHandler().getPriority();
    }

    @Override
    public int getSlot() {
        return this.getHandler().getSlot();
    }

    @Override
    @Nonnull
    public IItemList<T> getStorageList() {
        if (this.hasChanged) {
            this.hasChanged = false;
            this.cachedList.resetStatus();
            IItemList<T> ret = this.getAvailableItems((IItemList)this.cachedList, IterationCounter.fetchNewId());
            if (ret instanceof NetworkItemList) {
                for (IAEStack item : ret) {
                    this.cachedList.add(item);
                }
            }
            return this.cachedList;
        }
        return this.cachedList;
    }

    @Override
    public T injectItems(T input, Actionable mode, BaseActionSource src) {
        if (mode == Actionable.SIMULATE) {
            return this.getHandler().injectItems(input, mode, src);
        }
        ++this.localDepthSemaphore;
        T leftover = this.getHandler().injectItems(input, mode, src);
        --this.localDepthSemaphore;
        if (this.localDepthSemaphore == 0) {
            this.monitorDifference((IAEStack)input.copy(), leftover, false, src);
        }
        return leftover;
    }

    @Override
    public boolean isPrioritized(T input) {
        return this.getHandler().isPrioritized(input);
    }

    @Override
    public void removeListener(IMEMonitorHandlerReceiver<T> l) {
        this.listeners.remove(l);
    }

    @Override
    public boolean validForPass(int i) {
        return this.getHandler().validForPass(i);
    }

    @Override
    public IMENetworkInventory<T> getExternalNetworkInventory() {
        IMEInventoryHandler<T> handler = this.getHandler();
        if (handler instanceof IMENetworkInventory) {
            IMENetworkInventory networkInventory = (IMENetworkInventory)handler;
            return networkInventory;
        }
        return handler.getExternalNetworkInventory();
    }

    @Nullable
    public IMEInventoryHandler<T> getHandler() {
        switch (this.myChannel) {
            case ITEMS: {
                return this.myGridCache.getItemInventoryHandler();
            }
            case FLUIDS: {
                return this.myGridCache.getFluidInventoryHandler();
            }
        }
        return null;
    }

    public IGrid getGrid() {
        return this.myGridCache.getGrid();
    }

    private Iterator<Map.Entry<IMEMonitorHandlerReceiver<T>, Object>> getListeners() {
        return this.listeners.entrySet().iterator();
    }

    private T monitorDifference(IAEStack original, T leftOvers, boolean extraction, BaseActionSource src) {
        Object diff = original.copy();
        if (extraction) {
            diff.setStackSize(leftOvers == null ? 0L : -leftOvers.getStackSize());
        } else if (leftOvers != null) {
            diff.decStackSize(leftOvers.getStackSize());
        }
        if (diff.getStackSize() != 0L) {
            this.postChangesToListeners((Iterable<T>)ImmutableList.of(diff), src);
        }
        return leftOvers;
    }

    private void notifyListenersOfChange(Iterable<T> diff, BaseActionSource src) {
        this.hasChanged = true;
        Iterator<Map.Entry<IMEMonitorHandlerReceiver<T>, Object>> i = this.getListeners();
        while (i.hasNext()) {
            Map.Entry<IMEMonitorHandlerReceiver<T>, Object> o = i.next();
            IMEMonitorHandlerReceiver<T> receiver = o.getKey();
            if (receiver.isValid(o.getValue())) {
                receiver.postChange(this, diff, src);
                continue;
            }
            i.remove();
        }
    }

    private void postChangesToListeners(Iterable<T> changes, BaseActionSource src) {
        this.postChange(true, changes, src);
    }

    protected void postChange(boolean add, Iterable<T> changes, BaseActionSource src) {
        if (this.localDepthSemaphore > 0 || GLOBAL_DEPTH.contains(this)) {
            return;
        }
        GLOBAL_DEPTH.push(this);
        ++this.localDepthSemaphore;
        this.sendEvent = true;
        this.notifyListenersOfChange(changes, src);
        for (IAEStack changedItem : changes) {
            Collection<ItemWatcher> list;
            if (changedItem == null) continue;
            IAEStack difference = changedItem;
            if (!add) {
                difference = changedItem.copy();
                difference.setStackSize(-changedItem.getStackSize());
            }
            if (!this.myGridCache.getInterestManager().containsKey(changedItem) || (list = this.myGridCache.getInterestManager().get(changedItem)).isEmpty()) continue;
            IAEStack fullStack = this.getHandler().getAvailableItem(changedItem, IterationCounter.fetchNewId());
            if (fullStack == null) {
                fullStack = changedItem.copy();
                fullStack.setStackSize(0L);
            }
            this.myGridCache.getInterestManager().enableTransactions();
            LazyItemList itemList = new LazyItemList(this::getStorageList);
            for (ItemWatcher iw : list) {
                iw.getHost().onStackChange(itemList, fullStack, difference, src, this.getChannel());
            }
            this.myGridCache.getInterestManager().disableTransactions();
        }
        NetworkMonitor<?> last = GLOBAL_DEPTH.pop();
        --this.localDepthSemaphore;
        if (last != this) {
            throw new IllegalStateException("Invalid Access to Networked Storage API detected.");
        }
    }

    void forceUpdate() {
        this.hasChanged = true;
        Iterator<Map.Entry<IMEMonitorHandlerReceiver<T>, Object>> i = this.getListeners();
        while (i.hasNext()) {
            Map.Entry<IMEMonitorHandlerReceiver<T>, Object> o = i.next();
            IMEMonitorHandlerReceiver<T> receiver = o.getKey();
            if (receiver.isValid(o.getValue())) {
                receiver.onListUpdate();
                continue;
            }
            i.remove();
        }
    }

    void onTick() {
        if (this.sendEvent) {
            this.sendEvent = false;
            this.myGridCache.getGrid().postEvent(new MENetworkStorageEvent(this, this.myChannel));
        }
    }
}

