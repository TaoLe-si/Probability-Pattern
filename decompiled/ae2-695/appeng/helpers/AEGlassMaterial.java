/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.material.MapColor
 *  net.minecraft.block.material.Material
 */
package appeng.helpers;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

public class AEGlassMaterial
extends Material {
    public static final AEGlassMaterial INSTANCE = new AEGlassMaterial(MapColor.airColor);

    public AEGlassMaterial(MapColor color) {
        super(color);
    }

    public boolean isOpaque() {
        return false;
    }
}

