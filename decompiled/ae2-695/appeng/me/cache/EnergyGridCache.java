/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultiset
 *  com.google.common.collect.Multiset
 */
package appeng.me.cache;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergyGridProvider;
import appeng.api.networking.energy.IEnergyWatcher;
import appeng.api.networking.energy.IEnergyWatcherHost;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPostCacheConstruction;
import appeng.api.networking.events.MENetworkPowerIdleChange;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.events.MENetworkPowerStorage;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.me.Grid;
import appeng.me.GridNode;
import appeng.me.cache.PathGridCache;
import appeng.me.energy.EnergyThreshold;
import appeng.me.energy.EnergyWatcher;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class EnergyGridCache
implements IEnergyGrid {
    private final TreeSet<EnergyThreshold> interests = new TreeSet();
    private final double AvgLength = 40.0;
    private final Set<IAEPowerStorage> providers = new LinkedHashSet<IAEPowerStorage>();
    private final Set<IAEPowerStorage> requesters = new LinkedHashSet<IAEPowerStorage>();
    private final Multiset<IEnergyGridProvider> energyGridProviders = HashMultiset.create();
    private WeakReference<IEnergyGridProvider> lastGridProvider = new WeakReference<Object>(null);
    private final IGrid myGrid;
    private final HashMap<IGridNode, IEnergyWatcher> watchers = new HashMap();
    private final Set<IEnergyGrid> localSeen = new HashSet<IEnergyGrid>();
    private int availableTicksSinceUpdate = 0;
    private double globalAvailablePower = 0.0;
    private double globalMaxPower = 0.0;
    private double drainPerTick = 0.0;
    private double avgDrainPerTick = 0.0;
    private double avgInjectionPerTick = 0.0;
    private double tickDrainPerTick = 0.0;
    private double tickInjectionPerTick = 0.0;
    private boolean publicHasPower = false;
    private boolean hasPower = true;
    private boolean infinite = false;
    private boolean updateInfinite = false;
    private long ticksSinceHasPowerChange = 900L;
    private double extra = 0.0;
    private IAEPowerStorage lastProvider;
    private IAEPowerStorage lastRequester;
    private PathGridCache pgc;
    private double lastStoredPower = -1.0;

    public EnergyGridCache(IGrid g) {
        this.myGrid = g;
    }

    @MENetworkEventSubscribe
    public void postInit(MENetworkPostCacheConstruction pcc) {
        this.pgc = (PathGridCache)this.myGrid.getCache(IPathingGrid.class);
    }

    @MENetworkEventSubscribe
    public void EnergyNodeChanges(MENetworkPowerIdleChange ev) {
        GridNode node = (GridNode)ev.node;
        IGridBlock gb = node.getGridBlock();
        double newDraw = gb.getIdlePowerUsage();
        double diffDraw = newDraw - node.getPreviousDraw();
        node.setPreviousDraw(newDraw);
        this.drainPerTick += diffDraw;
    }

    @MENetworkEventSubscribe
    public void EnergyNodeChanges(MENetworkPowerStorage ev) {
        if (ev.storage.isAEPublicPowerStorage()) {
            switch (ev.type) {
                case PROVIDE_POWER: {
                    if (ev.storage.getPowerFlow() == AccessRestriction.WRITE) break;
                    this.providers.add(ev.storage);
                    break;
                }
                case REQUEST_POWER: {
                    if (ev.storage.getPowerFlow() == AccessRestriction.READ) break;
                    this.requesters.add(ev.storage);
                }
            }
        } else {
            new RuntimeException("Attempt to ask the IEnergyGrid to charge a non public energy store.").printStackTrace();
        }
    }

    @Override
    public void onUpdateTick() {
        if (!this.getInterests().isEmpty()) {
            double oldPower = this.lastStoredPower;
            this.lastStoredPower = this.getStoredPower();
            EnergyThreshold low = new EnergyThreshold(Math.min(oldPower, this.lastStoredPower), null);
            EnergyThreshold high = new EnergyThreshold(Math.max(oldPower, this.lastStoredPower), null);
            for (EnergyThreshold th : this.getInterests().subSet(low, true, high, true)) {
                ((EnergyWatcher)th.getWatcher()).post(this);
            }
        }
        if (this.updateInfinite) {
            this.updateInfinite();
        }
        this.avgDrainPerTick *= (this.AvgLength - 1.0) / this.AvgLength;
        this.avgInjectionPerTick *= (this.AvgLength - 1.0) / this.AvgLength;
        this.avgDrainPerTick += this.tickDrainPerTick / this.AvgLength;
        this.avgInjectionPerTick += this.tickInjectionPerTick / this.AvgLength;
        this.tickDrainPerTick = 0.0;
        this.tickInjectionPerTick = 0.0;
        boolean currentlyHasPower = false;
        if (this.infinite) {
            currentlyHasPower = true;
        } else if (this.drainPerTick > 1.0E-4) {
            double drained = this.extractAEPower(this.getIdlePowerUsage(), Actionable.MODULATE, PowerMultiplier.CONFIG);
            currentlyHasPower = drained >= this.drainPerTick - 0.001;
        } else {
            boolean bl = currentlyHasPower = this.extractAEPower(0.1, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0;
        }
        this.ticksSinceHasPowerChange = currentlyHasPower == this.hasPower ? ++this.ticksSinceHasPowerChange : 0L;
        this.hasPower = currentlyHasPower;
        if (this.hasPower && this.ticksSinceHasPowerChange > 30L) {
            this.publicPowerState(true, this.myGrid);
        } else if (!this.hasPower) {
            this.publicPowerState(false, this.myGrid);
        }
        ++this.availableTicksSinceUpdate;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier pm) {
        this.localSeen.clear();
        return pm.divide(this.extractAEPower(pm.multiply(amt), mode, this.localSeen));
    }

    @Override
    public double getIdlePowerUsage() {
        return this.drainPerTick + this.pgc.getChannelPowerUsage();
    }

    private void publicPowerState(boolean newState, IGrid grid) {
        if (this.publicHasPower == newState) {
            return;
        }
        this.publicHasPower = newState;
        ((Grid)this.myGrid).setImportantFlag(0, this.publicHasPower);
        grid.postEvent(new MENetworkPowerStatusChange());
    }

    private void refreshPower() {
        this.availableTicksSinceUpdate = 0;
        this.globalAvailablePower = 0.0;
        for (IAEPowerStorage p : this.providers) {
            this.globalAvailablePower += p.getAECurrentPower();
        }
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, Set<IEnergyGrid> seen) {
        if (!seen.add(this)) {
            return 0.0;
        }
        if (this.infinite) {
            if (mode == Actionable.MODULATE) {
                this.tickDrainPerTick += amt;
            }
            return amt;
        }
        double extractedPower = this.extra;
        if (mode == Actionable.SIMULATE) {
            if ((extractedPower += this.simulateExtract(extractedPower, amt)) < amt) {
                extractedPower = this.extractFromOtherGrids(amt, mode, seen, extractedPower);
            }
            return extractedPower;
        }
        this.extra = 0.0;
        if ((extractedPower = this.doExtract(extractedPower, amt)) > amt) {
            this.extra = extractedPower - amt;
            this.globalAvailablePower -= amt;
            this.tickDrainPerTick += amt;
            return amt;
        }
        if (extractedPower < amt) {
            extractedPower = this.extractFromOtherGrids(amt, mode, seen, extractedPower);
        }
        this.globalAvailablePower -= extractedPower;
        this.tickDrainPerTick += extractedPower;
        return extractedPower;
    }

    private double extractFromOtherGrids(double amt, Actionable mode, Set<IEnergyGrid> seen, double extractedPower) {
        IEnergyGridProvider energyGridProvider = (IEnergyGridProvider)this.lastGridProvider.get();
        if (energyGridProvider != null) {
            double extracted = energyGridProvider.extractAEPower(amt - extractedPower, mode, seen);
            if (extracted < 1.0E-8) {
                this.lastGridProvider = new WeakReference<Object>(null);
            } else {
                extractedPower += extracted;
            }
        }
        if (extractedPower < amt) {
            Iterator i = this.energyGridProviders.iterator();
            while (extractedPower < amt && i.hasNext()) {
                IEnergyGridProvider provider = (IEnergyGridProvider)i.next();
                double extracted = provider.extractAEPower(amt - extractedPower, mode, seen);
                if (!(extracted > 1.0E-8)) continue;
                this.lastGridProvider = new WeakReference<IEnergyGridProvider>(provider);
                extractedPower += extracted;
            }
        }
        return extractedPower;
    }

    @Override
    public double injectAEPower(double amt, Actionable mode, Set<IEnergyGrid> seen) {
        if (!seen.add(this)) {
            return 0.0;
        }
        double ignore = this.extra;
        amt += this.extra;
        if (mode == Actionable.SIMULATE) {
            Iterator<IAEPowerStorage> it = this.requesters.iterator();
            while (amt > 0.0 && it.hasNext()) {
                IAEPowerStorage node = it.next();
                amt = node.injectAEPower(amt, Actionable.SIMULATE);
            }
            Iterator i = this.energyGridProviders.iterator();
            while (amt > 0.0 && i.hasNext()) {
                amt = ((IEnergyGridProvider)i.next()).injectAEPower(amt, mode, seen);
            }
        } else {
            this.tickInjectionPerTick += amt - ignore;
            while (amt > 0.0 && !this.requesters.isEmpty()) {
                IAEPowerStorage node = this.getFirstRequester();
                amt = node.injectAEPower(amt, Actionable.MODULATE);
                if (!(amt > 0.0)) continue;
                this.requesters.remove(node);
                this.lastRequester = null;
            }
            Iterator i = this.energyGridProviders.iterator();
            while (amt > 0.0 && i.hasNext()) {
                IEnergyGridProvider what = (IEnergyGridProvider)i.next();
                HashSet<IEnergyGrid> listCopy = new HashSet<IEnergyGrid>(seen);
                double cannotHold = what.injectAEPower(amt, Actionable.SIMULATE, listCopy);
                what.injectAEPower(amt - cannotHold, mode, seen);
                amt = cannotHold;
            }
            this.extra = amt;
        }
        return Math.max(0.0, amt - this.buffer());
    }

    @Override
    public double getEnergyDemand(double maxRequired, Set<IEnergyGrid> seen) {
        if (!seen.add(this)) {
            return 0.0;
        }
        double required = this.buffer() - this.extra;
        Iterator<IAEPowerStorage> it = this.requesters.iterator();
        while (required < maxRequired && it.hasNext()) {
            IAEPowerStorage node = it.next();
            if (node.getPowerFlow() == AccessRestriction.READ) continue;
            required += Math.max(0.0, node.getAEMaxPower() - node.getAECurrentPower());
        }
        Iterator ix = this.energyGridProviders.iterator();
        while (required < maxRequired && ix.hasNext()) {
            IEnergyGridProvider node = (IEnergyGridProvider)ix.next();
            required += node.getEnergyDemand(maxRequired - required, seen);
        }
        return required;
    }

    @Override
    public void setHasInfiniteStore(boolean infinite) {
        this.updateInfinite = false;
        this.infinite = infinite;
    }

    @Override
    public boolean getHasInfiniteStore() {
        return this.infinite;
    }

    @Override
    public boolean calculateInfiniteStore(boolean currentInfinite, Set<IEnergyGrid> seen) {
        if (!seen.add(this)) {
            return currentInfinite;
        }
        if (!currentInfinite) {
            currentInfinite = this.providers.stream().anyMatch(IAEPowerStorage::isInfinite);
        }
        for (IEnergyGridProvider gridProvider : this.energyGridProviders) {
            currentInfinite |= gridProvider.calculateInfiniteStore(currentInfinite, seen);
        }
        return currentInfinite;
    }

    private double simulateExtract(double extractedPower, double amt) {
        Iterator<IAEPowerStorage> it = this.providers.iterator();
        while (extractedPower < amt && it.hasNext()) {
            IAEPowerStorage node = it.next();
            double req = amt - extractedPower;
            double newPower = node.extractAEPower(req, Actionable.SIMULATE, PowerMultiplier.ONE);
            extractedPower += newPower;
        }
        return extractedPower;
    }

    private double doExtract(double extractedPower, double amt) {
        while (extractedPower < amt && !this.providers.isEmpty()) {
            IAEPowerStorage node = this.getFirstProvider();
            double req = amt - extractedPower;
            double newPower = node.extractAEPower(req, Actionable.MODULATE, PowerMultiplier.ONE);
            extractedPower += newPower;
            if (!(newPower < req)) continue;
            this.providers.remove(node);
            this.lastProvider = null;
        }
        return extractedPower;
    }

    private IAEPowerStorage getFirstProvider() {
        if (this.lastProvider == null) {
            Iterator<IAEPowerStorage> i = this.providers.iterator();
            this.lastProvider = i.hasNext() ? i.next() : null;
        }
        return this.lastProvider;
    }

    @Override
    public double getAvgPowerUsage() {
        return this.avgDrainPerTick;
    }

    @Override
    public double getAvgPowerInjection() {
        return this.avgInjectionPerTick;
    }

    @Override
    public boolean isNetworkPowered() {
        return this.publicHasPower;
    }

    @Override
    public double injectPower(double amt, Actionable mode) {
        this.localSeen.clear();
        return this.injectAEPower(amt, mode, this.localSeen);
    }

    private IAEPowerStorage getFirstRequester() {
        if (this.lastRequester == null) {
            Iterator<IAEPowerStorage> i = this.requesters.iterator();
            this.lastRequester = i.hasNext() ? i.next() : null;
        }
        return this.lastRequester;
    }

    private double buffer() {
        return this.providers.isEmpty() ? 1000.0 : 0.0;
    }

    @Override
    public double getStoredPower() {
        if (this.availableTicksSinceUpdate > 90) {
            this.refreshPower();
        }
        return Math.max(0.0, this.globalAvailablePower);
    }

    @Override
    public double getMaxStoredPower() {
        return this.globalMaxPower;
    }

    @Override
    public double getEnergyDemand(double maxRequired) {
        this.localSeen.clear();
        return this.getEnergyDemand(maxRequired, this.localSeen);
    }

    private void updateInfinite() {
        HashSet<IEnergyGrid> grids = new HashSet<IEnergyGrid>();
        boolean infinite = this.calculateInfiniteStore(false, grids);
        for (IEnergyGrid grid : grids) {
            grid.setHasInfiniteStore(infinite);
        }
    }

    @Override
    public void removeNode(IGridNode node, IGridHost machine) {
        IEnergyWatcher myWatcher;
        IAEPowerStorage ps;
        if (machine instanceof IEnergyGridProvider) {
            this.energyGridProviders.remove((Object)machine);
            if (this.lastGridProvider.get() == machine) {
                this.lastGridProvider = new WeakReference<Object>(null);
            }
            this.updateInfinite = true;
        }
        GridNode gridNode = (GridNode)node;
        this.drainPerTick -= gridNode.getPreviousDraw();
        if (machine instanceof IAEPowerStorage && (ps = (IAEPowerStorage)((Object)machine)).isAEPublicPowerStorage()) {
            if (ps.getPowerFlow() != AccessRestriction.WRITE) {
                this.globalMaxPower -= ps.getAEMaxPower();
                this.globalAvailablePower -= ps.getAECurrentPower();
            }
            if (this.lastProvider == machine) {
                this.lastProvider = null;
            }
            if (this.lastRequester == machine) {
                this.lastRequester = null;
            }
            this.providers.remove(machine);
            this.requesters.remove(machine);
            if (((IAEPowerStorage)((Object)machine)).isInfinite()) {
                this.updateInfinite = true;
            }
        }
        if (machine instanceof IStackWatcherHost && (myWatcher = this.watchers.get(machine)) != null) {
            myWatcher.clear();
            this.watchers.remove(machine);
        }
    }

    @Override
    public void addNode(IGridNode node, IGridHost machine) {
        IAEPowerStorage ps;
        if (machine instanceof IEnergyGridProvider) {
            this.energyGridProviders.add((Object)((IEnergyGridProvider)((Object)machine)));
            this.updateInfinite |= !this.infinite;
        }
        GridNode gridNode = (GridNode)node;
        IGridBlock gb = gridNode.getGridBlock();
        gridNode.setPreviousDraw(gb.getIdlePowerUsage());
        this.drainPerTick += gridNode.getPreviousDraw();
        if (machine instanceof IAEPowerStorage && (ps = (IAEPowerStorage)((Object)machine)).isAEPublicPowerStorage()) {
            double max = ps.getAEMaxPower();
            double current = ps.getAECurrentPower();
            if (ps.getPowerFlow() != AccessRestriction.WRITE) {
                this.globalMaxPower += ps.getAEMaxPower();
            }
            if (current > 0.0 && ps.getPowerFlow() != AccessRestriction.WRITE) {
                this.globalAvailablePower += current;
                this.providers.add(ps);
            }
            if (current < max && ps.getPowerFlow() != AccessRestriction.READ) {
                this.requesters.add(ps);
            }
            if (((IAEPowerStorage)((Object)machine)).isInfinite()) {
                this.updateInfinite = true;
            }
        }
        if (machine instanceof IEnergyWatcherHost) {
            IEnergyWatcherHost swh = (IEnergyWatcherHost)((Object)machine);
            EnergyWatcher iw = new EnergyWatcher(this, swh);
            this.watchers.put(node, iw);
            swh.updateWatcher(iw);
        }
        this.myGrid.postEventTo(node, new MENetworkPowerStatusChange());
    }

    @Override
    public void onSplit(IGridStorage storageB) {
        this.updateInfinite = true;
        this.extra /= 2.0;
        storageB.dataObject().setDouble("extraEnergy", this.extra);
    }

    @Override
    public void onJoin(IGridStorage storageB) {
        this.updateInfinite = true;
        this.extra += storageB.dataObject().getDouble("extraEnergy");
    }

    @Override
    public void populateGridStorage(IGridStorage storage) {
        storage.dataObject().setDouble("extraEnergy", this.extra);
    }

    public TreeSet<EnergyThreshold> getInterests() {
        return this.interests;
    }
}

