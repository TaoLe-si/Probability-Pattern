/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.container.implementations.ContainerSkyChest;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.tile.storage.TileSkyChest;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiSkyChest
extends AEBaseGui {
    public GuiSkyChest(InventoryPlayer inventoryPlayer, TileSkyChest te) {
        super(new ContainerSkyChest(inventoryPlayer, te));
        this.ySize = 195;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.SkyChest.getLocal()), 8, 8, GuiColors.SkyChestTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 2, GuiColors.SkyChestInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/skychest.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected boolean enableSpaceClicking() {
        return !IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.InvTweaks);
    }
}

