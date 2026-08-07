/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiProgressBar;
import appeng.container.implementations.ContainerVibrationChamber;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.tile.misc.TileVibrationChamber;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.opengl.GL11;

public class GuiVibrationChamber
extends AEBaseGui {
    private final ContainerVibrationChamber cvc;
    private GuiProgressBar pb;

    public GuiVibrationChamber(InventoryPlayer inventoryPlayer, TileVibrationChamber te) {
        super(new ContainerVibrationChamber(inventoryPlayer, te));
        this.cvc = (ContainerVibrationChamber)this.inventorySlots;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.pb = new GuiProgressBar(this.cvc, "guis/vibchamber.png", 99, 36, 176, 14, 6, 18, GuiProgressBar.Direction.VERTICAL);
        this.buttonList.add(this.pb);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.VibrationChamber.getLocal()), 8, 6, GuiColors.VibrationChamberTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.VibrationChamberInventory.getColor());
        this.pb.setFullMsg(this.cvc.getAePerTick() * this.cvc.getCurrentProgress() / 100 + " AE/t");
        if (this.cvc.getCurrentProgress() > 0) {
            int i1 = this.cvc.getCurrentProgress();
            this.bindTexture("guis/vibchamber.png");
            GL11.glColor3f((float)1.0f, (float)1.0f, (float)1.0f);
            int l = -15;
            int k = 25;
            this.drawTexturedModalRect(81, 33 - i1, 176, 12 - i1, 14, i1 + 2);
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/vibchamber.png");
        this.pb.xPosition = 99 + this.guiLeft;
        this.pb.yPosition = 36 + this.guiTop;
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}

