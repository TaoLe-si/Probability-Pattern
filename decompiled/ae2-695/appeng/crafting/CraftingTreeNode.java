/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.world.World
 */
package appeng.crafting;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.MECraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.world.World;

public class CraftingTreeNode {
    private final int slot;
    private final CraftingJob job;
    private final IItemList<IAEItemStack> used = AEApi.instance().storage().createItemList();
    private final CraftingTreeProcess parent;
    private final World world;
    private final IAEItemStack what;
    private final ArrayList<CraftingTreeProcess> nodes = new ArrayList();
    private int bytes = 0;
    private boolean canEmit = false;
    private long missing = 0L;
    private long howManyEmitted = 0L;
    private boolean exhausted = false;
    private boolean sim;

    public CraftingTreeNode(ICraftingGrid cc, CraftingJob job, IAEItemStack wat, CraftingTreeProcess par, int slot, int depth) {
        this.what = wat;
        this.parent = par;
        this.slot = slot;
        this.world = job.getWorld();
        this.job = job;
        this.sim = false;
        this.canEmit = cc.canEmitFor(this.what);
        if (this.canEmit) {
            return;
        }
        for (ICraftingPatternDetails details : cc.getCraftingFor(this.what, this.parent == null ? null : this.parent.details, slot, this.world)) {
            if (this.parent != null && !this.parent.notRecursive(details)) continue;
            this.nodes.add(new CraftingTreeProcess(cc, job, details, this, depth + 1));
        }
    }

    boolean notRecursive(ICraftingPatternDetails details) {
        IAEItemStack[] o;
        for (IAEItemStack i : o = details.getCondensedOutputs()) {
            if (!i.equals(this.what)) continue;
            return false;
        }
        for (IAEItemStack i : o = details.getCondensedInputs()) {
            if (!i.equals(this.what)) continue;
            return false;
        }
        if (this.parent == null) {
            return true;
        }
        return this.parent.notRecursive(details);
    }

    IAEItemStack request(MECraftingInventory inv, long l, BaseActionSource src) throws CraftBranchFailure, InterruptedException {
        IAEItemStack is;
        IAEItemStack available;
        Collection<Object> itemList;
        this.job.handlePausing();
        ArrayList<IAEItemStack> thingsUsed = new ArrayList<IAEItemStack>();
        this.what.setStackSize(l);
        if (this.getSlot() >= 0 && this.parent != null && this.parent.details.isCraftable()) {
            IItemList<IAEItemStack> inventoryList = inv.getItemList();
            if (this.parent.details.canSubstitute()) {
                itemList = inventoryList.findFuzzy(this.what, FuzzyMode.IGNORE_ALL);
            } else {
                itemList = Lists.newArrayList();
                IAEItemStack iAEItemStack = inventoryList.findPrecise(this.what);
                if (iAEItemStack != null) {
                    itemList.add(iAEItemStack);
                }
            }
            for (IAEItemStack iAEItemStack : itemList) {
                if (!this.parent.details.isValidItemForSlot(this.getSlot(), iAEItemStack.getItemStack(), this.world)) continue;
                IAEItemStack iAEItemStack2 = iAEItemStack.copy();
                iAEItemStack2.setStackSize(l);
                available = inv.extractItems(iAEItemStack2, Actionable.MODULATE, src);
                if (available == null) continue;
                if (!this.exhausted && (is = this.job.checkUse(available)) != null) {
                    thingsUsed.add(is.copy());
                    this.used.add(is);
                }
                this.bytes = (int)((long)this.bytes + available.getStackSize());
                if ((l -= available.getStackSize()) != 0L) continue;
                return available;
            }
        } else {
            if (this.parent != null && this.parent.details.canSubstitute()) {
                itemList = inv.getItemList().findFuzzy(this.what, FuzzyMode.IGNORE_ALL);
            } else {
                itemList = Lists.newArrayList();
                IAEItemStack item = inv.getItemList().findPrecise(this.what);
                if (item != null) {
                    itemList.add(item);
                }
            }
            for (IAEItemStack iAEItemStack : itemList) {
                IAEItemStack iAEItemStack3 = iAEItemStack.copy();
                iAEItemStack3.setStackSize(this.what.getStackSize());
                available = inv.extractItems(iAEItemStack3, Actionable.MODULATE, src);
                if (available == null) continue;
                if (!this.exhausted && (is = this.job.checkUse(available)) != null) {
                    thingsUsed.add(is.copy());
                    this.used.add(is);
                }
                this.bytes = (int)((long)this.bytes + available.getStackSize());
                if ((l -= available.getStackSize()) != 0L) continue;
                return available;
            }
        }
        if (this.canEmit) {
            IAEItemStack wat = this.what.copy();
            wat.setStackSize(l);
            this.howManyEmitted = wat.getStackSize();
            this.bytes = (int)((long)this.bytes + wat.getStackSize());
            return wat;
        }
        this.exhausted = true;
        if (this.nodes.size() == 1) {
            CraftingTreeProcess pro = this.nodes.get(0);
            while (pro.possible && l > 0L) {
                IAEItemStack madeWhat = pro.getAmountCrafted(this.what);
                pro.request(inv, pro.getTimes(l, madeWhat.getStackSize()), src);
                madeWhat.setStackSize(l);
                IAEItemStack iAEItemStack = inv.extractItems(madeWhat, Actionable.MODULATE, src);
                if (iAEItemStack != null) {
                    this.bytes = (int)((long)this.bytes + iAEItemStack.getStackSize());
                    if ((l -= iAEItemStack.getStackSize()) > 0L) continue;
                    return iAEItemStack;
                }
                pro.possible = false;
            }
        } else if (this.nodes.size() > 1) {
            for (CraftingTreeProcess pro : this.nodes) {
                try {
                    while (pro.possible && l > 0L) {
                        MECraftingInventory mECraftingInventory = new MECraftingInventory(inv, true, true, true);
                        pro.request(mECraftingInventory, 1L, src);
                        IAEItemStack iAEItemStack = mECraftingInventory.extractItems((IAEItemStack)pro.getAmountCrafted(this.what).setStackSize(l), Actionable.MODULATE, src);
                        if (iAEItemStack != null) {
                            if (!mECraftingInventory.commit(src)) {
                                throw new CraftBranchFailure(this.what, l);
                            }
                            this.bytes = (int)((long)this.bytes + iAEItemStack.getStackSize());
                            if ((l -= iAEItemStack.getStackSize()) > 0L) continue;
                            return iAEItemStack;
                        }
                        pro.possible = false;
                    }
                }
                catch (CraftBranchFailure craftBranchFailure) {
                    pro.possible = true;
                }
            }
        }
        if (this.sim) {
            this.missing += l;
            this.bytes = (int)((long)this.bytes + l);
            IAEItemStack rv = this.what.copy();
            rv.setStackSize(l);
            return rv;
        }
        for (IAEItemStack o : thingsUsed) {
            this.job.refund(o.copy());
            o.setStackSize(-o.getStackSize());
            this.used.add(o);
        }
        throw new CraftBranchFailure(this.what, l);
    }

    void dive(CraftingJob job) {
        if (this.missing > 0L) {
            job.addMissing(this.getStack(this.missing));
        }
        job.addBytes(8 + this.bytes);
        for (CraftingTreeProcess pro : this.nodes) {
            pro.dive(job);
        }
    }

    IAEItemStack getStack(long size) {
        IAEItemStack is = this.what.copy();
        is.setStackSize(size);
        return is;
    }

    void setSimulate() {
        this.sim = true;
        this.missing = 0L;
        this.bytes = 0;
        this.used.resetStatus();
        this.exhausted = false;
        for (CraftingTreeProcess pro : this.nodes) {
            pro.setSimulate();
        }
    }

    public void setJob(MECraftingInventory storage, CraftingCPUCluster craftingCPUCluster, BaseActionSource src) {
        for (IAEItemStack i : this.used) {
            IAEItemStack ex = storage.extractItems(i, Actionable.MODULATE, src);
            if (ex == null || ex.getStackSize() != i.getStackSize()) {
                throw new CraftBranchFailure(i, i.getStackSize());
            }
            craftingCPUCluster.addStorage(ex);
        }
        if (this.howManyEmitted > 0L) {
            IAEItemStack i = this.what.copy();
            i.setStackSize(this.howManyEmitted);
            craftingCPUCluster.addEmitable(i);
        }
        for (CraftingTreeProcess pro : this.nodes) {
            pro.setJob(storage, craftingCPUCluster, src);
        }
    }

    void getPlan(IItemList<IAEItemStack> plan) {
        if (this.missing > 0L) {
            IAEItemStack o = this.what.copy();
            o.setStackSize(this.missing);
            plan.add(o);
        }
        if (this.howManyEmitted > 0L) {
            IAEItemStack i = this.what.copy();
            i.setCountRequestable(this.howManyEmitted);
            plan.addRequestable(i);
        }
        for (IAEItemStack i : this.used) {
            plan.add(i.copy());
        }
        for (CraftingTreeProcess pro : this.nodes) {
            pro.getPlan(plan);
        }
    }

    int getSlot() {
        return this.slot;
    }
}

