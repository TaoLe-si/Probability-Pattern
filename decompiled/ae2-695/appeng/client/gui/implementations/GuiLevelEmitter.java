/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.LevelType;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerLevelEmitter;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.parts.automation.PartLevelEmitter;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

public class GuiLevelEmitter
extends GuiUpgradeable {
    private MEGuiTextField amountTextField;
    private boolean isValidText;
    private GuiButton plus1;
    private GuiButton plus10;
    private GuiButton plus100;
    private GuiButton plus1000;
    private GuiButton minus1;
    private GuiButton minus10;
    private GuiButton minus100;
    private GuiButton minus1000;
    private GuiButton setButton;
    private GuiImgButton levelMode;
    private GuiImgButton craftingMode;

    public GuiLevelEmitter(InventoryPlayer inventoryPlayer, PartLevelEmitter te) {
        super(new ContainerLevelEmitter(inventoryPlayer, te));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.amountTextField = new MEGuiTextField(90, 12);
        this.amountTextField.x = this.guiLeft + 39;
        this.amountTextField.y = this.guiTop + 44;
        this.amountTextField.setFocused(true);
        ((ContainerLevelEmitter)this.inventorySlots).setTextField(this.amountTextField);
        this.validateText();
    }

    @Override
    protected void addButtons() {
        this.levelMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.LEVEL_TYPE, LevelType.ITEM_LEVEL);
        this.redstoneMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.REDSTONE_EMITTER, RedstoneMode.LOW_SIGNAL);
        this.fuzzyMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 48, Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.craftingMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 48, Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        int a = AEConfig.instance.levelByStackAmounts(0);
        int b = AEConfig.instance.levelByStackAmounts(1);
        int c = AEConfig.instance.levelByStackAmounts(2);
        int d = AEConfig.instance.levelByStackAmounts(3);
        this.plus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 17, 22, 20, "+" + a);
        this.buttonList.add(this.plus1);
        this.plus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 17, 28, 20, "+" + b);
        this.buttonList.add(this.plus10);
        this.plus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 17, 32, 20, "+" + c);
        this.buttonList.add(this.plus100);
        this.plus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 17, 38, 20, "+" + d);
        this.buttonList.add(this.plus1000);
        this.minus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 63, 22, 20, "-" + a);
        this.buttonList.add(this.minus1);
        this.minus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 63, 28, 20, "-" + b);
        this.buttonList.add(this.minus10);
        this.minus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 63, 32, 20, "-" + c);
        this.buttonList.add(this.minus100);
        this.minus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 63, 38, 20, "-" + d);
        this.buttonList.add(this.minus1000);
        this.setButton = new GuiButton(0, this.guiLeft + 134, this.guiTop + 40, 28, 20, GuiText.Set.getLocal());
        this.buttonList.add(this.setButton);
        this.buttonList.add(this.levelMode);
        this.buttonList.add(this.redstoneMode);
        this.buttonList.add(this.fuzzyMode);
        this.buttonList.add(this.craftingMode);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        boolean notCraftingMode = this.bc.getInstalledUpgrades(Upgrades.CRAFTING) == 0;
        this.amountTextField.setEnabled(notCraftingMode);
        this.setButton.enabled = notCraftingMode && this.isValidText;
        this.plus1.enabled = notCraftingMode;
        this.plus10.enabled = notCraftingMode;
        this.plus100.enabled = notCraftingMode;
        this.plus1000.enabled = notCraftingMode;
        this.minus1.enabled = notCraftingMode;
        this.minus10.enabled = notCraftingMode;
        this.minus100.enabled = notCraftingMode;
        this.minus1000.enabled = notCraftingMode;
        this.levelMode.enabled = notCraftingMode;
        this.redstoneMode.enabled = notCraftingMode;
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        if (this.craftingMode != null) {
            this.craftingMode.set(this.cvb.getCraftingMode());
        }
        if (this.levelMode != null) {
            this.levelMode.set(((ContainerLevelEmitter)this.cvb).getLevelMode());
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.amountTextField.drawTextBox();
    }

    @Override
    protected void handleButtonVisibility() {
        this.craftingMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.CRAFTING) > 0);
        this.fuzzyMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.FUZZY) > 0);
    }

    @Override
    protected String getBackground() {
        return "guis/lvlemitter.png";
    }

    @Override
    protected GuiText getName() {
        return GuiText.LevelEmitter;
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.setButton && this.setButton.enabled) {
            try {
                String amountString = Long.toString(this.getAmountLong());
                this.amountTextField.setText(amountString);
                NetworkHandler.instance.sendToServer(new PacketValueConfig("LevelEmitter.Value", amountString));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        } else if (btn == this.craftingMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.craftingMode.getSetting(), backwards));
        } else if (btn == this.levelMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.levelMode.getSetting(), backwards));
        } else {
            boolean isMinus;
            boolean isPlus = btn == this.plus1 || btn == this.plus10 || btn == this.plus100 || btn == this.plus1000;
            boolean bl = isMinus = btn == this.minus1 || btn == this.minus10 || btn == this.minus100 || btn == this.minus1000;
            if (isPlus || isMinus) {
                long result = this.addOrderAmount(this.getQty(btn));
                this.amountTextField.setText(Long.toString(result));
            }
        }
    }

    private long addOrderAmount(int i) {
        long resultL = this.getAmountLong();
        if (resultL == 1L && i > 1) {
            resultL = 0L;
        }
        if ((resultL += (long)i) < 1L) {
            resultL = 1L;
        }
        return resultL;
    }

    private long getAmountLong() {
        String out = this.amountTextField.getText();
        double resultD = Calculator.conversion(out);
        if (resultD <= 0.0 || Double.isNaN(resultD)) {
            return 0L;
        }
        return (long)ArithHelper.round(resultD, 0);
    }

    private void validateText() {
        String text = this.amountTextField.getText();
        double resultD = Calculator.conversion(text);
        this.isValidText = !Double.isNaN(resultD);
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        boolean notCraftingMode;
        boolean bl = notCraftingMode = this.bc.getInstalledUpgrades(Upgrades.CRAFTING) == 0;
        if (notCraftingMode) {
            this.amountTextField.mouseClicked(xCoord, yCoord, btn);
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void keyTyped(char character, int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28 || key == 156) {
                this.actionPerformed(this.setButton);
            } else {
                boolean typedTextbox = this.amountTextField.textboxKeyTyped(character, key);
                if (typedTextbox) {
                    this.validateText();
                } else {
                    super.keyTyped(character, key);
                }
            }
        }
    }
}

