/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.container.implementations.ContainerQNB;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.tile.qnb.TileQuantumBridge;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiQNB
extends AEBaseGui {
    public GuiQNB(InventoryPlayer inventoryPlayer, TileQuantumBridge te) {
        super(new ContainerQNB(inventoryPlayer, te));
        this.ySize = 166;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.QuantumLinkChamber.getLocal()), 8, 6, GuiColors.QuantumLinkChamberTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.QuantumLinkChamberInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/chest.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

