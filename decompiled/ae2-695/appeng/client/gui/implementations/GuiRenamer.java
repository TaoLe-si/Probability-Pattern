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
import appeng.container.implementations.ContainerRenamer;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.ICustomNameObject;
import java.io.IOException;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class GuiRenamer
extends AEBaseGui
implements IDropToFillTextField {
    private MEGuiTextField textField;

    public GuiRenamer(InventoryPlayer ip, ICustomNameObject obj) {
        super(new ContainerRenamer(ip, obj));
        this.xSize = 256;
        this.textField = new MEGuiTextField(231, 12){

            @Override
            public void onTextChange(String oldText) {
                String text = this.getText();
                if (!text.equals(oldText)) {
                    ((ContainerRenamer)GuiRenamer.this.inventorySlots).setCustomName(text);
                }
            }
        };
    }

    @Override
    public void initGui() {
        super.initGui();
        this.textField.x = this.guiLeft + 12;
        this.textField.y = this.guiTop + 35;
        this.textField.setFocused(true);
        ((ContainerRenamer)this.inventorySlots).setTextField(this.textField);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.Renamer.getLocal(), 12, 8, GuiColors.RenamerTitle.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/renamer.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.textField.drawTextBox();
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.textField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void keyTyped(char character, int key) {
        if (key == 28 || key == 156) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("QuartzKnife.ReName", this.textField.getText()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
            this.mc.thePlayer.closeScreen();
        } else if (!this.textField.textboxKeyTyped(character, key)) {
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

