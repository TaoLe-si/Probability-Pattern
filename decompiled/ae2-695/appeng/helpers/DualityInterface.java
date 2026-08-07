/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cofh.api.transport.IItemDuct
 *  com.google.common.collect.ImmutableSet
 *  com.gtnewhorizon.gtnhlib.capability.Capabilities
 *  cpw.mods.fml.common.Loader
 *  net.minecraft.block.Block
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ISidedInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.Vec3
 *  net.minecraft.world.World
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.AdvancedBlockingMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.crafting.ICraftingIconProvider;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkCraftingPushedPattern;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPart;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IConfigManager;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.features.registries.BlockingModeIgnoreItemRegistry;
import appeng.core.settings.TickRates;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.IPriorityHost;
import appeng.helpers.MultiCraftingTracker;
import appeng.helpers.UnlockCraftingEvent;
import appeng.me.GridAccessException;
import appeng.me.cache.NetworkMonitor;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.storage.MEMonitorIInventory;
import appeng.me.storage.MEMonitorPassThrough;
import appeng.me.storage.NullInventory;
import appeng.parts.automation.StackUpgradeInventory;
import appeng.parts.automation.UpgradeInventory;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.tile.networking.TileCableBus;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.InventoryAdaptor;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.ScheduledReason;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import appeng.util.inv.WrapperInvSlot;
import appeng.util.item.AEItemStack;
import cofh.api.transport.IItemDuct;
import com.google.common.collect.ImmutableSet;
import com.gtnewhorizon.gtnhlib.capability.Capabilities;
import cpw.mods.fml.common.Loader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class DualityInterface
implements IGridTickable,
IStorageMonitorable,
IInventoryDestination,
IAEAppEngInventory,
IConfigManagerHost,
ICraftingProvider,
IUpgradeableHost,
IPriorityHost {
    public static final int NUMBER_OF_STORAGE_SLOTS = 9;
    public static final int NUMBER_OF_CONFIG_SLOTS = 9;
    public static final int NUMBER_OF_PATTERN_SLOTS = 9;
    private static final Collection<Block> BAD_BLOCKS = new HashSet<Block>(100);
    private final int[] sides = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    private final IAEItemStack[] requireWork = new IAEItemStack[]{null, null, null, null, null, null, null, null, null};
    private final boolean[] hasFuzzyConfig = new boolean[]{false, false, false, false, false, false, false, false, false};
    private final MultiCraftingTracker craftingTracker;
    protected final AENetworkProxy gridProxy;
    private final IInterfaceHost iHost;
    private final BaseActionSource mySource;
    private final BaseActionSource interfaceRequestSource;
    private final ConfigManager cm = new ConfigManager(this);
    private final AppEngInternalAEInventory config = new AppEngInternalAEInventory(this, 9);
    private AppEngInternalInventory storage = new AppEngInternalInventory(this, 9);
    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, 36);
    private WrapperInvSlot slotInv = new WrapperInvSlot(this.storage);
    private final MEMonitorPassThrough<IAEItemStack> items = new MEMonitorPassThrough(new NullInventory(), StorageChannel.ITEMS);
    private final MEMonitorPassThrough<IAEFluidStack> fluids = new MEMonitorPassThrough(new NullInventory(), StorageChannel.FLUIDS);
    private final UpgradeInventory upgrades;
    private ItemStack stored;
    private IAEItemStack fuzzyItemStack;
    private boolean hasConfig = false;
    private int priority;
    public List<ICraftingPatternDetails> craftingList = null;
    public boolean sharedInventory = false;
    private List<ItemStack> waitingToSend = null;
    private IMEInventory<IAEItemStack> destination;
    private boolean isWorking = false;
    protected static final boolean EIO = Loader.isModLoaded((String)"EnderIO");
    private YesNo redstoneState = YesNo.UNDECIDED;
    private UnlockCraftingEvent unlockEvent;
    private List<IAEItemStack> unlockStacks;
    private int lastInputHash = 0;
    private ScheduledReason scheduledReason = ScheduledReason.UNDEFINED;

    public DualityInterface(AENetworkProxy networkProxy, IInterfaceHost ih) {
        this.gridProxy = networkProxy;
        this.gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        this.upgrades = new StackUpgradeInventory(this.gridProxy.getMachineRepresentation(), this, 4);
        this.cm.registerSetting(Settings.BLOCK, (Enum)YesNo.NO);
        this.cm.registerSetting(Settings.SMART_BLOCK, (Enum)YesNo.NO);
        this.cm.registerSetting(Settings.INTERFACE_TERMINAL, (Enum)YesNo.YES);
        this.cm.registerSetting(Settings.INSERTION_MODE, (Enum)InsertionMode.DEFAULT);
        this.cm.registerSetting(Settings.ADVANCED_BLOCKING_MODE, (Enum)AdvancedBlockingMode.DEFAULT);
        this.cm.registerSetting(Settings.LOCK_CRAFTING_MODE, (Enum)LockCraftingMode.NONE);
        this.cm.registerSetting(Settings.PATTERN_OPTIMIZATION, (Enum)YesNo.YES);
        this.cm.registerSetting(Settings.FUZZY_MODE, (Enum)FuzzyMode.IGNORE_ALL);
        this.iHost = ih;
        this.craftingTracker = new MultiCraftingTracker(this.iHost, 9);
        MachineSource actionSource = new MachineSource(this.iHost);
        this.mySource = actionSource;
        this.fluids.setChangeSource(actionSource);
        this.items.setChangeSource(actionSource);
        this.interfaceRequestSource = new InterfaceRequestSource(this.iHost);
    }

    @Override
    public void saveChanges() {
        this.iHost.saveChanges();
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removed, ItemStack added) {
        block15: {
            TileEntity te;
            if (mc == InvOperation.markDirty && (te = this.getHost().getTile()) != null && te.getWorldObj() != null) {
                te.getWorldObj().markTileEntityChunkModified(te.xCoord, te.yCoord, te.zCoord, te);
            }
            if (this.isWorking) {
                return;
            }
            if (inv == this.config) {
                this.readConfig();
            } else if (inv == this.patterns) {
                if (removed != null || added != null) {
                    this.updateCraftingList();
                }
            } else if (inv == this.storage) {
                if (slot >= 0) {
                    boolean had = this.hasWorkToDo();
                    this.updatePlan(slot);
                    boolean now = this.hasWorkToDo();
                    if (had != now) {
                        try {
                            if (now) {
                                this.gridProxy.getTick().alertDevice(this.gridProxy.getNode());
                                break block15;
                            }
                            this.gridProxy.getTick().sleepDevice(this.gridProxy.getNode());
                        }
                        catch (GridAccessException gridAccessException) {}
                    }
                }
            } else if (inv == this.upgrades && this.getInstalledUpgrades(Upgrades.LOCK_CRAFTING) == 0) {
                this.cm.putSetting(Settings.LOCK_CRAFTING_MODE, (Enum)LockCraftingMode.NONE);
                this.resetCraftingLock();
            }
        }
    }

    public void writeToNBT(NBTTagCompound data) {
        this.config.writeToNBT(data, "config");
        this.patterns.writeToNBT(data, "patterns");
        if (!this.sharedInventory) {
            this.storage.writeToNBT(data, "storage");
        }
        this.upgrades.writeToNBT(data, "upgrades");
        this.cm.writeToNBT(data);
        this.craftingTracker.writeToNBT(data);
        data.setInteger("priority", this.priority);
        if (this.unlockEvent == UnlockCraftingEvent.PULSE) {
            data.setByte("unlockEvent", (byte)1);
        } else if (this.unlockEvent == UnlockCraftingEvent.RESULT) {
            if (this.unlockStacks != null && !this.unlockStacks.isEmpty()) {
                data.setByte("unlockEvent", (byte)2);
                NBTTagList stackList = new NBTTagList();
                for (IAEItemStack stack : this.unlockStacks) {
                    NBTTagCompound stackTag = new NBTTagCompound();
                    stack.writeToNBT(stackTag);
                    stackList.appendTag((NBTBase)stackTag);
                }
                data.setTag("unlockStacks", (NBTBase)stackList);
            } else {
                AELog.error("Saving interface {}, locked waiting for stack, but stack is null!", this.iHost);
            }
        }
        NBTTagList waitingToSend = new NBTTagList();
        if (this.waitingToSend != null) {
            for (ItemStack is : this.waitingToSend) {
                NBTTagCompound item = new NBTTagCompound();
                is.writeToNBT(item);
                if (is.stackSize > 127) {
                    item.setInteger("Count", is.stackSize);
                }
                waitingToSend.appendTag((NBTBase)item);
            }
        }
        data.setTag("waitingToSend", (NBTBase)waitingToSend);
    }

    public void readFromNBT(NBTTagCompound data) {
        this.waitingToSend = null;
        NBTTagList waitingList = data.getTagList("waitingToSend", 10);
        if (waitingList != null) {
            for (int x = 0; x < waitingList.tagCount(); ++x) {
                ItemStack is;
                NBTTagCompound c = waitingList.getCompoundTagAt(x);
                if (c == null || (is = ItemStack.loadItemStackFromNBT((NBTTagCompound)c)) == null) continue;
                if (c.hasKey("Count", 3)) {
                    is.stackSize = c.getInteger("Count");
                }
                this.addToSendList(is);
            }
        }
        byte unlockEventType = data.getByte("unlockEvent");
        switch (unlockEventType) {
            case 0: {
                UnlockCraftingEvent unlockCraftingEvent = null;
                break;
            }
            case 1: {
                UnlockCraftingEvent unlockCraftingEvent = UnlockCraftingEvent.PULSE;
                break;
            }
            case 2: {
                UnlockCraftingEvent unlockCraftingEvent = UnlockCraftingEvent.RESULT;
                break;
            }
            default: {
                AELog.error("Unknown unlock event type {} in NBT for interface: {}", unlockEventType, data);
                UnlockCraftingEvent unlockCraftingEvent = this.unlockEvent = null;
            }
        }
        if (this.unlockEvent == UnlockCraftingEvent.RESULT) {
            NBTTagList stackList = data.getTagList("unlockStacks", 10);
            for (int index = 0; index < stackList.tagCount(); ++index) {
                NBTTagCompound stackTag = stackList.getCompoundTagAt(index);
                IAEItemStack unlockStack = AEItemStack.loadItemStackFromNBT(stackTag);
                if (unlockStack == null) {
                    AELog.error("Could not load unlock stack for interface from NBT: {}", data);
                    continue;
                }
                if (this.unlockStacks == null) {
                    this.unlockStacks = new ArrayList<IAEItemStack>();
                }
                this.unlockStacks.add(unlockStack);
            }
        } else {
            this.unlockStacks = null;
        }
        this.craftingTracker.readFromNBT(data);
        this.upgrades.readFromNBT(data, "upgrades");
        this.config.readFromNBT(data, "config");
        this.patterns.readFromNBT(data, "patterns");
        this.storage.readFromNBT(data, "storage");
        this.priority = data.getInteger("priority");
        this.cm.readFromNBT(data);
        this.readConfig();
        this.updateCraftingList();
    }

    private void addToSendList(ItemStack is) {
        if (is == null) {
            return;
        }
        if (this.waitingToSend == null) {
            this.waitingToSend = new LinkedList<ItemStack>();
        }
        this.waitingToSend.add(is);
        try {
            this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
        }
        catch (GridAccessException gridAccessException) {
            // empty catch block
        }
    }

    public void readConfig() {
        this.hasConfig = false;
        for (ItemStack p : this.config) {
            if (p == null) continue;
            this.hasConfig = true;
            break;
        }
        boolean had = this.hasWorkToDo();
        for (int x = 0; x < 9; ++x) {
            this.updatePlan(x);
        }
        boolean has = this.hasWorkToDo();
        if (had != has) {
            try {
                if (has) {
                    this.gridProxy.getTick().alertDevice(this.gridProxy.getNode());
                } else {
                    this.gridProxy.getTick().sleepDevice(this.gridProxy.getNode());
                }
            }
            catch (GridAccessException gridAccessException) {
                // empty catch block
            }
        }
        this.notifyNeighbors();
    }

    public void updateCraftingList() {
        boolean[] accountedFor = new boolean[this.patterns.getSizeInventory()];
        if (!this.gridProxy.isReady()) {
            return;
        }
        if (this.craftingList != null) {
            Iterator<ICraftingPatternDetails> i = this.craftingList.iterator();
            while (i.hasNext()) {
                ICraftingPatternDetails details = i.next();
                boolean found = false;
                for (int x = 0; x < accountedFor.length; ++x) {
                    ItemStack is = this.patterns.getStackInSlot(x);
                    if (details.getPattern() != is) continue;
                    found = true;
                    accountedFor[x] = true;
                }
                if (found) continue;
                i.remove();
            }
        }
        for (int x = 0; x < accountedFor.length; ++x) {
            if (accountedFor[x]) continue;
            this.addToCraftingList(x);
        }
        try {
            this.gridProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
        }
        catch (GridAccessException gridAccessException) {
            // empty catch block
        }
    }

    protected boolean hasWorkToDo() {
        if (this.hasItemsToSend()) {
            return true;
        }
        for (IAEItemStack requiredWork : this.requireWork) {
            if (requiredWork == null) continue;
            return true;
        }
        return false;
    }

    private void updatePlan(int slot) {
        ItemStack Stored;
        IAEItemStack req = this.config.getAEStackInSlot(slot);
        if (req != null && req.getStackSize() <= 0L) {
            this.config.setInventorySlotContents(slot, null);
            req = null;
        }
        int fuzzycards = this.getInstalledUpgrades(Upgrades.FUZZY);
        this.stored = Stored = this.storage.getStackInSlot(slot);
        if (req == null && Stored != null) {
            IAEItemStack work = AEApi.instance().storage().createItemStack(Stored);
            this.requireWork[slot] = (IAEItemStack)work.setStackSize(-work.getStackSize());
            return;
        }
        if (req != null) {
            if (Stored == null) {
                this.requireWork[slot] = req.copy();
                return;
            }
            if (fuzzycards == 1 && slot > 5 || fuzzycards == 2 && slot > 2 || fuzzycards == 3) {
                if (this.fuzzyItemStack == null) {
                    this.fuzzyItemStack = AEApi.instance().storage().createItemStack(Stored);
                }
                if (req.getStackSize() != (long)Stored.stackSize) {
                    this.requireWork[slot] = this.fuzzyItemStack.copy();
                    this.requireWork[slot].setStackSize(req.getStackSize() - (long)Stored.stackSize);
                    return;
                }
            } else if (req.isSameType(Stored)) {
                if (req.getStackSize() != (long)Stored.stackSize) {
                    this.requireWork[slot] = req.copy();
                    this.requireWork[slot].setStackSize(req.getStackSize() - (long)Stored.stackSize);
                    return;
                }
            } else {
                IAEItemStack work = AEApi.instance().storage().createItemStack(Stored);
                this.requireWork[slot] = (IAEItemStack)work.setStackSize(-work.getStackSize());
                return;
            }
        }
        this.requireWork[slot] = null;
    }

    public void notifyNeighbors() {
        TileEntity te;
        if (this.gridProxy.isActive()) {
            try {
                this.gridProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
                this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
            }
            catch (GridAccessException gridAccessException) {
                // empty catch block
            }
        }
        if ((te = this.iHost.getTileEntity()) != null && te.getWorldObj() != null) {
            Platform.notifyBlocksOfNeighbors(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord);
        }
    }

    protected void addToCraftingList(int slot) {
        ICraftingPatternItem cpi;
        ICraftingPatternDetails details;
        ItemStack is = this.patterns.getStackInSlot(slot);
        if (is == null) {
            return;
        }
        Item item = is.getItem();
        if (item instanceof ICraftingPatternItem && (details = (cpi = (ICraftingPatternItem)item).getPatternForItem(is, this.iHost.getTileEntity().getWorldObj())) != null) {
            if (this.craftingList == null) {
                this.craftingList = new LinkedList<ICraftingPatternDetails>();
            }
            details.setPriority(slot - 36 * this.getPriority());
            this.craftingList.add(details);
        }
    }

    protected boolean hasItemsToSend() {
        return this.waitingToSend != null && !this.waitingToSend.isEmpty();
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        IAEItemStack out = this.destination.injectItems(AEApi.instance().storage().createItemStack(stack), Actionable.SIMULATE, null);
        if (out == null) {
            return true;
        }
        return out.getStackSize() != (long)stack.stackSize;
    }

    public AppEngInternalAEInventory getConfig() {
        return this.config;
    }

    public AppEngInternalInventory getPatterns() {
        return this.patterns;
    }

    public AppEngInternalInventory getUpgrades() {
        return this.upgrades;
    }

    public AppEngInternalInventory setStorage(AppEngInternalInventory inv) {
        this.storage = inv;
        return this.storage;
    }

    public boolean setHasConfig(boolean ifHasConfig) {
        this.hasConfig = ifHasConfig;
        return this.hasConfig;
    }

    public int getConfigSize() {
        return this.config.getSizeInventory();
    }

    public WrapperInvSlot getSlotInv() {
        return this.slotInv;
    }

    public WrapperInvSlot setSlotInv(WrapperInvSlot slotInventory) {
        this.slotInv = slotInventory;
        return this.slotInv;
    }

    public void gridChanged() {
        try {
            this.items.setInternal(this.gridProxy.getStorage().getItemInventory());
            this.fluids.setInternal(this.gridProxy.getStorage().getFluidInventory());
        }
        catch (GridAccessException gae) {
            this.items.setInternal(new NullInventory());
            this.fluids.setInternal(new NullInventory());
        }
        this.notifyNeighbors();
    }

    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.SMART;
    }

    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this.iHost.getTileEntity());
    }

    public IInventory getInternalInventory() {
        return this.storage;
    }

    public List<ItemStack> getWaitingToSend() {
        return this.waitingToSend;
    }

    public void markDirty() {
        for (int slot = 0; slot < this.storage.getSizeInventory(); ++slot) {
            this.onChangeInventory(this.storage, slot, InvOperation.markDirty, null, null);
        }
    }

    public int[] getAccessibleSlotsFromSide(int side) {
        return this.sides;
    }

    public IAEItemStack[] fuzzyPoweredExtraction(IEnergySource energy, IMEInventory<IAEItemStack> cell, IAEItemStack itemStack, ItemStack is, BaseActionSource src, int iteration) {
        Collection<IAEItemStack> fzlist = null;
        IAEItemStack fuzzyItemStack = null;
        IAEItemStack pe = null;
        if (!(cell instanceof NetworkMonitor)) {
            return new IAEItemStack[]{null, fuzzyItemStack};
        }
        fzlist = ((NetworkMonitor)cell).getHandler().getSortedFuzzyItems(new ArrayList(), itemStack, (FuzzyMode)this.cm.getSetting(Settings.FUZZY_MODE), iteration);
        if (fzlist.iterator().hasNext()) {
            fuzzyItemStack = fzlist.iterator().next();
            if (fuzzyItemStack.isSameType(is) || is == null) {
                fuzzyItemStack.setStackSize(itemStack.getStackSize());
            } else {
                fuzzyItemStack = null;
            }
            if (fuzzyItemStack != null) {
                pe = Platform.poweredExtraction(energy, cell, fuzzyItemStack, src);
            }
        }
        return new IAEItemStack[]{pe, fuzzyItemStack};
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Interface.getMin(), TickRates.Interface.getMax(), !this.hasWorkToDo(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!this.gridProxy.isActive()) {
            if (AEConfig.instance.debugLogTiming) {
                TileEntity te = this.iHost.getTileEntity();
                AELog.debug("Timing: interface at (%d %d %d) is ticking while the grid is booting", te.xCoord, te.yCoord, te.zCoord);
            }
            return this.hasWorkToDo() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
        }
        boolean sentItems = false;
        if (this.hasItemsToSend()) {
            sentItems = this.pushItemsOut(this.iHost.getTargets());
        }
        boolean couldDoWork = this.updateStorage();
        boolean hasWorkToDo = this.hasWorkToDo();
        return hasWorkToDo || sentItems && this.hasItemsToSend() ? (couldDoWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER) : TickRateModulation.SLEEP;
    }

    private boolean pushItemsOut(EnumSet<ForgeDirection> possibleDirections) {
        if (!this.hasItemsToSend()) {
            return false;
        }
        TileEntity tile = this.iHost.getTileEntity();
        World w = tile.getWorldObj();
        Iterator<ItemStack> i = this.waitingToSend.iterator();
        boolean sentSomething = false;
        while (i.hasNext()) {
            ItemStack whatToSend = i.next();
            for (ForgeDirection s : possibleDirections) {
                TileEntity te = w.getTileEntity(tile.xCoord + s.offsetX, tile.yCoord + s.offsetY, tile.zCoord + s.offsetZ);
                if (te == null || te.getClass().getName().equals("li.cil.oc.common.tileentity.Adapter")) continue;
                if (te instanceof IInterfaceHost) {
                    IInterfaceHost host = (IInterfaceHost)te;
                    try {
                        if (host.getInterfaceDuality().sameGrid(this.gridProxy.getGrid())) {
                        }
                    }
                    catch (GridAccessException e) {}
                    continue;
                }
                InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, s.getOpposite());
                ItemStack Result = whatToSend;
                if (ad != null) {
                    Result = ad.addItems(whatToSend, this.getInsertionMode());
                } else if (EIO && te instanceof IItemDuct) {
                    Result = ((IItemDuct)te).insertItem(s.getOpposite(), whatToSend);
                }
                if (Result == null) {
                    whatToSend = null;
                    sentSomething = true;
                } else {
                    sentSomething |= Result.stackSize < whatToSend.stackSize;
                    whatToSend.stackSize = Result.stackSize;
                    whatToSend.setTagCompound(Result.getTagCompound());
                }
                if (whatToSend != null) continue;
                break;
            }
            if (whatToSend != null) continue;
            i.remove();
        }
        if (this.waitingToSend.isEmpty()) {
            this.waitingToSend = null;
        }
        return sentSomething;
    }

    private boolean updateStorage() {
        boolean didSomething = false;
        for (int x = 0; x < 9; ++x) {
            if (this.requireWork[x] == null) continue;
            didSomething = this.usePlan(x, this.requireWork[x]) || didSomething;
        }
        return didSomething;
    }

    private boolean usePlan(int x, IAEItemStack itemStack) {
        InventoryAdaptor adaptor = this.getAdaptor(x);
        int fuzzycards = this.getInstalledUpgrades(Upgrades.FUZZY);
        IAEItemStack acquired = null;
        this.isWorking = true;
        boolean changed = false;
        try {
            this.destination = this.gridProxy.getStorage().getItemInventory();
            IEnergyGrid src = this.gridProxy.getEnergy();
            if (itemStack.getStackSize() < 0L) {
                IAEItemStack toStore = itemStack.copy();
                toStore.setStackSize(-toStore.getStackSize());
                long diff = toStore.getStackSize();
                ItemStack canExtract = adaptor.simulateRemove((int)diff, toStore.getItemStack(), null);
                if (canExtract == null || (long)canExtract.stackSize != diff) {
                    changed = true;
                    throw new GridAccessException();
                }
                if ((toStore = Platform.poweredInsert(src, this.destination, toStore, this.interfaceRequestSource)) != null) {
                    diff -= toStore.getStackSize();
                }
                if (diff != 0L) {
                    changed = true;
                    ItemStack removed = adaptor.removeItems((int)diff, null, null);
                    if (removed == null) {
                        throw new IllegalStateException("bad attempt at managing inventory. ( removeItems )");
                    }
                    if ((long)removed.stackSize != diff) {
                        throw new IllegalStateException("bad attempt at managing inventory. ( removeItems )");
                    }
                    this.onStackReturnedToNetwork(AEItemStack.create(removed));
                }
            } else if (this.craftingTracker.isBusy(x)) {
                changed = this.handleCrafting(x, adaptor, itemStack);
            } else if (itemStack.getStackSize() > 0L) {
                if (adaptor.simulateAdd(itemStack.getItemStack()) != null) {
                    changed = true;
                    throw new GridAccessException();
                }
                if (fuzzycards == 1 && x > 5 || fuzzycards == 2 && x > 2 || fuzzycards == 3) {
                    int iteration = IterationCounter.fetchNewId();
                    IAEItemStack[] fpe = this.fuzzyPoweredExtraction(src, this.destination, itemStack, this.stored, this.interfaceRequestSource, iteration);
                    acquired = fpe[0];
                    this.fuzzyItemStack = fpe[1];
                    this.hasFuzzyConfig[x] = true;
                } else {
                    acquired = Platform.poweredExtraction(src, this.destination, itemStack, this.interfaceRequestSource);
                    this.fuzzyItemStack = null;
                }
                if (acquired != null) {
                    changed = true;
                    ItemStack issue = adaptor.addItems(acquired.getItemStack());
                    if (issue != null) {
                        throw new IllegalStateException("bad attempt at managing inventory. ( addItems )");
                    }
                } else {
                    changed = this.handleCrafting(x, adaptor, itemStack);
                    if (this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                        changed = true;
                    }
                }
            }
        }
        catch (GridAccessException gridAccessException) {
            // empty catch block
        }
        if (changed) {
            this.updatePlan(x);
        }
        this.isWorking = false;
        return changed;
    }

    private InventoryAdaptor getAdaptor(int slot) {
        return new AdaptorIInventory(this.slotInv.getWrapper(slot));
    }

    private boolean handleCrafting(int x, InventoryAdaptor d, IAEItemStack itemStack) {
        try {
            if (this.getInstalledUpgrades(Upgrades.CRAFTING) > 0 && itemStack != null) {
                return this.craftingTracker.handleCrafting(x, itemStack.getStackSize(), itemStack, d, this.iHost.getTileEntity().getWorldObj(), this.gridProxy.getGrid(), this.gridProxy.getCrafting(), this.mySource);
            }
        }
        catch (GridAccessException gridAccessException) {
            // empty catch block
        }
        return false;
    }

    @Override
    public int getInstalledUpgrades(Upgrades u) {
        if (this.upgrades == null) {
            return 0;
        }
        return this.upgrades.getInstalledUpgrades(u);
    }

    @Override
    public TileEntity getTile() {
        return (TileEntity)(this.iHost instanceof TileEntity ? this.iHost : null);
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        if (this.hasConfig()) {
            return new InterfaceInventory(this);
        }
        return this.items;
    }

    public boolean hasConfig() {
        return this.hasConfig;
    }

    @Override
    public IInventory getInventoryByName(String name) {
        if (name.equals("storage")) {
            return this.storage;
        }
        if (name.equals("patterns")) {
            return this.patterns;
        }
        if (name.equals("config")) {
            return this.config;
        }
        if (name.equals("upgrades")) {
            return this.upgrades;
        }
        return null;
    }

    public AppEngInternalInventory getStorage() {
        return this.storage;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        if (this.getInstalledUpgrades(Upgrades.CRAFTING) == 0) {
            this.cancelCrafting();
        }
        if (settingName == Settings.LOCK_CRAFTING_MODE && this.unlockEvent != null && !this.unlockEvent.matches((LockCraftingMode)newValue)) {
            this.resetCraftingLock();
        }
        this.markDirty();
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        if (this.hasConfig()) {
            return null;
        }
        return this.fluids;
    }

    private void cancelCrafting() {
        this.craftingTracker.cancel();
    }

    public IStorageMonitorable getMonitorable(ForgeDirection side, BaseActionSource src, IStorageMonitorable myInterface) {
        if (Platform.canAccess(this.gridProxy, src)) {
            return myInterface;
        }
        final DualityInterface di = this;
        return new IStorageMonitorable(){

            @Override
            public IMEMonitor<IAEItemStack> getItemInventory() {
                return new InterfaceInventory(di);
            }

            @Override
            public IMEMonitor<IAEFluidStack> getFluidInventory() {
                return null;
            }
        };
    }

    private boolean tileHasOnlyIgnoredItems(InventoryAdaptor ad) {
        for (ItemSlot i : ad) {
            ItemStack is = i.getItemStack();
            if (is == null || BlockingModeIgnoreItemRegistry.instance().isIgnored(is)) continue;
            return false;
        }
        return true;
    }

    private boolean shouldCheckFluid() {
        String hostName = this.iHost.getClass().getName();
        return hostName.contains("TileFluidInterface") || hostName.contains("PartFluidInterface");
    }

    private boolean inventoryCountsAsEmpty(TileEntity te, InventoryAdaptor ad, ForgeDirection side) {
        boolean isEmpty;
        String name = te.getBlockType().getUnlocalizedName();
        boolean bl = isEmpty = (name.equals("gt.blockmachines") || name.equals("tile.interface") || name.equals("tile.blockWritingTable")) && this.tileHasOnlyIgnoredItems(ad);
        if (this.shouldCheckFluid()) {
            isEmpty = name.equals("tile.interface");
        }
        return isEmpty;
    }

    public void notifyPushedPattern(IInterfaceHost pushingHost) {
        if (this.getInstalledUpgrades(Upgrades.ADVANCED_BLOCKING) == 0) {
            return;
        }
        TileEntity tile = this.iHost.getTileEntity();
        World w = tile.getWorldObj();
        EnumSet<ForgeDirection> possibleDirections = this.iHost.getTargets();
        for (ForgeDirection s : possibleDirections) {
            TileEntity te = w.getTileEntity(tile.xCoord + s.offsetX, tile.yCoord + s.offsetY, tile.zCoord + s.offsetZ);
            if (te == null) continue;
            try {
                IInterfaceHost host;
                TileCableBus cableBus;
                IPart part;
                if (te instanceof IInterfaceHost) {
                    IInterfaceHost host2 = (IInterfaceHost)te;
                    if (host2.getInterfaceDuality().sameGrid(this.gridProxy.getGrid()) || host2 == pushingHost) continue;
                    host2.getInterfaceDuality().receivePatternPushedEvent();
                    continue;
                }
                if (!(te instanceof TileCableBus) || !((part = (cableBus = (TileCableBus)te).getPart(s.getOpposite())) instanceof IInterfaceHost) || (host = (IInterfaceHost)((Object)part)) == pushingHost) continue;
                host.getInterfaceDuality().receivePatternPushedEvent();
            }
            catch (GridAccessException e) {}
        }
    }

    public void receivePatternPushedEvent() {
        this.lastInputHash = 0;
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (this.hasItemsToSend() || !this.gridProxy.isActive() || !this.craftingList.contains(patternDetails)) {
            this.scheduledReason = ScheduledReason.SOMETHING_STUCK;
            return false;
        }
        if (this.getCraftingLockedReason() != LockCraftingMode.NONE) {
            this.scheduledReason = ScheduledReason.LOCK_MODE;
            return false;
        }
        TileEntity tile = this.iHost.getTileEntity();
        World w = tile.getWorldObj();
        EnumSet<ForgeDirection> possibleDirections = this.iHost.getTargets();
        EnumSet<ForgeDirection> out = EnumSet.noneOf(ForgeDirection.class);
        boolean foundReason = false;
        for (ForgeDirection s : possibleDirections) {
            InventoryAdaptor ad;
            ICraftingMachine cm;
            TileEntity te = w.getTileEntity(tile.xCoord + s.offsetX, tile.yCoord + s.offsetY, tile.zCoord + s.offsetZ);
            if (te == null || te.getClass().getName().equals("li.cil.oc.common.tileentity.Adapter")) continue;
            if (te instanceof ICraftingMachine && (cm = (ICraftingMachine)te).acceptsPlans()) {
                if (!cm.pushPattern(patternDetails, table, s.getOpposite())) continue;
                this.onPushPatternSuccess(te, s.getOpposite(), patternDetails);
                return true;
            }
            if (te instanceof IInterfaceHost) {
                try {
                    if (((IInterfaceHost)te).getInterfaceDuality().sameGrid(this.gridProxy.getGrid())) {
                        if (foundReason) continue;
                        foundReason = true;
                        this.scheduledReason = ScheduledReason.SAME_NETWORK;
                    }
                }
                catch (GridAccessException e) {}
                continue;
            }
            if ((ad = InventoryAdaptor.getAdaptor(te, s.getOpposite())) != null) {
                if (this.isBlocking() && (!this.isSmartBlocking() || this.lastInputHash != patternDetails.hashCode()) && ad.containsItems() && !this.inventoryCountsAsEmpty(te, ad, s.getOpposite())) {
                    foundReason = true;
                    this.scheduledReason = ScheduledReason.BLOCKING_MODE;
                    continue;
                }
                if (!DualityInterface.acceptsItems(ad, table, this.getInsertionMode())) continue;
                for (int x = 0; x < table.getSizeInventory(); ++x) {
                    ItemStack is = table.getStackInSlot(x);
                    if (is == null) continue;
                    this.addToSendList(ad.addItems(is, this.getInsertionMode()));
                }
                out.add(s);
                this.pushItemsOut(out);
                this.onPushPatternSuccess(te, s.getOpposite(), patternDetails);
                return true;
            }
            if (!EIO || !(te instanceof IItemDuct)) continue;
            boolean hadAcceptedSome = false;
            for (int x = 0; x < table.getSizeInventory(); ++x) {
                ItemStack is = table.getStackInSlot(x);
                if (is == null) continue;
                ItemStack rest = ((IItemDuct)te).insertItem(s.getOpposite(), is);
                if (!hadAcceptedSome && rest != null && rest.stackSize == is.stackSize) break;
                hadAcceptedSome = true;
                this.addToSendList(rest);
            }
            if (!hadAcceptedSome) continue;
            out.add(s);
            this.pushItemsOut(out);
            this.onPushPatternSuccess(te, s.getOpposite(), patternDetails);
            return true;
        }
        if (!foundReason) {
            this.scheduledReason = ScheduledReason.NO_TARGET;
        }
        return false;
    }

    @Override
    public ScheduledReason getScheduledReason() {
        return this.scheduledReason;
    }

    @Override
    public boolean isBusy() {
        if (this.hasItemsToSend()) {
            this.scheduledReason = ScheduledReason.SOMETHING_STUCK;
            return true;
        }
        boolean busy = false;
        if (this.isBlocking()) {
            if (this.isSmartBlocking()) {
                return false;
            }
            EnumSet<ForgeDirection> possibleDirections = this.iHost.getTargets();
            TileEntity tile = this.iHost.getTileEntity();
            World w = tile.getWorldObj();
            boolean allAreBusy = true;
            for (ForgeDirection s : possibleDirections) {
                InventoryAdaptor ad;
                TileEntity te = w.getTileEntity(tile.xCoord + s.offsetX, tile.yCoord + s.offsetY, tile.zCoord + s.offsetZ);
                if (te != null && te.getClass().getName().equals("li.cil.oc.common.tileentity.Adapter") || (ad = InventoryAdaptor.getAdaptor(te, s.getOpposite())) == null || ad.simulateRemove(1, null, null) != null && !this.inventoryCountsAsEmpty(te, ad, s.getOpposite())) continue;
                allAreBusy = false;
                break;
            }
            if (busy = allAreBusy) {
                this.scheduledReason = ScheduledReason.BLOCKING_MODE;
            }
        }
        if (this.getCraftingLockedReason() != LockCraftingMode.NONE) {
            this.scheduledReason = ScheduledReason.LOCK_MODE;
            busy = true;
        }
        return busy;
    }

    private boolean sameGrid(IGrid grid) throws GridAccessException {
        return grid == this.gridProxy.getGrid();
    }

    private boolean isBlocking() {
        return this.cm.getSetting(Settings.BLOCK) == YesNo.YES;
    }

    private boolean isSmartBlocking() {
        return this.cm.getSetting(Settings.SMART_BLOCK) == YesNo.YES;
    }

    private InsertionMode getInsertionMode() {
        return (InsertionMode)this.cm.getSetting(Settings.INSERTION_MODE);
    }

    public boolean isFakeCraftingMode() {
        return this.getInstalledUpgrades(Upgrades.FAKE_CRAFTING) != 0;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static boolean acceptsItems(InventoryAdaptor ad, InventoryCrafting table, InsertionMode insertionMode) {
        for (int x = 0; x < table.getSizeInventory(); ++x) {
            int originalCount;
            int remainingCount;
            boolean moreItemsMayFit;
            ItemStack is = table.getStackInSlot(x);
            if (is == null) continue;
            is = is.copy();
            do {
                originalCount = is.stackSize;
                ItemStack remainingAfterSimulatedAdd = ad.simulateAdd(is.copy(), insertionMode);
                if (remainingAfterSimulatedAdd != null) {
                    if (!remainingAfterSimulatedAdd.isItemEqual(is)) return false;
                    remainingCount = originalCount - remainingAfterSimulatedAdd.stackSize;
                } else {
                    remainingCount = 0;
                }
                is.stackSize = remainingCount;
            } while (moreItemsMayFit = remainingCount > 0 && remainingCount < originalCount);
            if (is.stackSize <= 0) continue;
            return false;
        }
        return true;
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (this.gridProxy.isActive() && this.craftingList != null) {
            for (ICraftingPatternDetails details : this.craftingList) {
                craftingTracker.addCraftingOption(this, details);
            }
        }
    }

    public void addDrops(List<ItemStack> drops) {
        if (this.waitingToSend != null) {
            for (ItemStack is : this.waitingToSend) {
                if (is == null) continue;
                drops.add(is);
            }
        }
        for (ItemStack is : this.upgrades) {
            if (is == null) continue;
            drops.add(is);
        }
        for (ItemStack is : this.storage) {
            if (is == null) continue;
            drops.add(is);
        }
        for (ItemStack is : this.patterns) {
            if (is == null) continue;
            drops.add(is);
        }
    }

    public IUpgradeableHost getHost() {
        if (this.getPart() != null) {
            return (IUpgradeableHost)((Object)this.getPart());
        }
        if (this.getTile() instanceof IUpgradeableHost) {
            return (IUpgradeableHost)this.getTile();
        }
        return null;
    }

    private IPart getPart() {
        return (IPart)((Object)(this.iHost instanceof IPart ? this.iHost : null));
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingTracker.getRequestedJobs();
    }

    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack acquired, Actionable mode) {
        int slot = this.craftingTracker.getSlot(link);
        if (acquired != null && slot >= 0 && slot <= this.requireWork.length) {
            InventoryAdaptor adaptor = this.getAdaptor(slot);
            if (mode == Actionable.SIMULATE) {
                return AEItemStack.create(adaptor.simulateAdd(acquired.getItemStack()));
            }
            AEItemStack is = AEItemStack.create(adaptor.addItems(acquired.getItemStack()));
            this.updatePlan(slot);
            return is;
        }
        return acquired;
    }

    public void jobStateChange(ICraftingLink link) {
        this.craftingTracker.jobStateChange(link);
    }

    @Override
    public ItemStack getCrafterIcon() {
        TileEntity hostTile = this.iHost.getTileEntity();
        World hostWorld = hostTile.getWorldObj();
        String customName = null;
        if (((ICustomNameObject)((Object)this.iHost)).hasCustomName()) {
            customName = ((ICustomNameObject)((Object)this.iHost)).getCustomName();
        }
        EnumSet<ForgeDirection> possibleDirections = this.iHost.getTargets();
        for (ForgeDirection direction : possibleDirections) {
            int[] sides;
            ItemStack icon;
            ICraftingIconProvider craftingIconProvider;
            int xPos = hostTile.xCoord + direction.offsetX;
            int yPos = hostTile.yCoord + direction.offsetY;
            int zPos = hostTile.zCoord + direction.offsetZ;
            TileEntity directedTile = hostWorld.getTileEntity(xPos, yPos, zPos);
            if (directedTile == null) continue;
            if (directedTile instanceof IInterfaceHost) {
                try {
                    if (((IInterfaceHost)directedTile).getInterfaceDuality().sameGrid(this.gridProxy.getGrid())) {
                    }
                }
                catch (GridAccessException e) {}
                continue;
            }
            if ((craftingIconProvider = (ICraftingIconProvider)Capabilities.getCapability((TileEntity)directedTile, ICraftingIconProvider.class)) != null && (icon = craftingIconProvider.getMachineCraftingIcon()) != null) {
                if (customName != null) {
                    icon.setStackDisplayName(customName);
                }
                return icon;
            }
            InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(directedTile, direction.getOpposite());
            if (!(directedTile instanceof ICraftingMachine) && adaptor == null || directedTile instanceof IInventory && ((IInventory)directedTile).getSizeInventory() == 0 || directedTile instanceof ISidedInventory && ((sides = ((ISidedInventory)directedTile).getAccessibleSlotsFromSide(direction.getOpposite().ordinal())) == null || sides.length == 0)) continue;
            Block directedBlock = hostWorld.getBlock(xPos, yPos, zPos);
            ItemStack what = new ItemStack(directedBlock, 1, directedBlock.getDamageValue(hostWorld, xPos, yPos, zPos));
            try {
                ItemStack g;
                Vec3 from = Vec3.createVectorHelper((double)((double)hostTile.xCoord + 0.5), (double)((double)hostTile.yCoord + 0.5), (double)((double)hostTile.zCoord + 0.5));
                from = from.addVector((double)direction.offsetX * 0.501, (double)direction.offsetY * 0.501, (double)direction.offsetZ * 0.501);
                Vec3 to = from.addVector((double)direction.offsetX, (double)direction.offsetY, (double)direction.offsetZ);
                MovingObjectPosition mop = hostWorld.rayTraceBlocks(from, to, true);
                if (mop != null && !BAD_BLOCKS.contains(directedBlock) && mop.blockX == directedTile.xCoord && mop.blockY == directedTile.yCoord && mop.blockZ == directedTile.zCoord && (g = directedBlock.getPickBlock(mop, hostWorld, directedTile.xCoord, directedTile.yCoord, directedTile.zCoord, null)) != null) {
                    what = g;
                }
            }
            catch (Throwable t) {
                BAD_BLOCKS.add(directedBlock);
            }
            if (what.getItem() != null) {
                if (customName != null) {
                    what.setStackDisplayName(customName);
                }
                return what;
            }
            Item item = Item.getItemFromBlock((Block)directedBlock);
            if (item == null) continue;
            ItemStack icon2 = new ItemStack(item);
            if (customName != null) {
                icon2.setStackDisplayName(customName);
            }
            return icon2;
        }
        return null;
    }

    @Override
    public ICraftingMedium.BlockingMode getBlockingMode() {
        if (this.isBlocking() && this.isSmartBlocking()) {
            return ICraftingMedium.BlockingMode.SMART_BLOCKING;
        }
        if (this.isBlocking()) {
            return ICraftingMedium.BlockingMode.BLOCKING;
        }
        return ICraftingMedium.BlockingMode.NONE;
    }

    public String getTermName() {
        if (((ICustomNameObject)((Object)this.iHost)).hasCustomName()) {
            return ((ICustomNameObject)((Object)this.iHost)).getCustomName();
        }
        ItemStack item = this.getCrafterIcon();
        if (item != null) {
            return item.getUnlocalizedName();
        }
        return "Nothing";
    }

    public BaseActionSource getActionSource() {
        return this.interfaceRequestSource;
    }

    public void initialize() {
        this.updateCraftingList();
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        this.markDirty();
        this.craftingList = null;
        this.updateCraftingList();
        try {
            this.gridProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
        }
        catch (GridAccessException gridAccessException) {
            // empty catch block
        }
    }

    public void resetCraftingLock() {
        if (this.unlockEvent != null) {
            this.unlockEvent = null;
            this.unlockStacks = null;
            this.saveChanges();
        }
    }

    private void onPushPatternSuccess(TileEntity te, ForgeDirection s, ICraftingPatternDetails pattern) {
        if (this.isSmartBlocking()) {
            TileCableBus cableBus;
            IPart part;
            this.lastInputHash = pattern.hashCode();
            if (te instanceof IInterfaceHost) {
                IInterfaceHost oppositeHost = (IInterfaceHost)te;
                try {
                    if (oppositeHost.getInstalledUpgrades(Upgrades.ADVANCED_BLOCKING) > 0) {
                        oppositeHost.getInterfaceDuality().gridProxy.getGrid().postEvent(new MENetworkCraftingPushedPattern(this.iHost));
                    }
                }
                catch (GridAccessException gridAccessException) {}
            } else if (te instanceof TileCableBus && (part = (cableBus = (TileCableBus)te).getPart(s)) instanceof IInterfaceHost) {
                IInterfaceHost oppositeHost = (IInterfaceHost)((Object)part);
                try {
                    if (oppositeHost.getInstalledUpgrades(Upgrades.ADVANCED_BLOCKING) > 0) {
                        oppositeHost.getInterfaceDuality().gridProxy.getGrid().postEvent(new MENetworkCraftingPushedPattern(this.iHost));
                    }
                }
                catch (GridAccessException gridAccessException) {
                    // empty catch block
                }
            }
        }
        this.resetCraftingLock();
        LockCraftingMode lockMode = (LockCraftingMode)this.cm.getSetting(Settings.LOCK_CRAFTING_MODE);
        switch (lockMode) {
            case LOCK_UNTIL_PULSE: {
                this.unlockEvent = UnlockCraftingEvent.PULSE;
                this.saveChanges();
                break;
            }
            case LOCK_UNTIL_RESULT: {
                this.unlockEvent = UnlockCraftingEvent.RESULT;
                if (this.unlockStacks == null) {
                    this.unlockStacks = new ArrayList<IAEItemStack>();
                }
                for (IAEItemStack output : pattern.getCondensedOutputs()) {
                    this.unlockStacks.add(output.copy());
                }
                this.saveChanges();
            }
        }
    }

    public LockCraftingMode getCraftingLockedReason() {
        Enum<?> lockMode = this.cm.getSetting(Settings.LOCK_CRAFTING_MODE);
        if (lockMode == LockCraftingMode.LOCK_WHILE_LOW && !this.getRedstoneState()) {
            return LockCraftingMode.LOCK_WHILE_LOW;
        }
        if (lockMode == LockCraftingMode.LOCK_WHILE_HIGH && this.getRedstoneState()) {
            return LockCraftingMode.LOCK_WHILE_HIGH;
        }
        if (this.unlockEvent != null) {
            switch (this.unlockEvent) {
                case PULSE: {
                    return LockCraftingMode.LOCK_UNTIL_PULSE;
                }
                case RESULT: {
                    return LockCraftingMode.LOCK_UNTIL_RESULT;
                }
            }
        }
        return LockCraftingMode.NONE;
    }

    public List<IAEItemStack> getUnlockStacks() {
        return this.unlockStacks;
    }

    public void onStackReturnedToNetwork(IAEItemStack returnedStack) {
        if (this.unlockEvent != UnlockCraftingEvent.RESULT) {
            return;
        }
        if (this.unlockStacks == null) {
            AELog.error("interface was waiting for RESULT, but no result was set", new Object[0]);
            this.unlockEvent = null;
            return;
        }
        boolean changed = false;
        Iterator<IAEItemStack> iterator = this.unlockStacks.iterator();
        while (iterator.hasNext()) {
            IAEItemStack unlockStack = iterator.next();
            if (!unlockStack.equals(returnedStack)) continue;
            changed = true;
            unlockStack.decStackSize(returnedStack.getStackSize());
            if (unlockStack.getStackSize() > 0L) break;
            iterator.remove();
            break;
        }
        if (this.unlockStacks.isEmpty()) {
            this.unlockEvent = null;
            this.unlockStacks = null;
        }
        if (changed) {
            this.saveChanges();
        }
    }

    public void updateRedstoneState() {
        this.redstoneState = YesNo.UNDECIDED;
        if (this.unlockEvent == UnlockCraftingEvent.PULSE && this.getRedstoneState()) {
            this.unlockEvent = null;
            this.saveChanges();
        }
    }

    private boolean getRedstoneState() {
        if (this.redstoneState == YesNo.UNDECIDED) {
            TileEntity tile = this.getHost().getTile();
            this.redstoneState = tile.getWorldObj().isBlockIndirectlyGettingPowered(tile.xCoord, tile.yCoord, tile.zCoord) ? YesNo.YES : YesNo.NO;
        }
        return this.redstoneState == YesNo.YES;
    }

    private static class InterfaceRequestSource
    extends MachineSource {
        public InterfaceRequestSource(IActionHost v) {
            super(v);
        }
    }

    private class InterfaceInventory
    extends MEMonitorIInventory {
        public InterfaceInventory(DualityInterface tileInterface) {
            super(new AdaptorIInventory(tileInterface.storage));
            this.setActionSource(new MachineSource(DualityInterface.this.iHost));
        }

        @Override
        public IAEItemStack injectItems(IAEItemStack input, Actionable type, BaseActionSource src) {
            if (src instanceof InterfaceRequestSource) {
                return input;
            }
            return super.injectItems(input, type, src);
        }

        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable type, BaseActionSource src) {
            if (src instanceof InterfaceRequestSource) {
                return null;
            }
            return super.extractItems(request, type, src);
        }
    }
}

