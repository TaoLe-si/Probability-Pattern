/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.LevelType;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.SlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import appeng.parts.automation.PartLevelEmitter;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerLevelEmitter
extends ContainerUpgradeable {
    private final PartLevelEmitter lvlEmitter;
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField textField;
    @GuiSync(value=2)
    public LevelType lvType;
    @GuiSync(value=3)
    public long EmitterValue = -1L;
    @GuiSync(value=4)
    public YesNo cmType;

    public ContainerLevelEmitter(InventoryPlayer ip, PartLevelEmitter te) {
        super(ip, te);
        this.lvlEmitter = te;
    }

    @SideOnly(value=Side.CLIENT)
    public void setTextField(MEGuiTextField level) {
        this.textField = level;
    }

    public void setLevel(long l, EntityPlayer player) {
        this.lvlEmitter.setReportingValue(l);
        this.EmitterValue = l;
    }

    @Override
    protected void setupConfig() {
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
        IInventory inv = this.getUpgradeable().getInventoryByName("config");
        this.addSlotToContainer(new SlotFakeTypeOnly(inv, 0, 17, 42));
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 1;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            this.EmitterValue = this.lvlEmitter.getReportingValue();
            this.setCraftingMode((YesNo)this.getUpgradeable().getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE));
            this.setLevelMode((LevelType)this.getUpgradeable().getConfigManager().getSetting(Settings.LEVEL_TYPE));
            this.setFuzzyMode((FuzzyMode)this.getUpgradeable().getConfigManager().getSetting(Settings.FUZZY_MODE));
            this.setRedStoneMode((RedstoneMode)this.getUpgradeable().getConfigManager().getSetting(Settings.REDSTONE_EMITTER));
        }
        this.standardDetectAndSendChanges();
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("EmitterValue") && this.textField != null) {
            this.textField.setText(String.valueOf(this.EmitterValue));
            if (String.valueOf(oldValue).equals("-1")) {
                this.textField.setCursorPositionEnd();
            }
        }
    }

    @Override
    public YesNo getCraftingMode() {
        return this.cmType;
    }

    @Override
    public void setCraftingMode(YesNo cmType) {
        this.cmType = cmType;
    }

    public LevelType getLevelMode() {
        return this.lvType;
    }

    private void setLevelMode(LevelType lvType) {
        this.lvType = lvType;
    }
}

