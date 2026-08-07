/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiProgressBar;
import appeng.container.implementations.ContainerMAC;
import appeng.core.localization.GuiText;
import appeng.tile.crafting.TileMolecularAssembler;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiMAC
extends GuiUpgradeable {
    private final ContainerMAC container;
    private GuiProgressBar pb;

    public GuiMAC(InventoryPlayer inventoryPlayer, TileMolecularAssembler te) {
        super(new ContainerMAC(inventoryPlayer, te));
        this.ySize = 197;
        this.container = (ContainerMAC)this.inventorySlots;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.pb = new GuiProgressBar(this.container, "guis/mac.png", 139, 36, 148, 201, 6, 18, GuiProgressBar.Direction.VERTICAL);
        this.buttonList.add(this.pb);
    }

    @Override
    protected void addButtons() {
        this.redstoneMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.buttonList.add(this.redstoneMode);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.pb.setFullMsg(this.container.getCurrentProgress() + "%");
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.pb.xPosition = 148 + this.guiLeft;
        this.pb.yPosition = 48 + this.guiTop;
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    protected String getBackground() {
        return "guis/mac.png";
    }

    @Override
    protected GuiText getName() {
        return GuiText.MolecularAssembler;
    }
}

