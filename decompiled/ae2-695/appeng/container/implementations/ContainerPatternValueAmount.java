/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.world.World
 */
package appeng.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.tile.inventory.AppEngInternalInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.world.World;

public class ContainerPatternValueAmount
extends AEBaseContainer {
    private final Slot patternValue = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);
    private int valueIndex;

    public ContainerPatternValueAmount(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
        this.addSlotToContainer(this.patternValue);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.verifyPermissions(SecurityPermissions.CRAFT, false);
    }

    public IGrid getGrid() {
        IActionHost h = (IActionHost)this.getTarget();
        return h.getActionableNode().getGrid();
    }

    public World getWorld() {
        return this.getPlayerInv().player.worldObj;
    }

    public BaseActionSource getActionSrc() {
        return new PlayerSource(this.getPlayerInv().player, (IActionHost)this.getTarget());
    }

    public Slot getPatternValue() {
        return this.patternValue;
    }

    public int getValueIndex() {
        return this.valueIndex;
    }

    public void setValueIndex(int valueIndex) {
        this.valueIndex = valueIndex;
    }
}

