/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.CraftingMode;
import appeng.api.config.Settings;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCraftRequest;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.parts.reporting.PartTerminal;
import appeng.util.Platform;
import java.util.Iterator;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

public class GuiCraftAmount
extends GuiAmount {
    private GuiImgButton craftingMode;

    public GuiCraftAmount(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerCraftAmount(inventoryPlayer, te));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.craftingMode = new GuiImgButton(this.guiLeft + 10, this.guiTop + 53, Settings.CRAFTING_MODE, CraftingMode.STANDARD);
        this.buttonList.add(this.craftingMode);
        ((ContainerCraftAmount)this.inventorySlots).setAmountField(this.amountTextField);
    }

    @Override
    protected void setOriginGUI(Object target) {
        ItemStack stack;
        Iterator iterator;
        IDefinitions definitions = AEApi.instance().definitions();
        IParts parts = definitions.parts();
        if (target instanceof WirelessTerminalGuiObject) {
            iterator = definitions.items().wirelessTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack wirelessTerminalStack;
                this.myIcon = wirelessTerminalStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_WIRELESS_TERM;
        }
        if (target instanceof PartTerminal) {
            iterator = parts.terminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_ME;
        }
        if (target instanceof PartCraftingTerminal) {
            iterator = parts.craftingTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
        }
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
        this.nextBtn.displayString = GuiCraftAmount.isShiftKeyDown() && !GuiCraftAmount.isCtrlKeyDown() ? GuiText.Start.getLocal() : (!GuiCraftAmount.isShiftKeyDown() && GuiCraftAmount.isCtrlKeyDown() ? GuiText.Start.getLocal() : GuiText.Next.getLocal());
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
            if (btn == this.craftingMode) {
                GuiImgButton iBtn = (GuiImgButton)btn;
                Enum cv = iBtn.getCurrentValue();
                boolean backwards = Mouse.isButtonDown((int)1);
                Enum next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
                iBtn.set(next);
            }
            if (btn == this.nextBtn && btn.enabled) {
                NetworkHandler.instance.sendToServer(new PacketCraftRequest(this.getAmountLong(), GuiCraftAmount.isShiftKeyDown(), GuiCraftAmount.isCtrlKeyDown(), (CraftingMode)this.craftingMode.getCurrentValue()));
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

