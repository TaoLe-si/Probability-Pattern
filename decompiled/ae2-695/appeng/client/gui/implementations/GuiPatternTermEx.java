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

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.PatternSlotConfig;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerPatternTermEx;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiPatternTermEx
extends GuiMEMonitorable {
    private static final String SUBSITUTION_DISABLE = "0";
    private static final String SUBSITUTION_ENABLE = "1";
    private final ContainerPatternTermEx container;
    private GuiImgButton substitutionsEnabledBtn;
    private GuiImgButton substitutionsDisabledBtn;
    private GuiImgButton beSubstitutionsEnabledBtn;
    private GuiImgButton beSubstitutionsDisabledBtn;
    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;
    private GuiImgButton invertBtn;
    private GuiImgButton doubleBtn;
    private final GuiScrollbar processingScrollBar = new GuiScrollbar();

    public GuiPatternTermEx(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerPatternTermEx(inventoryPlayer, te));
        this.container = (ContainerPatternTermEx)this.inventorySlots;
        this.setReservedSpace(81);
        this.processingScrollBar.setHeight(70).setWidth(7).setLeft(6).setRange(0, 1, 1);
        this.processingScrollBar.setTexture("appliedenergistics2", "guis/pattern3.png", 242, 0);
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (this.encodeBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.Encode", GuiPatternTermEx.isCtrlKeyDown() ? (GuiPatternTermEx.isShiftKeyDown() ? "6" : SUBSITUTION_ENABLE) : (GuiPatternTermEx.isShiftKeyDown() ? "2" : SUBSITUTION_ENABLE)));
            } else if (this.clearBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.Clear", SUBSITUTION_ENABLE));
            } else if (this.invertBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.Invert", this.container.inverted ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            } else if (this.substitutionsEnabledBtn == btn || this.substitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.Substitute", this.substitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            } else if (this.beSubstitutionsEnabledBtn == btn || this.beSubstitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.BeSubstitute", this.beSubstitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            } else if (this.doubleBtn == btn) {
                int val;
                int n = val = Keyboard.isKeyDown((int)42) ? 1 : 0;
                if (Mouse.isButtonDown((int)1)) {
                    val |= 2;
                }
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.Double", String.valueOf(val)));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.substitutionsEnabledBtn = new GuiImgButton(this.guiLeft + 97, this.guiTop + this.ySize - 163, Settings.ACTIONS, ItemSubstitution.ENABLED);
        this.substitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsEnabledBtn);
        this.substitutionsDisabledBtn = new GuiImgButton(this.guiLeft + 97, this.guiTop + this.ySize - 163, Settings.ACTIONS, ItemSubstitution.DISABLED);
        this.substitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsDisabledBtn);
        this.beSubstitutionsEnabledBtn = new GuiImgButton(this.guiLeft + 97, this.guiTop + this.ySize - 143, Settings.ACTIONS, PatternBeSubstitution.ENABLED);
        this.beSubstitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsEnabledBtn);
        this.beSubstitutionsDisabledBtn = new GuiImgButton(this.guiLeft + 97, this.guiTop + this.ySize - 143, Settings.ACTIONS, PatternBeSubstitution.DISABLED);
        this.beSubstitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsDisabledBtn);
        this.clearBtn = new GuiImgButton(this.guiLeft + 87, this.guiTop + this.ySize - 163, Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);
        this.encodeBtn = new GuiImgButton(this.guiLeft + 147, this.guiTop + this.ySize - 142, Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);
        this.invertBtn = new GuiImgButton(this.guiLeft + 87, this.guiTop + this.ySize - 153, Settings.ACTIONS, this.container.inverted ? PatternSlotConfig.C_4_16 : PatternSlotConfig.C_16_4);
        this.invertBtn.setHalfSize(true);
        this.buttonList.add(this.invertBtn);
        this.doubleBtn = new GuiImgButton(this.guiLeft + 97, this.guiTop + this.ySize - 153, Settings.ACTIONS, ActionItems.DOUBLE);
        this.doubleBtn.setHalfSize(true);
        this.buttonList.add(this.doubleBtn);
        this.processingScrollBar.setTop(this.ySize - 164);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.fontRendererObj.drawString(GuiText.PatternTerminalEx.getLocal(), 8, this.ySize - 96 + 2 - this.getReservedSpace(), GuiColors.PatternTerminalEx.getColor());
        this.processingScrollBar.draw(this);
    }

    @Override
    protected String getBackground() {
        return this.container.inverted ? "guis/pattern4.png" : "guis/pattern3.png";
    }

    @Override
    protected void repositionSlot(AppEngSlot s) {
        s.yDisplayPosition = s.isPlayerSide() ? s.getY() + this.ySize - 78 - 5 : s.getY() + this.ySize - 78 - 3;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        if (this.container.substitute) {
            this.substitutionsEnabledBtn.visible = true;
            this.substitutionsDisabledBtn.visible = false;
        } else {
            this.substitutionsEnabledBtn.visible = false;
            this.substitutionsDisabledBtn.visible = true;
        }
        this.beSubstitutionsEnabledBtn.visible = this.container.beSubstitute;
        this.beSubstitutionsDisabledBtn.visible = !this.container.beSubstitute;
        int offset = this.container.inverted ? -54 : 0;
        this.substitutionsEnabledBtn.xPosition = this.guiLeft + 97 + offset;
        this.substitutionsDisabledBtn.xPosition = this.guiLeft + 97 + offset;
        this.beSubstitutionsEnabledBtn.xPosition = this.guiLeft + 97 + offset;
        this.beSubstitutionsDisabledBtn.xPosition = this.guiLeft + 97 + offset;
        this.doubleBtn.xPosition = this.guiLeft + 97 + offset;
        this.clearBtn.xPosition = this.guiLeft + 87 + offset;
        this.invertBtn.xPosition = this.guiLeft + 87 + offset;
        this.processingScrollBar.setCurrentScroll(this.container.activePage);
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        int currentScroll = this.processingScrollBar.getCurrentScroll();
        this.processingScrollBar.click(this, xCoord - this.guiLeft, yCoord - this.guiTop);
        if (btn == 2 && this.doubleBtn.mousePressed(this.mc, xCoord, yCoord)) {
            InventoryAction action = InventoryAction.SET_PATTERN_MULTI;
            PacketInventoryAction p = new PacketInventoryAction(action, 0, 0L);
            NetworkHandler.instance.sendToServer(p);
        } else {
            super.mouseClicked(xCoord, yCoord, btn);
        }
        if (currentScroll != this.processingScrollBar.getCurrentScroll()) {
            this.changeActivePage();
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int c, long d) {
        int currentScroll = this.processingScrollBar.getCurrentScroll();
        this.processingScrollBar.clickMove(y - this.guiTop);
        super.mouseClickMove(x, y, c, d);
        if (currentScroll != this.processingScrollBar.getCurrentScroll()) {
            this.changeActivePage();
        }
    }

    @Override
    public void handleMouseInput() {
        int y;
        int x;
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.processingScrollBar.contains((x = Mouse.getEventX() * this.width / this.mc.displayWidth) - this.guiLeft, (y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight) - this.guiTop)) {
            int currentScroll = this.processingScrollBar.getCurrentScroll();
            this.processingScrollBar.wheel(wheel);
            if (currentScroll != this.processingScrollBar.getCurrentScroll()) {
                this.changeActivePage();
            }
        }
    }

    private void changeActivePage() {
        try {
            NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminalEx.ActivePage", String.valueOf(this.processingScrollBar.getCurrentScroll())));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

