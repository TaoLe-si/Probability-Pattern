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
import appeng.tile.qnb.TileQuantumBridge;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerQNB
extends AEBaseContainer {
    public ContainerQNB(InventoryPlayer ip, TileQuantumBridge quantumBridge) {
        super(ip, quantumBridge, null);
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.QE_SINGULARITY, (IInventory)quantumBridge, 0, 80, 37, this.getInventoryPlayer()).setStackLimit(1));
        this.bindPlayerInventory(ip, 0, 84);
    }
}

