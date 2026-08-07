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
import appeng.api.config.AccessRestriction;
import appeng.api.config.ActionItems;
import appeng.api.config.FuzzyMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.OptionalSlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.misc.PartStorageBus;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.prioitylist.PrecisePriorityList;
import java.util.HashMap;
import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class ContainerStorageBus
extends ContainerUpgradeable {
    private final PartStorageBus storageBus;
    @GuiSync(value=3)
    public AccessRestriction rwMode = AccessRestriction.READ_WRITE;
    @GuiSync(value=4)
    public StorageFilter storageFilter = StorageFilter.EXTRACTABLE_ONLY;
    @GuiSync(value=7)
    public YesNo stickyMode = YesNo.NO;
    private static final HashMap<EntityPlayer, IteratorState> PartitionIteratorMap = new HashMap();
    @GuiSync(value=8)
    public ActionItems partitionMode;
    private int rowToUpdate = 0;

    public ContainerStorageBus(InventoryPlayer ip, PartStorageBus te) {
        super(ip, te);
        this.storageBus = te;
        this.partitionMode = PartitionIteratorMap.containsKey(ip.player) ? ActionItems.NEXT_PARTITION : ActionItems.WRENCH;
    }

    @Override
    protected int getHeight() {
        return 251;
    }

    @Override
    protected void setupConfig() {
        int xo = 8;
        int yo = 29;
        IInventory config = this.getUpgradeable().getInventoryByName("config");
        for (int y = 0; y < 7; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlotToContainer(new OptionalSlotFakeTypeOnly(config, this, y * 9 + x, 8, 29, x, y, y));
            }
        }
        IInventory upgrades = this.getUpgradeable().getInventoryByName("upgrades");
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 0, 187, 8, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 1, 187, 26, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 2, 187, 44, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 3, 187, 62, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 4, 187, 80, this.getInventoryPlayer()).setNotDraggable());
    }

    @Override
    protected boolean supportCapacity() {
        return true;
    }

    @Override
    public int availableUpgrades() {
        return 5;
    }

    private void sendRow(int row, int upgrades) {
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        int to = row <= -1 ? inv.getSizeInventory() : 18 + 9 * row;
        int offset = this.getToolboxSizeInventory();
        for (int from = row <= -1 ? 18 : 9 + 9 * row; from < to && upgrades > from / 9 - 2; ++from) {
            ItemStack stack = inv.getStackInSlot(from);
            if (stack == null) continue;
            for (ICrafting crafter : this.crafters) {
                if (crafter instanceof EntityPlayerMP) {
                    EntityPlayerMP playerMP = (EntityPlayerMP)crafter;
                    playerMP.isChangingQuantityOnly = false;
                }
                crafter.sendSlotContents((Container)this, from + offset, stack);
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        int upgrades;
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            this.setFuzzyMode((FuzzyMode)this.getUpgradeable().getConfigManager().getSetting(Settings.FUZZY_MODE));
            this.setReadWriteMode((AccessRestriction)this.getUpgradeable().getConfigManager().getSetting(Settings.ACCESS));
            this.setStorageFilter((StorageFilter)this.getUpgradeable().getConfigManager().getSetting(Settings.STORAGE_FILTER));
            this.setStickyMode((YesNo)this.getUpgradeable().getConfigManager().getSetting(Settings.STICKY_MODE));
        }
        if ((upgrades = this.getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY)) > 0) {
            int row;
            boolean needSync = this.storageBus.needSyncGUI;
            if (needSync) {
                row = -1;
                this.rowToUpdate = upgrades;
                this.storageBus.needSyncGUI = false;
            } else {
                row = this.rowToUpdate++;
            }
            if (row >= upgrades) {
                this.rowToUpdate = 0;
            }
            this.sendRow(row, upgrades);
        }
        this.standardDetectAndSendChanges();
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        if (this.getUpgradeable().getInstalledUpgrades(Upgrades.ORE_FILTER) > 0) {
            return false;
        }
        int upgrades = this.getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY);
        return upgrades > idx - 2;
    }

    public void clear() {
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        for (int x = 0; x < inv.getSizeInventory(); ++x) {
            inv.setInventorySlotContents(x, null);
        }
        this.detectAndSendChanges();
    }

    private void clearPartitionIterator(EntityPlayer player) {
        PartitionIteratorMap.remove(player);
        this.partitionMode = ActionItems.WRENCH;
    }

    public void partition(boolean clearIterator) {
        IteratorState it;
        EntityPlayer player = this.getInventoryPlayer().player;
        if (clearIterator) {
            this.clearPartitionIterator(player);
            return;
        }
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        MEInventoryHandler<IAEItemStack> cellInv = this.storageBus.getInternalHandler();
        if (cellInv == null) {
            this.clearPartitionIterator(player);
            return;
        }
        if (!PartitionIteratorMap.containsKey(player)) {
            cellInv.setPartitionList(new PrecisePriorityList<IAEItemStack>(AEApi.instance().storage().createItemList()));
            IItemList<IAEItemStack> list = cellInv.getAvailableItems(AEApi.instance().storage().createItemFilterList(), IterationCounter.fetchNewId());
            it = new IteratorState(list.iterator());
            PartitionIteratorMap.put(player, it);
            this.partitionMode = ActionItems.NEXT_PARTITION;
        } else {
            it = PartitionIteratorMap.get(player);
        }
        boolean skip = false;
        for (int x = 0; x < inv.getSizeInventory(); ++x) {
            if (skip) {
                inv.setInventorySlotContents(x, null);
                continue;
            }
            if (this.isSlotEnabled(x / 9)) {
                IAEItemStack AEis = it.next();
                if (AEis != null) {
                    ItemStack is = AEis.getItemStack();
                    is.stackSize = 1;
                    inv.setInventorySlotContents(x, is);
                    continue;
                }
                this.clearPartitionIterator(player);
                skip = true;
                inv.setInventorySlotContents(x, null);
                continue;
            }
            skip = true;
            inv.setInventorySlotContents(x, null);
        }
        if (!it.hasNext) {
            this.clearPartitionIterator(player);
        }
        this.detectAndSendChanges();
    }

    public AccessRestriction getReadWriteMode() {
        return this.rwMode;
    }

    public StorageFilter getStorageFilter() {
        return this.storageFilter;
    }

    public ActionItems getPartitionMode() {
        return this.partitionMode;
    }

    public void setPartitionMode(ActionItems action) {
        this.partitionMode = action;
    }

    private void setStorageFilter(StorageFilter storageFilter) {
        this.storageFilter = storageFilter;
    }

    public YesNo getStickyMode() {
        return this.stickyMode;
    }

    private void setStickyMode(YesNo stickyMode) {
        this.stickyMode = stickyMode;
    }

    private void setReadWriteMode(AccessRestriction rwMode) {
        this.rwMode = rwMode;
    }

    private static class IteratorState {
        private final Iterator<IAEItemStack> it;
        private boolean hasNext;

        public IteratorState(Iterator<IAEItemStack> it) {
            this.it = it;
            this.hasNext = it.hasNext();
        }

        public IAEItemStack next() {
            if (this.hasNext) {
                IAEItemStack is = this.it.next();
                this.hasNext = this.it.hasNext();
                return is;
            }
            return null;
        }
    }
}

