/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 */
package appeng.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class GuiAeButton
extends GuiButton
implements ITooltip {
    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\\n", 16);
    private String tootipString;

    public GuiAeButton(int id, int xPosition, int yPosition, int width, int height, String displayString, String tootipString) {
        super(id, xPosition, yPosition, width, height, displayString);
        this.tootipString = tootipString;
    }

    public void setTootipString(String tootipString) {
        this.tootipString = tootipString;
    }

    @Override
    public String getMessage() {
        if (this.tootipString != null) {
            return PATTERN_NEW_LINE.matcher(this.tootipString).replaceAll("\n");
        }
        return "";
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        super.drawButton(mc, mouseX, mouseY);
        if (this.height < 20) {
            int hoverState = this.getHoverState(this.field_146123_n);
            switch (hoverState) {
                case 0: {
                    this.drawHorizontalLine(this.xPosition + 2, this.xPosition + this.width - 2, this.yPosition + this.height - 2, -13882324);
                    break;
                }
                case 1: {
                    this.drawHorizontalLine(this.xPosition + 2, this.xPosition + this.width - 2, this.yPosition + this.height - 2, -11119018);
                    break;
                }
                case 2: {
                    this.drawHorizontalLine(this.xPosition + 2, this.xPosition + this.width - 2, this.yPosition + this.height - 2, -10721635);
                }
            }
            this.drawHorizontalLine(this.xPosition, this.xPosition + this.width - 1, this.yPosition + this.height - 1, -16777216);
        }
    }

    @Override
    public int xPos() {
        return this.xPosition;
    }

    @Override
    public int yPos() {
        return this.yPosition;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }
}

