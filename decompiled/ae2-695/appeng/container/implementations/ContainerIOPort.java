/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.api.config.FullnessMode;
import appeng.api.config.OperationMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.storage.TileIOPort;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerIOPort
extends ContainerUpgradeable {
    @GuiSync(value=2)
    public FullnessMode fMode = FullnessMode.EMPTY;
    @GuiSync(value=3)
    public OperationMode opMode = OperationMode.EMPTY;

    public ContainerIOPort(InventoryPlayer ip, TileIOPort te) {
        super(ip, te);
    }

    @Override
    protected int getHeight() {
        return 166;
    }

    @Override
    protected void setupConfig() {
        int x;
        int y;
        int offX = 19;
        int offY = 17;
        IInventory cells = this.getUpgradeable().getInventoryByName("cells");
        for (y = 0; y < 3; ++y) {
            for (x = 0; x < 2; ++x) {
                this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.STORAGE_CELLS, cells, x + y * 2, offX + x * 18, offY + y * 18, this.getInventoryPlayer()));
            }
        }
        offX = 122;
        offY = 17;
        for (y = 0; y < 3; ++y) {
            for (x = 0; x < 2; ++x) {
                this.addSlotToContainer(new SlotOutput(cells, 6 + x + y * 2, offX + x * 18, offY + y * 18, SlotRestrictedInput.PlacableItemType.STORAGE_CELLS.IIcon));
            }
        }
        IInventory upgrades = this.getUpgradeable().getInventoryByName("upgrades");
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 0, 187, 8, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 1, 187, 26, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 2, 187, 44, this.getInventoryPlayer()).setNotDraggable());
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 3;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            this.setOperationMode((OperationMode)this.getUpgradeable().getConfigManager().getSetting(Settings.OPERATION_MODE));
            this.setFullMode((FullnessMode)this.getUpgradeable().getConfigManager().getSetting(Settings.FULLNESS_MODE));
            this.setRedStoneMode((RedstoneMode)this.getUpgradeable().getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED));
        }
        this.standardDetectAndSendChanges();
    }

    public FullnessMode getFullMode() {
        return this.fMode;
    }

    private void setFullMode(FullnessMode fMode) {
        this.fMode = fMode;
    }

    public OperationMode getOperationMode() {
        return this.opMode;
    }

    private void setOperationMode(OperationMode opMode) {
        this.opMode = opMode;
    }
}

