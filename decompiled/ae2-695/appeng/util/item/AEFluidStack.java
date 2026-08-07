/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.Nonnull
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraftforge.fluids.Fluid
 *  net.minecraftforge.fluids.FluidStack
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAETagCompound;
import appeng.util.Platform;
import appeng.util.item.AESharedNBT;
import appeng.util.item.AEStack;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class AEFluidStack
extends AEStack<IAEFluidStack>
implements IAEFluidStack,
Comparable<AEFluidStack> {
    private final int myHash;
    private final Fluid fluid;
    private IAETagCompound tagCompound;

    private AEFluidStack(AEFluidStack is) {
        this.fluid = is.fluid;
        this.setStackSize(is.getStackSize());
        this.setCraftable(is.isCraftable());
        this.setCountRequestable(is.getCountRequestable());
        this.setCountRequestableCrafts(is.getCountRequestableCrafts());
        this.setUsedPercent(is.getUsedPercent());
        this.myHash = is.myHash;
    }

    private AEFluidStack(@Nonnull FluidStack is) {
        this.fluid = is.getFluid();
        if (this.fluid == null) {
            throw new IllegalArgumentException("Fluid is null.");
        }
        this.setStackSize(is.amount);
        this.setCraftable(false);
        this.setCountRequestable(0L);
        this.setCountRequestableCrafts(0L);
        this.setUsedPercent(0.0f);
        this.myHash = this.fluid.hashCode() ^ (this.tagCompound == null ? 0 : System.identityHashCode(this.tagCompound));
    }

    public static IAEFluidStack loadFluidStackFromNBT(NBTTagCompound i) {
        FluidStack fluidstack = FluidStack.loadFluidStackFromNBT((NBTTagCompound)i);
        if (fluidstack == null) {
            return null;
        }
        AEFluidStack fluid = AEFluidStack.create(fluidstack);
        fluid.setStackSize(i.getLong("Cnt"));
        fluid.setCountRequestable(i.getLong("Req"));
        fluid.setCraftable(i.getBoolean("Craft"));
        fluid.setCountRequestableCrafts(i.getLong("ReqMade"));
        fluid.setUsedPercent(i.getFloat("UsedPercent"));
        return fluid;
    }

    public static AEFluidStack create(Object a) {
        if (a == null) {
            return null;
        }
        if (a instanceof AEFluidStack) {
            ((IAEStack)a).copy();
        }
        if (a instanceof FluidStack) {
            return new AEFluidStack((FluidStack)a);
        }
        return null;
    }

    public static IAEFluidStack loadFluidStackFromPacket(ByteBuf data) throws IOException {
        byte mask = data.readByte();
        byte stackType = (byte)((mask & 0xC) >> 2);
        byte countReqType = (byte)((mask & 0x30) >> 4);
        boolean isCraftable = (mask & 0x40) > 0;
        boolean hasTagCompound = (mask & 0x80) > 0;
        NBTTagCompound d = new NBTTagCompound();
        byte len2 = data.readByte();
        byte[] name = new byte[len2];
        data.readBytes(name, 0, (int)len2);
        d.setString("FluidName", new String(name, StandardCharsets.UTF_8));
        d.setByte("Count", (byte)0);
        if (hasTagCompound) {
            int len = data.readInt();
            byte[] bd = new byte[len];
            data.readBytes(bd);
            DataInputStream di = new DataInputStream(new ByteArrayInputStream(bd));
            d.setTag("tag", (NBTBase)CompressedStreamTools.read((DataInputStream)di));
        }
        long stackSize = AEFluidStack.getPacketValue(stackType, data);
        long countRequestable = AEFluidStack.getPacketValue(countReqType, data);
        byte mask2 = data.readByte();
        byte countReqMadeType = (byte)(mask2 & 3);
        byte usedPercentType = (byte)((mask2 & 0xC) >> 2);
        long countRequestableCrafts = AEFluidStack.getPacketValue(countReqMadeType, data);
        long longUsedPercent = AEFluidStack.getPacketValue(usedPercentType, data);
        FluidStack fluidStack = FluidStack.loadFluidStackFromNBT((NBTTagCompound)d);
        if (fluidStack == null) {
            return null;
        }
        AEFluidStack fluid = AEFluidStack.create(fluidStack);
        fluid.setStackSize(stackSize);
        fluid.setCountRequestable(countRequestable);
        fluid.setCraftable(isCraftable);
        fluid.setCountRequestableCrafts(countRequestableCrafts);
        fluid.setUsedPercent((float)longUsedPercent / 10000.0f);
        return fluid;
    }

    @Override
    public void add(IAEFluidStack option) {
        if (option == null) {
            return;
        }
        this.incStackSize(option.getStackSize());
        this.setCountRequestable(this.getCountRequestable() + option.getCountRequestable());
        this.setCraftable(this.isCraftable() || option.isCraftable());
        this.setCountRequestableCrafts(this.getCountRequestableCrafts() + option.getCountRequestableCrafts());
    }

    @Override
    public void writeToNBT(NBTTagCompound i) {
        i.setString("FluidName", this.fluid.getName());
        i.setByte("Count", (byte)0);
        i.setLong("Cnt", this.getStackSize());
        i.setLong("Req", this.getCountRequestable());
        i.setBoolean("Craft", this.isCraftable());
        if (this.tagCompound != null) {
            i.setTag("tag", (NBTBase)this.tagCompound);
        } else {
            i.removeTag("tag");
        }
        if (this.getCountRequestableCrafts() != 0L) {
            i.setLong("ReqMade", this.getCountRequestableCrafts());
        }
        if (this.getUsedPercent() != 0.0f) {
            i.setFloat("UsedPercent", this.getUsedPercent());
        }
    }

    @Override
    public boolean fuzzyComparison(Object st, FuzzyMode mode) {
        if (st instanceof FluidStack) {
            return ((FluidStack)st).getFluid() == this.fluid;
        }
        if (st instanceof IAEFluidStack) {
            return ((IAEFluidStack)st).getFluid() == this.fluid;
        }
        return false;
    }

    @Override
    public IAEFluidStack copy() {
        return new AEFluidStack(this);
    }

    @Override
    public IAEFluidStack empty() {
        IAEFluidStack dup = this.copy();
        dup.reset();
        return dup;
    }

    @Override
    public IAETagCompound getTagCompound() {
        return this.tagCompound;
    }

    @Override
    public boolean isItem() {
        return false;
    }

    @Override
    public boolean isFluid() {
        return true;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.FLUIDS;
    }

    @Override
    public int compareTo(AEFluidStack b) {
        int diff = this.hashCode() - b.hashCode();
        return Integer.compare(diff, 0);
    }

    public int hashCode() {
        return this.myHash;
    }

    @Override
    public boolean equals(Object ia) {
        FluidStack is;
        if (ia instanceof AEFluidStack) {
            return ((AEFluidStack)ia).fluid == this.fluid && this.tagCompound == ((AEFluidStack)ia).tagCompound;
        }
        if (ia instanceof FluidStack && (is = (FluidStack)ia).getFluidID() == this.fluid.getID()) {
            NBTTagCompound ta = (NBTTagCompound)this.tagCompound;
            NBTTagCompound tb = is.tag;
            if (ta == tb) {
                return true;
            }
            if (ta == null && tb == null || ta != null && ta.hasNoTags() && tb == null || tb != null && tb.hasNoTags() && ta == null || ta != null && ta.hasNoTags() && tb != null && tb.hasNoTags()) {
                return true;
            }
            if (ta == null && tb != null || ta != null && tb == null) {
                return false;
            }
            if (AESharedNBT.isShared(tb)) {
                return ta == tb;
            }
            return Platform.NBTEqualityTest((NBTBase)ta, (NBTBase)tb);
        }
        return false;
    }

    public String toString() {
        return this.getFluidStack().toString();
    }

    @Override
    public boolean hasTagCompound() {
        return this.tagCompound != null;
    }

    @Override
    void writeIdentity(ByteBuf i) throws IOException {
        byte[] name = this.fluid.getName().getBytes(StandardCharsets.UTF_8);
        i.writeByte((int)((byte)name.length));
        i.writeBytes(name);
    }

    @Override
    void readNBT(ByteBuf i) throws IOException {
        if (this.hasTagCompound()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            CompressedStreamTools.write((NBTTagCompound)((NBTTagCompound)this.tagCompound), (DataOutput)data);
            byte[] tagBytes = bytes.toByteArray();
            int size = tagBytes.length;
            i.writeInt(size);
            i.writeBytes(tagBytes);
        }
    }

    @Override
    public FluidStack getFluidStack() {
        FluidStack is = new FluidStack(this.fluid, (int)Math.min(Integer.MAX_VALUE, this.getStackSize()));
        if (this.tagCompound != null) {
            is.tag = this.tagCompound.getNBTTagCompoundCopy();
        }
        return is;
    }

    @Override
    public Fluid getFluid() {
        return this.fluid;
    }

    @Override
    public String getLocalizedName() {
        return this.fluid.getName();
    }
}

