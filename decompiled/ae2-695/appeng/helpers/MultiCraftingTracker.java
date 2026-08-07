/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package appeng.helpers;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AELog;
import appeng.helpers.NonNullArrayIterator;
import appeng.util.InventoryAdaptor;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class MultiCraftingTracker {
    private final int size;
    private final ICraftingRequester owner;
    private Future<ICraftingJob>[] jobs = null;
    private ICraftingLink[] links = null;

    public MultiCraftingTracker(ICraftingRequester o, int size) {
        this.owner = o;
        this.size = size;
    }

    public void readFromNBT(NBTTagCompound extra) {
        for (int x = 0; x < this.size; ++x) {
            NBTTagCompound link = extra.getCompoundTag("links-" + x);
            if (link == null || link.hasNoTags()) continue;
            try {
                this.setLink(x, AEApi.instance().storage().loadCraftingLink(link, this.owner));
                continue;
            }
            catch (Throwable t) {
                AELog.error("Could not load crafting link! Slot=%s, Tag=%s, Owner=%s", x, link, this.owner);
                AELog.error(t);
            }
        }
    }

    public void writeToNBT(NBTTagCompound extra) {
        for (int x = 0; x < this.size; ++x) {
            ICraftingLink link = this.getLink(x);
            if (link == null) continue;
            NBTTagCompound ln = new NBTTagCompound();
            link.writeToNBT(ln);
            extra.setTag("links-" + x, (NBTBase)ln);
        }
    }

    public boolean handleCrafting(int x, long itemToCraft, IAEItemStack ais, InventoryAdaptor d, World w, IGrid g, ICraftingGrid cg, BaseActionSource mySrc) {
        block9: {
            if (ais != null && d.simulateAdd(ais.getItemStack()) == null) {
                Future<ICraftingJob> craftingJob = this.getJob(x);
                if (this.getLink(x) != null) {
                    return false;
                }
                if (craftingJob != null) {
                    try {
                        ICraftingJob job = null;
                        if (craftingJob.isDone()) {
                            job = craftingJob.get();
                        }
                        if (job == null) break block9;
                        ICraftingLink link = cg.submitJob(job, this.owner, null, false, mySrc);
                        this.setJob(x, null);
                        if (link != null) {
                            this.setLink(x, link);
                            return true;
                        }
                    }
                    catch (InterruptedException | ExecutionException job) {}
                } else if (this.getLink(x) == null) {
                    IAEItemStack aisC = ais.copy();
                    aisC.setStackSize(itemToCraft);
                    this.setJob(x, cg.beginCraftingJob(w, g, mySrc, aisC, null));
                }
            }
        }
        return false;
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        if (this.links == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(new NonNullArrayIterator<ICraftingLink>(this.links));
    }

    public void jobStateChange(ICraftingLink link) {
        if (this.links != null) {
            for (int x = 0; x < this.links.length; ++x) {
                if (this.links[x] != link) continue;
                this.setLink(x, null);
                return;
            }
        }
    }

    int getSlot(ICraftingLink link) {
        if (this.links != null) {
            for (int x = 0; x < this.links.length; ++x) {
                if (this.links[x] != link) continue;
                return x;
            }
        }
        return -1;
    }

    void cancel() {
        if (this.links != null) {
            for (ICraftingLink iCraftingLink : this.links) {
                if (iCraftingLink == null) continue;
                iCraftingLink.cancel();
            }
            this.links = null;
        }
        if (this.jobs != null) {
            for (Future<ICraftingJob> future : this.jobs) {
                if (future == null) continue;
                future.cancel(true);
            }
            this.jobs = null;
        }
    }

    boolean isBusy(int slot) {
        return this.getLink(slot) != null || this.getJob(slot) != null;
    }

    private ICraftingLink getLink(int slot) {
        if (this.links == null) {
            return null;
        }
        return this.links[slot];
    }

    private void setLink(int slot, ICraftingLink l) {
        if (this.links == null) {
            this.links = new ICraftingLink[this.size];
        }
        this.links[slot] = l;
        boolean hasStuff = false;
        for (int x = 0; x < this.links.length; ++x) {
            ICraftingLink g = this.links[x];
            if (g == null || g.isCanceled() || g.isDone()) {
                this.links[x] = null;
                continue;
            }
            hasStuff = true;
        }
        if (!hasStuff) {
            this.links = null;
        }
    }

    private Future<ICraftingJob> getJob(int slot) {
        if (this.jobs == null) {
            return null;
        }
        return this.jobs[slot];
    }

    private void setJob(int slot, Future<ICraftingJob> l) {
        if (this.jobs == null) {
            this.jobs = new Future[this.size];
        }
        this.jobs[slot] = l;
        boolean hasStuff = false;
        for (Future<ICraftingJob> job : this.jobs) {
            if (job == null) continue;
            hasStuff = true;
            break;
        }
        if (!hasStuff) {
            this.jobs = null;
        }
    }
}

