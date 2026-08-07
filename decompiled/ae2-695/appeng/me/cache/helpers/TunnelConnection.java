/*
 * Decompiled with CFR 0.152.
 */
package appeng.me.cache.helpers;

import appeng.api.networking.IGridConnection;
import appeng.parts.p2p.PartP2PTunnelME;

public class TunnelConnection {
    private final PartP2PTunnelME tunnel;
    private final IGridConnection c;

    public TunnelConnection(PartP2PTunnelME t, IGridConnection con) {
        this.tunnel = t;
        this.c = con;
    }

    public IGridConnection getConnection() {
        return this.c;
    }

    public PartP2PTunnelME getTunnel() {
        return this.tunnel;
    }
}

