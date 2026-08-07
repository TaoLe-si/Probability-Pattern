/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui;

import java.util.List;
import net.minecraft.item.ItemStack;

public interface IGuiTooltipHandler {
    default public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> currentToolTip) {
        return currentToolTip;
    }

    default public ItemStack getHoveredStack() {
        return null;
    }
}

