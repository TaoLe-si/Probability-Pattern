/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.AdvancedBlockingMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiSimpleImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.container.implementations.ContainerInterface;
import appeng.core.AELog;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IInterfaceHost;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiInterface
extends GuiUpgradeable {
    private GuiTabButton priority;
    private GuiImgButton BlockMode;
    private GuiImgButton SmartBlockMode;
    private GuiImgButton fuzzyMode;
    private GuiToggleButton interfaceMode;
    private GuiImgButton insertionMode;
    private GuiSimpleImgButton doublePatterns;
    private GuiToggleButton patternOptimization;
    private GuiImgButton advancedBlockingMode;
    private GuiImgButton lockCraftingMode;

    public GuiInterface(InventoryPlayer inventoryPlayer, IInterfaceHost te) {
        super(new ContainerInterface(inventoryPlayer, te));
        this.ySize = 211;
    }

    @Override
    protected void addButtons() {
        this.priority = new GuiTabButton(this.guiLeft + 154, this.guiTop, 66, GuiText.Priority.getLocal(), itemRender);
        this.buttonList.add(this.priority);
        int offset = 8;
        this.BlockMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + offset, Settings.BLOCK, YesNo.NO);
        this.buttonList.add(this.BlockMode);
        this.SmartBlockMode = new GuiImgButton(this.guiLeft - 36, this.guiTop + offset, Settings.SMART_BLOCK, YesNo.NO);
        this.buttonList.add(this.SmartBlockMode);
        this.interfaceMode = new GuiToggleButton(this.guiLeft - 18, this.guiTop + (offset += 18), 84, 85, GuiText.InterfaceTerminal.getLocal(), GuiText.InterfaceTerminalHint.getLocal());
        this.buttonList.add(this.interfaceMode);
        this.insertionMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + (offset += 18), Settings.INSERTION_MODE, InsertionMode.DEFAULT);
        this.buttonList.add(this.insertionMode);
        this.doublePatterns = new GuiSimpleImgButton(this.guiLeft - 18, this.guiTop + (offset += 18), 71, "");
        this.doublePatterns.enabled = false;
        this.buttonList.add(this.doublePatterns);
        this.patternOptimization = new GuiToggleButton(this.guiLeft - 18, this.guiTop + (offset += 18), 178, 194, GuiText.PatternOptimization.getLocal(), GuiText.PatternOptimizationHint.getLocal());
        this.buttonList.add(this.patternOptimization);
        this.advancedBlockingMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + (offset += 18), Settings.ADVANCED_BLOCKING_MODE, AdvancedBlockingMode.DEFAULT);
        this.advancedBlockingMode.visible = this.bc.getInstalledUpgrades(Upgrades.ADVANCED_BLOCKING) > 0;
        this.buttonList.add(this.advancedBlockingMode);
        this.lockCraftingMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + (offset += 18), Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE);
        this.lockCraftingMode.visible = this.bc.getInstalledUpgrades(Upgrades.LOCK_CRAFTING) > 0;
        this.buttonList.add(this.lockCraftingMode);
        this.fuzzyMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + (offset += 18), Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.fuzzyMode.visible = this.bc.getInstalledUpgrades(Upgrades.FUZZY) > 0;
        this.buttonList.add(this.fuzzyMode);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        if (this.BlockMode != null) {
            this.BlockMode.set(((ContainerInterface)this.cvb).getBlockingMode());
        }
        if (this.SmartBlockMode != null) {
            this.SmartBlockMode.set(((ContainerInterface)this.cvb).getSmartBlockingMode());
        }
        if (this.interfaceMode != null) {
            this.interfaceMode.setState(((ContainerInterface)this.cvb).getInterfaceTerminalMode() == YesNo.YES);
        }
        if (this.insertionMode != null) {
            this.insertionMode.set(((ContainerInterface)this.cvb).getInsertionMode());
        }
        if (this.doublePatterns != null) {
            this.doublePatterns.enabled = ((ContainerInterface)this.cvb).isAllowedToMultiplyPatterns;
            if (this.doublePatterns.enabled) {
                this.doublePatterns.setTooltip(ButtonToolTips.DoublePatterns.getLocal() + "\n" + ButtonToolTips.DoublePatternsHint.getLocal());
            } else {
                this.doublePatterns.setTooltip(ButtonToolTips.DoublePatterns.getLocal() + "\n" + ButtonToolTips.OptimizePatternsNoReq.getLocal());
            }
        }
        if (this.patternOptimization != null) {
            this.patternOptimization.setState(((ContainerInterface)this.cvb).getPatternOptimization() == YesNo.YES);
        }
        if (this.advancedBlockingMode != null) {
            this.advancedBlockingMode.set(((ContainerInterface)this.cvb).getAdvancedBlockingMode());
        }
        if (this.lockCraftingMode != null) {
            this.lockCraftingMode.set(((ContainerInterface)this.cvb).getLockCraftingMode());
        }
        if (this.fuzzyMode != null) {
            this.fuzzyMode.set(((ContainerInterface)this.cvb).getFuzzyMode());
        }
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.Interface.getLocal()), 8, 6, GuiColors.InterfaceTitle.getColor());
    }

    @Override
    protected String getBackground() {
        String string;
        switch (((ContainerInterface)this.cvb).getPatternCapacityCardsInstalled()) {
            case -1: {
                string = "guis/interfacenonenoconfig.png";
                break;
            }
            case 1: {
                string = "guis/interface2.png";
                break;
            }
            case 2: {
                string = "guis/interface3.png";
                break;
            }
            case 3: {
                string = "guis/interface4.png";
                break;
            }
            default: {
                string = "guis/interface.png";
            }
        }
        return string;
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.priority) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
        }
        if (btn == this.interfaceMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(Settings.INTERFACE_TERMINAL, backwards));
        }
        if (btn == this.BlockMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.BlockMode.getSetting(), backwards));
        }
        if (btn == this.SmartBlockMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.SmartBlockMode.getSetting(), backwards));
        }
        if (btn == this.insertionMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.insertionMode.getSetting(), backwards));
        }
        if (btn == this.doublePatterns) {
            try {
                int val;
                int n = val = Keyboard.isKeyDown((int)42) ? 1 : 0;
                if (backwards) {
                    val |= 2;
                }
                NetworkHandler.instance.sendToServer(new PacketValueConfig("Interface.DoublePatterns", String.valueOf(val)));
            }
            catch (Throwable e) {
                AELog.debug(e);
            }
        }
        if (btn == this.patternOptimization) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(Settings.PATTERN_OPTIMIZATION, backwards));
        }
        if (btn == this.advancedBlockingMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.advancedBlockingMode.getSetting(), backwards));
        }
        if (btn == this.lockCraftingMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.lockCraftingMode.getSetting(), backwards));
        }
        if (btn == this.fuzzyMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.fuzzyMode.getSetting(), backwards));
        }
    }

    @Override
    protected void handleButtonVisibility() {
        super.handleButtonVisibility();
        if (this.advancedBlockingMode != null) {
            this.advancedBlockingMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.ADVANCED_BLOCKING) > 0);
        }
        if (this.lockCraftingMode != null) {
            this.lockCraftingMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.LOCK_CRAFTING) > 0);
        }
        if (this.fuzzyMode != null) {
            this.fuzzyMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.FUZZY) > 0);
        }
    }
}

