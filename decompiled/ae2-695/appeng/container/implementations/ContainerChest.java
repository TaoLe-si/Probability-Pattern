/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.storage.TileChest;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerChest
extends AEBaseContainer {
    private final TileChest chest;

    public ContainerChest(InventoryPlayer ip, TileChest chest) {
        super(ip, chest, null);
        this.chest = chest;
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.STORAGE_CELLS, (IInventory)this.chest, 1, 80, 37, this.getInventoryPlayer()));
        this.bindPlayerInventory(ip, 0, 84);
    }
}

