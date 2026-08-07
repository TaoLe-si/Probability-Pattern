/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  codechicken.nei.FormattedTextField$TextFormatter
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiTextField
 *  org.lwjgl.input.Keyboard
 */
package appeng.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import appeng.core.localization.GuiColors;
import codechicken.nei.FormattedTextField;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

public class MEGuiTextField
implements ITooltip {
    protected GuiTextField field;
    private static final int PADDING = 2;
    private static boolean previousKeyboardRepeatEnabled;
    private static MEGuiTextField previousKeyboardRepeatEnabledField;
    private Method setFormatterMethod;
    private String tooltip;
    private int fontPad;
    public int x;
    public int y;
    public int w;
    public int h;

    public MEGuiTextField(int width, int height, String tooltip) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        try {
            Class<?> formattedTextFieldClass = Class.forName("codechicken.nei.FormattedTextField");
            Constructor<?> defaultConstructor = formattedTextFieldClass.getConstructor(FontRenderer.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            this.field = (GuiTextField)defaultConstructor.newInstance(fontRenderer, 0, 0, 0, 0);
            this.setFormatterMethod = formattedTextFieldClass.getMethod("setFormatter", FormattedTextField.TextFormatter.class);
        }
        catch (Throwable __) {
            this.field = new GuiTextField(fontRenderer, 0, 0, 0, 0);
        }
        this.w = width;
        this.h = height;
        this.field.setEnableBackgroundDrawing(false);
        this.field.setMaxStringLength(256);
        this.field.setTextColor(GuiColors.SearchboxText.getColor());
        this.field.setCursorPositionZero();
        this.setMessage(tooltip);
        this.fontPad = fontRenderer.getCharWidth('_');
        this.setDimensionsAndColor();
    }

    public MEGuiTextField(int width, int height) {
        this(width, height, "");
    }

    public MEGuiTextField() {
        this(0, 0);
    }

    public void setFormatter(Object formatter) {
        if (this.setFormatterMethod != null) {
            try {
                this.setFormatterMethod.invoke((Object)this.field, formatter);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    protected void setDimensionsAndColor() {
        this.field.xPosition = this.x + 2;
        this.field.yPosition = this.y + 2;
        this.field.width = this.w - 4 - this.fontPad;
        this.field.height = this.h - 4;
    }

    public void onTextChange(String oldText) {
    }

    public void mouseClicked(int xPos, int yPos, int button) {
        if (!this.isMouseIn(xPos, yPos)) {
            this.setFocused(false);
            return;
        }
        this.field.setCanLoseFocus(false);
        this.setFocused(true);
        if (button == 1) {
            this.setText("");
        } else {
            this.field.mouseClicked(xPos, yPos, button);
        }
        this.field.setCanLoseFocus(true);
    }

    public void mouseClickedNoFocusDrop(int xPos, int yPos, int button) {
        if (this.isMouseIn(xPos, yPos)) {
            this.field.setCanLoseFocus(false);
            if (button == 1) {
                this.setText("");
            } else {
                this.field.mouseClicked(xPos, yPos, button);
            }
        }
    }

    public boolean isMouseIn(int xCoord, int yCoord) {
        boolean withinXRange = this.x <= xCoord && xCoord < this.x + this.w;
        boolean withinYRange = this.y <= yCoord && yCoord < this.y + this.h;
        return withinXRange && withinYRange;
    }

    public boolean textboxKeyTyped(char keyChar, int keyID) {
        boolean handled;
        if (!this.isFocused()) {
            return false;
        }
        String oldText = this.getText();
        switch (keyID) {
            case 203: {
                if (GuiScreen.isShiftKeyDown()) {
                    if (!GuiScreen.isCtrlKeyDown()) {
                        this.field.setSelectionPos(this.field.getNthWordFromPos(-1, this.field.getSelectionEnd()));
                    } else {
                        this.field.setSelectionPos(this.field.getSelectionEnd() - 1);
                    }
                } else if (!GuiScreen.isCtrlKeyDown() && !this.field.getSelectedText().isEmpty()) {
                    this.field.setCursorPosition(this.field.getNthWordFromCursor(-1));
                } else if (GuiScreen.isCtrlKeyDown()) {
                    this.field.setCursorPosition(this.field.getNthWordFromCursor(-1));
                } else {
                    this.field.moveCursorBy(-1);
                }
                handled = true;
                break;
            }
            case 205: {
                if (GuiScreen.isShiftKeyDown()) {
                    if (!GuiScreen.isCtrlKeyDown()) {
                        this.field.setSelectionPos(this.field.getNthWordFromPos(1, this.field.getSelectionEnd()));
                    } else {
                        this.field.setSelectionPos(this.field.getSelectionEnd() + 1);
                    }
                } else if (!GuiScreen.isCtrlKeyDown() && !this.field.getSelectedText().isEmpty()) {
                    this.field.setCursorPosition(this.field.getNthWordFromCursor(1));
                } else if (GuiScreen.isCtrlKeyDown()) {
                    this.field.setCursorPosition(this.field.getNthWordFromCursor(1));
                } else {
                    this.field.moveCursorBy(1);
                }
                handled = true;
                break;
            }
            default: {
                handled = this.field.textboxKeyTyped(keyChar, keyID);
            }
        }
        if (!(handled || keyID != 28 && keyID != 156 && keyID != 1)) {
            this.setFocused(false);
        }
        if (handled) {
            this.onTextChange(oldText);
        }
        return handled;
    }

    public void drawTextBox() {
        if (this.field.getVisible()) {
            this.setDimensionsAndColor();
            GuiTextField.drawRect((int)(this.x + 1), (int)(this.y + 1), (int)(this.x + this.w - 1), (int)(this.y + this.h - 1), (int)(this.isFocused() ? GuiColors.SearchboxFocused.getColor() : GuiColors.SearchboxUnfocused.getColor()));
            this.field.drawTextBox();
        }
    }

    public void setText(String text, boolean ignoreTrigger) {
        String oldText = this.getText();
        int currentCursorPos = this.field.getCursorPosition();
        this.field.setText(text);
        this.field.setCursorPosition(currentCursorPos);
        if (!ignoreTrigger) {
            this.onTextChange(oldText);
        }
    }

    public void setText(String text) {
        this.setText(text, false);
    }

    public void setCursorPositionEnd() {
        this.field.setCursorPositionEnd();
    }

    public void setFocused(boolean focus) {
        if (this.field.isFocused() == focus) {
            return;
        }
        this.field.setFocused(focus);
        if (focus) {
            if (previousKeyboardRepeatEnabledField == null) {
                previousKeyboardRepeatEnabled = Keyboard.areRepeatEventsEnabled();
            }
            previousKeyboardRepeatEnabledField = this;
            Keyboard.enableRepeatEvents((boolean)true);
        } else if (previousKeyboardRepeatEnabledField == this) {
            previousKeyboardRepeatEnabledField = null;
            Keyboard.enableRepeatEvents((boolean)previousKeyboardRepeatEnabled);
        }
    }

    public void setEnabled(boolean enabled) {
        this.field.setEnabled(enabled);
    }

    public void setMaxStringLength(int size) {
        this.field.setMaxStringLength(size);
    }

    public boolean isFocused() {
        return this.field.isFocused();
    }

    public String getText() {
        return this.field.getText();
    }

    public void setMessage(String t) {
        this.tooltip = t;
    }

    @Override
    public String getMessage() {
        return this.tooltip;
    }

    @Override
    public boolean isVisible() {
        return this.field.getVisible();
    }

    public void setSelectionPos(int pos) {
        this.field.setSelectionPos(pos);
    }

    @Override
    public int xPos() {
        return this.x;
    }

    @Override
    public int yPos() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.w;
    }

    @Override
    public int getHeight() {
        return this.h;
    }
}

