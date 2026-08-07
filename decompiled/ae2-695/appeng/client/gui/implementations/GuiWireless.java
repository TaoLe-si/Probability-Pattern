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
import appeng.container.implementations.ContainerWireless;
import appeng.core.AEConfig;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.tile.networking.TileWireless;
import appeng.util.Platform;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

public class GuiWireless
extends AEBaseGui {
    private GuiImgButton units;

    public GuiWireless(InventoryPlayer inventoryPlayer, TileWireless te) {
        super(new ContainerWireless(inventoryPlayer, te));
        this.ySize = 166;
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
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.Wireless.getLocal()), 8, 6, GuiColors.WirelessTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.WirelessInventory.getColor());
        ContainerWireless cw = (ContainerWireless)this.inventorySlots;
        if (cw.getRange() > 0L) {
            String firstMessage = GuiText.Range.getLocal() + ": " + (double)cw.getRange() / 10.0 + " m";
            String secondMessage = GuiText.PowerUsageRate.getLocal() + ": " + Platform.formatPowerLong(cw.getDrain(), true);
            int strWidth = Math.max(this.fontRendererObj.getStringWidth(firstMessage), this.fontRendererObj.getStringWidth(secondMessage));
            int cOffset = this.xSize / 2 - strWidth / 2;
            this.fontRendererObj.drawString(firstMessage, cOffset, 20, GuiColors.WirelessRange.getColor());
            this.fontRendererObj.drawString(secondMessage, cOffset, 32, GuiColors.WirelessPowerUsageRate.getColor());
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/wireless.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

