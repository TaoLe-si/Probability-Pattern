/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ClassToInstanceMap
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.MutableClassToInstanceMap
 *  cpw.mods.fml.common.FMLCommonHandler
 *  cpw.mods.fml.relauncher.Side
 *  javax.annotation.Nonnull
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 */
package appeng.crafting.v2;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.core.AEConfig;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingCalculations;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingStepLimitExceeded;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.util.item.OreListMultiMap;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MutableClassToInstanceMap;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class CraftingContext {
    public final World world;
    public final IGrid meGrid;
    public final ICraftingGrid craftingGrid;
    public BaseActionSource actionSource;
    public final MECraftingInventory itemModel;
    public final MECraftingInventory byproductsInventory;
    public final MECraftingInventory availableCache;
    public boolean wasSimulated = false;
    private final List<RequestInProcessing<?>> liveRequests = new ArrayList(32);
    private final List<CraftingTask> resolvedTasks = new ArrayList<CraftingTask>();
    private final ArrayDeque<CraftingTask> tasksToProcess = new ArrayDeque(64);
    private boolean doingWork = false;
    private CraftingTask.State finishedState = CraftingTask.State.FAILURE;
    private final ImmutableMap<IAEItemStack, ImmutableList<ICraftingPatternDetails>> availablePatterns;
    private final Map<IAEItemStack, List<ICraftingPatternDetails>> precisePatternCache = new HashMap<IAEItemStack, List<ICraftingPatternDetails>>();
    private final Map<ICraftingPatternDetails, IAEItemStack> crafterIconCache = new HashMap<ICraftingPatternDetails, IAEItemStack>();
    private final OreListMultiMap<ICraftingPatternDetails> fuzzyPatternCache = new OreListMultiMap();
    private final IdentityHashMap<ICraftingPatternDetails, Boolean> isPatternComplexCache = new IdentityHashMap();
    private final ClassToInstanceMap<Object> userCaches = MutableClassToInstanceMap.create();

    public CraftingContext(@Nonnull World world, @Nonnull IGrid meGrid, @Nonnull BaseActionSource actionSource) {
        this.world = world;
        this.meGrid = meGrid;
        this.craftingGrid = (ICraftingGrid)meGrid.getCache(ICraftingGrid.class);
        this.actionSource = actionSource;
        IStorageGrid sg = (IStorageGrid)meGrid.getCache(IStorageGrid.class);
        this.itemModel = new MECraftingInventory(sg.getItemInventory(), this.actionSource, true, false, true);
        this.byproductsInventory = new MECraftingInventory();
        this.availableCache = new MECraftingInventory(sg.getItemInventory(), this.actionSource, false, false, false);
        this.availablePatterns = this.craftingGrid.getCraftingPatterns();
    }

    public <T> T getUserCache(Class<T> cacheType, Supplier<T> constructor) {
        Object instance = this.userCaches.getInstance(cacheType);
        if (instance == null) {
            instance = constructor.get();
            this.userCaches.putInstance(cacheType, instance);
        }
        return (T)instance;
    }

    public void addRequest(@Nonnull CraftingRequest<?> request) {
        if (this.doingWork) {
            throw new IllegalStateException("Trying to add requests while inside a CraftingTask handler, return requests in the StepOutput instead");
        }
        RequestInProcessing processing = new RequestInProcessing(request);
        processing.resolvers.addAll(CraftingCalculations.tryResolveCraftingRequest(request, this));
        Collections.reverse(processing.resolvers);
        this.liveRequests.add(processing);
        if (processing.resolvers.isEmpty()) {
            throw new IllegalStateException("No resolvers available for request " + request.toString());
        }
        this.queueNextTaskOf(processing, true);
    }

    public IAEItemStack getCrafterIconForPattern(@Nonnull ICraftingPatternDetails pattern) {
        return this.crafterIconCache.computeIfAbsent(pattern, ignored -> {
            if (this.craftingGrid instanceof CraftingGridCache) {
                List<ICraftingMedium> mediums = ((CraftingGridCache)this.craftingGrid).getMediums(pattern);
                for (ICraftingMedium medium : mediums) {
                    ItemStack stack = medium.getCrafterIcon();
                    if (stack == null) continue;
                    return AEItemStack.create(stack);
                }
            }
            return AEItemStack.create((ItemStack)AEApi.instance().definitions().blocks().iface().maybeStack(1).orNull());
        });
    }

    public List<ICraftingPatternDetails> getPrecisePatternsFor(@Nonnull IAEItemStack stack) {
        return this.precisePatternCache.compute(stack, (key, value) -> {
            if (value == null) {
                return (List)this.availablePatterns.getOrDefault((Object)stack, (Object)ImmutableList.of());
            }
            return value;
        });
    }

    public List<ICraftingPatternDetails> getFuzzyPatternsFor(@Nonnull IAEItemStack stack) {
        if (!this.fuzzyPatternCache.isPopulated()) {
            for (ImmutableList patternSet : this.availablePatterns.values()) {
                for (ICraftingPatternDetails pattern : patternSet) {
                    if (!pattern.canBeSubstitute()) continue;
                    for (IAEItemStack output : pattern.getOutputs()) {
                        this.fuzzyPatternCache.put(output.copy(), pattern);
                    }
                }
            }
            this.fuzzyPatternCache.freeze();
        }
        return this.fuzzyPatternCache.get(stack);
    }

    public boolean isPatternComplex(@Nonnull ICraftingPatternDetails pattern) {
        if (!pattern.isCraftable()) {
            return false;
        }
        Boolean cached = this.isPatternComplexCache.get(pattern);
        if (cached != null) {
            return cached;
        }
        IAEItemStack[] inputs = pattern.getInputs();
        IAEItemStack[] mcOutputs = this.simulateComplexCrafting(inputs, pattern);
        boolean isComplex = Arrays.stream(mcOutputs).anyMatch(Objects::nonNull);
        this.isPatternComplexCache.put(pattern, isComplex);
        return isComplex;
    }

    public IAEItemStack[] simulateComplexCrafting(IAEItemStack[] inputSlots, ICraftingPatternDetails pattern) {
        if (inputSlots.length > 9) {
            throw new IllegalArgumentException(inputSlots.length + " slots supplied to a simulated crafting task");
        }
        InventoryCrafting simulatedWorkbench = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
        for (int i = 0; i < inputSlots.length; ++i) {
            simulatedWorkbench.setInventorySlotContents(i, inputSlots[i] == null ? null : inputSlots[i].getItemStack());
        }
        if (this.world instanceof WorldServer) {
            FMLCommonHandler.instance().firePlayerCraftingEvent(Platform.getPlayer((WorldServer)this.world), pattern.getOutput(simulatedWorkbench, this.world), (IInventory)simulatedWorkbench);
        }
        IAEItemStack[] output = new IAEItemStack[9];
        for (int i = 0; i < output.length; ++i) {
            ItemStack mcOut = simulatedWorkbench.getStackInSlot(i);
            if (mcOut == null || mcOut.getItem() == null || mcOut.stackSize <= 0) {
                output[i] = null;
                continue;
            }
            Item item = mcOut.getItem();
            ItemStack container = Platform.getContainerItem(mcOut);
            if (container != null) {
                if (container.stackSize <= 0) {
                    output[i] = null;
                    continue;
                }
                output[i] = AEItemStack.create(container);
                continue;
            }
            --mcOut.stackSize;
            output[i] = mcOut.stackSize <= 0 ? null : AEItemStack.create(mcOut);
        }
        return output;
    }

    public CraftingTask.State doWork() {
        if (this.tasksToProcess.isEmpty()) {
            return this.finishedState;
        }
        if (FMLCommonHandler.instance().getSide() == Side.SERVER && this.resolvedTasks.size() > AEConfig.instance.maxCraftingSteps) {
            this.finishedState = CraftingTask.State.FAILURE;
            for (CraftingTask task : this.tasksToProcess) {
                if (task.request == null) continue;
                task.request.incomplete = true;
            }
            throw new CraftingStepLimitExceeded();
        }
        CraftingTask frontTask = this.tasksToProcess.getFirst();
        if (frontTask.getState() == CraftingTask.State.SUCCESS || frontTask.getState() == CraftingTask.State.FAILURE) {
            this.resolvedTasks.add(frontTask);
            this.tasksToProcess.removeFirst();
            return CraftingTask.State.NEEDS_MORE_WORK;
        }
        this.doingWork = true;
        CraftingTask.StepOutput out = frontTask.calculateOneStep(this);
        CraftingTask.State newState = frontTask.getState();
        this.doingWork = false;
        if (!out.extraInputsRequired.isEmpty()) {
            Set<ICraftingPatternDetails> parentPatterns = frontTask.request.patternParents;
            for (int ri = out.extraInputsRequired.size() - 1; ri >= 0; --ri) {
                CraftingRequest<?> request = out.extraInputsRequired.get(ri);
                request.patternParents.addAll(parentPatterns);
                this.addRequest(request);
            }
        } else if (newState == CraftingTask.State.SUCCESS) {
            if (this.tasksToProcess.getFirst() != frontTask) {
                throw new IllegalStateException("A crafting task got added to the queue without requesting more work.");
            }
            this.resolvedTasks.add(frontTask);
            this.tasksToProcess.removeFirst();
            this.finishedState = CraftingTask.State.SUCCESS;
        } else if (newState == CraftingTask.State.FAILURE) {
            this.tasksToProcess.clear();
            this.finishedState = CraftingTask.State.FAILURE;
            return CraftingTask.State.FAILURE;
        }
        return this.tasksToProcess.isEmpty() ? CraftingTask.State.SUCCESS : CraftingTask.State.NEEDS_MORE_WORK;
    }

    public List<CraftingTask> getResolvedTasks() {
        return Collections.unmodifiableList(this.resolvedTasks);
    }

    public List<RequestInProcessing<?>> getLiveRequests() {
        return Collections.unmodifiableList(this.liveRequests);
    }

    public String toString() {
        Set processed = Collections.newSetFromMap(new IdentityHashMap());
        return this.getResolvedTasks().stream().map(rt -> {
            boolean isNew = processed.add(rt);
            return (isNew ? "  " : "  [duplicate] ") + rt.toString();
        }).collect(Collectors.joining("\n"));
    }

    private boolean queueNextTaskOf(RequestInProcessing<?> request, boolean addResolverTask) {
        if (request.request.remainingToProcess <= 0L || request.resolvers.isEmpty()) {
            return false;
        }
        CraftingTask nextResolver = request.resolvers.remove(request.resolvers.size() - 1);
        if (addResolverTask && !request.resolvers.isEmpty()) {
            this.tasksToProcess.addFirst(new CheckOtherResolversTask(request));
        }
        if (request.resolvers.isEmpty()) {
            request.resolvers.trimToSize();
        }
        this.tasksToProcess.addFirst(nextResolver);
        return true;
    }

    public static final class RequestInProcessing<StackType extends IAEStack<StackType>> {
        public final CraftingRequest<StackType> request;
        public final ArrayList<CraftingTask> resolvers = new ArrayList(4);

        public RequestInProcessing(CraftingRequest<StackType> request) {
            this.request = request;
        }
    }

    private final class CheckOtherResolversTask<T extends IAEStack<T>>
    extends CraftingTask<T> {
        private final RequestInProcessing<?> myRequest;

        public CheckOtherResolversTask(RequestInProcessing<T> myRequest) {
            super(myRequest.request, 0);
            this.myRequest = myRequest;
        }

        @Override
        public CraftingTask.StepOutput calculateOneStep(CraftingContext context) {
            boolean needsMoreWork = CraftingContext.this.queueNextTaskOf(this.myRequest, false);
            this.state = needsMoreWork ? CraftingTask.State.NEEDS_MORE_WORK : (this.myRequest.request.remainingToProcess <= 0L ? CraftingTask.State.SUCCESS : CraftingTask.State.FAILURE);
            return new CraftingTask.StepOutput();
        }

        @Override
        public long partialRefund(CraftingContext context, long amount) {
            return 0L;
        }

        @Override
        public void fullRefund(CraftingContext context) {
        }

        @Override
        public void populatePlan(IItemList<IAEItemStack> targetPlan) {
        }

        @Override
        public void startOnCpu(CraftingContext context, CraftingCPUCluster cpuCluster, MECraftingInventory craftingInv) {
        }

        public String toString() {
            return "CheckOtherResolversTask{myRequest=" + this.myRequest + ", priority=" + this.priority + ", state=" + (Object)((Object)this.state) + '}';
        }
    }
}

