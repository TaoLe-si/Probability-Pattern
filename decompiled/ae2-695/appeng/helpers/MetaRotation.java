/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.util.IOrientable;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class MetaRotation
implements IOrientable {
    private final IBlockAccess w;
    private final int x;
    private final int y;
    private final int z;

    public MetaRotation(IBlockAccess world, int x, int y, int z) {
        this.w = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean canBeRotated() {
        return true;
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
        return ForgeDirection.getOrientation((int)this.w.getBlockMetadata(this.x, this.y, this.z));
    }

    @Override
    public void setOrientation(ForgeDirection forward, ForgeDirection up) {
        if (!(this.w instanceof World)) {
            throw new IllegalStateException(this.w.getClass().getName() + " received, expected World");
        }
        ((World)this.w).setBlockMetadataWithNotify(this.x, this.y, this.z, up.ordinal(), 3);
    }
}

