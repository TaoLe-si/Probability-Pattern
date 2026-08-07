/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.Gui
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.api.util.AEColor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

public abstract class GuiContextMenu {
    int x = 0;
    int y = 0;
    int width = 0;
    int visibleSections;
    int scrollOffset = 0;
    boolean isActive = false;
    protected List<?> list = new ArrayList();
    FontRenderer fontRenderer;
    private final int SECTION_HEIGHT = 12;

    public GuiContextMenu(int visibleRows) {
        this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
        this.SECTION_HEIGHT = 12;
        this.visibleSections = visibleRows;
    }

    public abstract void action(int var1);

    public abstract String getDrawText(int var1);

    public boolean isActive() {
        return this.isActive;
    }

    public void init(List<?> list, int x, int y) {
        this.list = list;
        this.x = x;
        this.y = y;
        this.width = this.getMaxWidth();
        this.isActive = true;
    }

    private int getMaxWidth() {
        int width = 0;
        for (int i = 0; i < this.list.size(); ++i) {
            int newWidth = this.fontRenderer.getStringWidth(this.getDrawText(i));
            if (newWidth <= width) continue;
            width = newWidth;
        }
        return width + 3;
    }

    public void clear() {
        this.x = 0;
        this.y = 0;
        this.isActive = false;
        this.scrollOffset = 0;
        this.list.clear();
    }

    public boolean mouseClick(int x, int y, int button) {
        if (this.isActive) {
            if (this.isMouseOn(x, y)) {
                for (int i = 0; i < this.visibleSections && i < this.list.size(); ++i) {
                    if (y >= this.y + 12 + 12 * i) continue;
                    this.action(i + this.scrollOffset);
                    return true;
                }
            } else {
                this.clear();
            }
        }
        return false;
    }

    public boolean mouseWheelEvent(int x, int y, int wheel) {
        if (this.isMouseOn(x, y)) {
            if (wheel > 0 && this.scrollOffset > 0) {
                --this.scrollOffset;
            } else if (wheel < 0 && this.scrollOffset < this.list.size() - this.visibleSections) {
                ++this.scrollOffset;
            }
            return true;
        }
        return false;
    }

    public boolean isMouseOn(int x, int y) {
        return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + 12 * this.visibleSections;
    }

    public void draw(int mouseX, int mouseY) {
        if (!this.isActive) {
            return;
        }
        int j = 0;
        for (int i = this.scrollOffset; j < this.visibleSections && i < this.list.size(); ++j, ++i) {
            int yPos = this.y + 12 * j;
            int xOff = this.x + this.width;
            int color = AEColor.LightGray.mediumVariant - 0x1000000;
            if (mouseX >= this.x && mouseX < xOff && mouseY >= yPos && mouseY < yPos + 12) {
                color = AEColor.Gray.mediumVariant - 0x1000000;
            }
            GL11.glPushMatrix();
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)1000.0f);
            Gui.drawRect((int)this.x, (int)yPos, (int)xOff, (int)(yPos + 12), (int)color);
            this.fontRenderer.drawString(this.getDrawText(i), this.x + 2, yPos + 2, 0x404040);
            Gui.drawRect((int)this.x, (int)yPos, (int)xOff, (int)(yPos + 1), (int)-12566464);
            Gui.drawRect((int)this.x, (int)(yPos + 12 - 1), (int)xOff, (int)(yPos + 12), (int)-12566464);
            Gui.drawRect((int)this.x, (int)yPos, (int)(this.x + 1), (int)(yPos + 12), (int)-12566464);
            Gui.drawRect((int)(xOff - 1), (int)(this.y + 12 * j), (int)xOff, (int)(yPos + 12), (int)-12566464);
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)0.0f);
            GL11.glPopMatrix();
        }
    }
}

