/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.util.StatCollector
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import appeng.client.texture.ExtraBlockTextures;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

public class GuiToggleButton
extends GuiButton
implements ITooltip {
    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\\n", 16);
    private final int iconIdxOn;
    private final int iconIdxOff;
    private final String displayName;
    private final String displayHint;
    private boolean isActive;

    public GuiToggleButton(int x, int y, int on, int off, String displayName, String displayHint) {
        super(0, 0, 16, "");
        this.iconIdxOn = on;
        this.iconIdxOff = off;
        this.displayName = displayName;
        this.displayHint = displayHint;
        this.xPosition = x;
        this.yPosition = y;
        this.width = 16;
        this.height = 16;
    }

    public void setState(boolean isOn) {
        this.isActive = isOn;
    }

    public void drawButton(Minecraft par1Minecraft, int par2, int par3) {
        if (this.visible) {
            int iconIndex = this.getIconIndex();
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            par1Minecraft.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
            this.field_146123_n = par2 >= this.xPosition && par3 >= this.yPosition && par2 < this.xPosition + this.width && par3 < this.yPosition + this.height;
            int uv_y = (int)Math.floor(iconIndex / 16);
            int uv_x = iconIndex - uv_y * 16;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 240, 240, 16, 16);
            this.drawTexturedModalRect(this.xPosition, this.yPosition, uv_x * 16, uv_y * 16, 16, 16);
            this.mouseDragged(par1Minecraft, par2, par3);
        }
    }

    private int getIconIndex() {
        return this.isActive ? this.iconIdxOn : this.iconIdxOff;
    }

    @Override
    public String getMessage() {
        if (this.displayName != null) {
            StringBuilder sb;
            int i;
            String name = StatCollector.translateToLocal((String)this.displayName);
            String value = StatCollector.translateToLocal((String)this.displayHint);
            if (name == null || name.isEmpty()) {
                name = this.displayName;
            }
            if (value == null || value.isEmpty()) {
                value = this.displayHint;
            }
            if ((i = (sb = new StringBuilder(value = PATTERN_NEW_LINE.matcher(value).replaceAll("\n"))).lastIndexOf("\n")) <= 0) {
                i = 0;
            }
            while (i + 30 < sb.length() && (i = sb.lastIndexOf(" ", i + 30)) != -1) {
                sb.replace(i, i + 1, "\n");
            }
            return name + '\n' + sb;
        }
        return null;
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
}

