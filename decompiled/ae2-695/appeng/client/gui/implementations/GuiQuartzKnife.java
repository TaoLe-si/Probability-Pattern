/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerQuartzKnife;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.items.contents.QuartzKnifeObj;
import java.io.IOException;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class GuiQuartzKnife
extends AEBaseGui
implements IDropToFillTextField {
    private MEGuiTextField textField;

    public GuiQuartzKnife(InventoryPlayer inventoryPlayer, QuartzKnifeObj te) {
        super(new ContainerQuartzKnife(inventoryPlayer, te));
        this.ySize = 184;
        this.textField = new MEGuiTextField(90, 12){

            @Override
            public void onTextChange(String oldText) {
                try {
                    String Out2 = this.getText();
                    ((ContainerQuartzKnife)GuiQuartzKnife.this.inventorySlots).setName(Out2);
                    NetworkHandler.instance.sendToServer(new PacketValueConfig("QuartzKnife.Name", Out2));
                }
                catch (IOException e) {
                    AELog.debug(e);
                }
            }
        };
    }

    @Override
    public void initGui() {
        super.initGui();
        this.textField.x = this.guiLeft + 21;
        this.textField.y = this.guiTop + 30;
        this.textField.setFocused(true);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.QuartzCuttingKnife.getLocal()), 8, 6, GuiColors.QuartzCuttingKnifeTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.QuartzCuttingKnifeInventory.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/quartzknife.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.textField.drawTextBox();
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.textField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void keyTyped(char character, int key) {
        if (!this.textField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        return this.textField.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        this.textField.setText(displayName);
    }
}

