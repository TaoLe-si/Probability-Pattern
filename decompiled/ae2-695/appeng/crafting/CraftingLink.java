/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package appeng.crafting;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingLinkNexus;
import net.minecraft.nbt.NBTTagCompound;

public class CraftingLink
implements ICraftingLink {
    private final ICraftingRequester req;
    private final ICraftingCPU cpu;
    private final String CraftID;
    private final boolean standalone;
    private boolean canceled = false;
    private boolean done = false;
    private CraftingLinkNexus tie;

    public CraftingLink(NBTTagCompound data, ICraftingRequester req) {
        this.CraftID = data.getString("CraftID");
        this.setCanceled(data.getBoolean("canceled"));
        this.setDone(data.getBoolean("done"));
        this.standalone = data.getBoolean("standalone");
        if (!data.hasKey("req") || !data.getBoolean("req")) {
            throw new IllegalStateException("Invalid Crafting Link for Object");
        }
        this.req = req;
        this.cpu = null;
    }

    public CraftingLink(NBTTagCompound data, ICraftingCPU cpu) {
        this.CraftID = data.getString("CraftID");
        this.setCanceled(data.getBoolean("canceled"));
        this.setDone(data.getBoolean("done"));
        this.standalone = data.getBoolean("standalone");
        if (!data.hasKey("req") || data.getBoolean("req")) {
            throw new IllegalStateException("Invalid Crafting Link for Object");
        }
        this.cpu = cpu;
        this.req = null;
    }

    @Override
    public boolean isCanceled() {
        if (this.canceled) {
            return true;
        }
        if (this.done) {
            return false;
        }
        if (this.tie == null) {
            return false;
        }
        return this.tie.isCanceled();
    }

    @Override
    public boolean isDone() {
        if (this.done) {
            return true;
        }
        if (this.canceled) {
            return false;
        }
        if (this.tie == null) {
            return false;
        }
        return this.tie.isDone();
    }

    @Override
    public void cancel() {
        if (this.done) {
            return;
        }
        this.setCanceled(true);
        if (this.tie != null) {
            this.tie.cancel();
        }
        this.tie = null;
    }

    @Override
    public boolean isStandalone() {
        return this.standalone;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("CraftID", this.CraftID);
        tag.setBoolean("canceled", this.isCanceled());
        tag.setBoolean("done", this.isDone());
        tag.setBoolean("standalone", this.standalone);
        tag.setBoolean("req", this.getRequester() != null);
    }

    @Override
    public String getCraftingID() {
        return this.CraftID;
    }

    public void setNexus(CraftingLinkNexus n) {
        if (this.tie != null) {
            this.tie.remove(this);
        }
        if (this.isCanceled() && n != null) {
            n.cancel();
            this.tie = null;
            return;
        }
        this.tie = n;
        if (n != null) {
            n.add(this);
        }
    }

    public IAEItemStack injectItems(IAEItemStack input, Actionable mode) {
        if (this.tie == null || this.tie.getRequest() == null || this.tie.getRequest().getRequester() == null) {
            return input;
        }
        return this.tie.getRequest().getRequester().injectCraftedItems(this.tie.getRequest(), input, mode);
    }

    public void markDone() {
        if (this.tie != null) {
            this.tie.markDone();
        }
    }

    void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    ICraftingRequester getRequester() {
        return this.req;
    }

    ICraftingCPU getCpu() {
        return this.cpu;
    }

    void setDone(boolean done) {
        this.done = done;
    }
}

