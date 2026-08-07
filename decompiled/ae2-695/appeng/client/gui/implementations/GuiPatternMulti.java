/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerPatternMulti;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternMultiSet;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import java.util.Iterator;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class GuiPatternMulti
extends GuiAmount {
    private static final int DEFAULT_VALUE = 0;
    private GuiImgButton symbolSwitch;

    public GuiPatternMulti(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerPatternMulti(inventoryPlayer, te));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.symbolSwitch = new GuiImgButton(this.guiLeft + 22, this.guiTop + 53, Settings.ACTIONS, ActionItems.MULTIPLY);
        this.buttonList.add(this.symbolSwitch);
        this.amountTextField.x = this.guiLeft + 48;
        this.amountTextField.w = 73;
        this.amountTextField.setText(String.valueOf(0));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }

    @Override
    protected void setOriginGUI(Object target) {
        IDefinitions definitions = AEApi.instance().definitions();
        IParts parts = definitions.parts();
        if (target instanceof PartPatternTerminal) {
            Iterator iterator = parts.patternTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack stack;
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        } else if (target instanceof PartPatternTerminalEx) {
            Iterator iterator = parts.patternTerminalEx().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack stack;
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
        try {
            int resultI = this.getAmount();
            this.symbolSwitch.set(resultI >= 0 ? ActionItems.MULTIPLY : ActionItems.DIVIDE);
            this.nextBtn.enabled = resultI < -1 || resultI > 1;
        }
        catch (NumberFormatException e) {
            this.nextBtn.enabled = false;
        }
        this.amountTextField.drawTextBox();
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        int resultI;
        super.actionPerformed(btn);
        try {
            if (btn == this.nextBtn && btn.enabled && ((resultI = this.getAmount()) > 1 || resultI < -1)) {
                NetworkHandler.instance.sendToServer(new PacketPatternMultiSet(this.originalGui.ordinal(), resultI));
            }
        }
        catch (NumberFormatException e) {
            this.amountTextField.setText(String.valueOf(0));
        }
        if (btn == this.symbolSwitch) {
            resultI = -this.getAmount();
            this.amountTextField.setText(Integer.toString(resultI));
        }
    }

    @Override
    protected int getAmount() {
        String out = this.amountTextField.getText();
        double resultD = Calculator.conversion(out);
        if (Double.isNaN(resultD)) {
            return 0;
        }
        return (int)ArithHelper.round(resultD, 0);
    }

    @Override
    protected void addAmount(int i) {
        this.amountTextField.setText(Long.toString(i + this.getAmount()));
        this.amountTextField.setCursorPositionEnd();
    }

    @Override
    protected String getBackground() {
        return "guis/patternMulti.png";
    }
}

