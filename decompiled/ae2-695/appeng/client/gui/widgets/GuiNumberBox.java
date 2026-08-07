/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiTextField
 */
package appeng.client.gui.widgets;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class GuiNumberBox
extends GuiTextField {
    private final Class type;

    public GuiNumberBox(FontRenderer fontRenderer, int x, int y, int width, int height, Class type) {
        super(fontRenderer, x, y, width, height);
        this.type = type;
    }

    public void writeText(String selectedText) {
        String original = this.getText();
        super.writeText(selectedText);
        try {
            if (this.type == Integer.TYPE || this.type == Integer.class) {
                Integer.parseInt(this.getText());
            } else if (this.type == Long.TYPE || this.type == Long.class) {
                Long.parseLong(this.getText());
            } else if (this.type == Double.TYPE || this.type == Double.class) {
                Double.parseDouble(this.getText());
            }
        }
        catch (NumberFormatException e) {
            this.setText(original);
        }
    }
}

