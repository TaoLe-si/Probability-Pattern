/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import appeng.client.texture.ExtraBlockTextures;
import appeng.util.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

public class GuiSimpleImgButton
extends GuiButton
implements ITooltip {
    private int iconIndex;
    private String tooltip;

    public GuiSimpleImgButton(int x, int y, int iconIndex, String tooltip) {
        super(0, 0, 16, "");
        this.xPosition = x;
        this.yPosition = y;
        this.width = 16;
        this.height = 16;
        this.iconIndex = iconIndex;
        this.tooltip = tooltip;
    }

    public void setVisibility(boolean vis) {
        this.visible = vis;
        this.enabled = vis;
    }

    public void drawButton(Minecraft par1Minecraft, int par2, int par3) {
        if (this.visible) {
            if (this.enabled) {
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            } else {
                GL11.glColor4f((float)0.5f, (float)0.5f, (float)0.5f, (float)1.0f);
            }
            par1Minecraft.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
            this.field_146123_n = par2 >= this.xPosition && par3 >= this.yPosition && par2 < this.xPosition + this.width && par3 < this.yPosition + this.height;
            int uv_y = this.iconIndex / 16;
            int uv_x = this.iconIndex - uv_y * 16;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 240, 240, 16, 16);
            this.drawTexturedModalRect(this.xPosition, this.yPosition, uv_x * 16, uv_y * 16, 16, 16);
            this.mouseDragged(par1Minecraft, par2, par3);
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    @Override
    public String getMessage() {
        if (Platform.isServer()) {
            return this.tooltip;
        }
        if (!this.tooltip.contains("\n")) {
            return this.tooltip;
        }
        int i = this.tooltip.indexOf("\n");
        String name = this.tooltip.substring(0, i);
        String value = this.tooltip.substring(i + 1);
        value = Minecraft.getMinecraft().fontRenderer.wrapFormattedStringToWidth(value, 150);
        return name + '\n' + value;
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
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    public int getIconIndex() {
        return this.iconIndex;
    }

    public void setIconIndex(int iconIndex) {
        this.iconIndex = iconIndex;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}

