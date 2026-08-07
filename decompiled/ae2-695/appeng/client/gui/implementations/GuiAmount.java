/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.inventory.Container
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

public abstract class GuiAmount
extends AEBaseGui {
    protected MEGuiTextField amountTextField;
    protected GuiTabButton originalGuiBtn;
    protected GuiButton nextBtn;
    protected GuiButton plus1;
    protected GuiButton plus10;
    protected GuiButton plus100;
    protected GuiButton plus1000;
    protected GuiButton minus1;
    protected GuiButton minus10;
    protected GuiButton minus100;
    protected GuiButton minus1000;
    protected GuiBridge originalGui;
    protected ItemStack myIcon;

    public GuiAmount(Container container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        int a = this.getButtonQtyByIndex(0);
        int b = this.getButtonQtyByIndex(1);
        int c = this.getButtonQtyByIndex(2);
        int d = this.getButtonQtyByIndex(3);
        this.plus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 26, 22, 20, "+" + a);
        this.buttonList.add(this.plus1);
        this.plus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 26, 28, 20, "+" + b);
        this.buttonList.add(this.plus10);
        this.plus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 26, 32, 20, "+" + c);
        this.buttonList.add(this.plus100);
        this.plus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 26, 38, 20, "+" + d);
        this.buttonList.add(this.plus1000);
        this.minus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 75, 22, 20, "-" + a);
        this.buttonList.add(this.minus1);
        this.minus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 75, 28, 20, "-" + b);
        this.buttonList.add(this.minus10);
        this.minus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 75, 32, 20, "-" + c);
        this.buttonList.add(this.minus100);
        this.minus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 75, 38, 20, "-" + d);
        this.buttonList.add(this.minus1000);
        this.nextBtn = new GuiButton(0, this.guiLeft + 128, this.guiTop + 51, 38, 20, GuiText.Next.getLocal());
        this.buttonList.add(this.nextBtn);
        Object target = ((AEBaseContainer)this.inventorySlots).getTarget();
        this.setOriginGUI(target);
        if (this.originalGui != null && this.myIcon != null) {
            this.originalGuiBtn = new GuiTabButton(this.guiLeft + 154, this.guiTop, this.myIcon, this.myIcon.getDisplayName(), itemRender);
            this.buttonList.add(this.originalGuiBtn);
        }
        this.amountTextField = new MEGuiTextField(61, 12);
        this.amountTextField.x = this.guiLeft + 60;
        this.amountTextField.y = this.guiTop + 55;
        this.amountTextField.setMaxStringLength(16);
        this.amountTextField.setFocused(true);
    }

    protected abstract void setOriginGUI(Object var1);

    protected int getButtonQtyByIndex(int index) {
        return AEConfig.instance.craftItemsByStackAmounts(index);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture(this.getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    protected void keyTyped(char character, int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28 || key == 156) {
                this.actionPerformed(this.nextBtn);
            }
            this.amountTextField.textboxKeyTyped(character, key);
            super.keyTyped(character, key);
        }
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        super.mouseClicked(xCoord, yCoord, btn);
        this.amountTextField.mouseClickedNoFocusDrop(xCoord, yCoord, btn);
    }

    protected void actionPerformed(GuiButton btn) {
        boolean isMinus;
        super.actionPerformed(btn);
        if (btn == this.originalGuiBtn) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(this.originalGui));
        }
        boolean isPlus = btn == this.plus1 || btn == this.plus10 || btn == this.plus100 || btn == this.plus1000;
        boolean bl = isMinus = btn == this.minus1 || btn == this.minus10 || btn == this.minus100 || btn == this.minus1000;
        if (isPlus || isMinus) {
            this.addAmount(this.getQty(btn));
        }
    }

    protected void addAmount(int i) {
        long resultL = this.getAmountLong();
        if (resultL == 1L && i > 1) {
            resultL = 0L;
        }
        if ((resultL += (long)i) < 1L) {
            resultL = 1L;
        }
        this.amountTextField.setText(Long.toString(resultL));
        this.amountTextField.setCursorPositionEnd();
    }

    protected int getAmount() {
        String out = this.amountTextField.getText();
        double resultD = Calculator.conversion(out);
        if (resultD <= 0.0 || Double.isNaN(resultD)) {
            return 0;
        }
        return (int)ArithHelper.round(resultD, 0);
    }

    protected long getAmountLong() {
        String out = this.amountTextField.getText();
        double resultD = Calculator.conversion(out);
        if (resultD <= 0.0 || Double.isNaN(resultD)) {
            return 0L;
        }
        return (long)ArithHelper.round(resultD, 0);
    }

    protected abstract String getBackground();
}

