/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 */
package appeng.container.implementations;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerMEPortableCell
extends ContainerMEMonitorable {
    private double powerMultiplier = 0.5;
    private final IPortableCell civ;
    private int ticks = 0;
    private final int slot;

    public ContainerMEPortableCell(InventoryPlayer ip, IPortableCell monitorable) {
        super(ip, monitorable, false);
        if (monitorable instanceof IInventorySlotAware) {
            int slotIndex = ((IInventorySlotAware)((Object)monitorable)).getInventorySlot();
            this.lockPlayerInventorySlot(slotIndex);
            this.slot = slotIndex;
        } else {
            this.slot = -1;
            this.lockPlayerInventorySlot(ip.currentItem);
        }
        this.civ = monitorable;
        this.bindPlayerInventory(ip, 0, 0);
    }

    @Override
    public void detectAndSendChanges() {
        ItemStack currentItem;
        ItemStack itemStack = currentItem = this.slot < 0 ? this.getPlayerInv().getCurrentItem() : this.getPlayerInv().getStackInSlot(this.slot);
        if (this.civ != null) {
            if (currentItem != this.civ.getItemStack()) {
                if (currentItem != null) {
                    if (Platform.isSameItem(this.civ.getItemStack(), currentItem)) {
                        this.getPlayerInv().setInventorySlotContents(this.getPlayerInv().currentItem, this.civ.getItemStack());
                    } else {
                        this.setValidContainer(false);
                    }
                } else {
                    this.setValidContainer(false);
                }
            }
        } else {
            this.setValidContainer(false);
        }
        ++this.ticks;
        if (this.ticks > 10) {
            this.civ.extractAEPower(this.getPowerMultiplier() * (double)this.ticks, Actionable.MODULATE, PowerMultiplier.CONFIG);
            this.ticks = 0;
        }
        super.detectAndSendChanges();
    }

    private double getPowerMultiplier() {
        return this.powerMultiplier;
    }

    void setPowerMultiplier(double powerMultiplier) {
        this.powerMultiplier = powerMultiplier;
    }
}

