/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.world.World
 */
package appeng.container.implementations;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.tile.inventory.AppEngInternalInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.world.World;

public class ContainerPatternItemRenamer
extends AEBaseContainer {
    private final Slot patternValue = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);

    public ContainerPatternItemRenamer(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
    }

    public IGrid getGrid() {
        IActionHost h = (IActionHost)this.getTarget();
        return h.getActionableNode().getGrid();
    }

    public World getWorld() {
        return this.getPlayerInv().player.worldObj;
    }

    public Slot getPatternValue() {
        return this.patternValue;
    }
}

