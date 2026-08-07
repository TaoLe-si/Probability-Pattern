/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.AppEngSlot;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.InventoryAction;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiPatternTerm
extends GuiMEMonitorable {
    private static final String SUBSITUTION_DISABLE = "0";
    private static final String SUBSITUTION_ENABLE = "1";
    private static final String CRAFTMODE_CRFTING = "1";
    private static final String CRAFTMODE_PROCESSING = "0";
    private final ContainerPatternTerm container;
    private GuiTabButton tabCraftButton;
    private GuiTabButton tabProcessButton;
    private GuiImgButton substitutionsEnabledBtn;
    private GuiImgButton substitutionsDisabledBtn;
    private GuiImgButton beSubstitutionsEnabledBtn;
    private GuiImgButton beSubstitutionsDisabledBtn;
    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;
    private GuiImgButton doubleBtn;

    public GuiPatternTerm(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerPatternTerm(inventoryPlayer, te));
        this.container = (ContainerPatternTerm)this.inventorySlots;
        this.setReservedSpace(81);
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        if (btn == 2 && this.doubleBtn.mousePressed(this.mc, xCoord, yCoord)) {
            InventoryAction action = InventoryAction.SET_PATTERN_MULTI;
            PacketInventoryAction p = new PacketInventoryAction(action, 0, 0L);
            NetworkHandler.instance.sendToServer(p);
        } else {
            super.mouseClicked(xCoord, yCoord, btn);
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (this.tabCraftButton == btn || this.tabProcessButton == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.CraftMode", this.tabProcessButton == btn ? "1" : "0"));
            } else if (this.encodeBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.Encode", GuiPatternTerm.isCtrlKeyDown() ? (GuiPatternTerm.isShiftKeyDown() ? "6" : "1") : (GuiPatternTerm.isShiftKeyDown() ? "2" : "1")));
            } else if (this.clearBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.Clear", "1"));
            } else if (this.substitutionsEnabledBtn == btn || this.substitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.Substitute", this.substitutionsEnabledBtn == btn ? "0" : "1"));
            } else if (this.beSubstitutionsEnabledBtn == btn || this.beSubstitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.BeSubstitute", this.beSubstitutionsEnabledBtn == btn ? "0" : "1"));
            } else if (this.doubleBtn == btn) {
                int val;
                int n = val = Keyboard.isKeyDown((int)42) ? 1 : 0;
                if (Mouse.isButtonDown((int)1)) {
                    val |= 2;
                }
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.Double", String.valueOf(val)));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.tabCraftButton = new GuiTabButton(this.guiLeft + 173, this.guiTop + this.ySize - 177, new ItemStack(Blocks.crafting_table), GuiText.CraftingPattern.getLocal(), itemRender);
        this.buttonList.add(this.tabCraftButton);
        this.tabProcessButton = new GuiTabButton(this.guiLeft + 173, this.guiTop + this.ySize - 177, new ItemStack(Blocks.furnace), GuiText.ProcessingPattern.getLocal(), itemRender);
        this.buttonList.add(this.tabProcessButton);
        this.substitutionsEnabledBtn = new GuiImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 163, Settings.ACTIONS, ItemSubstitution.ENABLED);
        this.substitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsEnabledBtn);
        this.substitutionsDisabledBtn = new GuiImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 163, Settings.ACTIONS, ItemSubstitution.DISABLED);
        this.substitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsDisabledBtn);
        this.beSubstitutionsEnabledBtn = new GuiImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 153, Settings.ACTIONS, PatternBeSubstitution.ENABLED);
        this.beSubstitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsEnabledBtn);
        this.beSubstitutionsDisabledBtn = new GuiImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 153, Settings.ACTIONS, PatternBeSubstitution.DISABLED);
        this.beSubstitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsDisabledBtn);
        this.clearBtn = new GuiImgButton(this.guiLeft + 74, this.guiTop + this.ySize - 163, Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);
        this.encodeBtn = new GuiImgButton(this.guiLeft + 147, this.guiTop + this.ySize - 142, Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);
        this.doubleBtn = new GuiImgButton(this.guiLeft + 74, this.guiTop + this.ySize - 153, Settings.ACTIONS, ActionItems.DOUBLE);
        this.doubleBtn.setHalfSize(true);
        this.buttonList.add(this.doubleBtn);
        this.updateButtonVisibility();
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.updateButtonVisibility();
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.fontRendererObj.drawString(GuiText.PatternTerminal.getLocal(), 8, this.ySize - 96 + 2 - this.getReservedSpace(), GuiColors.PatternTerminalTitle.getColor());
    }

    private void updateButtonVisibility() {
        if (!this.container.isCraftingMode()) {
            this.tabCraftButton.visible = false;
            this.tabProcessButton.visible = true;
            this.doubleBtn.visible = true;
        } else {
            this.tabCraftButton.visible = true;
            this.tabProcessButton.visible = false;
            this.doubleBtn.visible = false;
        }
        if (this.container.substitute) {
            this.substitutionsEnabledBtn.visible = true;
            this.substitutionsDisabledBtn.visible = false;
        } else {
            this.substitutionsEnabledBtn.visible = false;
            this.substitutionsDisabledBtn.visible = true;
        }
        this.beSubstitutionsEnabledBtn.visible = this.container.beSubstitute;
        this.beSubstitutionsDisabledBtn.visible = !this.container.beSubstitute;
    }

    @Override
    protected String getBackground() {
        if (this.container.isCraftingMode()) {
            return "guis/pattern.png";
        }
        return "guis/pattern2.png";
    }

    @Override
    protected void repositionSlot(AppEngSlot s) {
        s.yDisplayPosition = s.isPlayerSide() ? s.getY() + this.ySize - 78 - 5 : s.getY() + this.ySize - 78 - 3;
    }
}

