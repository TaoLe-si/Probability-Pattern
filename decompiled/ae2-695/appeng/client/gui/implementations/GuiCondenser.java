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
import appeng.client.gui.widgets.GuiProgressBar;
import appeng.container.implementations.ContainerCondenser;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.tile.misc.TileCondenser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

public class GuiCondenser
extends AEBaseGui {
    private final ContainerCondenser cvc;
    private GuiProgressBar pb;
    private GuiImgButton mode;

    public GuiCondenser(InventoryPlayer inventoryPlayer, TileCondenser te) {
        super(new ContainerCondenser(inventoryPlayer, te));
        this.cvc = (ContainerCondenser)this.inventorySlots;
        this.ySize = 197;
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (this.mode == btn) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(Settings.CONDENSER_OUTPUT, backwards));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.pb = new GuiProgressBar(this.cvc, "guis/condenser.png", 120 + this.guiLeft, 25 + this.guiTop, 178, 25, 6, 18, GuiProgressBar.Direction.VERTICAL, GuiText.StoredEnergy.getLocal());
        this.mode = new GuiImgButton(128 + this.guiLeft, 52 + this.guiTop, Settings.CONDENSER_OUTPUT, this.cvc.getOutput());
        this.buttonList.add(this.pb);
        this.buttonList.add(this.mode);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.Condenser.getLocal()), 8, 6, GuiColors.CondenserTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.CondenserInventory.getColor());
        this.mode.set(this.cvc.getOutput());
        this.mode.setFillVar(String.valueOf(this.cvc.getOutput().requiredPower));
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/condenser.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

