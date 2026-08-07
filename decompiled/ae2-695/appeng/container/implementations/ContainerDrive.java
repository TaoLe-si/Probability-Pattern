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
import appeng.tile.storage.TileDrive;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerDrive
extends AEBaseContainer {
    public ContainerDrive(InventoryPlayer ip, TileDrive drive) {
        super(ip, drive, null);
        for (int y = 0; y < 5; ++y) {
            for (int x = 0; x < 2; ++x) {
                this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.STORAGE_CELLS, (IInventory)drive, x + y * 2, 71 + x * 18, 14 + y * 18, this.getInventoryPlayer()));
            }
        }
        this.bindPlayerInventory(ip, 0, 117);
    }
}

