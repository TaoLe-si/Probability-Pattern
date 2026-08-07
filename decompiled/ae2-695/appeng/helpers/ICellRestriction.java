/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.helpers;

import net.minecraft.item.ItemStack;

public interface ICellRestriction {
    public String getCellData(ItemStack var1);

    public void setCellRestriction(ItemStack var1, String var2);
}

