/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.CopyMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.OptionalSlotRestrictedInput;
import appeng.container.slot.SlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.ICellRestriction;
import appeng.tile.inventory.AppEngNullInventory;
import appeng.tile.misc.TileCellWorkbench;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.iterators.NullIterator;
import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class ContainerCellWorkbench
extends ContainerUpgradeable {
    private final TileCellWorkbench workBench;
    private final AppEngNullInventory nullInventory = new AppEngNullInventory();
    @GuiSync(value=2)
    public CopyMode copyMode = CopyMode.CLEAR_ON_REMOVE;
    private ItemStack prevStack = null;
    private int lastUpgrades = 0;

    public ContainerCellWorkbench(InventoryPlayer ip, TileCellWorkbench te) {
        super(ip, te);
        this.workBench = te;
    }

    public void setFuzzy(FuzzyMode valueOf) {
        ICellWorkbenchItem cwi = this.workBench.getCell();
        if (cwi != null) {
            cwi.setFuzzyMode(this.workBench.getInventoryByName("cell").getStackInSlot(0), valueOf);
        }
    }

    public void nextWorkBenchCopyMode() {
        this.workBench.getConfigManager().putSetting(Settings.COPY_MODE, Platform.nextEnum(this.getWorkBenchCopyMode()));
    }

    private CopyMode getWorkBenchCopyMode() {
        return (CopyMode)this.workBench.getConfigManager().getSetting(Settings.COPY_MODE);
    }

    @Override
    protected int getHeight() {
        return 251;
    }

    @Override
    protected void setupConfig() {
        int z;
        IInventory cell = this.getUpgradeable().getInventoryByName("cell");
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.WORKBENCH_CELL, cell, 0, 152, 8, this.getInventoryPlayer()));
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        Upgrades upgradeInventory = new Upgrades();
        int offset = 0;
        int x = 8;
        int y = 29;
        for (int w = 0; w < 7; ++w) {
            for (z = 0; z < 9; ++z) {
                this.addSlotToContainer(new SlotFakeTypeOnly(inv, offset, 8 + z * 18, 29 + w * 18));
                ++offset;
            }
        }
        for (int zz = 0; zz < 3; ++zz) {
            for (z = 0; z < 8; ++z) {
                int iSLot = zz * 8 + z;
                this.addSlotToContainer(new OptionalSlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgradeInventory, this, iSLot, 187 + zz * 18, 8 + 18 * z, iSLot, this.getInventoryPlayer()));
            }
        }
    }

    @Override
    public int availableUpgrades() {
        ItemStack is = this.workBench.getInventoryByName("cell").getStackInSlot(0);
        if (this.prevStack != is) {
            this.prevStack = is;
            this.lastUpgrades = this.getCellUpgradeInventory().getSizeInventory();
        }
        return this.lastUpgrades;
    }

    @Override
    public void detectAndSendChanges() {
        ItemStack is = this.workBench.getInventoryByName("cell").getStackInSlot(0);
        if (Platform.isServer()) {
            if (this.workBench.getWorldObj().getTileEntity(this.workBench.xCoord, this.workBench.yCoord, this.workBench.zCoord) != this.workBench) {
                this.setValidContainer(false);
            }
            for (Object crafter : this.crafters) {
                ICrafting icrafting = (ICrafting)crafter;
                if (this.prevStack == is) continue;
                for (Object s : this.inventorySlots) {
                    if (!(s instanceof OptionalSlotRestrictedInput)) continue;
                    OptionalSlotRestrictedInput sri = (OptionalSlotRestrictedInput)((Object)s);
                    icrafting.sendSlotContents((Container)this, sri.slotNumber, sri.getStack());
                }
                ((EntityPlayerMP)icrafting).isChangingQuantityOnly = false;
            }
            this.setCopyMode(this.getWorkBenchCopyMode());
            this.setFuzzyMode(this.getWorkBenchFuzzyMode());
        }
        this.prevStack = is;
        this.standardDetectAndSendChanges();
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        return idx < this.availableUpgrades();
    }

    public IInventory getCellUpgradeInventory() {
        IInventory upgradeInventory = this.workBench.getCellUpgradeInventory();
        return upgradeInventory == null ? this.nullInventory : upgradeInventory;
    }

    public boolean haveCell() {
        return this.workBench.getCell() != null;
    }

    public boolean haveCellRestrictAble() {
        return this.haveCell() && this.workBench.getCell() instanceof ICellRestriction;
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("copyMode")) {
            this.workBench.getConfigManager().putSetting(Settings.COPY_MODE, this.getCopyMode());
        }
        super.onUpdate(field, oldValue, newValue);
    }

    public void clear() {
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        for (int x = 0; x < inv.getSizeInventory(); ++x) {
            inv.setInventorySlotContents(x, null);
        }
        this.detectAndSendChanges();
    }

    private FuzzyMode getWorkBenchFuzzyMode() {
        ICellWorkbenchItem cwi = this.workBench.getCell();
        if (cwi != null) {
            return cwi.getFuzzyMode(this.workBench.getInventoryByName("cell").getStackInSlot(0));
        }
        return FuzzyMode.IGNORE_ALL;
    }

    public void partition() {
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        IMEInventoryHandler cellInv = AEApi.instance().registries().cell().getCellInventory(this.getUpgradeable().getInventoryByName("cell").getStackInSlot(0), null, StorageChannel.ITEMS);
        Iterator<Object> i = new NullIterator();
        if (cellInv != null) {
            IItemList<IAEItemStack> list = cellInv.getAvailableItems(AEApi.instance().storage().createItemList(), IterationCounter.fetchNewId());
            i = list.iterator();
        }
        for (int x = 0; x < inv.getSizeInventory(); ++x) {
            if (i.hasNext()) {
                ItemStack g = ((IAEItemStack)i.next()).getItemStack();
                g.stackSize = 1;
                inv.setInventorySlotContents(x, g);
                continue;
            }
            inv.setInventorySlotContents(x, null);
        }
        this.detectAndSendChanges();
    }

    public CopyMode getCopyMode() {
        return this.copyMode;
    }

    private void setCopyMode(CopyMode copyMode) {
        this.copyMode = copyMode;
    }

    public class Upgrades
    implements IInventory {
        public int getSizeInventory() {
            return ContainerCellWorkbench.this.getCellUpgradeInventory().getSizeInventory();
        }

        public ItemStack getStackInSlot(int i) {
            return ContainerCellWorkbench.this.getCellUpgradeInventory().getStackInSlot(i);
        }

        public ItemStack decrStackSize(int i, int j) {
            IInventory inv = ContainerCellWorkbench.this.getCellUpgradeInventory();
            ItemStack is = inv.decrStackSize(i, j);
            inv.markDirty();
            return is;
        }

        public ItemStack getStackInSlotOnClosing(int i) {
            IInventory inv = ContainerCellWorkbench.this.getCellUpgradeInventory();
            ItemStack is = inv.getStackInSlotOnClosing(i);
            inv.markDirty();
            return is;
        }

        public void setInventorySlotContents(int i, ItemStack itemstack) {
            IInventory inv = ContainerCellWorkbench.this.getCellUpgradeInventory();
            inv.setInventorySlotContents(i, itemstack);
            inv.markDirty();
        }

        public String getInventoryName() {
            return "Upgrades";
        }

        public boolean hasCustomInventoryName() {
            return false;
        }

        public int getInventoryStackLimit() {
            return 1;
        }

        public void markDirty() {
        }

        public boolean isUseableByPlayer(EntityPlayer entityplayer) {
            return false;
        }

        public void openInventory() {
        }

        public void closeInventory() {
        }

        public boolean isItemValidForSlot(int i, ItemStack itemstack) {
            return ContainerCellWorkbench.this.getCellUpgradeInventory().isItemValidForSlot(i, itemstack);
        }
    }
}

