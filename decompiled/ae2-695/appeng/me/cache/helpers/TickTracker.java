/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.crash.CrashReportCategory
 */
package appeng.me.cache.helpers;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.DimensionalCoord;
import appeng.me.cache.TickManagerCache;
import appeng.parts.AEBasePart;
import javax.annotation.Nonnull;
import net.minecraft.crash.CrashReportCategory;

public class TickTracker
implements Comparable<TickTracker> {
    private final TickingRequest request;
    private final IGridTickable gt;
    private final IGridNode node;
    private final TickManagerCache host;
    private final long LastFiveTicksTime = 0L;
    private long lastTick;
    private int currentRate;

    public TickTracker(TickingRequest req, IGridNode node, IGridTickable gt, long currentTick, TickManagerCache tickManagerCache) {
        this.request = req;
        this.gt = gt;
        this.node = node;
        this.setCurrentRate((req.minTickRate + req.maxTickRate) / 2);
        this.setLastTick(currentTick);
        this.host = tickManagerCache;
    }

    public long getAvgNanos() {
        return this.LastFiveTicksTime / 5L;
    }

    public void setRate(int rate) {
        this.setCurrentRate(rate);
        if (this.getCurrentRate() < this.getRequest().minTickRate) {
            this.setCurrentRate(this.getRequest().minTickRate);
        }
        if (this.getCurrentRate() > this.getRequest().maxTickRate) {
            this.setCurrentRate(this.getRequest().maxTickRate);
        }
    }

    @Override
    public int compareTo(@Nonnull TickTracker t) {
        int nextTick = (int)(this.getLastTick() - this.host.getCurrentTick() + (long)this.getCurrentRate());
        int ts_nextTick = (int)(t.getLastTick() - this.host.getCurrentTick() + (long)t.getCurrentRate());
        return nextTick - ts_nextTick;
    }

    public void addEntityCrashInfo(CrashReportCategory crashreportcategory) {
        IGridTickable iGridTickable = this.getGridTickable();
        if (iGridTickable instanceof AEBasePart) {
            AEBasePart part = (AEBasePart)((Object)iGridTickable);
            part.addEntityCrashInfo(crashreportcategory);
        }
        crashreportcategory.addCrashSection("CurrentTickRate", (Object)this.getCurrentRate());
        crashreportcategory.addCrashSection("MinTickRate", (Object)this.getRequest().minTickRate);
        crashreportcategory.addCrashSection("MaxTickRate", (Object)this.getRequest().maxTickRate);
        crashreportcategory.addCrashSection("MachineType", (Object)this.getGridTickable().getClass().getName());
        crashreportcategory.addCrashSection("GridBlockType", (Object)this.getNode().getGridBlock().getClass().getName());
        crashreportcategory.addCrashSection("ConnectedSides", this.getNode().getConnectedSides());
        DimensionalCoord dc = this.getNode().getGridBlock().getLocation();
        if (dc != null) {
            crashreportcategory.addCrashSection("Location", (Object)dc);
        }
    }

    public int getCurrentRate() {
        return this.currentRate;
    }

    public void setCurrentRate(int currentRate) {
        this.currentRate = currentRate;
    }

    public long getLastTick() {
        return this.lastTick;
    }

    public void setLastTick(long lastTick) {
        this.lastTick = lastTick;
    }

    public IGridNode getNode() {
        return this.node;
    }

    public IGridTickable getGridTickable() {
        return this.gt;
    }

    public TickingRequest getRequest() {
        return this.request;
    }
}

