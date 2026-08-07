/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.grindstone.TileGrinder;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerGrinder
extends AEBaseContainer {
    public ContainerGrinder(InventoryPlayer ip, TileGrinder grinder) {
        super(ip, grinder, null);
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ORE, (IInventory)grinder, 0, 12, 17, this.getInventoryPlayer()));
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ORE, (IInventory)grinder, 1, 30, 17, this.getInventoryPlayer()));
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ORE, (IInventory)grinder, 2, 48, 17, this.getInventoryPlayer()));
        this.addSlotToContainer(new SlotInaccessible((IInventory)grinder, 6, 80, 40));
        this.addSlotToContainer(new SlotOutput((IInventory)grinder, 3, 112, 63, 47));
        this.addSlotToContainer(new SlotOutput((IInventory)grinder, 4, 130, 63, 47));
        this.addSlotToContainer(new SlotOutput((IInventory)grinder, 5, 148, 63, 47));
        this.bindPlayerInventory(ip, 0, 94);
    }
}

