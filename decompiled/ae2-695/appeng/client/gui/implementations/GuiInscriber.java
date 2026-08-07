/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiProgressBar;
import appeng.container.implementations.ContainerInscriber;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.tile.misc.TileInscriber;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiInscriber
extends AEBaseGui {
    private final ContainerInscriber cvc;
    private GuiProgressBar pb;

    public GuiInscriber(InventoryPlayer inventoryPlayer, TileInscriber te) {
        super(new ContainerInscriber(inventoryPlayer, te));
        this.cvc = (ContainerInscriber)this.inventorySlots;
        this.ySize = 176;
        this.xSize = this.hasToolbox() ? 246 : 211;
    }

    private boolean hasToolbox() {
        return ((ContainerUpgradeable)this.inventorySlots).hasToolbox();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.pb = new GuiProgressBar(this.cvc, "guis/inscriber.png", 135, 39, 135, 177, 6, 18, GuiProgressBar.Direction.VERTICAL);
        this.buttonList.add(this.pb);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.pb.setFullMsg(this.cvc.getCurrentProgress() * 100 / this.cvc.getMaxProgress() + "%");
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.Inscriber.getLocal()), 8, 6, GuiColors.InscriberTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.InscriberInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/inscriber.png");
        this.pb.xPosition = 135 + this.guiLeft;
        this.pb.yPosition = 39 + this.guiTop;
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, 177, this.ySize);
        if (this.drawUpgrades()) {
            this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35, 14 + this.cvc.availableUpgrades() * 18);
        }
        if (this.hasToolbox()) {
            this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90, 178, this.ySize - 90, 68, 68);
        }
    }

    private boolean drawUpgrades() {
        return true;
    }
}

