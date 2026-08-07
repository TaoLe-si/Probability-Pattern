/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package appeng.container.implementations;

import appeng.api.config.CraftingAllow;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.IWideReadableNumberConverter;
import appeng.util.ItemSorters;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class CraftingCPUStatus
implements Comparable<CraftingCPUStatus> {
    private static final IWideReadableNumberConverter NUMBER_CONVERTER = ReadableNumberConverter.INSTANCE;
    @Nullable
    private final ICraftingCPU serverCluster;
    private final String name;
    private final int serial;
    private final long storage;
    private final long usedStorage;
    private final long coprocessors;
    private final boolean isBusy;
    private final long totalItems;
    private final long remainingItems;
    private final IAEItemStack crafting;
    private final CraftingAllow allowMode;
    private final long craftingElapsedTime;

    public CraftingCPUStatus() {
        this.serverCluster = null;
        this.name = "ERROR";
        this.serial = 0;
        this.storage = 0L;
        this.usedStorage = 0L;
        this.coprocessors = 0L;
        this.isBusy = false;
        this.totalItems = 0L;
        this.remainingItems = 0L;
        this.crafting = null;
        this.allowMode = CraftingAllow.ALLOW_ALL;
        this.craftingElapsedTime = 0L;
    }

    public CraftingCPUStatus(ICraftingCPU cluster, int serial) {
        this.serverCluster = cluster;
        this.name = cluster.getName();
        this.serial = serial;
        this.isBusy = cluster.isBusy();
        if (this.isBusy) {
            this.crafting = cluster.getFinalOutput();
            this.usedStorage = cluster.getUsedStorage();
            this.totalItems = cluster.getStartItemCount();
            this.remainingItems = cluster.getRemainingItemCount();
            this.craftingElapsedTime = cluster.getElapsedTime();
        } else {
            this.crafting = null;
            this.usedStorage = 0L;
            this.totalItems = 0L;
            this.remainingItems = 0L;
            this.craftingElapsedTime = 0L;
        }
        this.storage = cluster.getAvailableStorage();
        this.coprocessors = cluster.getCoProcessors();
        this.allowMode = cluster.getCraftingAllowMode();
    }

    public CraftingCPUStatus(NBTTagCompound i) {
        this.serverCluster = null;
        this.name = i.getString("name");
        this.serial = i.getInteger("serial");
        this.storage = i.getLong("storage");
        this.usedStorage = i.getLong("usedStorage");
        this.coprocessors = i.getLong("coprocessors");
        this.isBusy = i.getBoolean("isBusy");
        this.totalItems = i.getLong("totalItems");
        this.remainingItems = i.getLong("remainingItems");
        this.crafting = i.hasKey("crafting") ? AEItemStack.loadItemStackFromNBT(i.getCompoundTag("crafting")) : null;
        this.allowMode = i.hasKey("allowMode") ? CraftingAllow.values()[i.getInteger("allowMode")] : CraftingAllow.ALLOW_ALL;
        this.craftingElapsedTime = i.hasKey("craftingElapsedTime") ? i.getLong("craftingElapsedTime") : 0L;
    }

    public CraftingCPUStatus(ByteBuf packet) throws IOException {
        this(CraftingCPUStatus.readNBTFromPacket(packet));
    }

    private static NBTTagCompound readNBTFromPacket(ByteBuf packet) throws IOException {
        int size = packet.readInt();
        byte[] tagBytes = new byte[size];
        packet.readBytes(tagBytes);
        ByteArrayInputStream di = new ByteArrayInputStream(tagBytes);
        return CompressedStreamTools.read((DataInputStream)new DataInputStream(di));
    }

    public void writeToNBT(NBTTagCompound i) {
        if (this.name != null && !this.name.isEmpty()) {
            i.setString("name", this.name);
        }
        i.setInteger("serial", this.serial);
        i.setLong("storage", this.storage);
        i.setLong("usedStorage", this.usedStorage);
        i.setLong("coprocessors", this.coprocessors);
        i.setBoolean("isBusy", this.isBusy);
        i.setLong("totalItems", this.totalItems);
        i.setLong("remainingItems", this.remainingItems);
        i.setLong("craftingElapsedTime", this.craftingElapsedTime);
        if (this.crafting != null) {
            NBTTagCompound stack = new NBTTagCompound();
            this.crafting.writeToNBT(stack);
            i.setTag("crafting", (NBTBase)stack);
        }
        i.setInteger("allowMode", this.allowMode.ordinal());
    }

    public void writeToPacket(ByteBuf i) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        CompressedStreamTools.write((NBTTagCompound)tag, (DataOutput)data);
        byte[] tagBytes = bytes.toByteArray();
        int size = tagBytes.length;
        i.writeInt(size);
        i.writeBytes(tagBytes);
    }

    @Nullable
    public ICraftingCPU getServerCluster() {
        return this.serverCluster;
    }

    public String getName() {
        return this.name;
    }

    public int getSerial() {
        return this.serial;
    }

    public long getStorage() {
        return this.storage;
    }

    public long getUsedStorage() {
        return this.usedStorage;
    }

    public long getCoprocessors() {
        return this.coprocessors;
    }

    public long getTotalItems() {
        return this.totalItems;
    }

    public long getRemainingItems() {
        return this.remainingItems;
    }

    public IAEItemStack getCrafting() {
        return this.crafting;
    }

    public boolean isBusy() {
        return this.isBusy;
    }

    public CraftingAllow allowMode() {
        return this.allowMode;
    }

    public long getCraftingElapsedTime() {
        return this.craftingElapsedTime;
    }

    @Override
    public int compareTo(CraftingCPUStatus o) {
        int a = ItemSorters.compareLong(o.getCoprocessors(), this.getCoprocessors());
        if (a != 0) {
            return a;
        }
        return ItemSorters.compareLong(o.getStorage(), this.getStorage());
    }

    public String formatCoprocessors() {
        return NumberFormat.getInstance().format(this.getCoprocessors());
    }

    public String formatShorterCoprocessors() {
        return NUMBER_CONVERTER.toWideReadableForm(this.getCoprocessors());
    }

    public String formatStorage() {
        return Platform.formatByteDouble(this.getStorage());
    }

    public String formatUsedStorage() {
        return Platform.formatByteDouble(this.getUsedStorage());
    }
}

