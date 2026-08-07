/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.implementations;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternItemRenamer;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternItemRenamer;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GuiPatternItemRenamer
extends AEBaseGui
implements IDropToFillTextField {
    private final MEGuiTextField textField;
    private final String oldName;
    private final int valueIndex;
    private GuiBridge originalGui;

    public GuiPatternItemRenamer(InventoryPlayer ip, ITerminalHost p) {
        super(new ContainerPatternItemRenamer(ip, p));
        GuiContainer gui = (GuiContainer)Minecraft.getMinecraft().currentScreen;
        if (gui != null && gui.theSlot != null && gui.theSlot.getHasStack()) {
            Slot slot = gui.theSlot;
            this.oldName = slot.getStack().getDisplayName();
            this.valueIndex = slot.slotNumber;
        } else {
            this.valueIndex = -1;
            this.oldName = "";
        }
        this.xSize = 256;
        this.textField = new MEGuiTextField(231, 12);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.textField.x = this.guiLeft + 12;
        this.textField.y = this.guiTop + 35;
        this.textField.setFocused(true);
        this.textField.setText(this.oldName);
        this.textField.setCursorPositionEnd();
        this.textField.setSelectionPos(0);
        this.setOriginGUI(((AEBaseContainer)this.inventorySlots).getTarget());
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

    protected void setOriginGUI(Object target) {
        if (target instanceof PartPatternTerminal) {
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        } else if (target instanceof PartPatternTerminalEx) {
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }
    }

    protected void keyTyped(char character, int key) {
        if (key == 28 || key == 156) {
            NetworkHandler.instance.sendToServer(new PacketPatternItemRenamer(this.originalGui.ordinal(), this.textField.getText(), this.valueIndex));
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

    public int getValueIndex() {
        return this.valueIndex;
    }

    public String getText() {
        return this.textField.getText();
    }
}

