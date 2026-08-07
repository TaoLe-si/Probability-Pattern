/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.IScrollSource;
import org.lwjgl.opengl.GL11;

public class GuiScrollbar
implements IScrollSource {
    private String txtBase = "minecraft";
    private String txtFile = "gui/container/creative_inventory/tabs.png";
    private int txtShiftX = 232;
    private int txtShiftY = 0;
    private int displayX = 0;
    private int displayY = 0;
    private int width = 12;
    private int height = 16;
    private int pageSize = 1;
    private int maxScroll = 0;
    private int minScroll = 0;
    private int currentScroll = 0;
    private boolean visible = true;
    private boolean isLatestClickOnScrollbar;

    public void setTexture(String base, String file, int shiftX, int shiftY) {
        this.txtBase = base;
        this.txtFile = file;
        this.txtShiftX = shiftX;
        this.txtShiftY = shiftY;
    }

    public void draw(AEBaseGui g) {
        if (!this.visible) {
            return;
        }
        g.bindTexture(this.txtBase, this.txtFile);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (this.getRange() == 0) {
            g.drawTexturedModalRect(this.displayX, this.displayY, this.txtShiftX + this.width, this.txtShiftY, this.width, 15);
        } else {
            int offset = (this.currentScroll - this.minScroll) * (this.height - 15) / this.getRange();
            g.drawTexturedModalRect(this.displayX, offset + this.displayY, this.txtShiftX, this.txtShiftY, this.width, 15);
        }
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    private int getRange() {
        return this.maxScroll - this.minScroll;
    }

    public int getLeft() {
        return this.displayX;
    }

    public GuiScrollbar setLeft(int v) {
        this.displayX = v;
        return this;
    }

    public int getTop() {
        return this.displayY;
    }

    public GuiScrollbar setTop(int v) {
        this.displayY = v;
        return this;
    }

    public int getWidth() {
        return this.width;
    }

    public GuiScrollbar setWidth(int v) {
        this.width = v;
        return this;
    }

    public int getHeight() {
        return this.height;
    }

    public GuiScrollbar setHeight(int v) {
        this.height = v;
        return this;
    }

    public void setRange(int min, int max, int pageSize) {
        this.minScroll = min;
        this.maxScroll = max;
        this.pageSize = pageSize;
        if (this.minScroll > this.maxScroll) {
            this.maxScroll = this.minScroll;
        }
        this.applyRange();
    }

    private void applyRange() {
        this.currentScroll = Math.max(Math.min(this.currentScroll, this.maxScroll), this.minScroll);
    }

    @Override
    public int getCurrentScroll() {
        return this.currentScroll;
    }

    public void setCurrentScroll(int currentScroll) {
        this.currentScroll = Math.max(Math.min(currentScroll, this.maxScroll), this.minScroll);
    }

    public boolean contains(int x, int y) {
        return x >= this.displayX && y >= this.displayY && x <= this.displayX + this.width && y <= this.displayY + this.height;
    }

    public void click(AEBaseGui aeBaseGui, int x, int y) {
        if (this.getRange() == 0) {
            return;
        }
        if (this.contains(x, y)) {
            this.currentScroll = y - this.displayY;
            this.currentScroll = this.minScroll + this.currentScroll * 2 * this.getRange() / this.height;
            this.currentScroll = this.currentScroll + 1 >> 1;
            this.applyRange();
            this.isLatestClickOnScrollbar = true;
        } else {
            this.isLatestClickOnScrollbar = false;
        }
    }

    public void clickMove(int y) {
        if (this.getRange() == 0 || !this.isLatestClickOnScrollbar) {
            return;
        }
        if (y >= this.displayY && y <= this.displayY + this.height) {
            this.currentScroll = y - this.displayY;
            this.currentScroll = this.minScroll + this.currentScroll * 2 * this.getRange() / this.height;
            this.currentScroll = this.currentScroll + 1 >> 1;
            this.applyRange();
        }
    }

    public void wheel(int delta) {
        delta = Math.max(Math.min(-delta, 1), -1);
        this.currentScroll += delta * this.pageSize;
        this.applyRange();
    }
}

