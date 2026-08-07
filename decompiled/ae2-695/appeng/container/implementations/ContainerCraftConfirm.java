/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ChatComponentText
 *  net.minecraft.util.IChatComponent
 *  net.minecraft.world.World
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.CraftingAllow;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerCPUTable;
import appeng.container.implementations.ContainerOptimizePatterns;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.container.interfaces.ICraftingCPUSelectorContainer;
import appeng.core.AELog;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCraftingTreeData;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingJobV2;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.parts.reporting.PartTerminal;
import appeng.tile.misc.TilePatternOptimizationMatrix;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class ContainerCraftConfirm
extends AEBaseContainer
implements ICraftingCPUSelectorContainer {
    private Future<ICraftingJob> job;
    private ICraftingJob result;
    @GuiSync(value=0)
    public long bytesUsed;
    @GuiSync(value=1)
    public long cpuBytesAvail;
    @GuiSync(value=2)
    public int cpuCoProcessors;
    @GuiSync(value=3)
    public boolean autoStart = false;
    @GuiSync(value=4)
    public boolean simulation = true;
    @GuiSync(value=5)
    public boolean autoStartAndFollow = false;
    @GuiSync(value=6)
    public boolean noCPU = true;
    @GuiSync(value=7)
    public String myName = "";
    @GuiSync(value=8)
    public boolean isAllowedToRunPatternOptimization = false;
    @GuiSync.Recurse(value=9)
    public final ContainerCPUTable cpuTable = new ContainerCPUTable(this, this::onCPUUpdate, false, this::cpuMatches);
    @GuiSync(value=10)
    public String serializedItemToCraft = "";

    public ContainerCraftConfirm(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
    }

    @Override
    public void selectCPU(int cpu) {
        this.cpuTable.selectCPU(cpu);
    }

    public void onCPUUpdate(ICraftingCPU cpu) {
        if (cpu == null) {
            this.setCpuAvailableBytes(0L);
            this.setCpuCoProcessors(0);
            this.setName("");
        } else {
            this.setName(cpu.getName());
            this.setCpuAvailableBytes(cpu.getAvailableStorage());
            this.setCpuCoProcessors(cpu.getCoProcessors());
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (this.bytesUsed != 0L) {
            this.cpuTable.detectAndSendChanges(this.getGrid(), this.crafters);
        }
        if (Platform.isClient()) {
            return;
        }
        this.setNoCPU(this.cpuTable.getCPUs().isEmpty());
        IGrid grid = this.getGrid();
        if (grid != null) {
            this.isAllowedToRunPatternOptimization = !this.getGrid().getMachines(TilePatternOptimizationMatrix.class).isEmpty();
        }
        super.detectAndSendChanges();
        if (this.getJob() != null && this.getJob().isDone()) {
            try {
                this.result = this.getJob().get();
                if (!this.result.isSimulation()) {
                    this.setSimulation(false);
                    if (this.isAutoStartAndFollow()) {
                        this.startJob(true);
                        return;
                    }
                    if (this.isAutoStart()) {
                        this.startJob();
                        return;
                    }
                } else {
                    this.setSimulation(true);
                }
                try {
                    PacketMEInventoryUpdate storageUpdate = new PacketMEInventoryUpdate(0);
                    PacketMEInventoryUpdate pendingUpdate = new PacketMEInventoryUpdate(1);
                    PacketMEInventoryUpdate missingUpdate = this.result.isSimulation() ? new PacketMEInventoryUpdate(2) : null;
                    IItemList<IAEItemStack> plan = AEApi.instance().storage().createItemList();
                    this.result.populatePlan(plan);
                    this.setUsedBytes(this.result.getByteTotal());
                    for (IAEItemStack plannedItem : plan) {
                        IAEItemStack toExtract = plannedItem.copy();
                        toExtract.reset();
                        toExtract.setStackSize(plannedItem.getStackSize());
                        IAEItemStack toCraft = plannedItem.copy();
                        toCraft.reset();
                        toCraft.setStackSize(plannedItem.getCountRequestable());
                        toCraft.setCountRequestableCrafts(plannedItem.getCountRequestableCrafts());
                        IStorageGrid sg = (IStorageGrid)this.getGrid().getCache(IStorageGrid.class);
                        MECraftingInventory items = this.result.getStorageAtBeginning();
                        IAEItemStack missing = null;
                        if (missingUpdate != null && this.result.isSimulation()) {
                            missing = toExtract.copy();
                            if ((toExtract = items.extractItems(toExtract, Actionable.SIMULATE, this.getActionSource())) == null) {
                                toExtract = missing.copy();
                                toExtract.setStackSize(0L);
                            }
                            missing.setStackSize(missing.getStackSize() - toExtract.getStackSize());
                        }
                        if (toExtract.getStackSize() > 0L && toCraft.getStackSize() <= 0L && (missing == null || missing.getStackSize() <= 0L)) {
                            long available;
                            IAEItemStack availableStack = items.extractItems((IAEItemStack)toExtract.copy().setStackSize(Long.MAX_VALUE), Actionable.SIMULATE, this.getActionSource());
                            long l = available = availableStack == null ? 0L : availableStack.getStackSize();
                            if (available > 0L) {
                                toExtract.setUsedPercent((float)toExtract.getStackSize() / ((float)available / 100.0f));
                            } else {
                                toExtract.setUsedPercent(0.0f);
                            }
                        }
                        if (toExtract.getStackSize() > 0L) {
                            storageUpdate.appendItem(toExtract);
                        }
                        if (toCraft.getStackSize() > 0L) {
                            pendingUpdate.appendItem(toCraft);
                        }
                        if (missingUpdate == null || missing == null || missing.getStackSize() <= 0L) continue;
                        missingUpdate.appendItem(missing);
                    }
                    List<PacketCraftingTreeData> treeUpdates = this.result instanceof CraftingJobV2 ? PacketCraftingTreeData.createChunks((CraftingJobV2)this.result) : null;
                    for (Object player : this.crafters) {
                        if (!(player instanceof EntityPlayerMP)) continue;
                        EntityPlayerMP playerMP = (EntityPlayerMP)player;
                        NetworkHandler.instance.sendTo(storageUpdate, playerMP);
                        NetworkHandler.instance.sendTo(pendingUpdate, playerMP);
                        if (missingUpdate != null) {
                            NetworkHandler.instance.sendTo(missingUpdate, playerMP);
                        }
                        if (treeUpdates == null) continue;
                        for (PacketCraftingTreeData pkt : treeUpdates) {
                            NetworkHandler.instance.sendTo(pkt, playerMP);
                        }
                    }
                }
                catch (IOException storageUpdate) {
                }
            }
            catch (Throwable e) {
                this.getPlayerInv().player.addChatMessage((IChatComponent)new ChatComponentText("Error: " + e.toString()));
                AELog.debug(e);
                this.setValidContainer(false);
                this.result = null;
            }
            this.setJob(null);
        }
        this.verifyPermissions(SecurityPermissions.CRAFT, false);
    }

    private IGrid getGrid() {
        IActionHost h = (IActionHost)this.getTarget();
        if (h == null || h.getActionableNode() == null) {
            return null;
        }
        return h.getActionableNode().getGrid();
    }

    public IAEItemStack getItemToCraft() {
        try {
            ByteBuf deserialized = Unpooled.wrappedBuffer((byte[])this.serializedItemToCraft.getBytes(StandardCharsets.ISO_8859_1));
            return AEApi.instance().storage().readItemFromPacket(deserialized);
        }
        catch (IOException e) {
            AELog.debug(e);
            AELog.debug("Deserializing IAEItemStack Failed", new Object[0]);
            return null;
        }
    }

    public boolean cpuCraftingSameItem(CraftingCPUStatus c) {
        if (c.getCrafting() == null || this.getItemToCraft() == null) {
            return false;
        }
        return c.getCrafting().isSameType(this.getItemToCraft());
    }

    public boolean cpuMatches(CraftingCPUStatus c) {
        if (c.allowMode() == CraftingAllow.ONLY_NONPLAYER) {
            return false;
        }
        if (this.getUsedBytes() <= 0L) {
            return false;
        }
        if (c.isBusy() && this.cpuCraftingSameItem(c)) {
            return c.getStorage() >= this.getUsedBytes() + c.getUsedStorage();
        }
        return c.getStorage() >= this.getUsedBytes() && !c.isBusy();
    }

    public void startJob() {
        this.startJob(false);
    }

    public void startJob(boolean followCraft) {
        if (this.result != null && !this.isSimulation() && this.getGrid() != null) {
            ICraftingGrid cc = (ICraftingGrid)this.getGrid().getCache(ICraftingGrid.class);
            CraftingCPUStatus selected = this.cpuTable.getSelectedCPU();
            ICraftingLink g = cc.submitJob(this.result, null, selected == null ? null : selected.getServerCluster(), true, this.getActionSrc(), followCraft);
            this.setAutoStart(false);
            this.setAutoStartAndFollow(false);
            if (g != null) {
                this.switchToOriginalGUI();
            }
        }
    }

    public void optimizePatterns() {
        if (this.result instanceof CraftingJobV2 && !this.isSimulation() && this.getGrid() != null && !this.getGrid().getMachines(TilePatternOptimizationMatrix.class).isEmpty()) {
            Platform.openGUI(this.getPlayerInv().player, this.getOpenContext().getTile(), this.getOpenContext().getSide(), GuiBridge.GUI_OPTIMIZE_PATTERNS);
            Container container = this.getPlayerInv().player.openContainer;
            if (container instanceof ContainerOptimizePatterns) {
                ContainerOptimizePatterns cop = (ContainerOptimizePatterns)container;
                cop.setResult(this.result);
            }
        }
    }

    public void switchToOriginalGUI() {
        GuiBridge originalGui = null;
        IActionHost ah = this.getActionHost();
        if (ah instanceof WirelessTerminalGuiObject) {
            originalGui = GuiBridge.GUI_WIRELESS_TERM;
        }
        if (ah instanceof PartTerminal) {
            originalGui = GuiBridge.GUI_ME;
        }
        if (ah instanceof PartCraftingTerminal) {
            originalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
        }
        if (ah instanceof PartPatternTerminal) {
            originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }
        if (ah instanceof PartPatternTerminalEx) {
            originalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }
        if (originalGui != null && this.getOpenContext() != null) {
            NetworkHandler.instance.sendTo(new PacketSwitchGuis(originalGui), (EntityPlayerMP)this.getInventoryPlayer().player);
            TileEntity te = this.getOpenContext().getTile();
            Platform.openGUI(this.getInventoryPlayer().player, te, this.getOpenContext().getSide(), originalGui);
        }
    }

    private BaseActionSource getActionSrc() {
        return new PlayerSource(this.getPlayerInv().player, (IActionHost)this.getTarget());
    }

    public void removeCraftingFromCrafters(ICrafting c) {
        super.removeCraftingFromCrafters(c);
        if (this.getJob() != null) {
            this.getJob().cancel(true);
            this.setJob(null);
        }
    }

    public void onContainerClosed(EntityPlayer par1EntityPlayer) {
        super.onContainerClosed(par1EntityPlayer);
        if (this.getJob() != null) {
            this.getJob().cancel(true);
            this.setJob(null);
        }
    }

    public World getWorld() {
        return this.getPlayerInv().player.worldObj;
    }

    public boolean isAutoStart() {
        return this.autoStart;
    }

    public boolean isAutoStartAndFollow() {
        return this.autoStartAndFollow;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public void setAutoStartAndFollow(boolean autoStartAndFollow) {
        this.autoStartAndFollow = autoStartAndFollow;
    }

    public long getUsedBytes() {
        return this.bytesUsed;
    }

    private void setUsedBytes(long bytesUsed) {
        this.bytesUsed = bytesUsed;
    }

    public long getCpuAvailableBytes() {
        return this.cpuBytesAvail;
    }

    private void setCpuAvailableBytes(long cpuBytesAvail) {
        this.cpuBytesAvail = cpuBytesAvail;
    }

    public int getCpuCoProcessors() {
        return this.cpuCoProcessors;
    }

    private void setCpuCoProcessors(int cpuCoProcessors) {
        this.cpuCoProcessors = cpuCoProcessors;
    }

    public int getSelectedCpu() {
        return this.cpuTable.selectedCpuSerial;
    }

    public String getName() {
        return this.myName;
    }

    private void setName(@Nonnull String myName) {
        this.myName = myName;
    }

    public boolean hasNoCPU() {
        return this.noCPU;
    }

    private void setNoCPU(boolean noCPU) {
        this.noCPU = noCPU;
    }

    public boolean isSimulation() {
        return this.simulation;
    }

    private void setSimulation(boolean simulation) {
        this.simulation = simulation;
    }

    private Future<ICraftingJob> getJob() {
        return this.job;
    }

    public void setJob(Future<ICraftingJob> job) {
        this.job = job;
    }

    public void setItemToCraft(@Nonnull IAEItemStack itemToCraft) {
        try {
            ByteBuf serialized = Unpooled.buffer();
            itemToCraft.writeToPacket(serialized);
            this.serializedItemToCraft = serialized.toString(StandardCharsets.ISO_8859_1);
        }
        catch (IOException e) {
            AELog.debug(e);
            AELog.debug("Deserializing IAEItemStack Failed", new Object[0]);
        }
    }
}

