/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.container.implementations;

import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.spatial.ISpatialCache;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.spatial.TileSpatialIOPort;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.util.ForgeDirection;

public class ContainerSpatialIOPort
extends AEBaseContainer {
    @GuiSync(value=0)
    public long currentPower;
    @GuiSync(value=1)
    public long maxPower;
    @GuiSync(value=2)
    public long reqPower;
    @GuiSync(value=3)
    public long eff;
    private IGrid network;
    private int delay = 40;

    public ContainerSpatialIOPort(InventoryPlayer ip, TileSpatialIOPort spatialIOPort) {
        super(ip, spatialIOPort, null);
        if (Platform.isServer()) {
            this.network = spatialIOPort.getGridNode(ForgeDirection.UNKNOWN).getGrid();
        }
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.SPATIAL_STORAGE_CELLS, (IInventory)spatialIOPort, 0, 52, 48, this.getInventoryPlayer()));
        this.addSlotToContainer(new SlotOutput((IInventory)spatialIOPort, 1, 113, 48, SlotRestrictedInput.PlacableItemType.SPATIAL_STORAGE_CELLS.IIcon));
        this.bindPlayerInventory(ip, 0, 115);
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            ++this.delay;
            if (this.delay > 15 && this.network != null) {
                this.delay = 0;
                IEnergyGrid eg = (IEnergyGrid)this.network.getCache(IEnergyGrid.class);
                ISpatialCache sc = (ISpatialCache)this.network.getCache(ISpatialCache.class);
                if (eg != null) {
                    this.setCurrentPower((long)(100.0 * eg.getStoredPower()));
                    this.setMaxPower((long)(100.0 * eg.getMaxStoredPower()));
                    this.setRequiredPower((long)(100.0 * PowerMultiplier.CONFIG.multiply(sc.requiredPower())));
                    this.setEfficency((long)(100.0f * sc.currentEfficiency()));
                }
            }
        }
        super.detectAndSendChanges();
    }

    public long getCurrentPower() {
        return this.currentPower;
    }

    private void setCurrentPower(long currentPower) {
        this.currentPower = currentPower;
    }

    public long getMaxPower() {
        return this.maxPower;
    }

    private void setMaxPower(long maxPower) {
        this.maxPower = maxPower;
    }

    public long getRequiredPower() {
        return this.reqPower;
    }

    private void setRequiredPower(long reqPower) {
        this.reqPower = reqPower;
    }

    public long getEfficency() {
        return this.eff;
    }

    private void setEfficency(long eff) {
        this.eff = eff;
    }
}

