/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.util.iterators;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import java.util.Iterator;
import net.minecraftforge.common.util.ForgeDirection;

public final class ProxyNodeIterator
implements Iterator<IGridNode> {
    private final Iterator<IGridHost> hosts;

    public ProxyNodeIterator(Iterator<IGridHost> hosts) {
        this.hosts = hosts;
    }

    @Override
    public boolean hasNext() {
        return this.hosts.hasNext();
    }

    @Override
    public IGridNode next() {
        IGridHost host = this.hosts.next();
        return host.getGridNode(ForgeDirection.UNKNOWN);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

