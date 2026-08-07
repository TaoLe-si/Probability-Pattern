/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerChest;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.tile.storage.TileChest;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiChest
extends AEBaseGui {
    private GuiTabButton priority;

    public GuiChest(InventoryPlayer inventoryPlayer, TileChest te) {
        super(new ContainerChest(inventoryPlayer, te));
        this.ySize = 166;
    }

    protected void actionPerformed(GuiButton par1GuiButton) {
        super.actionPerformed(par1GuiButton);
        if (par1GuiButton == this.priority) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.priority = new GuiTabButton(this.guiLeft + 154, this.guiTop, 66, GuiText.Priority.getLocal(), itemRender);
        this.buttonList.add(this.priority);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.Chest.getLocal()), 8, 6, GuiColors.ChestTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.ChestInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/chest.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

