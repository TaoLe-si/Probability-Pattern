/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.slot;

import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.SlotRestrictedInput;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class OptionalSlotRestrictedInput
extends SlotRestrictedInput {
    private final int groupNum;
    private final IOptionalSlotHost host;

    public OptionalSlotRestrictedInput(SlotRestrictedInput.PlacableItemType valid, IInventory i, IOptionalSlotHost host, int slotIndex, int x, int y, int grpNum, InventoryPlayer invPlayer) {
        super(valid, i, slotIndex, x, y, invPlayer);
        this.groupNum = grpNum;
        this.host = host;
    }

    @Override
    public boolean isEnabled() {
        if (this.host == null) {
            return false;
        }
        return this.host.isSlotEnabled(this.groupNum);
    }
}

