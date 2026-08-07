/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.world.World
 */
package appeng.container.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.INetworkTool;
import appeng.api.parts.IPart;
import appeng.api.util.IConfigManager;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.OptionalSlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import appeng.items.tools.ToolAdvancedNetworkTool;
import appeng.items.tools.ToolNetworkTool;
import appeng.parts.automation.PartExportBus;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ContainerUpgradeable
extends AEBaseContainer
implements IOptionalSlotHost {
    private final IUpgradeableHost upgradeable;
    @GuiSync(value=0)
    public RedstoneMode rsMode = RedstoneMode.IGNORE;
    @GuiSync(value=1)
    public FuzzyMode fzMode = FuzzyMode.IGNORE_ALL;
    @GuiSync(value=5)
    public YesNo cMode = YesNo.NO;
    @GuiSync(value=6)
    public SchedulingMode schedulingMode = SchedulingMode.DEFAULT;
    private int tbSlot;
    private INetworkTool tbInventory;

    public ContainerUpgradeable(InventoryPlayer ip, IUpgradeableHost te) {
        super(ip, (TileEntity)(te instanceof TileEntity ? te : null), (IPart)((Object)(te instanceof IPart ? te : null)));
        this.upgradeable = te;
        World w = null;
        int xCoord = 0;
        int yCoord = 0;
        int zCoord = 0;
        if (te instanceof TileEntity) {
            TileEntity myTile = (TileEntity)te;
            w = myTile.getWorldObj();
            xCoord = myTile.xCoord;
            yCoord = myTile.yCoord;
            zCoord = myTile.zCoord;
        }
        if (te instanceof IPart) {
            TileEntity mk = te.getTile();
            w = mk.getWorldObj();
            xCoord = mk.xCoord;
            yCoord = mk.yCoord;
            zCoord = mk.zCoord;
        }
        InventoryPlayer pi = this.getPlayerInv();
        for (int x = 0; x < pi.getSizeInventory(); ++x) {
            ItemStack pii = pi.getStackInSlot(x);
            if (pii == null || !(pii.getItem() instanceof ToolNetworkTool) && !(pii.getItem() instanceof ToolAdvancedNetworkTool)) continue;
            this.lockPlayerInventorySlot(x);
            this.tbSlot = x;
            this.tbInventory = (INetworkTool)((IGuiItem)pii.getItem()).getGuiObject(pii, w, xCoord, yCoord, zCoord);
            break;
        }
        if (this.hasToolbox()) {
            int size = this.tbInventory.getSize();
            int yBias = size == 3 ? 0 : 7;
            for (int v = 0; v < size; ++v) {
                for (int u = 0; u < size; ++u) {
                    this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, this.tbInventory, u + v * size, 186 + u * 18, this.getHeight() - 82 - yBias + v * 18, this.getInventoryPlayer()).setPlayerSide());
                }
            }
        }
        this.setupConfig();
        this.bindPlayerInventory(ip, 0, this.getHeight() - 82);
    }

    public boolean hasToolbox() {
        return this.tbInventory != null;
    }

    public int getToolboxSize() {
        return this.tbInventory.getSize();
    }

    public int getToolboxSizeInventory() {
        return this.hasToolbox() ? this.tbInventory.getSizeInventory() : 0;
    }

    protected int getHeight() {
        return 184;
    }

    protected void setupConfig() {
        this.setupUpgrades();
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        int y = 40;
        int x = 80;
        this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 0, 80, 40, 0, 0, 0));
        if (this.supportCapacity()) {
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 1, 80, 40, -1, 0, 1));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 2, 80, 40, 1, 0, 1));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 3, 80, 40, 0, -1, 1));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 4, 80, 40, 0, 1, 1));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 5, 80, 40, -1, -1, 2));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 6, 80, 40, 1, -1, 2));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 7, 80, 40, -1, 1, 2));
            this.addSlotToContainer(new OptionalSlotFakeTypeOnly(inv, this, 8, 80, 40, 1, 1, 2));
        }
    }

    protected void setupUpgrades() {
        IInventory upgrades = this.getUpgradeable().getInventoryByName("upgrades");
        if (this.availableUpgrades() > 0) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 0, 187, 8, this.getInventoryPlayer()).setNotDraggable());
        }
        if (this.availableUpgrades() > 1) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 1, 187, 26, this.getInventoryPlayer()).setNotDraggable());
        }
        if (this.availableUpgrades() > 2) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 2, 187, 44, this.getInventoryPlayer()).setNotDraggable());
        }
        if (this.availableUpgrades() > 3) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 3, 187, 62, this.getInventoryPlayer()).setNotDraggable());
        }
    }

    protected boolean supportCapacity() {
        return true;
    }

    public int availableUpgrades() {
        return 4;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            IConfigManager cm = this.getUpgradeable().getConfigManager();
            this.loadSettingsFromHost(cm);
        }
        this.checkToolbox();
        for (Object o : this.inventorySlots) {
            OptionalSlotFake fs;
            if (!(o instanceof OptionalSlotFake) || (fs = (OptionalSlotFake)((Object)o)).isEnabled() || fs.getDisplayStack() == null) continue;
            fs.clearStack();
        }
        this.standardDetectAndSendChanges();
    }

    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setFuzzyMode((FuzzyMode)cm.getSetting(Settings.FUZZY_MODE));
        this.setRedStoneMode((RedstoneMode)cm.getSetting(Settings.REDSTONE_CONTROLLED));
        if (this.getUpgradeable() instanceof PartExportBus) {
            this.setCraftingMode((YesNo)cm.getSetting(Settings.CRAFT_ONLY));
            this.setSchedulingMode((SchedulingMode)cm.getSetting(Settings.SCHEDULING_MODE));
        }
    }

    private void checkToolbox() {
        ItemStack currentItem;
        if (this.hasToolbox() && (currentItem = this.getPlayerInv().getStackInSlot(this.tbSlot)) != this.tbInventory.getItemStack()) {
            if (currentItem != null) {
                if (Platform.isSameItem(this.tbInventory.getItemStack(), currentItem)) {
                    this.getPlayerInv().setInventorySlotContents(this.tbSlot, this.tbInventory.getItemStack());
                } else {
                    this.setValidContainer(false);
                }
            } else {
                this.setValidContainer(false);
            }
        }
    }

    protected void standardDetectAndSendChanges() {
        super.detectAndSendChanges();
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        if (this.getUpgradeable().getInstalledUpgrades(Upgrades.ORE_FILTER) > 0) {
            return false;
        }
        if (idx == 0) {
            return true;
        }
        int upgrades = this.getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY);
        if (idx == 1 && upgrades > 0) {
            return true;
        }
        return idx == 2 && upgrades > 1;
    }

    public FuzzyMode getFuzzyMode() {
        return this.fzMode;
    }

    public void setFuzzyMode(FuzzyMode fzMode) {
        this.fzMode = fzMode;
    }

    public YesNo getCraftingMode() {
        return this.cMode;
    }

    public void setCraftingMode(YesNo cMode) {
        this.cMode = cMode;
    }

    public RedstoneMode getRedStoneMode() {
        return this.rsMode;
    }

    void setRedStoneMode(RedstoneMode rsMode) {
        this.rsMode = rsMode;
    }

    public SchedulingMode getSchedulingMode() {
        return this.schedulingMode;
    }

    private void setSchedulingMode(SchedulingMode schedulingMode) {
        this.schedulingMode = schedulingMode;
    }

    IUpgradeableHost getUpgradeable() {
        return this.upgradeable;
    }
}

