/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.World
 */
package appeng.me.cache.helpers;

import appeng.api.networking.IGridNode;
import appeng.me.cache.helpers.TunnelConnection;
import appeng.parts.p2p.PartP2PTunnelME;
import appeng.util.IWorldCallable;
import java.util.HashMap;
import net.minecraft.world.World;

public class Connections
implements IWorldCallable<Void> {
    private final HashMap<IGridNode, TunnelConnection> connections = new HashMap();
    private final PartP2PTunnelME me;
    private boolean create = false;
    private boolean destroy = false;

    public Connections(PartP2PTunnelME o) {
        this.me = o;
    }

    @Override
    public Void call(World world) throws Exception {
        this.me.updateConnections(this);
        return null;
    }

    public void markDestroy() {
        this.setCreate(false);
        this.setDestroy(true);
    }

    public void markCreate() {
        this.setCreate(true);
        this.setDestroy(false);
    }

    public HashMap<IGridNode, TunnelConnection> getConnections() {
        return this.connections;
    }

    public boolean isCreate() {
        return this.create;
    }

    private void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isDestroy() {
        return this.destroy;
    }

    private void setDestroy(boolean destroy) {
        this.destroy = destroy;
    }
}

