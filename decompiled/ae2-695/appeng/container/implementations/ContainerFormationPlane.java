/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.OptionalSlotFakeTypeOnly;
import appeng.container.slot.SlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import appeng.parts.automation.PartFormationPlane;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerFormationPlane
extends ContainerUpgradeable {
    @GuiSync(value=6)
    public YesNo placeMode;

    public ContainerFormationPlane(InventoryPlayer ip, PartFormationPlane te) {
        super(ip, te);
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
                if (y < 2) {
                    this.addSlotToContainer(new SlotFakeTypeOnly(config, y * 9 + x, 8 + x * 18, 29 + y * 18));
                    continue;
                }
                this.addSlotToContainer(new OptionalSlotFakeTypeOnly(config, this, y * 9 + x, 8, 29, x, y, y - 2));
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

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            this.setFuzzyMode((FuzzyMode)this.getUpgradeable().getConfigManager().getSetting(Settings.FUZZY_MODE));
            this.setPlaceMode((YesNo)this.getUpgradeable().getConfigManager().getSetting(Settings.PLACE_BLOCK));
        }
        this.standardDetectAndSendChanges();
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        int upgrades = this.getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY);
        return upgrades > idx;
    }

    public YesNo getPlaceMode() {
        return this.placeMode;
    }

    private void setPlaceMode(YesNo placeMode) {
        this.placeMode = placeMode;
    }
}

