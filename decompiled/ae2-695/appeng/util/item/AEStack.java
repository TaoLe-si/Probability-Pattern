/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package appeng.util.item;

import appeng.api.storage.data.IAEStack;
import io.netty.buffer.ByteBuf;
import java.io.IOException;

public abstract class AEStack<StackType extends IAEStack<StackType>>
implements IAEStack<StackType> {
    private boolean isCraftable;
    private long stackSize;
    private long countRequestable;
    private long countRequestableCrafts;
    private float usedPercent;

    static long getPacketValue(byte type, ByteBuf tag) {
        if (type == 0) {
            long l = tag.readByte();
            return l -= -128L;
        }
        if (type == 1) {
            long l = tag.readShort();
            return l -= -32768L;
        }
        if (type == 2) {
            long l = tag.readInt();
            return l -= Integer.MIN_VALUE;
        }
        return tag.readLong();
    }

    @Override
    public long getStackSize() {
        return this.stackSize;
    }

    @Override
    public StackType setStackSize(long ss) {
        this.stackSize = ss;
        return (StackType)this;
    }

    @Override
    public StackType setUsedPercent(float percent) {
        this.usedPercent = percent;
        return (StackType)this;
    }

    @Override
    public float getUsedPercent() {
        return this.usedPercent;
    }

    @Override
    public long getCountRequestable() {
        return this.countRequestable;
    }

    @Override
    public StackType setCountRequestable(long countRequestable) {
        this.countRequestable = countRequestable;
        return (StackType)this;
    }

    @Override
    public long getCountRequestableCrafts() {
        return this.countRequestableCrafts;
    }

    @Override
    public StackType setCountRequestableCrafts(long countRequestableCrafts) {
        this.countRequestableCrafts = countRequestableCrafts;
        return (StackType)this;
    }

    @Override
    public boolean isCraftable() {
        return this.isCraftable;
    }

    @Override
    public StackType setCraftable(boolean isCraftable) {
        this.isCraftable = isCraftable;
        return (StackType)this;
    }

    @Override
    public StackType reset() {
        this.stackSize = 0L;
        this.setCountRequestable(0L);
        this.setCraftable(false);
        this.setCountRequestableCrafts(0L);
        this.setUsedPercent(0.0f);
        return (StackType)this;
    }

    @Override
    public boolean isMeaningful() {
        return this.stackSize != 0L || this.countRequestable > 0L || this.isCraftable;
    }

    @Override
    public void incStackSize(long i) {
        this.stackSize += i;
    }

    @Override
    public void decStackSize(long i) {
        this.stackSize -= i;
    }

    @Override
    public void incCountRequestable(long i) {
        this.countRequestable += i;
    }

    @Override
    public void decCountRequestable(long i) {
        this.countRequestable -= i;
    }

    @Override
    public void writeToPacket(ByteBuf i) throws IOException {
        byte mask = (byte)(this.getType(0L) | this.getType(this.stackSize) << 2 | this.getType(this.countRequestable) << 4 | (byte)(this.isCraftable ? 1 : 0) << 6 | (this.hasTagCompound() ? 1 : 0) << 7);
        i.writeByte((int)mask);
        this.writeIdentity(i);
        this.readNBT(i);
        this.putPacketValue(i, this.stackSize);
        this.putPacketValue(i, this.countRequestable);
        long longUsedPercent = (long)(this.usedPercent * 10000.0f);
        byte mask2 = (byte)(this.getType(this.countRequestableCrafts) | this.getType(longUsedPercent) << 2);
        i.writeByte((int)mask2);
        this.putPacketValue(i, this.countRequestableCrafts);
        this.putPacketValue(i, longUsedPercent);
    }

    private byte getType(long num) {
        if (num <= 255L) {
            return 0;
        }
        if (num <= 65535L) {
            return 1;
        }
        if (num <= 0xFFFFFFFFL) {
            return 2;
        }
        return 3;
    }

    abstract boolean hasTagCompound();

    abstract void writeIdentity(ByteBuf var1) throws IOException;

    abstract void readNBT(ByteBuf var1) throws IOException;

    private void putPacketValue(ByteBuf tag, long num) {
        if (num <= 255L) {
            tag.writeByte((int)((byte)(num + -128L)));
        } else if (num <= 65535L) {
            tag.writeShort((int)((short)(num + -32768L)));
        } else if (num <= 0xFFFFFFFFL) {
            tag.writeInt((int)(num + Integer.MIN_VALUE));
        } else {
            tag.writeLong(num);
        }
    }
}

