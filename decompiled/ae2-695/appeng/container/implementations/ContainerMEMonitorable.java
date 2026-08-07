/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableSet
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.CraftingAllow;
import appeng.api.config.PinsState;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.TypeFilter;
import appeng.api.config.ViewItems;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.implementations.tiles.IMEChest;
import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.parts.IPart;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.ITerminalPins;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IPinsHandler;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.PinsHandler;
import appeng.items.storage.ItemViewCell;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class ContainerMEMonitorable
extends AEBaseContainer
implements IConfigManagerHost,
IConfigurableObject,
IMEMonitorHandlerReceiver<IAEItemStack>,
IPinsHandler {
    private final SlotRestrictedInput[] cellView = new SlotRestrictedInput[5];
    private final IMEMonitor<IAEItemStack> monitor;
    private final IItemList<IAEItemStack> items = AEApi.instance().storage().createItemList();
    private final IConfigManager clientCM;
    private final ITerminalHost host;
    private PinsHandler pinsHandler = null;
    @GuiSync(value=99)
    public boolean canAccessViewCells = false;
    @GuiSync(value=98)
    public boolean hasPower = false;
    private IConfigManagerHost gui;
    private IConfigManager serverCM;
    private IGridNode networkNode;
    protected SlotRestrictedInput patternRefiller = null;
    private int lastUpdate = 0;

    public ContainerMEMonitorable(InventoryPlayer ip, ITerminalHost monitorable) {
        this(ip, monitorable, true);
    }

    protected ContainerMEMonitorable(InventoryPlayer ip, ITerminalHost monitorable, boolean bindInventory) {
        super(ip, monitorable instanceof TileEntity ? (TileEntity)monitorable : null, monitorable instanceof IPart ? (IPart)((Object)monitorable) : null);
        this.host = monitorable;
        this.clientCM = new ConfigManager(this);
        this.clientCM.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        this.clientCM.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        this.clientCM.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        this.clientCM.registerSetting(Settings.TYPE_FILTER, TypeFilter.ALL);
        this.clientCM.registerSetting(Settings.PINS_STATE, PinsState.DISABLED);
        if (Platform.isServer()) {
            if (monitorable instanceof ITerminalPins) {
                ITerminalPins t = (ITerminalPins)((Object)monitorable);
                this.pinsHandler = t.getPinsHandler(ip.player);
            }
            this.serverCM = monitorable.getConfigManager();
            this.monitor = monitorable.getItemInventory();
            if (this.monitor != null) {
                IGridNode node;
                this.monitor.addListener(this, null);
                this.setCellInventory(this.monitor);
                if (monitorable instanceof IPortableCell) {
                    this.setPowerSource((IEnergySource)((Object)monitorable));
                } else if (monitorable instanceof IMEChest) {
                    this.setPowerSource((IEnergySource)((Object)monitorable));
                } else if (monitorable instanceof IGridHost && (node = ((IGridHost)((Object)monitorable)).getGridNode(ForgeDirection.UNKNOWN)) != null) {
                    this.networkNode = node;
                    IGrid g = node.getGrid();
                    if (g != null) {
                        this.setPowerSource(new ChannelPowerSrc(this.networkNode, (IEnergySource)g.getCache(IEnergyGrid.class)));
                    }
                }
            } else {
                this.setValidContainer(false);
            }
        } else {
            this.monitor = null;
        }
        this.canAccessViewCells = false;
        if (monitorable instanceof IViewCellStorage) {
            for (int y = 0; y < 5; ++y) {
                this.cellView[y] = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.VIEW_CELL, ((IViewCellStorage)((Object)monitorable)).getViewCellStorage(), y, 206, y * 18 + 8, this.getInventoryPlayer());
                this.cellView[y].setAllowEdit(this.canAccessViewCells);
                this.addSlotToContainer(this.cellView[y]);
            }
        }
        if (this.isAPatternTerminal()) {
            this.patternRefiller = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, ((IUpgradeableHost)((Object)monitorable)).getInventoryByName("upgrades"), 0, 206, 101, this.getInventoryPlayer());
            this.addSlotToContainer(this.patternRefiller);
        }
        if (bindInventory) {
            this.bindPlayerInventory(ip, 0, 0);
        }
    }

    public IGridNode getNetworkNode() {
        return this.networkNode;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            if (this.monitor != this.host.getItemInventory()) {
                this.setValidContainer(false);
            }
            for (Settings set : this.serverCM.getSettings()) {
                Enum<?> sideRemote;
                Enum<?> sideLocal = this.serverCM.getSetting(set);
                if (sideLocal == (sideRemote = this.clientCM.getSetting(set))) continue;
                this.clientCM.putSetting(set, sideLocal);
                for (Object crafter : this.crafters) {
                    try {
                        NetworkHandler.instance.sendTo(new PacketValueConfig(set.name(), sideLocal.name()), (EntityPlayerMP)crafter);
                    }
                    catch (IOException e) {
                        AELog.debug(e);
                    }
                }
            }
            if (this.pinsHandler != null) {
                if (this.clientCM.getSetting(Settings.PINS_STATE) != this.pinsHandler.getPinsState()) {
                    this.clientCM.putSetting(Settings.PINS_STATE, this.pinsHandler.getPinsState());
                    this.updatePins(true);
                } else {
                    this.updatePins(false);
                }
            }
            if (!this.items.isEmpty()) {
                try {
                    IItemList<IAEItemStack> monitorCache = this.monitor.getStorageList();
                    PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();
                    for (IAEItemStack is : this.items) {
                        IAEItemStack send = monitorCache.findPrecise(is);
                        if (send == null) {
                            is.setStackSize(0L);
                            piu.appendItem(is);
                            continue;
                        }
                        piu.appendItem(send);
                    }
                    if (!piu.isEmpty()) {
                        this.items.resetStatus();
                        for (IAEItemStack c : this.crafters) {
                            if (!(c instanceof EntityPlayer)) continue;
                            NetworkHandler.instance.sendTo(piu, (EntityPlayerMP)c);
                        }
                    }
                }
                catch (IOException e) {
                    AELog.debug(e);
                }
            }
            this.updatePowerStatus();
            boolean oldAccessible = this.canAccessViewCells;
            boolean bl = this.canAccessViewCells = this.host instanceof WirelessTerminalGuiObject || this.hasAccess(SecurityPermissions.BUILD, false);
            if (this.canAccessViewCells != oldAccessible) {
                for (int y = 0; y < 5; ++y) {
                    if (this.cellView[y] == null) continue;
                    this.cellView[y].setAllowEdit(this.canAccessViewCells);
                }
            }
            super.detectAndSendChanges();
        }
    }

    protected void updatePowerStatus() {
        try {
            if (this.networkNode != null) {
                this.setPowered(this.networkNode.isActive());
            } else if (this.getPowerSource() instanceof IEnergyGrid) {
                this.setPowered(((IEnergyGrid)this.getPowerSource()).isNetworkPowered());
            } else {
                this.setPowered(this.getPowerSource().extractAEPower(1.0, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.8);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("canAccessViewCells")) {
            for (int y = 0; y < 5; ++y) {
                if (this.cellView[y] == null) continue;
                this.cellView[y].setAllowEdit(this.canAccessViewCells);
            }
        }
        super.onUpdate(field, oldValue, newValue);
    }

    public void addCraftingToCrafters(ICrafting c) {
        super.addCraftingToCrafters(c);
        this.queueInventory(c);
    }

    private void queueInventory(ICrafting c) {
        if (Platform.isServer() && c instanceof EntityPlayer && this.monitor != null) {
            try {
                PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();
                IItemList<IAEItemStack> monitorCache = this.monitor.getStorageList();
                for (IAEItemStack send : monitorCache) {
                    try {
                        piu.appendItem(send);
                    }
                    catch (BufferOverflowException boe) {
                        NetworkHandler.instance.sendTo(piu, (EntityPlayerMP)c);
                        piu = new PacketMEInventoryUpdate();
                        piu.appendItem(send);
                    }
                }
                NetworkHandler.instance.sendTo(piu, (EntityPlayerMP)c);
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
    }

    public void removeCraftingFromCrafters(ICrafting c) {
        super.removeCraftingFromCrafters(c);
        if (this.crafters.isEmpty() && this.monitor != null) {
            this.monitor.removeListener(this);
        }
    }

    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (this.monitor != null) {
            this.monitor.removeListener(this);
        }
    }

    @Override
    public boolean isValid(Object verificationToken) {
        return true;
    }

    @Override
    public void postChange(IBaseMonitor<IAEItemStack> monitor, Iterable<IAEItemStack> change, BaseActionSource source) {
        for (IAEItemStack is : change) {
            this.items.add(is);
        }
    }

    @Override
    public void onListUpdate() {
        for (Object c : this.crafters) {
            if (!(c instanceof ICrafting)) continue;
            ICrafting cr = (ICrafting)c;
            this.queueInventory(cr);
        }
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        if (this.getGui() != null) {
            this.getGui().updateSetting(manager, settingName, newValue);
        }
    }

    @Override
    public IConfigManager getConfigManager() {
        if (Platform.isServer()) {
            return this.serverCM;
        }
        return this.clientCM;
    }

    public ItemStack[] getViewCells() {
        ItemStack[] list = new ItemStack[this.cellView.length];
        for (int x = 0; x < this.cellView.length; ++x) {
            list[x] = this.cellView[x].getStack();
        }
        return list;
    }

    public SlotRestrictedInput getCellViewSlot(int index) {
        return this.cellView[index];
    }

    public SlotRestrictedInput[] getCellViewSlots() {
        return this.cellView;
    }

    public void toggleViewCell(int slotIdx) {
        if (!this.canAccessViewCells) {
            return;
        }
        Slot slot = this.getSlot(slotIdx);
        if (!(slot instanceof SlotRestrictedInput)) {
            return;
        }
        ItemStack cellStack = slot.getStack();
        if (cellStack == null) {
            return;
        }
        Item item = cellStack.getItem();
        if (!(item instanceof ItemViewCell)) {
            return;
        }
        ItemViewCell viewCell = (ItemViewCell)item;
        viewCell.toggleViewMode(cellStack);
        this.detectAndSendChanges();
    }

    public boolean isPowered() {
        return this.hasPower;
    }

    private void setPowered(boolean isPowered) {
        this.hasPower = isPowered;
    }

    private IConfigManagerHost getGui() {
        return this.gui;
    }

    public void setGui(@Nonnull IConfigManagerHost gui) {
        this.gui = gui;
    }

    public boolean isAPatternTerminal() {
        return false;
    }

    public IMEMonitor<IAEItemStack> getMonitor() {
        return this.monitor;
    }

    protected void refillBlankPatterns(Slot slot) {
        if (Platform.isServer()) {
            ItemStack blanks = slot.getStack();
            int blanksToRefill = 64;
            if (blanks != null) {
                blanksToRefill -= blanks.stackSize;
            }
            if (blanksToRefill <= 0) {
                return;
            }
            AEItemStack request = AEItemStack.create((ItemStack)AEApi.instance().definitions().materials().blankPattern().maybeStack(blanksToRefill).get());
            IAEItemStack extracted = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), request, this.getActionSource());
            if (extracted != null) {
                if (blanks != null) {
                    blanks.stackSize = (int)((long)blanks.stackSize + extracted.getStackSize());
                } else {
                    blanks = extracted.getItemStack();
                }
                slot.putStack(blanks);
            }
        }
    }

    public void updatePins(boolean forceUpdate) {
        ITerminalHost iTerminalHost;
        if (this.pinsHandler == null || !((iTerminalHost = this.host) instanceof ITerminalPins)) {
            return;
        }
        ITerminalPins itp = (ITerminalPins)((Object)iTerminalHost);
        boolean isActive = this.pinsHandler.getPinsState() != PinsState.DISABLED;
        ++this.lastUpdate;
        if (!forceUpdate && this.lastUpdate <= 20) {
            return;
        }
        this.lastUpdate = 0;
        if (isActive) {
            ICraftingGrid cc = (ICraftingGrid)itp.getGrid().getCache(ICraftingGrid.class);
            ImmutableList cpuList = cc.getCpus().asList();
            ArrayList<IAEItemStack> craftedItems = new ArrayList<IAEItemStack>();
            for (int i = 0; i < cpuList.size(); ++i) {
                BaseActionSource baseActionSource;
                ICraftingCPU cpu = (ICraftingCPU)cpuList.get(i);
                if (cpu.getCraftingAllowMode() == CraftingAllow.ONLY_NONPLAYER || cpu.getFinalOutput() == null || !((baseActionSource = cpu.getCurrentJobSource()) instanceof PlayerSource)) continue;
                PlayerSource src = (PlayerSource)baseActionSource;
                if (src.player != this.pinsHandler.getPlayer() || craftedItems.contains(cpu.getFinalOutput())) continue;
                if (cpu.isBusy()) {
                    craftedItems.add(0, cpu.getFinalOutput().copy());
                    continue;
                }
                craftedItems.add(cpu.getFinalOutput().copy());
            }
            this.pinsHandler.addItemsToPins(craftedItems);
        }
        this.pinsHandler.update(forceUpdate);
        this.onListUpdate();
    }

    @Override
    public void setPin(ItemStack is, int idx) {
        ITerminalHost iTerminalHost;
        if (this.pinsHandler == null || !((iTerminalHost = this.host) instanceof ITerminalPins)) {
            return;
        }
        ITerminalPins itp = (ITerminalPins)((Object)iTerminalHost);
        if (is == null) {
            ICraftingGrid cc = (ICraftingGrid)itp.getGrid().getCache(ICraftingGrid.class);
            ImmutableSet<ICraftingCPU> cpuSet = cc.getCpus();
            for (ICraftingCPU cpu : cpuSet) {
                if (cpu.getCraftingAllowMode() == CraftingAllow.ONLY_NONPLAYER || cpu.getFinalOutput() == null || !cpu.getFinalOutput().isSameType(this.getPin(idx))) continue;
                if (!cpu.isBusy()) {
                    cpu.resetFinalOutput();
                    continue;
                }
                return;
            }
        }
        this.pinsHandler.setPin(idx, is);
        this.updatePins(true);
    }

    @Override
    public ItemStack getPin(int idx) {
        if (this.pinsHandler == null) {
            return null;
        }
        return this.pinsHandler.getPin(idx);
    }

    @Override
    public void setPinsState(PinsState pinsState) {
        if (this.pinsHandler == null) {
            return;
        }
        this.clientCM.putSetting(Settings.PINS_STATE, pinsState);
        this.pinsHandler.setPinsState(pinsState);
        this.updatePins(true);
    }

    @Override
    public PinsState getPinsState() {
        return this.pinsHandler != null ? this.pinsHandler.getPinsState() : PinsState.DISABLED;
    }

    public PinsHandler getPinsHandler() {
        return this.pinsHandler;
    }
}

