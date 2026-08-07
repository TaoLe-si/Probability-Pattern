/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.api.implementations.guiobjects.INetworkTool;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.container.implementations.ContainerNetworkTool;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiNetworkTool
extends AEBaseGui {
    private GuiToggleButton tFacades;

    public GuiNetworkTool(InventoryPlayer inventoryPlayer, INetworkTool te) {
        super(new ContainerNetworkTool(inventoryPlayer, te));
        this.ySize = 166;
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (btn == this.tFacades) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("NetworkTool", "Toggle"));
            }
        }
        catch (IOException e) {
            AELog.debug(e);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.tFacades = new GuiToggleButton(this.guiLeft - 18, this.guiTop + 8, 23, 22, GuiText.TransparentFacades.getLocal(), GuiText.TransparentFacadesHint.getLocal());
        this.buttonList.add(this.tFacades);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        if (this.tFacades != null) {
            this.tFacades.setState(((ContainerNetworkTool)this.inventorySlots).isFacadeMode());
        }
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.NetworkTool.getLocal()), 8, 6, GuiColors.NetworkToolTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.NetworkToolInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/toolbox.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

