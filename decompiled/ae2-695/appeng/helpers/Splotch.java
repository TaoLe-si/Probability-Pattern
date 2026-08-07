/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.util.Vec3
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.util.AEColor;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class Splotch {
    private final ForgeDirection side;
    private final boolean lumen;
    private final AEColor color;
    private final int pos;

    public Splotch(AEColor col, boolean lit, ForgeDirection side, Vec3 position) {
        double y;
        double x;
        this.color = col;
        this.lumen = lit;
        if (side == ForgeDirection.SOUTH || side == ForgeDirection.NORTH) {
            x = position.xCoord;
            y = position.yCoord;
        } else if (side == ForgeDirection.UP || side == ForgeDirection.DOWN) {
            x = position.xCoord;
            y = position.zCoord;
        } else {
            x = position.yCoord;
            y = position.zCoord;
        }
        int a = (int)(x * 15.0);
        int b = (int)(y * 15.0);
        this.pos = a | b << 4;
        this.side = side;
    }

    public Splotch(ByteBuf data) {
        this.pos = data.readByte();
        byte val = data.readByte();
        this.side = ForgeDirection.getOrientation((int)(val & 7));
        this.color = AEColor.values()[val >> 3 & 0xF];
        this.lumen = (val >> 7 & 1) > 0;
    }

    public void writeToStream(ByteBuf stream) {
        stream.writeByte(this.pos);
        int val = this.getSide().ordinal() | this.getColor().ordinal() << 3 | (this.isLumen() ? 128 : 0);
        stream.writeByte(val);
    }

    public float x() {
        return (float)(this.pos & 0xF) / 15.0f;
    }

    public float y() {
        return (float)(this.pos >> 4 & 0xF) / 15.0f;
    }

    public int getSeed() {
        int val = this.getSide().ordinal() | this.getColor().ordinal() << 3 | (this.isLumen() ? 128 : 0);
        return Math.abs(this.pos + val);
    }

    public ForgeDirection getSide() {
        return this.side;
    }

    public AEColor getColor() {
        return this.color;
    }

    public boolean isLumen() {
        return this.lumen;
    }
}

