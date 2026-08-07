/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraftforge.common.util.ForgeDirection
 *  org.apache.commons.lang3.ArrayUtils
 */
package appeng.container;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.parts.IPart;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.ItemSearchDTO;
import appeng.client.me.InternalSlotME;
import appeng.client.me.SlotME;
import appeng.container.ContainerOpenContext;
import appeng.container.guisync.GuiSync;
import appeng.container.guisync.SyncData;
import appeng.container.implementations.ContainerCellWorkbench;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.implementations.ContainerWirelessTerm;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotCraftingTerm;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotInaccessible;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketHighlightBlockStorage;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketPartialItem;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.IPinsHandler;
import appeng.helpers.InventoryAction;
import appeng.items.materials.ItemMultiMaterial;
import appeng.me.Grid;
import appeng.me.MachineSet;
import appeng.me.NetworkList;
import appeng.me.cache.NetworkMonitor;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.AEBasePart;
import appeng.parts.automation.UpgradeInventory;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.AEBaseInvTile;
import appeng.tile.AEBaseTile;
import appeng.tile.grid.AENetworkPowerTile;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;
import appeng.util.InventoryAdaptor;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.inv.AdaptorPlayerHand;
import appeng.util.item.AEItemStack;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;

public abstract class AEBaseContainer
extends Container {
    private final InventoryPlayer invPlayer;
    private final BaseActionSource mySrc;
    private final HashSet<Integer> locked = new HashSet();
    private final TileEntity tileEntity;
    private final IPart part;
    private final IGuiItemObject obj;
    private final List<PacketPartialItem> dataChunks = new LinkedList<PacketPartialItem>();
    private final HashMap<Integer, SyncData> syncData = new HashMap();
    private boolean isContainerValid = true;
    private String customName;
    private ContainerOpenContext openContext;
    private IMEInventoryHandler<IAEItemStack> cellInv;
    private IEnergySource powerSrc;
    private boolean sentCustomName;
    private int ticksSinceCheck = 900;
    private IAEItemStack clientRequestedTargetItem = null;

    public AEBaseContainer(InventoryPlayer ip, TileEntity myTile, IPart myPart) {
        this(ip, myTile, myPart, null);
    }

    public AEBaseContainer(InventoryPlayer ip, TileEntity myTile, IPart myPart, IGuiItemObject gio) {
        this.invPlayer = ip;
        this.tileEntity = myTile;
        this.part = myPart;
        this.obj = gio;
        this.mySrc = new PlayerSource(ip.player, this.getActionHost());
        this.prepareSync();
    }

    protected IActionHost getActionHost() {
        if (this.obj instanceof IActionHost) {
            return (IActionHost)((Object)this.obj);
        }
        if (this.tileEntity instanceof IActionHost) {
            return (IActionHost)this.tileEntity;
        }
        if (this.part instanceof IActionHost) {
            return (IActionHost)((Object)this.part);
        }
        return null;
    }

    private void prepareSync() {
        this.walkSyncFields(0, ((Object)((Object)this)).getClass().getFields(), new Field[0]);
    }

    private void walkSyncFields(int offset, Field[] fields, Field[] currentIndirections) {
        for (Field f : fields) {
            Annotation annotation;
            if (f.isAnnotationPresent(GuiSync.Recurse.class)) {
                annotation = f.getAnnotation(GuiSync.Recurse.class);
                this.walkSyncFields(offset + annotation.value(), f.getType().getFields(), (Field[])ArrayUtils.add((Object[])currentIndirections, (Object)f));
            }
            if (!f.isAnnotationPresent(GuiSync.class)) continue;
            annotation = f.getAnnotation(GuiSync.class);
            int channel = offset + annotation.value();
            if (this.syncData.containsKey(channel)) {
                AELog.warn("Channel already in use: " + channel + " for " + f.getName(), new Object[0]);
                continue;
            }
            this.syncData.put(channel, new SyncData(this, currentIndirections, f, channel));
        }
    }

    public AEBaseContainer(InventoryPlayer ip, Object anchor) {
        this.invPlayer = ip;
        this.tileEntity = anchor instanceof TileEntity ? (TileEntity)anchor : null;
        this.part = anchor instanceof IPart ? (IPart)anchor : null;
        IGuiItemObject iGuiItemObject = this.obj = anchor instanceof IGuiItemObject ? (IGuiItemObject)anchor : null;
        if (this.tileEntity == null && this.part == null && this.obj == null) {
            throw new IllegalArgumentException("Must have a valid anchor, instead " + anchor + " in " + ip);
        }
        this.mySrc = new PlayerSource(ip.player, this.getActionHost());
        this.prepareSync();
    }

    public void postPartial(PacketPartialItem packetPartialItem) {
        this.dataChunks.add(packetPartialItem);
        if (packetPartialItem.getPageCount() == this.dataChunks.size()) {
            this.parsePartials();
        }
    }

    private void parsePartials() {
        int total = 0;
        for (PacketPartialItem ppi : this.dataChunks) {
            total += ppi.getSize();
        }
        byte[] buffer = new byte[total];
        int cursor = 0;
        for (PacketPartialItem ppi : this.dataChunks) {
            cursor = ppi.write(buffer, cursor);
        }
        try {
            NBTTagCompound data = CompressedStreamTools.readCompressed((InputStream)new ByteArrayInputStream(buffer));
            if (data != null) {
                this.setTargetStack(AEApi.instance().storage().createItemStack(ItemStack.loadItemStackFromNBT((NBTTagCompound)data)));
            }
        }
        catch (IOException e) {
            AELog.debug(e);
        }
        this.dataChunks.clear();
    }

    public IAEItemStack getTargetStack() {
        return this.clientRequestedTargetItem;
    }

    public void setTargetStack(IAEItemStack stack) {
        if (Platform.isClient()) {
            ItemStack b;
            ItemStack a = stack == null ? null : stack.getItemStack();
            ItemStack itemStack = b = this.clientRequestedTargetItem == null ? null : this.clientRequestedTargetItem.getItemStack();
            if (Platform.isSameItemPrecise(a, b)) {
                return;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            NBTTagCompound item = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(item);
            }
            try {
                CompressedStreamTools.writeCompressed((NBTTagCompound)item, (OutputStream)stream);
                int maxChunkSize = 30000;
                LinkedList<byte[]> miniPackets = new LinkedList<byte[]>();
                byte[] data = stream.toByteArray();
                ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, stream.size());
                while (bis.available() > 0) {
                    int nextBLock = bis.available() > 30000 ? 30000 : bis.available();
                    byte[] nextSegment = new byte[nextBLock];
                    bis.read(nextSegment);
                    miniPackets.add(nextSegment);
                }
                bis.close();
                stream.close();
                int page = 0;
                for (byte[] packet : miniPackets) {
                    PacketPartialItem ppi = new PacketPartialItem(page, miniPackets.size(), packet);
                    ++page;
                    NetworkHandler.instance.sendToServer(ppi);
                }
            }
            catch (IOException e) {
                AELog.debug(e);
                return;
            }
        }
        this.clientRequestedTargetItem = stack == null ? null : stack.copy();
    }

    public BaseActionSource getActionSource() {
        return this.mySrc;
    }

    public void verifyPermissions(SecurityPermissions security, boolean requirePower) {
        if (Platform.isClient()) {
            return;
        }
        ++this.ticksSinceCheck;
        if (this.ticksSinceCheck < 20) {
            return;
        }
        this.ticksSinceCheck = 0;
        this.setValidContainer(this.isValidContainer() && this.hasAccess(security, requirePower));
    }

    protected boolean hasAccess(SecurityPermissions perm, boolean requirePower) {
        IGrid g;
        IGridNode gn;
        IActionHost host = this.getActionHost();
        if (host != null && (gn = host.getActionableNode()) != null && (g = gn.getGrid()) != null) {
            IEnergyGrid eg;
            if (requirePower && !(eg = (IEnergyGrid)g.getCache(IEnergyGrid.class)).isNetworkPowered()) {
                return false;
            }
            ISecurityGrid sg = (ISecurityGrid)g.getCache(ISecurityGrid.class);
            if (sg.hasPermission(this.getInventoryPlayer().player, perm)) {
                return true;
            }
        }
        return false;
    }

    public void lockPlayerInventorySlot(int idx) {
        this.locked.add(idx);
    }

    public Object getTarget() {
        if (this.tileEntity != null) {
            return this.tileEntity;
        }
        if (this.part != null) {
            return this.part;
        }
        if (this.obj != null) {
            return this.obj;
        }
        return null;
    }

    public InventoryPlayer getPlayerInv() {
        return this.getInventoryPlayer();
    }

    public TileEntity getTileEntity() {
        return this.tileEntity;
    }

    public final void updateFullProgressBar(int idx, long value) {
        if (this.syncData.containsKey(idx)) {
            this.syncData.get(idx).update(value);
            return;
        }
        this.updateProgressBar(idx, (int)value);
    }

    public void stringSync(int idx, String value) {
        if (this.syncData.containsKey(idx)) {
            this.syncData.get(idx).update(value);
        }
    }

    protected void bindPlayerInventory(InventoryPlayer inventoryPlayer, int offsetX, int offsetY) {
        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                if (this.locked.contains(j + i * 9 + 9)) {
                    this.addSlotToContainer(new SlotDisabled((IInventory)inventoryPlayer, j + i * 9 + 9, 8 + j * 18 + offsetX, offsetY + i * 18));
                    continue;
                }
                this.addSlotToContainer(new SlotPlayerInv((IInventory)inventoryPlayer, j + i * 9 + 9, 8 + j * 18 + offsetX, offsetY + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            if (this.locked.contains(i)) {
                this.addSlotToContainer(new SlotDisabled((IInventory)inventoryPlayer, i, 8 + i * 18 + offsetX, 58 + offsetY));
                continue;
            }
            this.addSlotToContainer(new SlotPlayerHotBar((IInventory)inventoryPlayer, i, 8 + i * 18 + offsetX, 58 + offsetY));
        }
    }

    protected Slot addSlotToContainer(Slot newSlot) {
        if (newSlot instanceof AppEngSlot) {
            AppEngSlot s = (AppEngSlot)newSlot;
            s.setContainer(this);
            return super.addSlotToContainer(newSlot);
        }
        throw new IllegalArgumentException("Invalid Slot [" + newSlot + "]for AE Container instead of AppEngSlot.");
    }

    public void detectAndSendChanges() {
        this.sendCustomName();
        if (Platform.isServer()) {
            for (Object crafter : this.crafters) {
                ICrafting icrafting = (ICrafting)crafter;
                for (SyncData sd : this.syncData.values()) {
                    sd.tick(icrafting);
                }
            }
        }
        super.detectAndSendChanges();
    }

    public ItemStack transferStackInSlot(EntityPlayer p, int idx) {
        if (Platform.isClient()) {
            return null;
        }
        boolean hasMETiles = false;
        for (Object is : this.inventorySlots) {
            if (!(is instanceof InternalSlotME)) continue;
            hasMETiles = true;
            break;
        }
        if (hasMETiles && Platform.isClient()) {
            return null;
        }
        AppEngSlot clickSlot = (AppEngSlot)((Object)this.inventorySlots.get(idx));
        if (clickSlot instanceof SlotDisabled || clickSlot instanceof SlotInaccessible) {
            return null;
        }
        if (clickSlot != null && clickSlot.getHasStack()) {
            AppEngSlot cs;
            ItemStack tis = clickSlot.getStack();
            if (tis == null) {
                return null;
            }
            ArrayList<AppEngSlot> selectedSlots = new ArrayList<AppEngSlot>();
            if (clickSlot.isPlayerSide()) {
                tis = this.shiftStoreItem(tis);
                for (Object e : this.inventorySlots) {
                    cs = (AppEngSlot)((Object)e);
                    if (cs.isPlayerSide() || cs instanceof SlotFake || cs instanceof SlotCraftingMatrix || !cs.isItemValid(tis)) continue;
                    selectedSlots.add(cs);
                }
            } else {
                for (Object e : this.inventorySlots) {
                    cs = (AppEngSlot)((Object)e);
                    if (!cs.isPlayerSide() || cs instanceof SlotFake || cs instanceof SlotCraftingMatrix || !cs.isItemValid(tis)) continue;
                    selectedSlots.add(cs);
                }
            }
            if (selectedSlots.isEmpty() && clickSlot.isPlayerSide() && tis != null) {
                for (Object e : this.inventorySlots) {
                    cs = (AppEngSlot)((Object)e);
                    ItemStack destination = cs.getStack();
                    if (cs.isPlayerSide() || !(cs instanceof SlotFake)) continue;
                    if (Platform.isSameItemPrecise(destination, tis)) break;
                    if (destination != null) continue;
                    cs.putStack(tis.copy());
                    cs.onSlotChanged();
                    this.updateSlot(cs);
                    break;
                }
            }
            if (tis != null) {
                int placeAble;
                for (Slot slot : selectedSlots) {
                    ItemStack t;
                    if (slot instanceof SlotDisabled || slot instanceof SlotME || !slot.isItemValid(tis) || !slot.getHasStack() || !Platform.isSameItemPrecise(tis, t = slot.getStack())) continue;
                    int maxSize = t.getMaxStackSize();
                    if (maxSize > slot.getSlotStackLimit()) {
                        maxSize = slot.getSlotStackLimit();
                    }
                    if (tis.stackSize < (placeAble = maxSize - t.stackSize)) {
                        placeAble = tis.stackSize;
                    }
                    t.stackSize += placeAble;
                    tis.stackSize -= placeAble;
                    if (tis.stackSize <= 0) {
                        clickSlot.putStack(null);
                        slot.onSlotChanged();
                        this.updateSlot(clickSlot);
                        this.updateSlot(slot);
                        return null;
                    }
                    this.updateSlot(slot);
                }
                for (Slot slot : selectedSlots) {
                    if (slot instanceof SlotDisabled || slot instanceof SlotME || ItemMultiMaterial.instance.getType(tis) != null && ContainerUpgradeable.class.isAssignableFrom(((Object)((Object)this)).getClass()) && !(slot.inventory instanceof UpgradeInventory) && !(clickSlot.inventory instanceof UpgradeInventory) && !(slot.inventory instanceof ContainerCellWorkbench.Upgrades) || !slot.isItemValid(tis)) continue;
                    if (slot.getHasStack()) {
                        ItemStack t = slot.getStack();
                        if (!Platform.isSameItemPrecise(t, tis)) continue;
                        int maxSize = t.getMaxStackSize();
                        if (slot.getSlotStackLimit() < maxSize) {
                            maxSize = slot.getSlotStackLimit();
                        }
                        if (tis.stackSize < (placeAble = maxSize - t.stackSize)) {
                            placeAble = tis.stackSize;
                        }
                        t.stackSize += placeAble;
                        tis.stackSize -= placeAble;
                        if (tis.stackSize <= 0) {
                            clickSlot.putStack(null);
                            slot.onSlotChanged();
                            this.updateSlot(clickSlot);
                            this.updateSlot(slot);
                            return null;
                        }
                        this.updateSlot(slot);
                        continue;
                    }
                    int maxSize = tis.getMaxStackSize();
                    if (maxSize > slot.getSlotStackLimit()) {
                        maxSize = slot.getSlotStackLimit();
                    }
                    ItemStack tmp = tis.copy();
                    if (tmp.stackSize > maxSize) {
                        tmp.stackSize = maxSize;
                    }
                    tis.stackSize -= tmp.stackSize;
                    slot.putStack(tmp);
                    if (tis.stackSize <= 0) {
                        clickSlot.putStack(null);
                        slot.onSlotChanged();
                        this.updateSlot(clickSlot);
                        this.updateSlot(slot);
                        return null;
                    }
                    this.updateSlot(slot);
                }
            }
            clickSlot.putStack(tis != null ? tis.copy() : null);
        }
        this.updateSlot(clickSlot);
        return null;
    }

    public final void updateProgressBar(int idx, int value) {
        if (this.syncData.containsKey(idx)) {
            this.syncData.get(idx).update(value);
        }
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        if (this.isValidContainer()) {
            if (this.tileEntity instanceof IInventory) {
                return ((IInventory)this.tileEntity).isUseableByPlayer(entityplayer);
            }
            return true;
        }
        return false;
    }

    public boolean canDragIntoSlot(Slot s) {
        return ((AppEngSlot)s).isDraggable();
    }

    public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
        if (slot >= 0 && slot < this.inventorySlots.size()) {
            Object is;
            Slot s = this.getSlot(slot);
            if (s instanceof SlotCraftingTerm) {
                switch (action) {
                    case CRAFT_SHIFT: 
                    case CRAFT_ITEM: 
                    case CRAFT_STACK: {
                        ((SlotCraftingTerm)s).doClick(action, (EntityPlayer)player);
                        this.updateHeld(player);
                    }
                }
            }
            if (s instanceof SlotFake) {
                ItemStack hand = player.inventory.getItemStack();
                switch (action) {
                    case PICKUP_OR_SET_DOWN: {
                        if (hand == null) {
                            s.putStack(null);
                            break;
                        }
                        s.putStack(hand.copy());
                        break;
                    }
                    case PLACE_SINGLE: {
                        if (hand == null) break;
                        is = hand.copy();
                        ((ItemStack)is).stackSize = 1;
                        s.putStack((ItemStack)is);
                        break;
                    }
                    case SPLIT_OR_PLACE_SINGLE: {
                        is = s.getStack();
                        if (is != null) {
                            if (hand == null) {
                                if (((ItemStack)is).stackSize > 1) {
                                    --((ItemStack)is).stackSize;
                                }
                            } else if (hand.isItemEqual((ItemStack)is)) {
                                ((ItemStack)is).stackSize = Math.min(is.getMaxStackSize(), ((ItemStack)is).stackSize + 1);
                            } else {
                                is = hand.copy();
                                ((ItemStack)is).stackSize = 1;
                            }
                            s.putStack((ItemStack)is);
                            break;
                        }
                        if (hand == null) break;
                        is = hand.copy();
                        ((ItemStack)is).stackSize = 1;
                        s.putStack((ItemStack)is);
                        break;
                    }
                }
            }
            if (action == InventoryAction.MOVE_REGION) {
                if (s instanceof SlotFake || s instanceof SlotPatternTerm) {
                    return;
                }
                LinkedList<Slot> from = new LinkedList<Slot>();
                for (Object j : this.inventorySlots) {
                    if (!(j instanceof Slot) || j.getClass() != s.getClass()) continue;
                    from.add((Slot)j);
                }
                is = from.iterator();
                while (is.hasNext()) {
                    Slot fr = (Slot)is.next();
                    this.transferStackInSlot((EntityPlayer)player, fr.slotNumber);
                }
            }
            if (action == InventoryAction.SET_PIN && (is = this) instanceof IPinsHandler) {
                IPinsHandler iph = (IPinsHandler)is;
                if (id == -1L) {
                    iph.setPin(null, slot);
                    return;
                }
                ItemStack hand = player.inventory.getItemStack();
                if (hand == null) {
                    return;
                }
                if (iph.getPin(slot) != null && hand.isItemEqual(iph.getPin(slot))) {
                    this.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, this.inventorySlots.size(), id);
                } else {
                    iph.setPin(player.inventory.getItemStack(), slot);
                }
            }
            return;
        }
        IAEItemStack slotItem = this.clientRequestedTargetItem;
        switch (action) {
            case SHIFT_CLICK: {
                if (this.getPowerSource() == null || this.getCellInventory() == null) {
                    return;
                }
                if (slotItem == null) break;
                IAEItemStack ais = slotItem.copy();
                ItemStack myItem = ais.getItemStack();
                ais.setStackSize(myItem.getMaxStackSize());
                InventoryAdaptor adp = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                myItem.stackSize = (int)ais.getStackSize();
                myItem = adp.simulateAdd(myItem);
                if (myItem != null) {
                    ais.setStackSize(ais.getStackSize() - (long)myItem.stackSize);
                }
                if ((ais = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource())) == null) break;
                adp.addItems(ais.getItemStack());
                break;
            }
            case ROLL_DOWN: {
                if (this.getPowerSource() == null || this.getCellInventory() == null) {
                    return;
                }
                boolean releaseQty = true;
                ItemStack isg = player.inventory.getItemStack();
                if (isg == null) break;
                IAEItemStack ais = AEApi.instance().storage().createItemStack(isg);
                ais.setStackSize(1L);
                IAEItemStack extracted = ais.copy();
                ais = Platform.poweredInsert(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource());
                if (ais != null) break;
                AdaptorPlayerHand ia = new AdaptorPlayerHand((EntityPlayer)player);
                ItemStack fail = ((InventoryAdaptor)ia).removeItems(1, extracted.getItemStack(), null);
                if (fail == null) {
                    this.getCellInventory().extractItems(extracted, Actionable.MODULATE, this.getActionSource());
                }
                this.updateHeld(player);
                break;
            }
            case ROLL_UP: 
            case PICKUP_SINGLE: {
                if (this.getPowerSource() == null || this.getCellInventory() == null) {
                    return;
                }
                if (slotItem == null) break;
                int liftQty = 1;
                ItemStack item = player.inventory.getItemStack();
                if (item != null) {
                    if (item.stackSize >= item.getMaxStackSize()) {
                        liftQty = 0;
                    }
                    if (!Platform.isSameItemPrecise(slotItem.getItemStack(), item)) {
                        liftQty = 0;
                    }
                }
                if (liftQty <= 0) break;
                IAEItemStack ais = slotItem.copy();
                ais.setStackSize(1L);
                ais = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource());
                if (ais == null) break;
                AdaptorPlayerHand ia = new AdaptorPlayerHand((EntityPlayer)player);
                ItemStack fail = ((InventoryAdaptor)ia).addItems(ais.getItemStack());
                if (fail != null) {
                    this.getCellInventory().injectItems(ais, Actionable.MODULATE, this.getActionSource());
                }
                this.updateHeld(player);
                break;
            }
            case PICKUP_OR_SET_DOWN: {
                if (this.getPowerSource() == null || this.getCellInventory() == null) {
                    return;
                }
                if (player.inventory.getItemStack() == null) {
                    if (slotItem == null) break;
                    IAEItemStack ais = slotItem.copy();
                    ais.setStackSize(ais.getItemStack().getMaxStackSize());
                    ais = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource());
                    if (ais != null) {
                        player.inventory.setItemStack(ais.getItemStack());
                    } else {
                        player.inventory.setItemStack(null);
                    }
                    this.updateHeld(player);
                    break;
                }
                IAEItemStack ais = AEApi.instance().storage().createItemStack(player.inventory.getItemStack());
                ais = Platform.poweredInsert(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource());
                if (ais != null) {
                    player.inventory.setItemStack(ais.getItemStack());
                } else {
                    player.inventory.setItemStack(null);
                }
                this.updateHeld(player);
                break;
            }
            case SPLIT_OR_PLACE_SINGLE: {
                if (this.getPowerSource() == null || this.getCellInventory() == null) {
                    return;
                }
                if (player.inventory.getItemStack() == null) {
                    if (slotItem == null) break;
                    IAEItemStack ais = slotItem.copy();
                    long maxSize = ais.getItemStack().getMaxStackSize();
                    ais.setStackSize(maxSize);
                    ais = this.getCellInventory().extractItems(ais, Actionable.SIMULATE, this.getActionSource());
                    if (ais != null) {
                        long stackSize = Math.min(maxSize, ais.getStackSize());
                        ais.setStackSize(stackSize + 1L >> 1);
                        ais = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource());
                    }
                    if (ais != null) {
                        player.inventory.setItemStack(ais.getItemStack());
                    } else {
                        player.inventory.setItemStack(null);
                    }
                    this.updateHeld(player);
                    break;
                }
                IAEItemStack ais = AEApi.instance().storage().createItemStack(player.inventory.getItemStack());
                ais.setStackSize(1L);
                ais = Platform.poweredInsert(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource());
                if (ais != null) break;
                ItemStack is = player.inventory.getItemStack();
                --is.stackSize;
                if (is.stackSize <= 0) {
                    player.inventory.setItemStack(null);
                }
                this.updateHeld(player);
                break;
            }
            case CREATIVE_DUPLICATE: {
                if (!player.capabilities.isCreativeMode || slotItem == null) break;
                ItemStack is = slotItem.getItemStack();
                is.stackSize = is.getMaxStackSize();
                player.inventory.setItemStack(is);
                this.updateHeld(player);
                break;
            }
            case MOVE_REGION: {
                if (this.getPowerSource() == null || this.getCellInventory() == null) {
                    return;
                }
                if (slotItem == null) break;
                int playerInv = 36;
                for (int slotNum = 0; slotNum < 36; ++slotNum) {
                    IAEItemStack ais = slotItem.copy();
                    ItemStack myItem = ais.getItemStack();
                    ais.setStackSize(myItem.getMaxStackSize());
                    InventoryAdaptor adp = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                    myItem.stackSize = (int)ais.getStackSize();
                    myItem = adp.simulateAdd(myItem);
                    if (myItem != null) {
                        ais.setStackSize(ais.getStackSize() - (long)myItem.stackSize);
                    }
                    if ((ais = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), ais, this.getActionSource())) == null) {
                        return;
                    }
                    adp.addItems(ais.getItemStack());
                }
                break;
            }
            case FIND_ITEMS: {
                ContainerWirelessTerm wirelessTerm;
                Class[] checkedMachineClasses = new Class[]{TileDrive.class, TileChest.class, PartStorageBus.class};
                if (slotItem == null) {
                    return;
                }
                IGrid g = null;
                Object adp = this;
                if (adp instanceof ContainerWirelessTerm && (adp = (wirelessTerm = (ContainerWirelessTerm)adp).getMonitor()) instanceof NetworkMonitor) {
                    NetworkMonitor networkMonitor = (NetworkMonitor)adp;
                    g = networkMonitor.getGrid();
                } else {
                    IActionHost host = this.getActionHost();
                    if (host == null) {
                        return;
                    }
                    IGridNode gn = host.getActionableNode();
                    if (gn != null) {
                        g = gn.getGrid();
                    }
                }
                if (g == null) {
                    return;
                }
                ArrayList<ItemSearchDTO> coords = new ArrayList<ItemSearchDTO>();
                ArrayList<IGridNode> machineList = new ArrayList<IGridNode>();
                Class<PartStorageBus> classType = null;
                if (slotItem.getChannel() == StorageChannel.ITEMS) {
                    classType = PartStorageBus.class;
                } else if (slotItem.getChannel() == StorageChannel.FLUIDS) {
                    // empty if block
                }
                NetworkList grids = g.getAllRecursiveGridConnections(classType);
                for (Grid subnet : grids) {
                    for (Class type : checkedMachineClasses) {
                        MachineSet subMachines = (MachineSet)subnet.getMachines(type);
                        if (subMachines.isEmpty()) continue;
                        machineList.addAll(subMachines);
                    }
                }
                int machineCount = 0;
                for (IGridNode gridNode : machineList) {
                    IAEItemStack result;
                    ICellContainer innerMachine;
                    if (machineCount > AEConfig.instance.maxMachineChecks) break;
                    ++machineCount;
                    IGridHost machine = gridNode.getMachine();
                    if (machine instanceof TileDrive) {
                        innerMachine = (TileDrive)machine;
                        for (int i = 0; i < ((AEBaseInvTile)((Object)innerMachine)).getSizeInventory(); ++i) {
                            IAEItemStack result2;
                            MEInventoryHandler<IAEItemStack> cell = ((TileDrive)innerMachine).getCellInvBySlot(i);
                            if (cell == null || cell.getChannel() != slotItem.getChannel() || (result2 = cell.getAvailableItem(slotItem, IterationCounter.fetchNewId())) == null) continue;
                            String blockName = ((AEBaseTile)((Object)innerMachine)).getCustomName();
                            coords.add(new ItemSearchDTO(((TileDrive)innerMachine).getLocation(), result2, blockName, i, ((AEBaseTile)((Object)innerMachine)).getForward(), ((AEBaseTile)((Object)innerMachine)).getUp()));
                        }
                    }
                    if (machine instanceof PartStorageBus) {
                        MEInventoryHandler<IAEItemStack> handler;
                        innerMachine = (PartStorageBus)machine;
                        if (((PartStorageBus)innerMachine).getConnectedGrid() != null || (handler = ((PartStorageBus)innerMachine).getInternalHandler()) == null || (result = handler.getAvailableItem(slotItem, IterationCounter.fetchNewId())) == null) continue;
                        coords.add(new ItemSearchDTO(((AEBasePart)((Object)innerMachine)).getLocation(), result, ((AEBasePart)((Object)innerMachine)).getCustomName()));
                    }
                    if (!(machine instanceof TileChest)) continue;
                    innerMachine = (TileChest)machine;
                    try {
                        IMEInventoryHandler handler = ((TileChest)innerMachine).getHandler(slotItem.getChannel());
                        result = handler.getAvailableItem(slotItem, IterationCounter.fetchNewId());
                        if (result == null) continue;
                        coords.add(new ItemSearchDTO(((AENetworkPowerTile)((Object)innerMachine)).getLocation(), result, ((AEBaseTile)((Object)innerMachine)).getCustomName()));
                    }
                    catch (Exception exception) {}
                }
                this.highlightBlocks(player, coords);
                break;
            }
        }
    }

    protected void updateHeld(EntityPlayerMP p) {
        if (Platform.isServer()) {
            try {
                NetworkHandler.instance.sendTo(new PacketInventoryAction(InventoryAction.UPDATE_HAND, 0, AEItemStack.create(p.inventory.getItemStack())), p);
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
    }

    protected void highlightBlocks(EntityPlayerMP p, List<ItemSearchDTO> coords) {
        if (Platform.isServer()) {
            try {
                NetworkHandler.instance.sendTo(new PacketHighlightBlockStorage(coords), p);
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
    }

    private ItemStack shiftStoreItem(ItemStack input) {
        if (this.getPowerSource() == null || this.getCellInventory() == null) {
            return input;
        }
        IAEItemStack ais = Platform.poweredInsert(this.getPowerSource(), this.getCellInventory(), AEApi.instance().storage().createItemStack(input), this.getActionSource());
        if (ais == null) {
            return null;
        }
        return ais.getItemStack();
    }

    private void updateSlot(Slot clickSlot) {
        this.detectAndSendChanges();
    }

    private void sendCustomName() {
        if (!this.sentCustomName) {
            this.sentCustomName = true;
            if (Platform.isServer()) {
                ICustomNameObject name = null;
                if (this.part instanceof ICustomNameObject) {
                    name = (ICustomNameObject)((Object)this.part);
                }
                if (this.tileEntity instanceof ICustomNameObject) {
                    name = (ICustomNameObject)this.tileEntity;
                }
                if (this.obj instanceof ICustomNameObject) {
                    name = (ICustomNameObject)((Object)this.obj);
                }
                if (this instanceof ICustomNameObject) {
                    name = (ICustomNameObject)((Object)this);
                }
                if (name != null) {
                    if (name.hasCustomName()) {
                        this.setCustomName(name.getCustomName());
                    }
                    if (this.getCustomName() != null) {
                        try {
                            NetworkHandler.instance.sendTo(new PacketValueConfig("CustomName", this.getCustomName()), (EntityPlayerMP)this.getInventoryPlayer().player);
                        }
                        catch (IOException e) {
                            AELog.debug(e);
                        }
                    }
                }
            }
        }
    }

    public void swapSlotContents(int slotA, int slotB) {
        ItemStack testB;
        int inventorySize = this.inventorySlots.size();
        if (slotA < 0 || slotA >= inventorySize || slotB < 0 || slotB >= inventorySize) {
            return;
        }
        Slot a = this.getSlot(slotA);
        Slot b = this.getSlot(slotB);
        if (a == null || b == null) {
            return;
        }
        ItemStack isA = a.getStack();
        ItemStack isB = b.getStack();
        if (isA == null && isB == null) {
            return;
        }
        if (isA != null && !a.canTakeStack(this.getInventoryPlayer().player)) {
            return;
        }
        if (isB != null && !b.canTakeStack(this.getInventoryPlayer().player)) {
            return;
        }
        if (isB != null && !a.isItemValid(isB)) {
            return;
        }
        if (isA != null && !b.isItemValid(isA)) {
            return;
        }
        ItemStack testA = isB == null ? null : isB.copy();
        ItemStack itemStack = testB = isA == null ? null : isA.copy();
        if (testA != null && testA.stackSize > a.getSlotStackLimit()) {
            if (testB != null) {
                return;
            }
            int totalA = testA.stackSize;
            testA.stackSize = a.getSlotStackLimit();
            testB = testA.copy();
            testB.stackSize = totalA - testA.stackSize;
        }
        if (testB != null && testB.stackSize > b.getSlotStackLimit()) {
            if (testA != null) {
                return;
            }
            int totalB = testB.stackSize;
            testB.stackSize = b.getSlotStackLimit();
            testA = testB.copy();
            testA.stackSize = totalB - testA.stackSize;
        }
        a.putStack(testA);
        b.putStack(testB);
    }

    public void onUpdate(String field, Object oldValue, Object newValue) {
    }

    public void onSlotChange(Slot s) {
    }

    public boolean isValidForSlot(Slot s, ItemStack i) {
        return true;
    }

    public IMEInventoryHandler<IAEItemStack> getCellInventory() {
        return this.cellInv;
    }

    public void setCellInventory(IMEInventoryHandler<IAEItemStack> cellInv) {
        this.cellInv = cellInv;
    }

    public String getCustomName() {
        return this.customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public InventoryPlayer getInventoryPlayer() {
        return this.invPlayer;
    }

    public boolean isValidContainer() {
        return this.isContainerValid;
    }

    public void setValidContainer(boolean isContainerValid) {
        this.isContainerValid = isContainerValid;
    }

    public ContainerOpenContext getOpenContext() {
        return this.openContext;
    }

    public void setOpenContext(ContainerOpenContext openContext) {
        this.openContext = openContext;
    }

    public IEnergySource getPowerSource() {
        return this.powerSrc;
    }

    public void setPowerSource(IEnergySource powerSrc) {
        this.powerSrc = powerSrc;
    }
}

