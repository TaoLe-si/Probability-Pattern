/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.FullnessMode;
import appeng.api.config.OperationMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.definitions.IDefinitions;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerIOPort;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.tile.storage.TileIOPort;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

public class GuiIOPort
extends GuiUpgradeable {
    private GuiImgButton fullMode;
    private GuiImgButton operationMode;

    public GuiIOPort(InventoryPlayer inventoryPlayer, TileIOPort te) {
        super(new ContainerIOPort(inventoryPlayer, te));
        this.ySize = 166;
    }

    @Override
    protected void addButtons() {
        this.redstoneMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.fullMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.FULLNESS_MODE, FullnessMode.EMPTY);
        this.operationMode = new GuiImgButton(this.guiLeft + 80, this.guiTop + 17, Settings.OPERATION_MODE, OperationMode.EMPTY);
        this.buttonList.add(this.operationMode);
        this.buttonList.add(this.redstoneMode);
        this.buttonList.add(this.fullMode);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.IOPort.getLocal()), 8, 6, GuiColors.IOPortTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.IOPortInventory.getColor());
        if (this.redstoneMode != null) {
            this.redstoneMode.set(this.cvb.getRedStoneMode());
        }
        if (this.operationMode != null) {
            this.operationMode.set(((ContainerIOPort)this.cvb).getOperationMode());
        }
        if (this.fullMode != null) {
            this.fullMode.set(((ContainerIOPort)this.cvb).getFullMode());
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        IDefinitions definitions = AEApi.instance().definitions();
        for (ItemStack cell1kStack : definitions.items().cell1k().maybeStack(1).asSet()) {
            this.drawItem(offsetX + 66 - 8, offsetY + 17, cell1kStack);
        }
        for (ItemStack driveStack : definitions.blocks().drive().maybeStack(1).asSet()) {
            this.drawItem(offsetX + 94 + 8, offsetY + 17, driveStack);
        }
    }

    @Override
    protected String getBackground() {
        return "guis/ioport.png";
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.fullMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.fullMode.getSetting(), backwards));
        }
        if (btn == this.operationMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.operationMode.getSetting(), backwards));
        }
    }
}

