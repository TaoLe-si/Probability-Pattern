/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.world.World
 */
package appeng.crafting;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.DimensionalCoord;
import appeng.core.AELog;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingCalculationFailure;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.MECraftingInventory;
import appeng.hooks.TickHandler;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.google.common.base.Stopwatch;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class CraftingJob
implements ICraftingJob,
Runnable {
    private static final String LOG_CRAFTING_JOB = "CraftingJob (%s) issued by %s requesting [%s] using %s bytes took %s ms";
    private static final String LOG_MACHINE_SOURCE_DETAILS = "Machine[object=%s, %s]";
    private final MECraftingInventory original;
    private final World world;
    private final IItemList<IAEItemStack> crafting = AEApi.instance().storage().createItemList();
    private final IItemList<IAEItemStack> missing = AEApi.instance().storage().createItemList();
    private final HashMap<String, TwoIntegers> opsAndMultiplier = new HashMap();
    private final Object monitor = new Object();
    private final Stopwatch watch = Stopwatch.createUnstarted();
    private CraftingTreeNode tree;
    private final IAEItemStack output;
    private boolean simulate = false;
    private MECraftingInventory availableCheck;
    private long bytes = 0L;
    private final BaseActionSource actionSrc;
    private final ICraftingCallback callback;
    private boolean running = false;
    private boolean done = false;
    private int time = 5;
    private int incTime = Integer.MAX_VALUE;

    private World wrapWorld(World w) {
        return w;
    }

    public CraftingJob(World w, IGrid grid, BaseActionSource actionSrc, IAEItemStack what, ICraftingCallback callback) {
        this.world = this.wrapWorld(w);
        this.output = what.copy();
        this.actionSrc = actionSrc;
        this.callback = callback;
        ICraftingGrid cc = (ICraftingGrid)grid.getCache(ICraftingGrid.class);
        IStorageGrid sg = (IStorageGrid)grid.getCache(IStorageGrid.class);
        this.original = new MECraftingInventory(sg.getItemInventory(), actionSrc, false, false, false);
        this.setTree(this.getCraftingTree(cc, what));
        this.availableCheck = null;
    }

    private CraftingTreeNode getCraftingTree(ICraftingGrid cc, IAEItemStack what) {
        return new CraftingTreeNode(cc, this, what, null, -1, 0);
    }

    void refund(IAEItemStack o) {
        this.availableCheck.injectItems(o, Actionable.MODULATE, this.actionSrc);
    }

    IAEItemStack checkUse(IAEItemStack available) {
        return this.availableCheck.extractItems(available, Actionable.MODULATE, this.actionSrc);
    }

    void addTask(IAEItemStack what, long crafts, ICraftingPatternDetails details, int depth) {
        if (crafts > 0L) {
            what = what.copy();
            what.setStackSize(what.getStackSize() * crafts);
            this.crafting.add(what);
        }
    }

    void addMissing(IAEItemStack what) {
        what = what.copy();
        this.missing.add(what);
    }

    @Override
    public Future<ICraftingJob> schedule() {
        return CraftingGridCache.getCraftingPool().submit(this, this);
    }

    @Override
    public void run() {
        try {
            try {
                TickHandler.INSTANCE.registerCraftingSimulation(this.world, this);
                this.handlePausing();
                Stopwatch timer = Stopwatch.createStarted();
                MECraftingInventory craftingInventory = new MECraftingInventory(this.original, true, false, true);
                craftingInventory.ignore(this.output);
                this.availableCheck = new MECraftingInventory(this.original, false, false, false);
                this.getTree().request(craftingInventory, this.output.getStackSize(), this.actionSrc);
                this.getTree().dive(this);
                for (String s : this.opsAndMultiplier.keySet()) {
                    TwoIntegers ti = this.opsAndMultiplier.get(s);
                    AELog.crafting(s + " * " + 0L + " = " + 0L * 0L, new Object[0]);
                }
                this.logCraftingJob("real", timer);
            }
            catch (CraftBranchFailure e) {
                this.simulate = true;
                try {
                    Stopwatch timer = Stopwatch.createStarted();
                    MECraftingInventory craftingInventory = new MECraftingInventory(this.original, true, false, true);
                    craftingInventory.ignore(this.output);
                    this.availableCheck = new MECraftingInventory(this.original, false, false, false);
                    this.getTree().setSimulate();
                    this.getTree().request(craftingInventory, this.output.getStackSize(), this.actionSrc);
                    this.getTree().dive(this);
                    for (String s : this.opsAndMultiplier.keySet()) {
                        TwoIntegers ti = this.opsAndMultiplier.get(s);
                        AELog.crafting(s + " * " + 0L + " = " + 0L * 0L, new Object[0]);
                    }
                    this.logCraftingJob("simulate", timer);
                }
                catch (CraftBranchFailure | CraftingCalculationFailure e1) {
                    AELog.debug(e1);
                }
                catch (InterruptedException e1) {
                    AELog.crafting("Crafting calculation canceled.", new Object[0]);
                    this.finish();
                    return;
                }
            }
            catch (CraftingCalculationFailure f) {
                AELog.debug(f);
            }
            catch (InterruptedException e1) {
                AELog.crafting("Crafting calculation canceled.", new Object[0]);
                this.finish();
                return;
            }
            AELog.craftingDebug("crafting job now done", new Object[0]);
        }
        catch (Throwable t) {
            this.finish();
            throw new IllegalStateException(t);
        }
        this.finish();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void handlePausing() throws InterruptedException {
        if (this.incTime > 100) {
            this.incTime = 0;
            Object object = this.monitor;
            synchronized (object) {
                if (this.watch.elapsed(TimeUnit.MICROSECONDS) > (long)this.time) {
                    this.running = false;
                    this.watch.stop();
                    this.monitor.notify();
                }
                if (!this.running) {
                    AELog.craftingDebug("crafting job will now sleep", new Object[0]);
                    while (!this.running) {
                        this.monitor.wait();
                    }
                    AELog.craftingDebug("crafting job now active", new Object[0]);
                }
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        ++this.incTime;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void finish() {
        if (this.callback != null) {
            this.callback.calculationComplete(this);
        }
        this.availableCheck = null;
        Object object = this.monitor;
        synchronized (object) {
            this.running = false;
            this.done = true;
            this.monitor.notify();
        }
    }

    @Override
    public boolean isSimulation() {
        return this.simulate;
    }

    @Override
    public long getByteTotal() {
        return this.bytes;
    }

    @Override
    public void populatePlan(IItemList<IAEItemStack> plan) {
        if (this.getTree() != null) {
            this.getTree().getPlan(plan);
        }
    }

    @Override
    public IAEItemStack getOutput() {
        return this.output;
    }

    public boolean isDone() {
        return this.done;
    }

    World getWorld() {
        return this.world;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean simulateFor(int milli) {
        this.time = milli;
        Object object = this.monitor;
        synchronized (object) {
            if (this.done) {
                return false;
            }
            this.watch.reset();
            this.watch.start();
            this.running = true;
            AELog.craftingDebug("main thread is now going to sleep", new Object[0]);
            this.monitor.notify();
            while (this.running) {
                try {
                    this.monitor.wait();
                }
                catch (InterruptedException interruptedException) {}
            }
            AELog.craftingDebug("main thread is now active", new Object[0]);
        }
        return true;
    }

    void addBytes(long crafts) {
        this.bytes += crafts;
    }

    public CraftingTreeNode getTree() {
        return this.tree;
    }

    private void setTree(CraftingTreeNode tree) {
        this.tree = tree;
    }

    private void logCraftingJob(String type, Stopwatch timer) {
        if (AELog.isCraftingLogEnabled()) {
            String actionSource;
            String itemToOutput = this.output.toString();
            long elapsedTime = timer.elapsed(TimeUnit.MILLISECONDS);
            if (this.actionSrc instanceof MachineSource) {
                IActionHost machineSource = ((MachineSource)this.actionSrc).via;
                IGridNode actionableNode = machineSource.getActionableNode();
                IGridHost machine = actionableNode.getMachine();
                DimensionalCoord location = actionableNode.getGridBlock().getLocation();
                actionSource = String.format(LOG_MACHINE_SOURCE_DETAILS, machine, location);
            } else {
                BaseActionSource machineSource = this.actionSrc;
                if (machineSource instanceof PlayerSource) {
                    PlayerSource source = (PlayerSource)machineSource;
                    EntityPlayer player = source.player;
                    actionSource = player.toString();
                } else {
                    actionSource = "[unknown source]";
                }
            }
            AELog.crafting(LOG_CRAFTING_JOB, type, actionSource, itemToOutput, this.bytes, elapsedTime);
        }
    }

    @Override
    public boolean supportsCPUCluster(ICraftingCPU cluster) {
        return cluster instanceof CraftingCPUCluster;
    }

    @Override
    public void startCrafting(MECraftingInventory storage, ICraftingCPU craftingCPUCluster, BaseActionSource src) {
        CraftingCPUCluster cluster = (CraftingCPUCluster)craftingCPUCluster;
        this.tree.setJob(storage, cluster, src);
    }

    @Override
    public MECraftingInventory getStorageAtBeginning() {
        return this.original;
    }

    private static class TwoIntegers {
        private final long perOp = 0L;
        private final long times = 0L;

        private TwoIntegers() {
        }
    }
}

