/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiAmount;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternValueSet;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GuiPatternValueAmount
extends GuiAmount {
    private final int valueIndex;
    private final int originalAmount;

    public GuiPatternValueAmount(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerPatternValueAmount(inventoryPlayer, te));
        GuiContainer gui = (GuiContainer)Minecraft.getMinecraft().currentScreen;
        if (gui != null && gui.theSlot != null && gui.theSlot.getHasStack()) {
            Slot slot = gui.theSlot;
            this.originalAmount = slot.getStack().stackSize;
            this.valueIndex = slot.slotNumber;
        } else {
            this.valueIndex = -1;
            this.originalAmount = 0;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.amountTextField.setText(String.valueOf(this.originalAmount));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }

    @Override
    protected void setOriginGUI(Object target) {
        ItemStack stack;
        Iterator iterator;
        IDefinitions definitions = AEApi.instance().definitions();
        IParts parts = definitions.parts();
        if (target instanceof PartPatternTerminal) {
            iterator = parts.patternTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }
        if (target instanceof PartPatternTerminalEx) {
            iterator = parts.patternTerminalEx().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, GuiColors.CraftAmountSelectAmount.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.nextBtn.displayString = GuiText.Set.getLocal();
        this.nextBtn.enabled = this.valueIndex >= 0;
        try {
            int resultI = this.getAmount();
            this.nextBtn.enabled = resultI > 0;
        }
        catch (NumberFormatException e) {
            this.nextBtn.enabled = false;
        }
        this.amountTextField.drawTextBox();
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (btn == this.nextBtn && btn.enabled) {
                NetworkHandler.instance.sendToServer(new PacketPatternValueSet(this.originalGui.ordinal(), this.getAmount(), this.valueIndex));
            }
        }
        catch (NumberFormatException e) {
            this.amountTextField.setText("1");
        }
    }

    @Override
    protected String getBackground() {
        return "guis/craftAmt.png";
    }
}

