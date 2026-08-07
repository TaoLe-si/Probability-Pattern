/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.IRecipe
 */
package appeng.container.slot;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.container.slot.AppEngCraftingSlot;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.InventoryAction;
import appeng.items.storage.ItemViewCell;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorPlayerHand;
import appeng.util.item.AEItemStack;
import appeng.util.prioitylist.IPartitionList;
import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public class SlotCraftingTerm
extends AppEngCraftingSlot {
    private final IInventory craftInv;
    private final IInventory pattern;
    private final BaseActionSource mySrc;
    private final IEnergySource energySrc;
    private final IStorageMonitorable storage;
    private final IContainerCraftingPacket container;

    public SlotCraftingTerm(EntityPlayer player, BaseActionSource mySrc, IEnergySource energySrc, IStorageMonitorable storage, IInventory cMatrix, IInventory secondMatrix, IInventory output, int x, int y, IContainerCraftingPacket ccp) {
        super(player, cMatrix, output, 0, x, y);
        this.energySrc = energySrc;
        this.storage = storage;
        this.mySrc = mySrc;
        this.pattern = cMatrix;
        this.craftInv = secondMatrix;
        this.container = ccp;
    }

    public IInventory getCraftingMatrix() {
        return this.craftInv;
    }

    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        return false;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer p, ItemStack is) {
    }

    public void doClick(InventoryAction action, EntityPlayer who) {
        ItemStack res;
        if (this.getStack() == null) {
            return;
        }
        if (Platform.isClient()) {
            return;
        }
        IMEMonitor<IAEItemStack> inv = this.storage.getItemInventory();
        int howManyPerCraft = this.getStack().stackSize;
        int maxTimesToCraft = 0;
        InventoryAdaptor ia = null;
        if (action == InventoryAction.CRAFT_SHIFT) {
            ia = InventoryAdaptor.getAdaptor(who, null);
            maxTimesToCraft = (int)Math.floor((double)this.getStack().getMaxStackSize() / (double)howManyPerCraft);
        } else if (action == InventoryAction.CRAFT_STACK) {
            ia = new AdaptorPlayerHand(who);
            maxTimesToCraft = (int)Math.floor((double)this.getStack().getMaxStackSize() / (double)howManyPerCraft);
        } else {
            ia = new AdaptorPlayerHand(who);
            maxTimesToCraft = 1;
        }
        maxTimesToCraft = this.capCraftingAttempts(maxTimesToCraft);
        if (ia == null) {
            return;
        }
        ItemStack rs = Platform.cloneItemStack(this.getStack());
        if (rs == null) {
            return;
        }
        rs.stackSize *= maxTimesToCraft;
        if (ia.simulateAdd(rs) != null) {
            return;
        }
        IItemList<IAEItemStack> all = inv.getStorageList();
        while ((res = this.craftItem(who, rs, inv, all)) != null) {
            rs.stackSize -= res.stackSize;
            ItemStack extra = ia.addItems(res);
            if (extra != null) {
                ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                drops.add(extra);
                Platform.spawnDrops(who.worldObj, (int)who.posX, (int)who.posY, (int)who.posZ, drops);
                break;
            }
            if (rs.stackSize > 0) continue;
        }
    }

    private int capCraftingAttempts(int maxTimesToCraft) {
        return maxTimesToCraft;
    }

    private ItemStack craftItem(EntityPlayer p, ItemStack request, IMEMonitor<IAEItemStack> inv, IItemList all) {
        ItemStack is = this.getStack();
        if (is != null && Platform.isSameItem(request, is)) {
            ItemStack[] set = new ItemStack[this.getPattern().getSizeInventory()];
            int multiple = request.stackSize / is.stackSize;
            if (Platform.isServer()) {
                IPartitionList<IAEItemStack> filter;
                InventoryCrafting ic = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
                for (int x = 0; x < 9; ++x) {
                    ic.setInventorySlotContents(x, this.getPattern().getStackInSlot(x));
                }
                IRecipe r = Platform.findMatchingRecipe(ic, p.worldObj);
                if (r == null) {
                    if (request.stackSize > is.stackSize) {
                        return null;
                    }
                    Item target = request.getItem();
                    if (target.isDamageable() && target.isRepairable()) {
                        boolean isBad = false;
                        for (int x = 0; x < ic.getSizeInventory(); ++x) {
                            ItemStack pis = ic.getStackInSlot(x);
                            if (pis == null || pis.getItem() == target) continue;
                            isBad = true;
                        }
                        if (!isBad) {
                            super.onPickupFromSlot(p, is);
                            p.openContainer.onCraftMatrixChanged(this.craftInv);
                            return request;
                        }
                    }
                    return null;
                }
                is = r.getCraftingResult(ic);
                if (inv != null && !this.extractItems(p, inv, all, is, set, multiple, ic, r, filter = ItemViewCell.createFilter(this.container.getViewCells()))) {
                    this.cleanup(p, inv, set);
                    multiple = 1;
                    this.extractItems(p, inv, all, is, set, 1, ic, r, filter);
                }
            }
            int crafted = 0;
            if (this.preCraft(p, inv, set, is)) {
                for (int i = 0; i < multiple; ++i) {
                    this.makeItem(p, is);
                    ++crafted;
                    if (!this.postCraft(p, inv, set, is) && i < multiple - 1) break;
                }
            }
            is.stackSize *= crafted;
            this.cleanup(p, inv, set);
            p.openContainer.onCraftMatrixChanged(this.craftInv);
            return is;
        }
        return null;
    }

    private boolean extractItems(EntityPlayer p, IMEMonitor<IAEItemStack> inv, IItemList all, ItemStack is, ItemStack[] set, int multiple, InventoryCrafting ic, IRecipe r, IPartitionList<IAEItemStack> filter) {
        for (int x = 0; x < this.getPattern().getSizeInventory(); ++x) {
            if (this.getPattern().getStackInSlot(x) == null) continue;
            set[x] = Platform.extractItemsByRecipe(this.energySrc, this.mySrc, inv, p.worldObj, r, is, ic, this.getPattern().getStackInSlot(x), x, all, Actionable.MODULATE, filter, multiple);
            if (set[x] != null) continue;
            if (multiple > 1) {
                return false;
            }
            set[x] = this.getPattern().getStackInSlot(x).copy();
            set[x].stackSize = 0;
        }
        return true;
    }

    private boolean preCraft(EntityPlayer p, IMEMonitor<IAEItemStack> inv, ItemStack[] set, ItemStack result) {
        return true;
    }

    private void makeItem(EntityPlayer p, ItemStack is) {
        super.onPickupFromSlot(p, is);
    }

    private boolean postCraft(EntityPlayer p, IMEMonitor<IAEItemStack> inv, ItemStack[] set, ItemStack result) {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        boolean hadEmptyStacks = false;
        if (Platform.isServer()) {
            for (int x = 0; x < this.craftInv.getSizeInventory(); ++x) {
                if (this.craftInv.getStackInSlot(x) == null) {
                    if (set[x] == null) continue;
                    if (set[x].stackSize > 0) {
                        ItemStack s = set[x].copy();
                        s.stackSize = 1;
                        this.craftInv.setInventorySlotContents(x, s);
                        --set[x].stackSize;
                    }
                    hadEmptyStacks |= set[x].stackSize == 0;
                    continue;
                }
                if (set[x] == null || Platform.isSameItem(this.craftInv.getStackInSlot(x), set[x])) continue;
                IAEItemStack fail = inv.injectItems(AEItemStack.create(set[x]), Actionable.MODULATE, this.mySrc);
                if (fail != null) {
                    drops.add(fail.getItemStack());
                }
                set[x] = null;
                hadEmptyStacks = true;
            }
        }
        if (drops.size() > 0) {
            Platform.spawnDrops(p.worldObj, (int)p.posX, (int)p.posY, (int)p.posZ, drops);
        }
        return !hadEmptyStacks;
    }

    private void cleanup(EntityPlayer p, IMEMonitor<IAEItemStack> inv, ItemStack[] set) {
        if (Platform.isServer()) {
            ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
            for (ItemStack itemStack : set) {
                IAEItemStack fail;
                if (itemStack == null || itemStack.stackSize <= 0 || (fail = (IAEItemStack)inv.injectItems(AEItemStack.create(itemStack), Actionable.MODULATE, this.mySrc)) == null) continue;
                drops.add(fail.getItemStack());
            }
            if (drops.size() > 0) {
                Platform.spawnDrops(p.worldObj, (int)p.posX, (int)p.posY, (int)p.posZ, drops);
            }
        }
    }

    IInventory getPattern() {
        return this.pattern;
    }
}

