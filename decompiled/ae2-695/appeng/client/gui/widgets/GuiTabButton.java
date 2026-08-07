/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.entity.RenderItem
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import appeng.client.texture.ExtraBlockTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class GuiTabButton
extends GuiButton
implements ITooltip {
    private final RenderItem itemRenderer;
    private final String message;
    private int hideEdge = 0;
    private int myIcon = -1;
    private ItemStack myItem;

    public GuiTabButton(int x, int y, int ico, String message, RenderItem ir) {
        super(0, 0, 16, "");
        this.xPosition = x;
        this.yPosition = y;
        this.width = 22;
        this.height = 22;
        this.myIcon = ico;
        this.message = message;
        this.itemRenderer = ir;
    }

    public GuiTabButton(int x, int y, ItemStack ico, String message, RenderItem ir) {
        super(0, 0, 16, "");
        this.xPosition = x;
        this.yPosition = y;
        this.width = 22;
        this.height = 22;
        this.myItem = ico;
        this.message = message;
        this.itemRenderer = ir;
    }

    public void drawButton(Minecraft minecraft, int x, int y) {
        if (this.visible) {
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            minecraft.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
            this.field_146123_n = x >= this.xPosition && y >= this.yPosition && x < this.xPosition + this.width && y < this.yPosition + this.height;
            int uv_x = this.hideEdge > 0 ? 11 : 13;
            int offsetX = this.hideEdge > 0 ? 1 : 0;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, uv_x * 16, 0, 25, 22);
            if (this.myIcon >= 0) {
                int uv_y = (int)Math.floor(this.myIcon / 16);
                uv_x = this.myIcon - uv_y * 16;
                this.drawTexturedModalRect(offsetX + this.xPosition + 3, this.yPosition + 3, uv_x * 16, uv_y * 16, 16, 16);
            }
            this.mouseDragged(minecraft, x, y);
            if (this.myItem != null) {
                this.zLevel = 100.0f;
                this.itemRenderer.zLevel = 100.0f;
                GL11.glEnable((int)2896);
                GL11.glEnable((int)32826);
                RenderHelper.enableGUIStandardItemLighting();
                FontRenderer fontrenderer = minecraft.fontRenderer;
                this.itemRenderer.renderItemAndEffectIntoGUI(fontrenderer, minecraft.renderEngine, this.myItem, offsetX + this.xPosition + 3, this.yPosition + 3);
                GL11.glDisable((int)2896);
                this.itemRenderer.zLevel = 0.0f;
                this.zLevel = 0.0f;
            }
        }
    }

    @Override
    public String getMessage() {
        return this.message;
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
        return 22;
    }

    @Override
    public int getHeight() {
        return 22;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    public int getHideEdge() {
        return this.hideEdge;
    }

    public void setHideEdge(int hideEdge) {
        this.hideEdge = hideEdge;
    }
}

