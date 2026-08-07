/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.tileentity.TileEntity
 */
package appeng.helpers;

import appeng.api.networking.IGridHost;
import appeng.api.util.DimensionalCoord;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;

@Deprecated
public interface IInterfaceTerminalSupport
extends IGridHost {
    public DimensionalCoord getLocation();

    public PatternsConfiguration[] getPatternsConfigurations();

    @Deprecated
    public IInventory getPatterns(int var1);

    public String getName();

    public TileEntity getTileEntity();

    @Deprecated
    default public long getSortValue() {
        TileEntity te = this.getTileEntity();
        return (long)te.zCoord << 24 ^ (long)te.xCoord << 8 ^ (long)te.yCoord;
    }

    default public boolean shouldDisplay() {
        return true;
    }

    public static class PatternsConfiguration {
        @Deprecated
        public int offset;
        public int size;

        public PatternsConfiguration(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }
}

