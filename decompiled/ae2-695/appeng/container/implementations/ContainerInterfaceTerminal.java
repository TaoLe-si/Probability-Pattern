/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.container.implementations;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IInterfaceViewable;
import appeng.container.AEBaseContainer;
import appeng.core.features.registries.InterfaceTerminalRegistry;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.helpers.InventoryAction;
import appeng.items.misc.ItemEncodedPattern;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorPlayerHand;
import appeng.util.inv.ItemSlot;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

public final class ContainerInterfaceTerminal
extends AEBaseContainer {
    private int nextId = 0;
    private final Map<IInterfaceViewable, InvTracker> tracked = new HashMap<IInterfaceViewable, InvTracker>();
    private final Map<Long, InvTracker> trackedById = new HashMap<Long, InvTracker>();
    private PacketInterfaceTerminalUpdate dirty;
    private boolean isDirty;
    private IGrid grid;
    private IActionHost anchor;
    private boolean wasOff;

    public ContainerInterfaceTerminal(InventoryPlayer ip, IActionHost anchor) {
        super(ip, anchor);
        assert (anchor != null);
        this.anchor = anchor;
        if (Platform.isServer()) {
            this.grid = anchor.getActionableNode().getGrid();
            this.dirty = this.updateList();
            if (this.dirty != null) {
                this.isDirty = true;
            } else {
                this.dirty = new PacketInterfaceTerminalUpdate();
            }
        }
        this.bindPlayerInventory(ip, 14, 3);
    }

    @Override
    public void detectAndSendChanges() {
        PartInterfaceTerminal terminal;
        if (Platform.isClient()) {
            return;
        }
        super.detectAndSendChanges();
        if (this.grid == null) {
            return;
        }
        IGridNode agn = this.anchor.getActionableNode();
        if (!agn.isActive()) {
            if (!this.wasOff) {
                PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
                update.setDisconnect();
                update.encode();
                this.wasOff = true;
                NetworkHandler.instance.sendTo(update, (EntityPlayerMP)this.getPlayerInv().player);
            }
            return;
        }
        this.wasOff = false;
        IActionHost iActionHost = this.anchor;
        if (iActionHost instanceof PartInterfaceTerminal && (terminal = (PartInterfaceTerminal)iActionHost).needsUpdate()) {
            PacketInterfaceTerminalUpdate update = this.updateList();
            if (update != null) {
                update.encode();
                NetworkHandler.instance.sendTo(update, (EntityPlayerMP)this.getPlayerInv().player);
            }
        } else if (this.isDirty) {
            this.dirty.encode();
            NetworkHandler.instance.sendTo(this.dirty, (EntityPlayerMP)this.getPlayerInv().player);
            this.dirty = new PacketInterfaceTerminalUpdate();
            this.isDirty = false;
        }
    }

    @Override
    public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
        InvTracker inv = this.trackedById.get(id);
        if (inv != null) {
            ItemStack handStack = player.inventory.getItemStack();
            if (handStack != null && !(handStack.getItem() instanceof ItemEncodedPattern)) {
                return;
            }
            ItemStack slotStack = inv.patterns.getStackInSlot(slot);
            AdaptorPlayerHand playerHand = new AdaptorPlayerHand((EntityPlayer)player);
            switch (action) {
                case PICKUP_OR_SET_DOWN: {
                    if (handStack != null) {
                        for (int s = 0; s < inv.patterns.getSizeInventory(); ++s) {
                            if (!Platform.isSameItemPrecise(inv.patterns.getStackInSlot(s), handStack)) continue;
                            return;
                        }
                    }
                    if (slotStack == null) {
                        if (handStack == null) {
                            return;
                        }
                        inv.patterns.setInventorySlotContents(slot, ((InventoryAdaptor)playerHand).removeItems(1, null, null));
                    } else {
                        if (handStack != null && handStack.stackSize > 1) {
                            return;
                        }
                        inv.patterns.setInventorySlotContents(slot, ((InventoryAdaptor)playerHand).removeItems(1, null, null));
                        ((InventoryAdaptor)playerHand).addItems(slotStack.copy());
                    }
                    this.syncIfaceSlot(inv, id, slot, inv.patterns.getStackInSlot(slot));
                    break;
                }
                case SHIFT_CLICK: {
                    InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player.inventory, ForgeDirection.UNKNOWN);
                    ItemStack leftOver = this.mergeToPlayerInventory(playerInv, slotStack);
                    if (leftOver != null) break;
                    inv.patterns.setInventorySlotContents(slot, null);
                    this.syncIfaceSlot(inv, id, slot, null);
                    break;
                }
                case MOVE_REGION: {
                    InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                    ArrayList<Integer> valid = new ArrayList<Integer>();
                    for (int i = 0; i < inv.patterns.getSizeInventory(); ++i) {
                        ItemStack toExtract = inv.patterns.getStackInSlot(i);
                        if (toExtract == null) continue;
                        ItemStack leftOver = this.mergeToPlayerInventory(playerInv, toExtract);
                        if (leftOver != null) break;
                        inv.patterns.setInventorySlotContents(i, null);
                        valid.add(i);
                    }
                    if (valid.size() <= 0) break;
                    int[] validIndices = Ints.toArray(valid);
                    NBTTagList tag = new NBTTagList();
                    for (int i = 0; i < valid.size(); ++i) {
                        tag.appendTag((NBTBase)new NBTTagCompound());
                    }
                    this.dirty.addOverwriteEntry(id).setItems(validIndices, tag);
                    this.isDirty = true;
                    break;
                }
                case CREATIVE_DUPLICATE: {
                    if (!player.capabilities.isCreativeMode) break;
                    ((InventoryAdaptor)playerHand).addItems(handStack);
                    break;
                }
                default: {
                    return;
                }
            }
            this.updateHeld(player);
        }
    }

    private void syncIfaceSlot(InvTracker inv, long id, int slot, ItemStack stack) {
        int[] validIndices = new int[]{slot};
        NBTTagList list = new NBTTagList();
        NBTTagCompound item = new NBTTagCompound();
        if (stack != null) {
            stack.writeToNBT(item);
        }
        list.appendTag((NBTBase)item);
        inv.updateNBT();
        this.dirty.addOverwriteEntry(id).setItems(validIndices, list);
        this.isDirty = true;
    }

    private ItemStack mergeToPlayerInventory(InventoryAdaptor playerInv, ItemStack stack) {
        if (stack == null) {
            return null;
        }
        for (ItemSlot slot : playerInv) {
            if (!Platform.isSameItemPrecise(slot.getItemStack(), stack) || slot.getItemStack().stackSize >= slot.getItemStack().getMaxStackSize()) continue;
            ++slot.getItemStack().stackSize;
            return null;
        }
        return playerInv.addItems(stack);
    }

    private PacketInterfaceTerminalUpdate updateList() {
        PacketInterfaceTerminalUpdate update = null;
        Set<Class<? extends IInterfaceViewable>> supported = InterfaceTerminalRegistry.instance().getSupportedClasses();
        HashSet<IInterfaceViewable> visited = new HashSet<IInterfaceViewable>();
        for (Class<? extends IInterfaceViewable> c : supported) {
            for (IGridNode node : this.grid.getMachines(c)) {
                PartP2PTunnel p2pTunnel;
                IInterfaceViewable machine = (IInterfaceViewable)node.getMachine();
                if (this.tracked.containsKey(machine)) {
                    InvTracker known = this.tracked.get(machine);
                    String name = machine.getName();
                    if (!Objects.equals(known.name, name)) {
                        if (update == null) {
                            update = new PacketInterfaceTerminalUpdate();
                        }
                        update.addRenamedEntry(known.id, name);
                        known.name = name;
                    }
                    boolean isActive = node.isActive();
                    if (!known.online && isActive) {
                        known.online = true;
                        if (update == null) {
                            update = new PacketInterfaceTerminalUpdate();
                        }
                        known.updateNBT();
                        update.addOverwriteEntry(known.id).setOnline(true).setItems(new int[0], known.invNbt);
                    } else if (known.online && !isActive) {
                        known.online = false;
                        if (update == null) {
                            update = new PacketInterfaceTerminalUpdate();
                        }
                        update.addOverwriteEntry(known.id).setOnline(false);
                    }
                    visited.add(machine);
                    continue;
                }
                if (!machine.shouldDisplay()) continue;
                if (update == null) {
                    update = new PacketInterfaceTerminalUpdate();
                }
                InvTracker entry = new InvTracker(this.nextId++, machine, node.isActive());
                update.addNewEntry(entry.id, entry.name, entry.online).setLoc(entry.x, entry.y, entry.z, entry.dim, entry.side.ordinal()).setItems(entry.rows, entry.rowSize, entry.invNbt).setReps(machine.getSelfRep(), machine.getDisplayRep()).setP2POutput(machine instanceof PartP2PTunnel && (p2pTunnel = (PartP2PTunnel)((Object)machine)).isOutput());
                this.tracked.put(machine, entry);
                this.trackedById.put(entry.id, entry);
                visited.add(machine);
            }
        }
        Iterator<Map.Entry<IInterfaceViewable, InvTracker>> it = this.tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IInterfaceViewable, InvTracker> entry = it.next();
            if (visited.contains(entry.getKey())) continue;
            if (update == null) {
                update = new PacketInterfaceTerminalUpdate();
            }
            this.trackedById.remove(entry.getValue().id);
            it.remove();
            update.addRemovalEntry(entry.getValue().id);
        }
        return update;
    }

    private boolean isDifferent(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return false;
        }
        if (a == null || b == null) {
            return true;
        }
        return !ItemStack.areItemStacksEqual((ItemStack)a, (ItemStack)b);
    }

    private static class InvTracker {
        private final long id;
        private String name;
        private final IInventory patterns;
        private final int rowSize;
        private final int rows;
        private final int x;
        private final int y;
        private final int z;
        private final int dim;
        private final ForgeDirection side;
        private boolean online;
        private NBTTagList invNbt;

        InvTracker(long id, IInterfaceViewable machine, boolean online) {
            ForgeDirection forgeDirection;
            DimensionalCoord location = machine.getLocation();
            this.id = id;
            this.name = machine.getName();
            this.patterns = machine.getPatterns();
            this.rowSize = machine.rowSize();
            this.rows = machine.rows();
            this.x = location.x;
            this.y = location.y;
            this.z = location.z;
            this.dim = location.getDimension();
            if (machine instanceof AEBasePart) {
                AEBasePart hasSide = (AEBasePart)((Object)machine);
                forgeDirection = hasSide.getSide();
            } else {
                forgeDirection = ForgeDirection.UNKNOWN;
            }
            this.side = forgeDirection;
            this.online = online;
            this.invNbt = new NBTTagList();
            this.updateNBT();
        }

        private void updateNBT(int row, int idx) {
            ItemStack stack = this.patterns.getStackInSlot(row * this.rowSize + idx);
            if (stack != null) {
                NBTTagCompound itemNbt = this.invNbt.getCompoundTagAt(idx);
                stack.writeToNBT(itemNbt);
            } else {
                this.invNbt.func_150304_a(idx, (NBTBase)new NBTTagCompound());
            }
        }

        private void updateNBT() {
            this.invNbt = new NBTTagList();
            for (int i = 0; i < this.rows; ++i) {
                for (int j = 0; j < this.rowSize; ++j) {
                    int offset = this.rowSize * i;
                    ItemStack stack = this.patterns.getStackInSlot(offset + j);
                    if (stack != null) {
                        this.invNbt.appendTag((NBTBase)stack.writeToNBT(new NBTTagCompound()));
                        continue;
                    }
                    this.invNbt.appendTag((NBTBase)new NBTTagCompound());
                }
            }
        }
    }
}

