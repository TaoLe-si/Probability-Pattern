/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.util.IOrientable;
import net.minecraftforge.common.util.ForgeDirection;

public class NullRotation
implements IOrientable {
    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public ForgeDirection getForward() {
        return ForgeDirection.SOUTH;
    }

    @Override
    public ForgeDirection getUp() {
        return ForgeDirection.UP;
    }

    @Override
    public void setOrientation(ForgeDirection forward, ForgeDirection up) {
    }
}

