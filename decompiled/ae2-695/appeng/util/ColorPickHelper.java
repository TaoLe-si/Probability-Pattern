/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import appeng.core.localization.GuiColors;

public class ColorPickHelper {
    public static GuiColors selectColorFromThreshold(float threshold) {
        GuiColors color = null;
        color = threshold <= 25.0f ? GuiColors.CraftConfirmPercent25 : (threshold <= 50.0f ? GuiColors.CraftConfirmPercent50 : (threshold <= 75.0f ? GuiColors.CraftConfirmPercent75 : GuiColors.CraftConfirmPercent100));
        return color;
    }
}

