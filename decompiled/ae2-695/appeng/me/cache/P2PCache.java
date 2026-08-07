/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.LinkedHashMultimap
 *  com.google.common.collect.Multimap
 */
package appeng.me.cache;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridCache;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.ticking.ITickManager;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cache.helpers.TunnelCollection;
import appeng.parts.p2p.PartP2PInterface;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelME;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;

public class P2PCache
implements IGridCache {
    private final IGrid myGrid;
    private final HashMap<Long, PartP2PTunnel> inputs = new HashMap();
    private final Multimap<Long, PartP2PTunnel> outputs = LinkedHashMultimap.create();
    private final TunnelCollection NullColl = new TunnelCollection(null, null);

    public P2PCache(IGrid g) {
        this.myGrid = g;
    }

    @MENetworkEventSubscribe
    public void bootComplete(MENetworkBootingStatusChange bootStatus) {
        ITickManager tm = (ITickManager)this.myGrid.getCache(ITickManager.class);
        for (PartP2PTunnel me : this.inputs.values()) {
            if (!(me instanceof PartP2PTunnelME)) continue;
            tm.wakeDevice(me.getGridNode());
        }
    }

    @MENetworkEventSubscribe
    public void bootComplete(MENetworkPowerStatusChange power) {
        ITickManager tm = (ITickManager)this.myGrid.getCache(ITickManager.class);
        for (PartP2PTunnel me : this.inputs.values()) {
            if (!(me instanceof PartP2PTunnelME)) continue;
            tm.wakeDevice(me.getGridNode());
        }
    }

    @Override
    public void onUpdateTick() {
    }

    @Override
    public void removeNode(IGridNode node, IGridHost machine) {
        if (machine instanceof PartP2PTunnel) {
            PartP2PTunnel t = (PartP2PTunnel)machine;
            if (machine instanceof PartP2PTunnelME && !node.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
                return;
            }
            if (t.isOutput()) {
                this.outputs.remove((Object)t.getFrequency(), (Object)t);
            } else {
                this.inputs.remove(t.getFrequency());
            }
            this.updateTunnel(t.getFrequency(), false);
        }
    }

    @Override
    public void addNode(IGridNode node, IGridHost machine) {
        if (machine instanceof PartP2PTunnel) {
            PartP2PTunnel t = (PartP2PTunnel)machine;
            if (machine instanceof PartP2PTunnelME && !node.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
                return;
            }
            if (t.isOutput()) {
                this.outputs.put((Object)t.getFrequency(), (Object)t);
            } else {
                this.inputs.put(t.getFrequency(), t);
            }
            this.updateTunnel(t.getFrequency(), false);
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

    private void updateTunnel(long freq, boolean configChange) {
        boolean pausedRebuild = false;
        if (this.inputs.get(freq) instanceof PartP2PInterface) {
            CraftingGridCache.pauseRebuilds();
            pausedRebuild = true;
        }
        for (PartP2PTunnel p : this.outputs.get((Object)freq)) {
            if (configChange) {
                p.onTunnelConfigChange();
            }
            p.onTunnelNetworkChange();
        }
        PartP2PTunnel in = this.inputs.get(freq);
        if (in != null) {
            if (configChange) {
                in.onTunnelConfigChange();
            }
            in.onTunnelNetworkChange();
        }
        if (pausedRebuild) {
            CraftingGridCache.unpauseRebuilds();
        }
    }

    public void updateFreq(PartP2PTunnel t, long newFrequency) {
        this.unbind(t);
        t.setFrequency(newFrequency);
        if (t.isOutput()) {
            this.outputs.put((Object)t.getFrequency(), (Object)t);
        } else {
            this.inputs.put(t.getFrequency(), t);
        }
        this.updateTunnel(t.getFrequency(), true);
    }

    public void unbind(PartP2PTunnel t) {
        if (this.outputs.containsValue((Object)t)) {
            this.outputs.remove((Object)t.getFrequency(), (Object)t);
        }
        if (this.inputs.containsValue(t)) {
            this.inputs.remove(t.getFrequency());
        }
        t.setFrequency(0L);
    }

    public TunnelCollection<PartP2PTunnel> getOutputs(long freq, Class<? extends PartP2PTunnel> c) {
        PartP2PTunnel in = this.inputs.get(freq);
        if (in == null) {
            return this.NullColl;
        }
        TunnelCollection<PartP2PTunnel> out = this.inputs.get(freq).getCollection(this.outputs.get((Object)freq), c);
        if (out == null) {
            return this.NullColl;
        }
        return out;
    }

    public PartP2PTunnel getInput(long freq) {
        return this.inputs.get(freq);
    }
}

