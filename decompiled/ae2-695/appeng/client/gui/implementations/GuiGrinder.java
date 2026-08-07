/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.container.implementations.ContainerGrinder;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.tile.grindstone.TileGrinder;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiGrinder
extends AEBaseGui {
    public GuiGrinder(InventoryPlayer inventoryPlayer, TileGrinder te) {
        super(new ContainerGrinder(inventoryPlayer, te));
        this.ySize = 176;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.GrindStone.getLocal()), 8, 6, GuiColors.GrindStoneTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.GrindStoneInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/grinder.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

