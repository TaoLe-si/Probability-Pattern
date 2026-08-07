/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.api.AEApi;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IStorageMonitorable;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.SlotCraftingTerm;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.packets.PacketPatternSlot;
import appeng.helpers.IContainerCraftingPacket;
import java.io.IOException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotPatternTerm
extends SlotCraftingTerm {
    private final int groupNum;
    private final IOptionalSlotHost host;

    public SlotPatternTerm(EntityPlayer player, BaseActionSource mySrc, IEnergySource energySrc, IStorageMonitorable storage, IInventory cMatrix, IInventory secondMatrix, IInventory output, int x, int y, IOptionalSlotHost h, int groupNumber, IContainerCraftingPacket c) {
        super(player, mySrc, energySrc, storage, cMatrix, secondMatrix, output, x, y, c);
        this.host = h;
        this.groupNum = groupNumber;
    }

    public AppEngPacket getRequest(boolean shift) throws IOException {
        return new PacketPatternSlot(this.getPattern(), AEApi.instance().storage().createItemStack(this.getStack()), shift);
    }

    @Override
    public ItemStack getStack() {
        if (!this.isEnabled() && this.getDisplayStack() != null) {
            this.clearStack();
        }
        return super.getStack();
    }

    @Override
    public boolean isEnabled() {
        if (this.host == null) {
            return false;
        }
        return this.host.isSlotEnabled(this.groupNum);
    }
}

