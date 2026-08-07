/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Multimap
 *  com.google.common.collect.SetMultimap
 *  com.gtnewhorizon.gtnhlib.util.map.ItemStackMap
 *  net.minecraft.item.ItemStack
 */
package appeng.me.cache;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellCacheRegistry;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.core.AEConfig;
import appeng.me.cache.NetworkMonitor;
import appeng.me.cache.SecurityCache;
import appeng.me.helpers.GenericInterestManager;
import appeng.me.storage.ItemWatcher;
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.NetworkInventoryHandler;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;
import appeng.util.IterationCounter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.item.ItemStack;

public class GridStorageCache
implements IStorageGrid {
    private final IGrid myGrid;
    private final HashSet<ICellProvider> activeCellProviders = new HashSet();
    private final HashSet<ICellProvider> inactiveCellProviders = new HashSet();
    private final SetMultimap<IAEStack, ItemWatcher> interests = HashMultimap.create();
    private final GenericInterestManager<ItemWatcher> interestManager = new GenericInterestManager<ItemWatcher>((Multimap<IAEStack, ItemWatcher>)this.interests);
    private final NetworkMonitor<IAEItemStack> itemMonitor = new NetworkMonitor(this, StorageChannel.ITEMS);
    private final NetworkMonitor<IAEFluidStack> fluidMonitor = new NetworkMonitor(this, StorageChannel.FLUIDS);
    private final HashMap<IGridNode, IStackWatcher> watchers = new HashMap();
    private NetworkInventoryHandler<IAEItemStack> myItemNetwork;
    private NetworkInventoryHandler<IAEFluidStack> myFluidNetwork;
    private double itemBytesTotal;
    private double itemBytesUsed;
    private long itemTypesTotal;
    private long itemTypesUsed;
    private long itemCellG;
    private long itemCellB;
    private long itemCellO;
    private long itemCellR;
    private long itemCellCount;
    private double fluidBytesTotal;
    private double fluidBytesUsed;
    private long fluidTypesTotal;
    private long fluidTypesUsed;
    private long fluidCellG;
    private long fluidCellB;
    private long fluidCellO;
    private long fluidCellR;
    private long fluidCellCount;
    private double essentiaBytesTotal;
    private double essentiaBytesUsed;
    private long essentiaTypesTotal;
    private long essentiaTypesUsed;
    private long essentiaCellG;
    private long essentiaCellB;
    private long essentiaCellO;
    private long essentiaCellR;
    private long essentiaCellCount;
    private int ticksCount;
    private int networkBytesUpdateFrequency;
    private final ItemStackMap<Integer> itemCells = new ItemStackMap();
    private final ItemStackMap<Integer> fluidCells = new ItemStackMap();
    private final ItemStackMap<Integer> essentiaCells = new ItemStackMap();
    private static final int CELL_GREEN = 1;
    private static final int CELL_BLUE = 2;
    private static final int CELL_ORANGE = 3;
    private static final int CELL_RED = 4;

    public GridStorageCache(IGrid g) {
        this.myGrid = g;
        this.ticksCount = this.networkBytesUpdateFrequency = (int)(AEConfig.instance.networkBytesUpdateFrequency * 20.0);
    }

    @Override
    public void onUpdateTick() {
        this.itemMonitor.onTick();
        this.fluidMonitor.onTick();
        if (this.ticksCount < this.networkBytesUpdateFrequency) {
            ++this.ticksCount;
        } else {
            this.ticksCount = 0;
            this.updateBytesInfo();
        }
    }

    @Override
    public void removeNode(IGridNode node, IGridHost machine) {
        IStackWatcher myWatcher;
        if (machine instanceof ICellContainer) {
            ICellContainer cc = (ICellContainer)machine;
            CellChangeTracker tracker = new CellChangeTracker();
            this.removeCellProvider(cc, tracker);
            this.inactiveCellProviders.remove(cc);
            this.getGrid().postEvent(new MENetworkCellArrayUpdate());
            tracker.applyChanges();
        }
        if (machine instanceof IStackWatcherHost && (myWatcher = this.watchers.get(machine)) != null) {
            myWatcher.clear();
            this.watchers.remove(machine);
        }
    }

    @Override
    public void addNode(IGridNode node, IGridHost machine) {
        if (machine instanceof ICellContainer) {
            ICellContainer cc = (ICellContainer)machine;
            this.inactiveCellProviders.add(cc);
            this.getGrid().postEvent(new MENetworkCellArrayUpdate());
            if (node.isActive()) {
                CellChangeTracker tracker = new CellChangeTracker();
                this.addCellProvider(cc, tracker);
                tracker.applyChanges();
            }
        }
        if (machine instanceof IStackWatcherHost) {
            IStackWatcherHost swh = (IStackWatcherHost)((Object)machine);
            ItemWatcher iw = new ItemWatcher(this, swh);
            this.watchers.put(node, iw);
            swh.updateWatcher(iw);
        }
    }

    @Override
    public void onSplit(IGridStorage storageB) {
    }

    @Override
    public void onJoin(IGridStorage storageB) {
    }

    @Override
    public void populateGridStorage(IGridStorage storage) {
    }

    private CellChangeTracker addCellProvider(ICellProvider cc, CellChangeTracker tracker) {
        if (this.inactiveCellProviders.contains(cc)) {
            this.inactiveCellProviders.remove(cc);
            this.activeCellProviders.add(cc);
            BaseActionSource actionSrc = new BaseActionSource();
            if (cc instanceof IActionHost) {
                actionSrc = new MachineSource((IActionHost)((Object)cc));
            }
            for (IMEInventoryHandler h : cc.getCellArray(StorageChannel.ITEMS)) {
                tracker.postChanges(StorageChannel.ITEMS, 1, h, actionSrc);
            }
            for (IMEInventoryHandler h : cc.getCellArray(StorageChannel.FLUIDS)) {
                tracker.postChanges(StorageChannel.FLUIDS, 1, h, actionSrc);
            }
        }
        return tracker;
    }

    private CellChangeTracker removeCellProvider(ICellProvider cc, CellChangeTracker tracker) {
        if (this.activeCellProviders.contains(cc)) {
            this.activeCellProviders.remove(cc);
            this.inactiveCellProviders.add(cc);
            BaseActionSource actionSrc = new BaseActionSource();
            if (cc instanceof IActionHost) {
                actionSrc = new MachineSource((IActionHost)((Object)cc));
            }
            for (IMEInventoryHandler h : cc.getCellArray(StorageChannel.ITEMS)) {
                tracker.postChanges(StorageChannel.ITEMS, -1, h, actionSrc);
            }
            for (IMEInventoryHandler h : cc.getCellArray(StorageChannel.FLUIDS)) {
                tracker.postChanges(StorageChannel.FLUIDS, -1, h, actionSrc);
            }
        }
        return tracker;
    }

    @MENetworkEventSubscribe
    public void cellUpdate(MENetworkCellArrayUpdate ev) {
        this.myItemNetwork = null;
        this.myFluidNetwork = null;
        LinkedList<ICellProvider> ll = new LinkedList<ICellProvider>();
        ll.addAll(this.inactiveCellProviders);
        ll.addAll(this.activeCellProviders);
        CellChangeTracker tracker = new CellChangeTracker();
        for (ICellProvider cc : ll) {
            boolean active = true;
            if (cc instanceof IActionHost) {
                IGridNode node = ((IActionHost)((Object)cc)).getActionableNode();
                boolean bl = active = node != null && node.isActive();
            }
            if (active) {
                this.addCellProvider(cc, tracker);
                continue;
            }
            this.removeCellProvider(cc, tracker);
        }
        this.itemMonitor.forceUpdate();
        this.fluidMonitor.forceUpdate();
        tracker.applyChanges();
    }

    private void postChangesToNetwork(StorageChannel chan, int upOrDown, IItemList availableItems, BaseActionSource src) {
        switch (chan) {
            case FLUIDS: {
                this.fluidMonitor.postChange(upOrDown > 0, availableItems, src);
                break;
            }
            case ITEMS: {
                this.itemMonitor.postChange(upOrDown > 0, availableItems, src);
                break;
            }
        }
    }

    IMEInventoryHandler<IAEItemStack> getItemInventoryHandler() {
        if (this.myItemNetwork == null) {
            this.buildNetworkStorage(StorageChannel.ITEMS);
        }
        return this.myItemNetwork;
    }

    private void buildNetworkStorage(StorageChannel chan) {
        SecurityCache security = (SecurityCache)this.getGrid().getCache(ISecurityGrid.class);
        switch (chan) {
            case FLUIDS: {
                this.myFluidNetwork = new NetworkInventoryHandler(StorageChannel.FLUIDS, security);
                for (ICellProvider cc : this.activeCellProviders) {
                    for (IMEInventoryHandler h : cc.getCellArray(chan)) {
                        this.myFluidNetwork.addNewStorage(h);
                    }
                }
                break;
            }
            case ITEMS: {
                this.myItemNetwork = new NetworkInventoryHandler(StorageChannel.ITEMS, security);
                for (ICellProvider cc : this.activeCellProviders) {
                    for (IMEInventoryHandler h : cc.getCellArray(chan)) {
                        this.myItemNetwork.addNewStorage(h);
                    }
                }
                break;
            }
        }
    }

    IMEInventoryHandler<IAEFluidStack> getFluidInventoryHandler() {
        if (this.myFluidNetwork == null) {
            this.buildNetworkStorage(StorageChannel.FLUIDS);
        }
        return this.myFluidNetwork;
    }

    @Override
    public void postAlterationOfStoredItems(StorageChannel chan, Iterable<? extends IAEStack> input, BaseActionSource src) {
        if (chan == StorageChannel.ITEMS) {
            this.itemMonitor.postChange(true, input, src);
        } else if (chan == StorageChannel.FLUIDS) {
            this.fluidMonitor.postChange(true, input, src);
        }
    }

    @Override
    public void registerCellProvider(ICellProvider provider) {
        this.inactiveCellProviders.add(provider);
        this.addCellProvider(provider, new CellChangeTracker()).applyChanges();
    }

    @Override
    public void unregisterCellProvider(ICellProvider provider) {
        this.removeCellProvider(provider, new CellChangeTracker()).applyChanges();
        this.inactiveCellProviders.remove(provider);
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        return this.itemMonitor;
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        return this.fluidMonitor;
    }

    public GenericInterestManager<ItemWatcher> getInterestManager() {
        return this.interestManager;
    }

    IGrid getGrid() {
        return this.myGrid;
    }

    private void updateItemCellStatus(double bytesTotalToAdd, double bytesUsedToAdd, int cellStatus, double typesTotalToAdd, double typesUsedToAdd) {
        this.itemBytesTotal += bytesTotalToAdd;
        this.itemBytesUsed += bytesUsedToAdd;
        switch (cellStatus) {
            case 1: {
                ++this.itemCellG;
                break;
            }
            case 2: {
                ++this.itemCellB;
                break;
            }
            case 3: {
                ++this.itemCellO;
                break;
            }
            case 4: {
                ++this.itemCellR;
            }
        }
        this.itemTypesTotal = (long)((double)this.itemTypesTotal + typesTotalToAdd);
        this.itemTypesUsed = (long)((double)this.itemTypesUsed + typesUsedToAdd);
        ++this.itemCellCount;
    }

    private void updateFluidCellStatus(double bytesTotalToAdd, double bytesUsedToAdd, int cellStatus, double typesTotalToAdd, double typesUsedToAdd) {
        this.fluidBytesTotal += bytesTotalToAdd;
        this.fluidBytesUsed += bytesUsedToAdd;
        switch (cellStatus) {
            case 1: {
                ++this.fluidCellG;
                break;
            }
            case 2: {
                ++this.fluidCellB;
                break;
            }
            case 3: {
                ++this.fluidCellO;
                break;
            }
            case 4: {
                ++this.fluidCellR;
            }
        }
        this.fluidTypesTotal = (long)((double)this.fluidTypesTotal + typesTotalToAdd);
        this.fluidTypesUsed = (long)((double)this.fluidTypesUsed + typesUsedToAdd);
        ++this.fluidCellCount;
    }

    private void updateEssentiaCellStatus(double bytesTotalToAdd, double bytesUsedToAdd, int cellStatus, double typesTotalToAdd, double typesUsedToAdd) {
        this.essentiaBytesTotal += bytesTotalToAdd;
        this.essentiaBytesUsed += bytesUsedToAdd;
        switch (cellStatus) {
            case 1: {
                ++this.essentiaCellG;
                break;
            }
            case 2: {
                ++this.essentiaCellB;
                break;
            }
            case 3: {
                ++this.essentiaCellO;
                break;
            }
            case 4: {
                ++this.essentiaCellR;
            }
        }
        this.essentiaTypesTotal = (long)((double)this.essentiaTypesTotal + typesTotalToAdd);
        this.essentiaTypesUsed = (long)((double)this.essentiaTypesUsed + typesUsedToAdd);
        ++this.essentiaCellCount;
    }

    private void updateCellsStatusFromRegistry(ICellCacheRegistry iccr, ItemStack newCellStack) {
        switch (iccr.getCellType()) {
            case ITEM: {
                this.updateItemCellStatus(iccr.getTotalBytes(), iccr.getUsedBytes(), iccr.getCellStatus(), iccr.getTotalTypes(), iccr.getUsedTypes());
                this.putItemStackIntoMap(this.itemCells, newCellStack, iccr.getCellStatus());
                break;
            }
            case FLUID: {
                this.updateFluidCellStatus(iccr.getTotalBytes(), iccr.getUsedBytes(), iccr.getCellStatus(), iccr.getTotalTypes(), iccr.getUsedTypes());
                this.putItemStackIntoMap(this.fluidCells, newCellStack, iccr.getCellStatus());
                break;
            }
            case ESSENTIA: {
                this.updateEssentiaCellStatus(iccr.getTotalBytes(), iccr.getUsedBytes(), iccr.getCellStatus(), iccr.getTotalTypes(), iccr.getUsedTypes());
                this.putItemStackIntoMap(this.essentiaCells, newCellStack, iccr.getCellStatus());
            }
        }
    }

    private void updateBytesInfo() {
        this.resetCellInfo();
        try {
            for (ICellProvider icp : this.activeCellProviders) {
                TileChest tc;
                ItemStack stack;
                ICellCacheRegistry iccr;
                if (icp instanceof TileDrive) {
                    TileDrive td = (TileDrive)icp;
                    for (int index = 0; index < td.getCellCount(); ++index) {
                        IMEInventory<IAEItemStack> iMEInventory;
                        MEInventoryHandler<IAEItemStack> cellInv = td.getCellInvBySlot(index);
                        if (cellInv == null || !((iMEInventory = cellInv.getInternal()) instanceof ICellCacheRegistry) || !(iccr = (ICellCacheRegistry)((Object)iMEInventory)).canGetInv()) continue;
                        ItemStack stack2 = td.getStackInSlot(index);
                        this.updateCellsStatusFromRegistry(iccr, stack2);
                    }
                    continue;
                }
                if (!(icp instanceof TileChest) || (stack = (tc = (TileChest)icp).getStackInSlot(1)) == null) continue;
                IMEInventoryHandler handler = tc.getInternalHandler(StorageChannel.ITEMS);
                if (handler == null) {
                    handler = tc.getInternalHandler(StorageChannel.FLUIDS);
                }
                if (!(handler instanceof ICellCacheRegistry) || !(iccr = (ICellCacheRegistry)((Object)handler)).canGetInv()) continue;
                this.updateCellsStatusFromRegistry(iccr, stack);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void putItemStackIntoMap(ItemStackMap<Integer> map, ItemStack stack, int cellStatus) {
        ItemStack newStack = new ItemStack(stack.getItem(), 1, stack.getItemDamage());
        if (map.computeIfPresent((Object)newStack, (currentStack, stackCount) -> stackCount + 1) == null) {
            map.put(newStack, (Object)1);
        }
    }

    private void resetCellInfo() {
        this.itemBytesTotal = 0.0;
        this.itemBytesUsed = 0.0;
        this.itemTypesTotal = 0L;
        this.itemTypesUsed = 0L;
        this.itemCellG = 0L;
        this.itemCellB = 0L;
        this.itemCellO = 0L;
        this.itemCellR = 0L;
        this.itemCellCount = 0L;
        this.fluidBytesTotal = 0.0;
        this.fluidBytesUsed = 0.0;
        this.fluidTypesTotal = 0L;
        this.fluidTypesUsed = 0L;
        this.fluidCellG = 0L;
        this.fluidCellB = 0L;
        this.fluidCellO = 0L;
        this.fluidCellR = 0L;
        this.fluidCellCount = 0L;
        this.essentiaBytesTotal = 0.0;
        this.essentiaBytesUsed = 0.0;
        this.essentiaTypesTotal = 0L;
        this.essentiaTypesUsed = 0L;
        this.essentiaCellG = 0L;
        this.essentiaCellB = 0L;
        this.essentiaCellO = 0L;
        this.essentiaCellR = 0L;
        this.essentiaCellCount = 0L;
        this.itemCells.clear();
        this.fluidCells.clear();
        this.essentiaCells.clear();
    }

    public ItemStackMap<Integer> getItemCells() {
        return this.itemCells;
    }

    public ItemStackMap<Integer> getFluidCells() {
        return this.fluidCells;
    }

    public ItemStackMap<Integer> getEssentiaCells() {
        return this.essentiaCells;
    }

    public double getItemBytesTotal() {
        return this.itemBytesTotal;
    }

    public double getItemBytesUsed() {
        return this.itemBytesUsed;
    }

    public long getItemTypesTotal() {
        return this.itemTypesTotal;
    }

    public long getItemTypesUsed() {
        return this.itemTypesUsed;
    }

    public long getItemCellG() {
        return this.itemCellG;
    }

    public long getItemCellB() {
        return this.itemCellB;
    }

    public long getItemCellO() {
        return this.itemCellO;
    }

    public long getItemCellR() {
        return this.itemCellR;
    }

    public long getItemCellCount() {
        return this.itemCellCount;
    }

    public double getFluidBytesTotal() {
        return this.fluidBytesTotal;
    }

    public double getFluidBytesUsed() {
        return this.fluidBytesUsed;
    }

    public long getFluidTypesTotal() {
        return this.fluidTypesTotal;
    }

    public long getFluidTypesUsed() {
        return this.fluidTypesUsed;
    }

    public long getFluidCellG() {
        return this.fluidCellG;
    }

    public long getFluidCellB() {
        return this.fluidCellB;
    }

    public long getFluidCellO() {
        return this.fluidCellO;
    }

    public long getFluidCellR() {
        return this.fluidCellR;
    }

    public long getFluidCellCount() {
        return this.fluidCellCount;
    }

    public double getEssentiaBytesTotal() {
        return this.essentiaBytesTotal;
    }

    public double getEssentiaBytesUsed() {
        return this.essentiaBytesUsed;
    }

    public long getEssentiaTypesTotal() {
        return this.essentiaTypesTotal;
    }

    public long getEssentiaTypesUsed() {
        return this.essentiaTypesUsed;
    }

    public long getEssentiaCellG() {
        return this.essentiaCellG;
    }

    public long getEssentiaCellB() {
        return this.essentiaCellB;
    }

    public long getEssentiaCellO() {
        return this.essentiaCellO;
    }

    public long getEssentiaCellR() {
        return this.essentiaCellR;
    }

    public long getEssentiaCellCount() {
        return this.essentiaCellCount;
    }

    private class CellChangeTracker {
        final List<CellChangeTrackerRecord> data = new LinkedList<CellChangeTrackerRecord>();

        private CellChangeTracker() {
        }

        public void postChanges(StorageChannel channel, int i, IMEInventoryHandler<? extends IAEStack> h, BaseActionSource actionSrc) {
            this.data.add(new CellChangeTrackerRecord(channel, i, h, actionSrc));
        }

        public void applyChanges() {
            for (CellChangeTrackerRecord rec : this.data) {
                rec.applyChanges();
            }
        }
    }

    private class CellChangeTrackerRecord {
        final StorageChannel channel;
        final int up_or_down;
        final IItemList list;
        final BaseActionSource src;

        public CellChangeTrackerRecord(StorageChannel channel, int i, IMEInventoryHandler<? extends IAEStack> h, BaseActionSource actionSrc) {
            this.channel = channel;
            this.up_or_down = i;
            this.src = actionSrc;
            this.list = channel == StorageChannel.ITEMS ? h.getAvailableItems(AEApi.instance().storage().createItemList(), IterationCounter.fetchNewId()) : (channel == StorageChannel.FLUIDS ? h.getAvailableItems(AEApi.instance().storage().createFluidList(), IterationCounter.fetchNewId()) : null);
        }

        public void applyChanges() {
            GridStorageCache.this.postChangesToNetwork(this.channel, this.up_or_down, this.list, this.src);
        }
    }
}

