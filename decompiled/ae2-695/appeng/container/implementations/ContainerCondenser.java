/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 */
package appeng.container.implementations;

import appeng.api.config.CondenserOutput;
import appeng.api.config.Settings;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.misc.TileCondenser;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class ContainerCondenser
extends AEBaseContainer
implements IProgressProvider {
    private final TileCondenser condenser;
    @GuiSync(value=0)
    public long requiredEnergy = 0L;
    @GuiSync(value=1)
    public long storedPower = 0L;
    @GuiSync(value=2)
    public CondenserOutput output = CondenserOutput.TRASH;

    public ContainerCondenser(InventoryPlayer ip, TileCondenser condenser) {
        super(ip, condenser, null);
        this.condenser = condenser;
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.TRASH, (IInventory)condenser, 0, 51, 52, ip));
        this.addSlotToContainer(new SlotOutput((IInventory)condenser, 1, 105, 52, -1));
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.STORAGE_COMPONENT, condenser.getInternalInventory(), 2, 101, 26, ip).setStackLimit(1));
        this.bindPlayerInventory(ip, 0, 115);
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            double maxStorage = this.condenser.getStorage();
            double requiredEnergy = this.condenser.getRequiredPower();
            this.requiredEnergy = requiredEnergy == 0.0 ? (long)((int)maxStorage) : (long)((int)Math.min(requiredEnergy, maxStorage));
            this.storedPower = (int)this.condenser.getStoredPower();
            this.setOutput((CondenserOutput)this.condenser.getConfigManager().getSetting(Settings.CONDENSER_OUTPUT));
        }
        super.detectAndSendChanges();
    }

    @Override
    public int getCurrentProgress() {
        return (int)this.storedPower;
    }

    @Override
    public int getMaxProgress() {
        return (int)this.requiredEnergy;
    }

    public CondenserOutput getOutput() {
        return this.output;
    }

    private void setOutput(CondenserOutput output) {
        this.output = output;
    }
}

