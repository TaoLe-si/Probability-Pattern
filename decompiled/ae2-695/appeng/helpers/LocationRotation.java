/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.IBlockAccess
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.util.IOrientable;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

public class LocationRotation
implements IOrientable {
    private final IBlockAccess w;
    private final int x;
    private final int y;
    private final int z;

    public LocationRotation(IBlockAccess world, int x, int y, int z) {
        this.w = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public ForgeDirection getForward() {
        if (this.getUp().offsetY == 0) {
            return ForgeDirection.UP;
        }
        return ForgeDirection.SOUTH;
    }

    @Override
    public ForgeDirection getUp() {
        int num = Math.abs(this.x + this.y + this.z) % 6;
        return ForgeDirection.getOrientation((int)num);
    }

    @Override
    public void setOrientation(ForgeDirection forward, ForgeDirection up) {
    }
}

