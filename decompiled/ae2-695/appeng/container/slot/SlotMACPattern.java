/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.implementations.ContainerMAC;
import appeng.container.slot.AppEngSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotMACPattern
extends AppEngSlot {
    private final ContainerMAC mac;

    public SlotMACPattern(ContainerMAC mac, IInventory i, int slotIdx, int x, int y) {
        super(i, slotIdx, x, y);
        this.mac = mac;
    }

    @Override
    public boolean isItemValid(ItemStack i) {
        return this.mac.isValidItemForSlot(this.getSlotIndex(), i);
    }
}

