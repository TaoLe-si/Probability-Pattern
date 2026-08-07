/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.world.World
 */
package appeng.util;

import javax.annotation.Nullable;
import net.minecraft.world.World;

public interface IWorldCallable<T> {
    @Nullable
    public T call(@Nullable World var1) throws Exception;
}

