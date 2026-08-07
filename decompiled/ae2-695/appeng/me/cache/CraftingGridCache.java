/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.ImmutableCollection
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Multimap
 *  javax.annotation.Nonnull
 *  net.minecraft.world.World
 */
package appeng.me.cache;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.CraftingAllow;
import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingWatcher;
import appeng.api.networking.crafting.ICraftingWatcherHost;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.events.MENetworkCraftingCpuChange;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPostCacheConstruction;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.core.AEConfig;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingLink;
import appeng.crafting.CraftingLinkNexus;
import appeng.crafting.CraftingWatcher;
import appeng.crafting.v2.CraftingJobV2;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.GenericInterestManager;
import appeng.tile.crafting.TileCraftingStorageTile;
import appeng.tile.crafting.TileCraftingTile;
import appeng.util.ItemSorters;
import appeng.util.item.OreListMultiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import net.minecraft.world.World;

public class CraftingGridCache
implements ICraftingGrid,
ICraftingProviderHelper,
ICellProvider,
IMEInventoryHandler<IAEStack> {
    private static final ExecutorService CRAFTING_POOL;
    private static final Comparator<ICraftingPatternDetails> COMPARATOR;
    private final Set<CraftingCPUCluster> craftingCPUClusters = new HashSet<CraftingCPUCluster>();
    private final Set<ICraftingProvider> craftingProviders = new HashSet<ICraftingProvider>();
    private final Map<IGridNode, ICraftingWatcher> craftingWatchers = new HashMap<IGridNode, ICraftingWatcher>();
    private final IGrid grid;
    private final Map<ICraftingPatternDetails, List<ICraftingMedium>> craftingMethods = new HashMap<ICraftingPatternDetails, List<ICraftingMedium>>();
    private final OreListMultiMap<ICraftingPatternDetails> craftableItemSubstitutes = new OreListMultiMap();
    private final Map<IAEItemStack, ImmutableList<ICraftingPatternDetails>> craftableItems = new HashMap<IAEItemStack, ImmutableList<ICraftingPatternDetails>>();
    private final Set<IAEItemStack> emitableItems = new HashSet<IAEItemStack>();
    private final Map<String, CraftingLinkNexus> craftingLinks = new HashMap<String, CraftingLinkNexus>();
    private final Multimap<IAEStack, CraftingWatcher> interests = HashMultimap.create();
    private final GenericInterestManager<CraftingWatcher> interestManager = new GenericInterestManager<CraftingWatcher>(this.interests);
    private IStorageGrid storageGrid;
    private IEnergyGrid energyGrid;
    private boolean updateList = false;
    private static int pauseRebuilds;
    private static Set<CraftingGridCache> rebuildNeeded;

    public CraftingGridCache(IGrid grid) {
        this.grid = grid;
    }

    @MENetworkEventSubscribe
    public void afterCacheConstruction(MENetworkPostCacheConstruction cacheConstruction) {
        this.storageGrid = (IStorageGrid)this.grid.getCache(IStorageGrid.class);
        this.energyGrid = (IEnergyGrid)this.grid.getCache(IEnergyGrid.class);
        this.storageGrid.registerCellProvider(this);
    }

    @Override
    public void onUpdateTick() {
        if (this.updateList) {
            this.updateList = false;
            this.updateCPUClusters();
        }
        this.craftingLinks.values().removeIf(craftingLinkNexus -> craftingLinkNexus.isDead(this.grid, this));
        for (CraftingCPUCluster cpu : this.craftingCPUClusters) {
            cpu.tryExtractItems();
            cpu.updateCraftingLogic(this.grid, this.energyGrid, this);
        }
    }

    @Override
    public void removeNode(IGridNode gridNode, IGridHost machine) {
        ICraftingWatcher craftingWatcher;
        if (machine instanceof ICraftingWatcherHost && (craftingWatcher = this.craftingWatchers.get(machine)) != null) {
            craftingWatcher.clear();
            this.craftingWatchers.remove(machine);
        }
        if (machine instanceof ICraftingRequester) {
            for (CraftingLinkNexus link : this.craftingLinks.values()) {
                if (!link.isMachine(machine)) continue;
                link.removeNode();
            }
        }
        if (machine instanceof TileCraftingTile) {
            this.updateList = true;
        }
        if (machine instanceof ICraftingProvider) {
            this.craftingProviders.remove(machine);
            this.updatePatterns();
        }
    }

    @Override
    public void addNode(IGridNode gridNode, IGridHost machine) {
        if (machine instanceof ICraftingWatcherHost) {
            ICraftingWatcherHost watcherHost = (ICraftingWatcherHost)((Object)machine);
            CraftingWatcher watcher = new CraftingWatcher(this, watcherHost);
            this.craftingWatchers.put(gridNode, watcher);
            watcherHost.updateWatcher(watcher);
        }
        if (machine instanceof ICraftingRequester) {
            for (ICraftingLink link : ((ICraftingRequester)machine).getRequestedJobs()) {
                if (!(link instanceof CraftingLink)) continue;
                this.addLink((CraftingLink)link);
            }
        }
        if (machine instanceof TileCraftingTile) {
            this.updateList = true;
        }
        if (machine instanceof ICraftingProvider) {
            this.craftingProviders.add((ICraftingProvider)((Object)machine));
            this.updatePatterns();
        }
    }

    @Override
    public void onSplit(IGridStorage destinationStorage) {
    }

    @Override
    public void onJoin(IGridStorage sourceStorage) {
    }

    @Override
    public void populateGridStorage(IGridStorage destinationStorage) {
    }

    public static void pauseRebuilds() {
        ++pauseRebuilds;
    }

    public static void unpauseRebuilds() {
        if (--pauseRebuilds == 0 && rebuildNeeded.size() > 0) {
            ImmutableSet needed = ImmutableSet.copyOf(rebuildNeeded);
            rebuildNeeded.clear();
            for (CraftingGridCache cache : needed) {
                cache.updatePatterns();
            }
        }
    }

    private void updatePatterns() {
        if (pauseRebuilds != 0) {
            rebuildNeeded.add(this);
            return;
        }
        Map<IAEItemStack, ImmutableList<ICraftingPatternDetails>> oldItems = this.craftableItems;
        this.craftingMethods.clear();
        this.craftableItems.clear();
        this.craftableItemSubstitutes.clear();
        this.emitableItems.clear();
        this.storageGrid.postAlterationOfStoredItems(StorageChannel.ITEMS, oldItems.keySet(), new BaseActionSource());
        for (ICraftingProvider provider : this.craftingProviders) {
            provider.provideCrafting(this);
        }
        this.setPatternsFromCraftingMethods();
        this.storageGrid.postAlterationOfStoredItems(StorageChannel.ITEMS, this.craftableItems.keySet(), new BaseActionSource());
    }

    public void setMockPatternsFromMethods() {
        this.craftableItems.clear();
        this.craftableItemSubstitutes.clear();
        this.emitableItems.clear();
        this.setPatternsFromCraftingMethods();
    }

    private void setPatternsFromCraftingMethods() {
        HashMap<IAEItemStack, Set> tmpCraft = new HashMap<IAEItemStack, Set>();
        for (ICraftingPatternDetails iCraftingPatternDetails : this.craftingMethods.keySet()) {
            for (IAEItemStack out : iCraftingPatternDetails.getOutputs()) {
                out = out.copy();
                out.reset();
                out.setCraftable(true);
                if (iCraftingPatternDetails.canBeSubstitute()) {
                    this.craftableItemSubstitutes.put(out, iCraftingPatternDetails);
                }
                Set methods = tmpCraft.computeIfAbsent(out, k -> new TreeSet<ICraftingPatternDetails>(COMPARATOR));
                methods.add(iCraftingPatternDetails);
            }
        }
        this.craftableItemSubstitutes.freeze();
        for (Map.Entry entry : tmpCraft.entrySet()) {
            this.craftableItems.put((IAEItemStack)entry.getKey(), (ImmutableList<ICraftingPatternDetails>)ImmutableList.copyOf((Collection)((Collection)entry.getValue())));
        }
    }

    private void updateCPUClusters() {
        this.craftingCPUClusters.clear();
        for (Object cls : StreamSupport.stream(this.grid.getMachinesClasses().spliterator(), false).filter(TileCraftingStorageTile.class::isAssignableFrom).toArray()) {
            for (IGridNode cst : this.grid.getMachines((Class)cls)) {
                TileCraftingStorageTile tile = (TileCraftingStorageTile)cst.getMachine();
                CraftingCPUCluster cluster = (CraftingCPUCluster)tile.getCluster();
                if (cluster == null) continue;
                this.craftingCPUClusters.add(cluster);
                if (cluster.getLastCraftingLink() == null) continue;
                this.addLink((CraftingLink)cluster.getLastCraftingLink());
            }
        }
    }

    public void addLink(CraftingLink link) {
        if (link.isStandalone()) {
            return;
        }
        CraftingLinkNexus nexus = this.craftingLinks.get(link.getCraftingID());
        if (nexus == null) {
            nexus = new CraftingLinkNexus(link.getCraftingID());
            this.craftingLinks.put(link.getCraftingID(), nexus);
        }
        link.setNexus(nexus);
    }

    @MENetworkEventSubscribe
    public void updateCPUClusters(MENetworkCraftingCpuChange c) {
        this.updateList = true;
    }

    @MENetworkEventSubscribe
    public void updateCPUClusters(MENetworkCraftingPatternChange c) {
        this.updatePatterns();
    }

    @Override
    public void addCraftingOption(ICraftingMedium medium, ICraftingPatternDetails api) {
        List<ICraftingMedium> details = this.craftingMethods.get(api);
        if (details == null) {
            details = new ArrayList<ICraftingMedium>();
            details.add(medium);
            this.craftingMethods.put(api, details);
        } else {
            details.add(medium);
        }
    }

    @Override
    public void setEmitable(IAEItemStack someItem) {
        this.emitableItems.add(someItem.copy());
    }

    @Override
    public List<IMEInventoryHandler> getCellArray(StorageChannel channel) {
        ArrayList<IMEInventoryHandler> list = new ArrayList<IMEInventoryHandler>(1);
        if (channel == StorageChannel.ITEMS) {
            list.add(this);
        }
        return list;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.WRITE;
    }

    @Override
    public boolean isPrioritized(IAEStack input) {
        return true;
    }

    @Override
    public boolean canAccept(IAEStack input) {
        for (CraftingCPUCluster cpu : this.craftingCPUClusters) {
            if (!cpu.canAccept(input)) continue;
            return true;
        }
        return false;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int i) {
        return i == 1;
    }

    @Override
    public boolean isAutoCraftingInventory() {
        return true;
    }

    @Override
    public IAEStack injectItems(IAEStack input, Actionable type, BaseActionSource src) {
        for (CraftingCPUCluster cpu : this.craftingCPUClusters) {
            input = cpu.injectItems(input, type, src);
        }
        return input;
    }

    @Override
    public IAEStack extractItems(IAEStack request, Actionable mode, BaseActionSource src) {
        return null;
    }

    @Override
    public IItemList<IAEStack> getAvailableItems(IItemList<IAEStack> out, int iteration) {
        for (IAEItemStack stack : this.craftableItems.keySet()) {
            out.addCrafting(stack);
        }
        for (IAEItemStack st : this.emitableItems) {
            out.addCrafting(st);
        }
        return out;
    }

    @Override
    public IAEStack getAvailableItem(@Nonnull IAEStack request, int iteration) {
        return null;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public ImmutableMap<IAEItemStack, ImmutableList<ICraftingPatternDetails>> getCraftingPatterns() {
        return ImmutableMap.copyOf(this.craftableItems);
    }

    @Override
    public ImmutableCollection<ICraftingPatternDetails> getCraftingFor(IAEItemStack whatToCraft, ICraftingPatternDetails details, int slotIndex, World world) {
        ImmutableList<ICraftingPatternDetails> res = this.craftableItems.get(whatToCraft);
        if (res == null) {
            if (details != null && details.isCraftable()) {
                for (IAEItemStack ais : this.craftableItems.keySet()) {
                    if (ais.getItem() != whatToCraft.getItem() || ais.getItem().getHasSubtypes() && ais.getItemDamage() != whatToCraft.getItemDamage() || !details.isValidItemForSlot(slotIndex, ais.getItemStack(), world)) continue;
                    return (ImmutableCollection)this.craftableItems.get(ais);
                }
            }
            return ImmutableSet.of();
        }
        return res;
    }

    public static ExecutorService getCraftingPool() {
        return CRAFTING_POOL;
    }

    @Override
    public Future<ICraftingJob> beginCraftingJob(World world, IGrid grid, BaseActionSource actionSrc, IAEItemStack slotItem, ICraftingCallback cb) {
        return this.beginCraftingJob(world, grid, actionSrc, slotItem, CraftingMode.STANDARD, cb);
    }

    public Future<ICraftingJob> beginCraftingJob(World world, IGrid grid, BaseActionSource actionSrc, IAEItemStack slotItem, CraftingMode craftingMode, ICraftingCallback cb) {
        ICraftingJob iCraftingJob;
        if (world == null || grid == null || actionSrc == null || slotItem == null) {
            throw new IllegalArgumentException("Invalid Crafting Job Request");
        }
        switch (AEConfig.instance.craftingCalculatorVersion) {
            case 1: {
                iCraftingJob = new CraftingJob(world, grid, actionSrc, slotItem, cb);
                break;
            }
            case 2: {
                iCraftingJob = new CraftingJobV2(world, grid, actionSrc, slotItem, craftingMode, cb);
                break;
            }
            default: {
                throw new IllegalStateException("Invalid crafting calculator version");
            }
        }
        ICraftingJob job = iCraftingJob;
        return job.schedule();
    }

    @Override
    public ICraftingLink submitJob(ICraftingJob job, ICraftingRequester requestingMachine, ICraftingCPU target, boolean prioritizePower, BaseActionSource src) {
        return this.submitJob(job, requestingMachine, target, prioritizePower, src, false);
    }

    @Override
    public ICraftingLink submitJob(ICraftingJob job, ICraftingRequester requestingMachine, ICraftingCPU target, final boolean prioritizePower, BaseActionSource src, boolean followCraft) {
        if (job.isSimulation()) {
            return null;
        }
        CraftingCPUCluster cpuCluster = null;
        if (target instanceof CraftingCPUCluster) {
            cpuCluster = (CraftingCPUCluster)target;
        }
        if (target == null) {
            ArrayList<CraftingCPUCluster> validCpusClusters = new ArrayList<CraftingCPUCluster>();
            for (CraftingCPUCluster cpu : this.craftingCPUClusters) {
                boolean canOrder = false;
                canOrder |= cpu.isActive() && cpu.isBusy() && job.getOutput().isSameType(cpu.getFinalOutput()) && cpu.getAvailableStorage() >= cpu.getUsedStorage() + job.getByteTotal();
                if (!(canOrder |= cpu.isActive() && !cpu.isBusy() && cpu.getAvailableStorage() >= job.getByteTotal())) continue;
                if (src.isPlayer() && cpu.getCraftingAllowMode() != CraftingAllow.ONLY_NONPLAYER) {
                    validCpusClusters.add(cpu);
                    continue;
                }
                if (src.isPlayer() || cpu.getCraftingAllowMode() == CraftingAllow.ONLY_PLAYER) continue;
                validCpusClusters.add(cpu);
            }
            validCpusClusters.sort(new Comparator<CraftingCPUCluster>(){

                private int compareInternal(CraftingCPUCluster firstCluster, CraftingCPUCluster nextCluster) {
                    int comparison = ItemSorters.compareLong(nextCluster.getCoProcessors(), firstCluster.getCoProcessors());
                    if (comparison == 0) {
                        comparison = ItemSorters.compareLong(nextCluster.getAvailableStorage(), firstCluster.getAvailableStorage());
                    }
                    if (comparison == 0) {
                        return nextCluster.getName().compareTo(firstCluster.getName());
                    }
                    return comparison;
                }

                @Override
                public int compare(CraftingCPUCluster firstCluster, CraftingCPUCluster nextCluster) {
                    if (firstCluster.isBusy() != nextCluster.isBusy()) {
                        return Boolean.compare(nextCluster.isBusy(), firstCluster.isBusy());
                    }
                    if (prioritizePower) {
                        return this.compareInternal(firstCluster, nextCluster);
                    }
                    return this.compareInternal(nextCluster, firstCluster);
                }
            });
            if (!validCpusClusters.isEmpty()) {
                cpuCluster = (CraftingCPUCluster)validCpusClusters.get(0);
            }
        }
        if (cpuCluster != null) {
            ICraftingLink submittedJob = cpuCluster.submitJob(this.grid, job, src, requestingMachine);
            if (src instanceof PlayerSource) {
                PlayerSource playerSource = (PlayerSource)src;
                if (followCraft) {
                    cpuCluster.togglePlayerFollowStatus(playerSource.player.getCommandSenderName());
                }
            }
            return submittedJob;
        }
        return null;
    }

    @Override
    public ImmutableSet<ICraftingCPU> getCpus() {
        return ImmutableSet.copyOf((Iterator)new ActiveCpuIterator(this.craftingCPUClusters));
    }

    @Override
    public boolean canEmitFor(IAEItemStack someItem) {
        return this.emitableItems.contains(someItem);
    }

    @Override
    public boolean isRequesting(IAEItemStack what) {
        for (CraftingCPUCluster cluster : this.craftingCPUClusters) {
            if (!cluster.isMaking(what)) continue;
            return true;
        }
        return false;
    }

    public List<ICraftingMedium> getMediums(ICraftingPatternDetails key) {
        ImmutableList mediums = this.craftingMethods.get(key);
        if (mediums == null) {
            mediums = ImmutableList.of();
        }
        return mediums;
    }

    public boolean hasCpu(ICraftingCPU cpu) {
        return this.craftingCPUClusters.contains(cpu);
    }

    public GenericInterestManager<CraftingWatcher> getInterestManager() {
        return this.interestManager;
    }

    static {
        COMPARATOR = (firstDetail, nextDetail) -> nextDetail.getPriority() - firstDetail.getPriority();
        ThreadFactory factory = ar -> new Thread(ar, "AE Crafting Calculator");
        CRAFTING_POOL = Executors.newCachedThreadPool(factory);
        pauseRebuilds = 0;
        rebuildNeeded = new HashSet<CraftingGridCache>();
    }

    private static class ActiveCpuIterator
    implements Iterator<ICraftingCPU> {
        private final Iterator<CraftingCPUCluster> iterator;
        private CraftingCPUCluster cpuCluster;

        public ActiveCpuIterator(Collection<CraftingCPUCluster> o) {
            this.iterator = o.iterator();
            this.cpuCluster = null;
        }

        @Override
        public boolean hasNext() {
            this.findNext();
            return this.cpuCluster != null;
        }

        private void findNext() {
            while (this.iterator.hasNext() && this.cpuCluster == null) {
                this.cpuCluster = this.iterator.next();
                if (this.cpuCluster.isActive() && !this.cpuCluster.isDestroyed()) continue;
                this.cpuCluster = null;
            }
        }

        @Override
        public ICraftingCPU next() {
            CraftingCPUCluster o = this.cpuCluster;
            this.cpuCluster = null;
            return o;
        }

        @Override
        public void remove() {
        }
    }
}

