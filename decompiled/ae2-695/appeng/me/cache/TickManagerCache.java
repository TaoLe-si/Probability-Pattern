/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.crash.CrashReport
 *  net.minecraft.crash.CrashReportCategory
 *  net.minecraft.util.ReportedException
 */
package appeng.me.cache;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.DimensionalCoord;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.me.cache.helpers.TickTracker;
import java.util.HashMap;
import java.util.PriorityQueue;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;

public class TickManagerCache
implements ITickManager {
    private final IGrid myGrid;
    private final HashMap<IGridNode, TickTracker> alertable = new HashMap();
    private final HashMap<IGridNode, TickTracker> sleeping = new HashMap();
    private final HashMap<IGridNode, TickTracker> awake = new HashMap();
    private final PriorityQueue<TickTracker> upcomingTicks = new PriorityQueue();
    private long currentTick = 0L;

    public TickManagerCache(IGrid g) {
        this.myGrid = g;
    }

    public long getCurrentTick() {
        return this.currentTick;
    }

    public long getAvgNanoTime(IGridNode node) {
        TickTracker tt = this.awake.get(node);
        if (tt == null) {
            tt = this.sleeping.get(node);
        }
        if (tt == null) {
            return -1L;
        }
        return tt.getAvgNanos();
    }

    @Override
    public void onUpdateTick() {
        TickTracker tt = null;
        try {
            ++this.currentTick;
            while (!this.upcomingTicks.isEmpty()) {
                tt = this.upcomingTicks.peek();
                int diff = (int)(this.currentTick - tt.getLastTick());
                if (diff >= tt.getCurrentRate()) {
                    this.upcomingTicks.poll();
                    long tickStartTime = 0L;
                    if (AEConfig.instance.debugLogTiming) {
                        tickStartTime = System.nanoTime();
                    }
                    TickRateModulation mod = tt.getGridTickable().tickingRequest(tt.getNode(), diff);
                    if (AEConfig.instance.debugLogTiming) {
                        DimensionalCoord c = tt.getNode().getGridBlock().getLocation();
                        AELog.debug("Timing: machine tick at (%d %d %d) took %d ns, new state is %s", c.x, c.y, c.z, System.nanoTime() - tickStartTime, mod.toString());
                    }
                    switch (mod) {
                        case FASTER: {
                            tt.setRate(tt.getCurrentRate() - 2);
                            break;
                        }
                        case IDLE: {
                            tt.setRate(tt.getRequest().maxTickRate);
                            break;
                        }
                        case SAME: {
                            break;
                        }
                        case SLEEP: {
                            this.sleepDevice(tt.getNode());
                            break;
                        }
                        case SLOWER: {
                            tt.setRate(tt.getCurrentRate() + 1);
                            break;
                        }
                        case URGENT: {
                            tt.setRate(0);
                            break;
                        }
                    }
                    if (!this.awake.containsKey(tt.getNode())) continue;
                    this.addToQueue(tt);
                    continue;
                }
                return;
            }
        }
        catch (Throwable t) {
            CrashReport crashreport = CrashReport.makeCrashReport((Throwable)t, (String)"Ticking GridNode");
            CrashReportCategory crashreportcategory = crashreport.makeCategory(tt.getGridTickable().getClass().getSimpleName() + " being ticked.");
            tt.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    private void addToQueue(TickTracker tt) {
        tt.setLastTick(this.currentTick);
        this.upcomingTicks.add(tt);
    }

    @Override
    public void removeNode(IGridNode gridNode, IGridHost machine) {
        if (machine instanceof IGridTickable) {
            this.alertable.remove(gridNode);
            this.upcomingTicks.remove(this.sleeping.remove(gridNode));
            this.upcomingTicks.remove(this.awake.remove(gridNode));
        }
    }

    @Override
    public void addNode(IGridNode gridNode, IGridHost machine) {
        TickingRequest tr;
        if (machine instanceof IGridTickable && (tr = ((IGridTickable)((Object)machine)).getTickingRequest(gridNode)) != null) {
            TickTracker tt = new TickTracker(tr, gridNode, (IGridTickable)((Object)machine), this.currentTick, this);
            if (tr.canBeAlerted) {
                this.alertable.put(gridNode, tt);
            }
            if (tr.isSleeping) {
                this.sleeping.put(gridNode, tt);
            } else {
                this.awake.put(gridNode, tt);
                this.addToQueue(tt);
            }
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

    @Override
    public boolean alertDevice(IGridNode node) {
        TickTracker tt = this.alertable.get(node);
        if (tt == null) {
            return false;
        }
        this.sleeping.remove(node);
        this.awake.put(node, tt);
        tt.setLastTick(tt.getLastTick() - (long)tt.getRequest().maxTickRate);
        tt.setCurrentRate(tt.getRequest().minTickRate);
        this.upcomingTicks.remove(tt);
        this.upcomingTicks.add(tt);
        return true;
    }

    @Override
    public boolean sleepDevice(IGridNode node) {
        if (this.awake.containsKey(node)) {
            TickTracker gt = this.awake.get(node);
            this.awake.remove(node);
            this.sleeping.put(node, gt);
            this.upcomingTicks.remove(gt);
            return true;
        }
        return false;
    }

    @Override
    public boolean wakeDevice(IGridNode node) {
        if (this.sleeping.containsKey(node)) {
            TickTracker gt = this.sleeping.get(node);
            this.sleeping.remove(node);
            this.awake.put(node, gt);
            this.addToQueue(gt);
            return true;
        }
        return false;
    }
}

