/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.Settings;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerSpatialIOPort;
import appeng.core.AEConfig;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.tile.spatial.TileSpatialIOPort;
import appeng.util.Platform;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

public class GuiSpatialIOPort
extends AEBaseGui {
    private final ContainerSpatialIOPort container;
    private GuiImgButton units;

    public GuiSpatialIOPort(InventoryPlayer inventoryPlayer, TileSpatialIOPort te) {
        super(new ContainerSpatialIOPort(inventoryPlayer, te));
        this.ySize = 199;
        this.container = (ContainerSpatialIOPort)this.inventorySlots;
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.units) {
            AEConfig.instance.nextPowerUnit(backwards);
            this.units.set(AEConfig.instance.selectedPowerUnit());
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.units = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.POWER_UNITS, AEConfig.instance.selectedPowerUnit());
        this.buttonList.add(this.units);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.StoredPower.getLocal() + ": " + Platform.formatPowerLong(this.container.getCurrentPower(), false), 13, 21, GuiColors.SpatialIOStoredPower.getColor());
        this.fontRendererObj.drawString(GuiText.MaxPower.getLocal() + ": " + Platform.formatPowerLong(this.container.getMaxPower(), false), 13, 31, GuiColors.SpatialIOMaxPower.getColor());
        this.fontRendererObj.drawString(GuiText.RequiredPower.getLocal() + ": " + Platform.formatPowerLong(this.container.getRequiredPower(), false), 13, 78, GuiColors.SpatialIORequiredPower.getColor());
        this.fontRendererObj.drawString(GuiText.Efficiency.getLocal() + ": " + (float)this.container.getEfficency() / 100.0f + '%', 13, 88, GuiColors.SpatialIOEfficiency.getColor());
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.SpatialIOPort.getLocal()), 8, 6, GuiColors.SpatialIOTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96, GuiColors.SpatialIOInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/spatialio.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

