/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.api.config.PowerMultiplier;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.AEConfig;
import appeng.tile.networking.TileWireless;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerWireless
extends AEBaseContainer {
    private final TileWireless wirelessTerminal;
    private final SlotRestrictedInput boosterSlot;
    @GuiSync(value=1)
    public long range = 0L;
    @GuiSync(value=2)
    public long drain = 0L;

    public ContainerWireless(InventoryPlayer ip, TileWireless te) {
        super(ip, te, null);
        this.wirelessTerminal = te;
        this.boosterSlot = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.RANGE_BOOSTER, (IInventory)this.wirelessTerminal, 0, 80, 47, this.getInventoryPlayer());
        this.addSlotToContainer(this.boosterSlot);
        this.bindPlayerInventory(ip, 0, 84);
    }

    @Override
    public void detectAndSendChanges() {
        int boosters = this.boosterSlot.getStack() == null ? 0 : this.boosterSlot.getStack().stackSize;
        this.setRange((long)(10.0 * AEConfig.instance.wireless_getMaxRange(boosters)));
        this.setDrain((long)(100.0 * PowerMultiplier.CONFIG.multiply(AEConfig.instance.wireless_getPowerDrain(boosters))));
        super.detectAndSendChanges();
    }

    public long getRange() {
        return this.range;
    }

    private void setRange(long range) {
        this.range = range;
    }

    public long getDrain() {
        return this.drain;
    }

    private void setDrain(long drain) {
        this.drain = drain;
    }
}

