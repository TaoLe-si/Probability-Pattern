/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.FMLCommonHandler
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 */
package appeng.crafting;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.MECraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.Platform;
import cpw.mods.fml.common.FMLCommonHandler;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class CraftingTreeProcess {
    private final CraftingTreeNode parent;
    final ICraftingPatternDetails details;
    private final CraftingJob job;
    private final Map<CraftingTreeNode, Long> nodes = new HashMap<CraftingTreeNode, Long>();
    private final int depth;
    boolean possible = true;
    private World world;
    private long crafts = 0L;
    private boolean containerItems;
    private boolean limitQty;
    private boolean fullSimulation;
    private long bytes = 0L;

    public CraftingTreeProcess(ICraftingGrid cc, CraftingJob job, ICraftingPatternDetails details, CraftingTreeNode craftingTreeNode, int depth) {
        this.parent = craftingTreeNode;
        this.details = details;
        this.job = job;
        this.depth = depth;
        World world = job.getWorld();
        if (details.isCraftable()) {
            int x;
            IAEItemStack[] list = details.getInputs();
            InventoryCrafting ic = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
            IAEItemStack[] is = details.getInputs();
            for (x = 0; x < ic.getSizeInventory(); ++x) {
                ic.setInventorySlotContents(x, is[x] == null ? null : is[x].getItemStack());
            }
            FMLCommonHandler.instance().firePlayerCraftingEvent(Platform.getPlayer((WorldServer)world), details.getOutput(ic, world), (IInventory)ic);
            for (x = 0; x < ic.getSizeInventory(); ++x) {
                ItemStack g = ic.getStackInSlot(x);
                if (g == null || g.stackSize <= 1) continue;
                this.fullSimulation = true;
            }
            for (IAEItemStack part : details.getCondensedInputs()) {
                ItemStack g = part.getItemStack();
                boolean isAnInput = false;
                for (IAEItemStack a : details.getCondensedOutputs()) {
                    if (g == null || a == null || !a.equals(g)) continue;
                    isAnInput = true;
                    break;
                }
                if (isAnInput) {
                    this.limitQty = true;
                }
                if (!g.getItem().hasContainerItem(g)) continue;
                this.containerItems = true;
                this.limitQty = true;
            }
            boolean complicated = false;
            if (this.containerItems) {
                for (int x2 = 0; x2 < list.length; ++x2) {
                    IAEItemStack part = list[x2];
                    if (part == null) continue;
                    this.nodes.put(new CraftingTreeNode(cc, job, part.copy(), this, x2, depth + 1), part.getStackSize());
                }
            } else {
                block5: for (IAEItemStack part : details.getCondensedInputs()) {
                    for (int x3 = 0; x3 < list.length; ++x3) {
                        IAEItemStack comparePart = list[x3];
                        if (part == null || !part.equals(comparePart)) continue;
                        this.nodes.put(new CraftingTreeNode(cc, job, part.copy(), this, x3, depth + 1), part.getStackSize());
                        continue block5;
                    }
                }
            }
        } else {
            for (IAEItemStack part : details.getCondensedInputs()) {
                ItemStack g = part.getItemStack();
                boolean isAnInput = false;
                for (IAEItemStack a : details.getCondensedOutputs()) {
                    if (g == null || a == null || !a.equals(g)) continue;
                    isAnInput = true;
                    break;
                }
                if (!isAnInput) continue;
                this.limitQty = true;
            }
            for (IAEItemStack part : details.getCondensedInputs()) {
                this.nodes.put(new CraftingTreeNode(cc, job, part.copy(), this, -1, depth + 1), part.getStackSize());
            }
        }
    }

    boolean notRecursive(ICraftingPatternDetails details) {
        return this.parent == null || this.parent.notRecursive(details);
    }

    long getTimes(long remaining, long stackSize) {
        if (this.limitQty || this.fullSimulation) {
            return 1L;
        }
        return remaining / stackSize + (long)(remaining % stackSize != 0L ? 1 : 0);
    }

    void request(MECraftingInventory inv, long i, BaseActionSource src) throws CraftBranchFailure, InterruptedException {
        this.job.handlePausing();
        if (this.fullSimulation) {
            InventoryCrafting ic = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
            for (Map.Entry<CraftingTreeNode, Long> entry : this.nodes.entrySet()) {
                IAEItemStack item = entry.getKey().getStack(entry.getValue());
                IAEItemStack stack = entry.getKey().request(inv, item.getStackSize(), src);
                ic.setInventorySlotContents(entry.getKey().getSlot(), stack.getItemStack());
            }
            FMLCommonHandler.instance().firePlayerCraftingEvent(Platform.getPlayer((WorldServer)this.world), this.details.getOutput(ic, this.world), (IInventory)ic);
            for (int x = 0; x < ic.getSizeInventory(); ++x) {
                ItemStack is = ic.getStackInSlot(x);
                is = Platform.getContainerItem(is);
                IAEItemStack o = AEApi.instance().storage().createItemStack(is);
                if (o == null) continue;
                ++this.bytes;
                inv.injectItems(o, Actionable.MODULATE, src);
            }
        } else {
            for (Map.Entry<CraftingTreeNode, Long> entry : this.nodes.entrySet()) {
                IAEItemStack item = entry.getKey().getStack(entry.getValue());
                IAEItemStack stack = entry.getKey().request(inv, item.getStackSize() * i, src);
                if (!this.containerItems) continue;
                ItemStack is = Platform.getContainerItem(stack.getItemStack());
                IAEItemStack o = AEApi.instance().storage().createItemStack(is);
                if (o == null) continue;
                ++this.bytes;
                inv.injectItems(o, Actionable.MODULATE, src);
            }
        }
        for (IAEItemStack out : this.details.getCondensedOutputs()) {
            IAEItemStack o = out.copy();
            o.setStackSize(o.getStackSize() * i);
            inv.injectItems(o, Actionable.MODULATE, src);
        }
        this.crafts += i;
    }

    void dive(CraftingJob job) {
        job.addTask(this.getAmountCrafted(this.parent.getStack(1L)), this.crafts, this.details, this.depth);
        for (CraftingTreeNode pro : this.nodes.keySet()) {
            pro.dive(job);
        }
        job.addBytes(8L + this.crafts + this.bytes);
    }

    IAEItemStack getAmountCrafted(IAEItemStack what2) {
        for (IAEItemStack is : this.details.getCondensedOutputs()) {
            if (!is.equals(what2)) continue;
            what2 = what2.copy();
            what2.setStackSize(is.getStackSize());
            return what2;
        }
        for (IAEItemStack is : this.details.getCondensedOutputs()) {
            if (is.getItem() != what2.getItem() || !is.getItem().isDamageable() && is.getItemDamage() != what2.getItemDamage()) continue;
            what2 = is.copy();
            what2.setStackSize(is.getStackSize());
            return what2;
        }
        throw new IllegalStateException("Crafting Tree construction failed.");
    }

    void setSimulate() {
        this.crafts = 0L;
        this.bytes = 0L;
        for (CraftingTreeNode pro : this.nodes.keySet()) {
            pro.setSimulate();
        }
    }

    void setJob(MECraftingInventory storage, CraftingCPUCluster craftingCPUCluster, BaseActionSource src) throws CraftBranchFailure {
        craftingCPUCluster.addCrafting(this.details, this.crafts);
        for (CraftingTreeNode pro : this.nodes.keySet()) {
            pro.setJob(storage, craftingCPUCluster, src);
        }
    }

    void getPlan(IItemList<IAEItemStack> plan) {
        for (IAEItemStack i : this.details.getOutputs()) {
            i = i.copy();
            i.setCountRequestable(i.getStackSize() * this.crafts);
            plan.addRequestable(i);
        }
        for (CraftingTreeNode pro : this.nodes.keySet()) {
            pro.getPlan(plan);
        }
    }
}

