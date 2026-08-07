/*
 * Decompiled with CFR 0.152.
 */
package appeng.crafting;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.crafting.CraftingLink;
import appeng.me.cache.CraftingGridCache;

public class CraftingLinkNexus {
    private final String craftID;
    private boolean canceled = false;
    private boolean done = false;
    private int tickOfDeath = 0;
    private CraftingLink req;
    private CraftingLink cpu;

    public CraftingLinkNexus(String craftID) {
        this.craftID = craftID;
    }

    public boolean isDead(IGrid g, CraftingGridCache craftingGridCache) {
        if (this.canceled || this.done) {
            return true;
        }
        if (this.getRequest() == null || this.cpu == null) {
            ++this.tickOfDeath;
        } else {
            IGridNode actionableNode;
            boolean hasCpu = craftingGridCache.hasCpu(this.cpu.getCpu());
            ICraftingRequester requester = this.getRequest().getRequester();
            boolean hasMachine = false;
            if (requester != null && (actionableNode = requester.getActionableNode()) != null) {
                boolean bl = hasMachine = actionableNode.getGrid() == g;
            }
            this.tickOfDeath = hasCpu && hasMachine ? 0 : (this.tickOfDeath += 60);
        }
        if (this.tickOfDeath > 60) {
            this.cancel();
            return true;
        }
        return false;
    }

    void cancel() {
        this.canceled = true;
        if (this.getRequest() != null) {
            this.getRequest().setCanceled(true);
            if (this.getRequest().getRequester() != null) {
                this.getRequest().getRequester().jobStateChange(this.getRequest());
            }
        }
        if (this.cpu != null) {
            this.cpu.setCanceled(true);
        }
    }

    void remove(CraftingLink craftingLink) {
        if (this.getRequest() == craftingLink) {
            this.setRequest(null);
        } else if (this.cpu == craftingLink) {
            this.cpu = null;
        }
    }

    void add(CraftingLink craftingLink) {
        if (craftingLink.getCpu() != null) {
            this.cpu = craftingLink;
        } else if (craftingLink.getRequester() != null) {
            this.setRequest(craftingLink);
        }
    }

    boolean isCanceled() {
        return this.canceled;
    }

    boolean isDone() {
        return this.done;
    }

    void markDone() {
        this.done = true;
        if (this.getRequest() != null) {
            this.getRequest().setDone(true);
            if (this.getRequest().getRequester() != null) {
                this.getRequest().getRequester().jobStateChange(this.getRequest());
            }
        }
        if (this.cpu != null) {
            this.cpu.setDone(true);
        }
    }

    public boolean isMachine(IGridHost machine) {
        return this.getRequest() == machine;
    }

    public void removeNode() {
        if (this.getRequest() != null) {
            this.getRequest().setNexus(null);
        }
        this.setRequest(null);
        this.tickOfDeath = 0;
    }

    public CraftingLink getRequest() {
        return this.req;
    }

    public void setRequest(CraftingLink req) {
        this.req = req;
    }
}

