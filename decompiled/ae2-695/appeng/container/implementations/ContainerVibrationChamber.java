/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.misc.TileVibrationChamber;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerVibrationChamber
extends AEBaseContainer
implements IProgressProvider {
    private static final int MAX_BURN_TIME = 200;
    private final int aePerTick = 5;
    private final TileVibrationChamber vibrationChamber;
    @GuiSync(value=0)
    public int burnProgress = 0;
    @GuiSync(value=1)
    public int burnSpeed = 100;

    public ContainerVibrationChamber(InventoryPlayer ip, TileVibrationChamber vibrationChamber) {
        super(ip, vibrationChamber, null);
        this.vibrationChamber = vibrationChamber;
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.FUEL, (IInventory)vibrationChamber, 0, 80, 37, this.getInventoryPlayer()));
        this.bindPlayerInventory(ip, 0, 84);
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.burnProgress = (int)(this.vibrationChamber.getMaxBurnTime() <= 0.0 ? 0.0 : 12.0 * this.vibrationChamber.getBurnTime() / this.vibrationChamber.getMaxBurnTime());
            this.burnSpeed = this.vibrationChamber.getBurnSpeed();
        }
        super.detectAndSendChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.burnProgress > 0 ? this.burnSpeed : 0;
    }

    @Override
    public int getMaxProgress() {
        return 200;
    }

    public int getAePerTick() {
        return this.aePerTick;
    }
}

