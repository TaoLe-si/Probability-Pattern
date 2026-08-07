/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.widgets;

import net.minecraft.item.ItemStack;

public interface IDropToFillTextField {
    public boolean isOverTextField(int var1, int var2);

    public void setTextFieldValue(String var1, int var2, int var3, ItemStack var4);
}

