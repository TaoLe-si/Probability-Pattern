/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
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
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.World;

public class ContainerPatternMulti
extends AEBaseContainer {
    public ContainerPatternMulti(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
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
}

