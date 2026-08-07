/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.world.World
 */
package appeng.helpers;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public interface ICustomCollision {
    public Iterable<AxisAlignedBB> getSelectedBoundingBoxesFromPool(World var1, int var2, int var3, int var4, Entity var5, boolean var6);

    public void addCollidingBlockToList(World var1, int var2, int var3, int var4, AxisAlignedBB var5, List<AxisAlignedBB> var6, Entity var7);
}

