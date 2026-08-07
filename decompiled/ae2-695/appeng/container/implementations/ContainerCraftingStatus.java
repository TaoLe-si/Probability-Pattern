/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.container.implementations;

import appeng.api.storage.ITerminalHost;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerCPUTable;
import appeng.container.implementations.ContainerCraftingCPU;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.container.interfaces.ICraftingCPUSelectorContainer;
import java.util.List;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCraftingStatus
extends ContainerCraftingCPU
implements ICraftingCPUSelectorContainer {
    @GuiSync.Recurse(value=5)
    public ContainerCPUTable cpuTable = new ContainerCPUTable(this, this::setCPU, true, c -> true);

    public ContainerCraftingStatus(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
    }

    public ContainerCPUTable getCPUTable() {
        return this.cpuTable;
    }

    @Override
    public void detectAndSendChanges() {
        this.cpuTable.detectAndSendChanges(this.getNetwork(), this.crafters);
        super.detectAndSendChanges();
    }

    @Override
    public void selectCPU(int serial) {
        this.cpuTable.selectCPU(serial);
    }

    public List<CraftingCPUStatus> getCPUs() {
        return this.cpuTable.getCPUs();
    }
}

