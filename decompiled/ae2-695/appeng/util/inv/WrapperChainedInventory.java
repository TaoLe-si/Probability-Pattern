/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class WrapperChainedInventory
implements IInventory {
    private int fullSize = 0;
    private List<IInventory> l;
    private Map<Integer, InvOffset> offsets;

    public WrapperChainedInventory(IInventory ... inventories) {
        this.setInventory(inventories);
    }

    private void setInventory(IInventory ... a) {
        this.l = ImmutableList.copyOf((Object[])a);
        this.calculateSizes();
    }

    private void calculateSizes() {
        this.offsets = new HashMap<Integer, InvOffset>();
        int offset = 0;
        for (IInventory in : this.l) {
            InvOffset io = new InvOffset();
            io.offset = offset;
            io.size = in.getSizeInventory();
            io.i = in;
            for (int y = 0; y < io.size; ++y) {
                this.offsets.put(y + io.offset, io);
            }
            offset += io.size;
        }
        this.fullSize = offset;
    }

    public WrapperChainedInventory(List<IInventory> inventories) {
        this.setInventory(inventories);
    }

    private void setInventory(List<IInventory> a) {
        this.l = a;
        this.calculateSizes();
    }

    public void cycleOrder() {
        if (this.l.size() > 1) {
            ArrayList<IInventory> newOrder = new ArrayList<IInventory>(this.l.size());
            newOrder.add(this.l.get(this.l.size() - 1));
            for (int x = 0; x < this.l.size() - 1; ++x) {
                newOrder.add(this.l.get(x));
            }
            this.setInventory(newOrder);
        }
    }

    public IInventory getInv(int idx) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            return io.i;
        }
        return null;
    }

    public int getInvSlot(int idx) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            return idx - io.offset;
        }
        return 0;
    }

    public int getSizeInventory() {
        return this.fullSize;
    }

    public ItemStack getStackInSlot(int idx) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            return io.i.getStackInSlot(idx - io.offset);
        }
        return null;
    }

    public ItemStack decrStackSize(int idx, int var2) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            return io.i.decrStackSize(idx - io.offset, var2);
        }
        return null;
    }

    public ItemStack getStackInSlotOnClosing(int idx) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            return io.i.getStackInSlotOnClosing(idx - io.offset);
        }
        return null;
    }

    public void setInventorySlotContents(int idx, ItemStack var2) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            io.i.setInventorySlotContents(idx - io.offset, var2);
        }
    }

    public String getInventoryName() {
        return "ChainedInv";
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    public int getInventoryStackLimit() {
        int smallest = Integer.MAX_VALUE;
        for (IInventory i : this.l) {
            smallest = Math.min(smallest, i.getInventoryStackLimit());
        }
        return smallest;
    }

    public void markDirty() {
        for (IInventory i : this.l) {
            i.markDirty();
        }
    }

    public boolean isUseableByPlayer(EntityPlayer var1) {
        return false;
    }

    public void openInventory() {
    }

    public void closeInventory() {
    }

    public boolean isItemValidForSlot(int idx, ItemStack itemstack) {
        InvOffset io = this.offsets.get(idx);
        if (io != null) {
            return io.i.isItemValidForSlot(idx - io.offset, itemstack);
        }
        return false;
    }

    private static class InvOffset {
        private int offset;
        private int size;
        private IInventory i;

        private InvOffset() {
        }
    }
}

