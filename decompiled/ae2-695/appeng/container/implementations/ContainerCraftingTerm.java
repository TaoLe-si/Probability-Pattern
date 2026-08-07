/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.CraftingManager
 */
package appeng.container.implementations;

import appeng.api.storage.ITerminalHost;
import appeng.container.ContainerNull;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotCraftingTerm;
import appeng.helpers.IContainerCraftingPacket;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

public class ContainerCraftingTerm
extends ContainerMEMonitorable
implements IAEAppEngInventory,
IContainerCraftingPacket {
    private final PartCraftingTerminal ct;
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final SlotCraftingMatrix[] craftingSlots = new SlotCraftingMatrix[9];
    private final SlotCraftingTerm outputSlot;

    public ContainerCraftingTerm(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable, false);
        this.ct = (PartCraftingTerminal)monitorable;
        IInventory crafting = this.ct.getInventoryByName("crafting");
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                SlotCraftingMatrix slotCraftingMatrix = new SlotCraftingMatrix(this, crafting, x + y * 3, 37 + x * 18, -72 + y * 18);
                this.craftingSlots[x + y * 3] = slotCraftingMatrix;
                this.addSlotToContainer(slotCraftingMatrix);
            }
        }
        this.outputSlot = new SlotCraftingTerm(this.getPlayerInv().player, this.getActionSource(), this.getPowerSource(), monitorable, crafting, crafting, this.output, 131, -54, this);
        this.addSlotToContainer(this.outputSlot);
        this.bindPlayerInventory(ip, 0, 0);
        this.onCraftMatrixChanged(crafting);
    }

    public void onCraftMatrixChanged(IInventory par1IInventory) {
        ContainerNull cn = new ContainerNull();
        InventoryCrafting ic = new InventoryCrafting((Container)cn, 3, 3);
        for (int x = 0; x < 9; ++x) {
            ic.setInventorySlotContents(x, this.craftingSlots[x].getStack());
        }
        this.outputSlot.putStack(CraftingManager.getInstance().findMatchingRecipe(ic, this.getPlayerInv().player.worldObj));
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
    }

    @Override
    public IInventory getInventoryByName(String name) {
        if (name.equals("player")) {
            return this.getInventoryPlayer();
        }
        return this.ct.getInventoryByName(name);
    }

    @Override
    public boolean useRealItems() {
        return true;
    }
}

