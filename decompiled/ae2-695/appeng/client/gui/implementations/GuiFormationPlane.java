/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerFormationPlane;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.parts.automation.PartFormationPlane;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

public class GuiFormationPlane
extends GuiUpgradeable {
    private GuiTabButton priority;
    private GuiImgButton placeMode;

    public GuiFormationPlane(InventoryPlayer inventoryPlayer, PartFormationPlane te) {
        super(new ContainerFormationPlane(inventoryPlayer, te));
        this.ySize = 251;
    }

    @Override
    protected void addButtons() {
        this.placeMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.PLACE_BLOCK, YesNo.YES);
        this.fuzzyMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 48, Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.priority = new GuiTabButton(this.guiLeft + 154, this.guiTop, 66, GuiText.Priority.getLocal(), itemRender);
        this.buttonList.add(this.priority);
        this.buttonList.add(this.placeMode);
        this.buttonList.add(this.fuzzyMode);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.FormationPlane.getLocal()), 8, 6, GuiColors.FormationPlaneTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.FormationPlaneInventory.getColor());
        if (this.fuzzyMode != null) {
            this.fuzzyMode.set(this.cvb.getFuzzyMode());
        }
        if (this.placeMode != null) {
            this.placeMode.set(((ContainerFormationPlane)this.cvb).getPlaceMode());
        }
    }

    @Override
    protected String getBackground() {
        return "guis/storagebus.png";
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.priority) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
        } else if (btn == this.placeMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.placeMode.getSetting(), backwards));
        }
    }
}

