/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.ActionItems;
import appeng.api.config.CopyMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.container.implementations.ContainerCellWorkbench;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.tile.misc.TileCellWorkbench;
import appeng.util.Platform;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

public class GuiCellWorkbench
extends GuiUpgradeable {
    private final ContainerCellWorkbench workbench;
    private GuiImgButton clear;
    private GuiImgButton partition;
    private GuiToggleButton copyMode;
    protected GuiImgButton cellRestriction;

    public GuiCellWorkbench(InventoryPlayer inventoryPlayer, TileCellWorkbench te) {
        super(new ContainerCellWorkbench(inventoryPlayer, te));
        this.workbench = (ContainerCellWorkbench)this.inventorySlots;
        this.ySize = 251;
    }

    @Override
    protected void addButtons() {
        this.clear = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.ACTIONS, ActionItems.CLOSE);
        this.partition = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.ACTIONS, ActionItems.WRENCH);
        this.copyMode = new GuiToggleButton(this.guiLeft - 18, this.guiTop + 48, 181, 197, GuiText.CopyMode.getLocal(), GuiText.CopyModeDesc.getLocal());
        this.fuzzyMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 68, Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.oreFilter = new GuiImgButton(this.guiLeft - 18, this.guiTop + 68, Settings.ACTIONS, ActionItems.ORE_FILTER);
        this.cellRestriction = new GuiImgButton(this.guiLeft + 134, this.guiTop + 8, Settings.ACTIONS, ActionItems.CELL_RESTRICTION);
        this.buttonList.add(this.fuzzyMode);
        this.buttonList.add(this.partition);
        this.buttonList.add(this.clear);
        this.buttonList.add(this.copyMode);
        this.buttonList.add(this.oreFilter);
        this.buttonList.add(this.cellRestriction);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.handleButtonVisibility();
        this.bindTexture(this.getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, 177, this.ySize);
        if (this.drawUpgrades()) {
            if (this.workbench.availableUpgrades() <= 8) {
                this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35, 7 + this.workbench.availableUpgrades() * 18);
                this.drawTexturedModalRect(offsetX + 177, offsetY + (7 + this.workbench.availableUpgrades() * 18), 177, 151, 35, 7);
            } else if (this.workbench.availableUpgrades() <= 16) {
                this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35, 151);
                this.drawTexturedModalRect(offsetX + 177, offsetY + 151, 177, 151, 35, 7);
                int dx = this.workbench.availableUpgrades() - 8;
                this.drawTexturedModalRect(offsetX + 177 + 27, offsetY, 186, 0, 27, 7 + dx * 18);
                if (dx == 8) {
                    this.drawTexturedModalRect(offsetX + 177 + 27, offsetY + (7 + dx * 18), 186, 151, 27, 7);
                } else {
                    this.drawTexturedModalRect(offsetX + 177 + 27 + 4, offsetY + (7 + dx * 18), 190, 151, 27, 7);
                }
            } else {
                this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35, 151);
                this.drawTexturedModalRect(offsetX + 177, offsetY + 151, 177, 151, 35, 7);
                this.drawTexturedModalRect(offsetX + 177 + 27, offsetY, 186, 0, 27, 151);
                this.drawTexturedModalRect(offsetX + 177 + 27, offsetY + 151, 186, 151, 27, 7);
                int dx = this.workbench.availableUpgrades() - 16;
                this.drawTexturedModalRect(offsetX + 177 + 27 + 18, offsetY, 186, 0, 27, 7 + dx * 18);
                if (dx == 8) {
                    this.drawTexturedModalRect(offsetX + 177 + 27 + 18, offsetY + (7 + dx * 18), 186, 151, 27, 7);
                } else {
                    this.drawTexturedModalRect(offsetX + 177 + 27 + 18 + 4, offsetY + (7 + dx * 18), 190, 151, 27, 7);
                }
            }
        }
        if (this.hasToolbox()) {
            switch (this.getToolboxSize()) {
                case 3: {
                    this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90, 178, this.ySize - 90, 68, 68);
                    break;
                }
                case 5: {
                    this.bindTexture(this.getAdvancedBackground());
                    this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90 - 7, 0, 0, 104, 104);
                    break;
                }
                default: {
                    this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90, 178, this.ySize - 90, 68, 68);
                }
            }
        }
    }

    @Override
    protected void handleButtonVisibility() {
        this.copyMode.setState(this.workbench.getCopyMode() == CopyMode.CLEAR_ON_REMOVE);
        boolean hasFuzzy = false;
        boolean hasOreFilter = false;
        IInventory inv = this.workbench.getCellUpgradeInventory();
        for (int x = 0; x < inv.getSizeInventory(); ++x) {
            ItemStack is = inv.getStackInSlot(x);
            if (is == null || !(is.getItem() instanceof IUpgradeModule)) continue;
            if (((IUpgradeModule)is.getItem()).getType(is) == Upgrades.FUZZY) {
                hasFuzzy = true;
            }
            if (((IUpgradeModule)is.getItem()).getType(is) != Upgrades.ORE_FILTER) continue;
            hasOreFilter = true;
        }
        this.fuzzyMode.setVisibility(!hasOreFilter && hasFuzzy);
        this.oreFilter.setVisibility(hasOreFilter);
        this.cellRestriction.setVisibility(this.workbench.haveCellRestrictAble());
    }

    @Override
    protected String getBackground() {
        return "guis/cellworkbench.png";
    }

    @Override
    protected boolean drawUpgrades() {
        return this.workbench.availableUpgrades() > 0;
    }

    @Override
    protected GuiText getName() {
        return GuiText.CellWorkbench;
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        try {
            if (btn == this.copyMode) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("CellWorkbench.Action", "CopyMode"));
            } else if (btn == this.partition) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("CellWorkbench.Action", "Partition"));
            } else if (btn == this.clear) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("CellWorkbench.Action", "Clear"));
            } else if (btn == this.fuzzyMode) {
                boolean backwards = Mouse.isButtonDown((int)1);
                FuzzyMode fz = (FuzzyMode)this.fuzzyMode.getCurrentValue();
                fz = Platform.rotateEnum(fz, backwards, Settings.FUZZY_MODE.getPossibleValues());
                NetworkHandler.instance.sendToServer(new PacketValueConfig("CellWorkbench.Fuzzy", fz.name()));
            } else if (btn == this.cellRestriction) {
                NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_CELL_RESTRICTION));
            } else {
                super.actionPerformed(btn);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }
}

