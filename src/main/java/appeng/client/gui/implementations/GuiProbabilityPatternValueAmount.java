/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
// NOTE: lives in AE2's GUI package so it can reuse AEBaseGui's slot rendering / bindTexture.
package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.zincglux.statpatterns.container.ContainerProbabilityPatternValueAmount;
import com.zincglux.statpatterns.network.ProbabilityPatternNetwork;
import com.zincglux.statpatterns.network.ProbabilityPatternValueSetPacket;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.core.AEConfig;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;

/**
 * Fully self-implemented "set pattern value amount" dialog — deliberately does NOT extend
 * AE's {@code GuiAmount}/{@code GuiPatternValueAmount} (inheriting those gave none of the
 * vanilla behaviour). It re-implements the whole vanilla amount dialog by hand:
 * <ul>
 * <li>read-only {@code patternValue} slot (from {@link ContainerProbabilityPatternValueAmount})
 * shows the item being adjusted,</li>
 * <li>+/-1/10/100/1000 quick buttons, an input field, a Set button and Enter-to-confirm,</li>
 * <li>confirming sends {@link ProbabilityPatternValueSetPacket} which writes the quantity
 * and reopens OUR probability terminal (never the vanilla one).</li>
 * </ul>
 */
public class GuiProbabilityPatternValueAmount extends AEBaseGui {

    private MEGuiTextField amountTextField;
    private GuiButton nextBtn;
    private GuiButton plus1;
    private GuiButton plus10;
    private GuiButton plus100;
    private GuiButton plus1000;
    private GuiButton minus1;
    private GuiButton minus10;
    private GuiButton minus100;
    private GuiButton minus1000;
    private final ContainerProbabilityPatternValueAmount container;
    private final int originalAmount;

    public GuiProbabilityPatternValueAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerProbabilityPatternValueAmount(inventoryPlayer, (ProbabilityPatternTerminalPart) te));
        this.container = (ContainerProbabilityPatternValueAmount) this.inventorySlots;
        final ItemStack shown = this.container.getPatternValue()
            .getStack();
        this.originalAmount = shown == null ? 1 : shown.stackSize;
    }

    @Override
    public void initGui() {
        super.initGui();
        final int a = getButtonQtyByIndex(0);
        final int b = getButtonQtyByIndex(1);
        final int c = getButtonQtyByIndex(2);
        final int d = getButtonQtyByIndex(3);
        this.buttonList.add(this.plus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 26, 22, 20, "+" + a));
        this.buttonList.add(this.plus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 26, 28, 20, "+" + b));
        this.buttonList.add(this.plus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 26, 32, 20, "+" + c));
        this.buttonList.add(this.plus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 26, 38, 20, "+" + d));
        this.buttonList.add(this.minus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 75, 22, 20, "-" + a));
        this.buttonList.add(this.minus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 75, 28, 20, "-" + b));
        this.buttonList.add(this.minus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 75, 32, 20, "-" + c));
        this.buttonList.add(this.minus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 75, 38, 20, "-" + d));
        this.buttonList.add(
            this.nextBtn = new GuiButton(0, this.guiLeft + 128, this.guiTop + 51, 38, 20, GuiText.Next.getLocal()));

        this.amountTextField = new MEGuiTextField(61, 12);
        this.amountTextField.x = this.guiLeft + 60;
        this.amountTextField.y = this.guiTop + 55;
        this.amountTextField.setMaxStringLength(16);
        this.amountTextField.setFocused(true);
        this.amountTextField.setText(String.valueOf(this.originalAmount));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj
            .drawString(GuiText.SelectAmount.getLocal(), 8, 6, GuiColors.CraftAmountSelectAmount.getColor());
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(this.getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.nextBtn.displayString = GuiText.Set.getLocal();
        try {
            final int result = this.getAmount();
            this.nextBtn.enabled = result > 0;
        } catch (final NumberFormatException e) {
            this.nextBtn.enabled = false;
        }
        this.amountTextField.drawTextBox();
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28 || key == 156) {
                this.actionPerformed(this.nextBtn);
            }
            this.amountTextField.textboxKeyTyped(character, key);
            super.keyTyped(character, key);
        }
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        super.mouseClicked(xCoord, yCoord, btn);
        this.amountTextField.mouseClickedNoFocusDrop(xCoord, yCoord, btn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);
        final boolean isPlus = btn == this.plus1 || btn == this.plus10 || btn == this.plus100 || btn == this.plus1000;
        final boolean isMinus = btn == this.minus1 || btn == this.minus10
            || btn == this.minus100
            || btn == this.minus1000;
        if (isPlus || isMinus) {
            this.addAmount(this.getQty(btn));
        } else if (btn == this.nextBtn && btn.enabled) {
            try {
                final int amount = this.getAmount();
                ProbabilityPatternNetwork.CHANNEL.sendToServer(
                    new ProbabilityPatternValueSetPacket(ProbabilityPatternValueSetPacket.Action.SET, 0, amount));
            } catch (final NumberFormatException e) {
                this.amountTextField.setText("1");
            }
        }
    }

    protected int getButtonQtyByIndex(final int index) {
        return AEConfig.instance.craftItemsByStackAmounts(index);
    }

    protected void addAmount(final int i) {
        long resultL = this.getAmountLong();
        if (resultL == 1L && i > 1) {
            resultL = 0L;
        }
        resultL += i;
        if (resultL < 1L) {
            resultL = 1L;
        }
        this.amountTextField.setText(Long.toString(resultL));
        this.amountTextField.setCursorPositionEnd();
    }

    protected int getAmount() {
        final String out = this.amountTextField.getText();
        final double resultD = Calculator.conversion(out);
        return !(resultD <= 0.0) && !Double.isNaN(resultD) ? (int) ArithHelper.round(resultD, 0) : 0;
    }

    protected long getAmountLong() {
        final String out = this.amountTextField.getText();
        final double resultD = Calculator.conversion(out);
        return !(resultD <= 0.0) && !Double.isNaN(resultD) ? (long) ArithHelper.round(resultD, 0) : 0L;
    }

    protected String getBackground() {
        return "guis/craftAmt.png";
    }
}
