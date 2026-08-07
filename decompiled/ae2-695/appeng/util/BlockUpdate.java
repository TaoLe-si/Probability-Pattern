/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.World
 */
package appeng.util;

import appeng.util.IWorldCallable;
import appeng.util.Platform;
import net.minecraft.world.World;

public class BlockUpdate
implements IWorldCallable<Boolean> {
    private final int x;
    private final int y;
    private final int z;

    public BlockUpdate(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Boolean call(World world) throws Exception {
        if (world.blockExists(this.x, this.y, this.z)) {
            world.notifyBlocksOfNeighborChange(this.x, this.y, this.z, Platform.AIR_BLOCK);
        }
        return true;
    }
}

