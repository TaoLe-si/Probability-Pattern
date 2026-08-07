/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.CraftingAllow;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CraftingItemList;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCompressedNBT;
import appeng.core.sync.packets.PacketCraftingRemainingOperations;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.ICustomNameObject;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.crafting.TileCraftingTile;
import appeng.util.Platform;
import java.io.IOException;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.ForgeDirection;

public class ContainerCraftingCPU
extends AEBaseContainer
implements IMEMonitorHandlerReceiver<IAEItemStack>,
ICustomNameObject {
    private final IItemList<IAEItemStack> list = AEApi.instance().storage().createItemList();
    private IGrid network;
    private CraftingCPUCluster monitor = null;
    private String cpuName = null;
    @GuiSync(value=0)
    public long elapsed = -1L;
    @GuiSync(value=1)
    public int allow = 0;

    public ContainerCraftingCPU(InventoryPlayer ip, Object te) {
        super(ip, te);
        IGridHost host = (IGridHost)(te instanceof IGridHost ? te : null);
        if (host != null) {
            this.findNode(host, ForgeDirection.UNKNOWN);
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                this.findNode(host, d);
            }
        }
        if (te instanceof TileCraftingTile) {
            this.setCPU((ICraftingCPU)((Object)((IAEMultiBlock)te).getCluster()));
        }
        if (this.getNetwork() == null && Platform.isServer()) {
            this.setValidContainer(false);
        }
    }

    private void findNode(IGridHost host, ForgeDirection d) {
        IGridNode node;
        if (this.getNetwork() == null && (node = host.getGridNode(d)) != null) {
            this.setNetwork(node.getGrid());
        }
    }

    protected void setCPU(ICraftingCPU c) {
        if (c == this.getMonitor()) {
            return;
        }
        if (this.getMonitor() != null) {
            this.getMonitor().removeListener(this);
        }
        for (Object g : this.crafters) {
            if (!(g instanceof EntityPlayer)) continue;
            try {
                NetworkHandler.instance.sendTo(new PacketValueConfig("CraftingStatus", "Clear"), (EntityPlayerMP)g);
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
        if (c instanceof CraftingCPUCluster) {
            this.cpuName = c.getName();
            this.setMonitor((CraftingCPUCluster)c);
            this.list.resetStatus();
            this.getMonitor().getListOfItem(this.list, CraftingItemList.ALL);
            this.getMonitor().addListener(this, (Object)null);
            this.setElapsedTime(0L);
            this.allow = this.getMonitor().getCraftingAllowMode().ordinal();
        } else {
            this.setMonitor(null);
            this.cpuName = "";
            this.setElapsedTime(-1L);
        }
    }

    public void cancelCrafting() {
        if (this.getMonitor() != null) {
            this.getMonitor().cancel();
        }
        this.setElapsedTime(-1L);
    }

    public void removeCraftingFromCrafters(ICrafting c) {
        super.removeCraftingFromCrafters(c);
        if (this.crafters.isEmpty() && this.getMonitor() != null) {
            this.getMonitor().removeListener(this);
        }
    }

    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (this.getMonitor() != null) {
            this.getMonitor().removeListener(this);
        }
    }

    public void sendUpdateFollowPacket(List<String> playersFollowingCurrentCraft) {
        NBTTagCompound nbttc = new NBTTagCompound();
        NBTTagList tagList = new NBTTagList();
        if (playersFollowingCurrentCraft != null) {
            for (String name : playersFollowingCurrentCraft) {
                tagList.appendTag((NBTBase)new NBTTagString(name));
            }
        }
        nbttc.setTag("playNameList", (NBTBase)tagList);
        for (String g : this.crafters) {
            if (!(g instanceof EntityPlayerMP)) continue;
            EntityPlayerMP epmp = (EntityPlayerMP)g;
            try {
                NetworkHandler.instance.sendTo(new PacketCompressedNBT(nbttc), epmp);
            }
            catch (IOException iOException) {}
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer() && this.getMonitor() != null && !this.list.isEmpty()) {
            try {
                this.setElapsedTime(this.getMonitor().getElapsedTime());
                NBTTagCompound nbttc = new NBTTagCompound();
                NBTTagList tagList = new NBTTagList();
                List<String> playersFollowingCurrentCraft = this.getPlayersFollowingCurrentCraft();
                if (playersFollowingCurrentCraft != null) {
                    for (String name : playersFollowingCurrentCraft) {
                        tagList.appendTag((NBTBase)new NBTTagString(name));
                    }
                }
                nbttc.setTag("playNameList", (NBTBase)tagList);
                PacketMEInventoryUpdate a = new PacketMEInventoryUpdate(0);
                PacketMEInventoryUpdate b = new PacketMEInventoryUpdate(1);
                PacketMEInventoryUpdate c = new PacketMEInventoryUpdate(2);
                PacketCompressedNBT d = new PacketCompressedNBT(nbttc);
                for (IAEItemStack out : this.list) {
                    a.appendItem(this.getMonitor().getItemStack(out, CraftingItemList.STORAGE));
                    b.appendItem(this.getMonitor().getItemStack(out, CraftingItemList.ACTIVE));
                    c.appendItem(this.getMonitor().getItemStack(out, CraftingItemList.PENDING));
                }
                this.list.resetStatus();
                for (IAEItemStack g : this.crafters) {
                    if (!(g instanceof EntityPlayerMP)) continue;
                    EntityPlayerMP epmp = (EntityPlayerMP)g;
                    if (!a.isEmpty()) {
                        NetworkHandler.instance.sendTo(a, epmp);
                    }
                    if (!b.isEmpty()) {
                        NetworkHandler.instance.sendTo(b, epmp);
                    }
                    if (!c.isEmpty()) {
                        NetworkHandler.instance.sendTo(c, epmp);
                    }
                    NetworkHandler.instance.sendTo(d, epmp);
                    NetworkHandler.instance.sendTo(new PacketCraftingRemainingOperations(this.getMonitor().getRemainingOperations()), epmp);
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        super.detectAndSendChanges();
    }

    @Override
    public boolean isValid(Object verificationToken) {
        return true;
    }

    @Override
    public void postChange(IBaseMonitor<IAEItemStack> monitor, Iterable<IAEItemStack> change, BaseActionSource actionSource) {
        for (IAEItemStack is : change) {
            is = is.copy();
            is.setStackSize(1L);
            this.list.add(is);
        }
    }

    @Override
    public void onListUpdate() {
    }

    @Override
    public String getCustomName() {
        return this.cpuName;
    }

    @Override
    public boolean hasCustomName() {
        return this.cpuName != null && this.cpuName.length() > 0;
    }

    @Override
    public void setCustomName(String name) {
        this.cpuName = name;
    }

    public long getElapsedTime() {
        return this.elapsed;
    }

    private void setElapsedTime(long elapsed) {
        this.elapsed = elapsed;
    }

    public CraftingCPUCluster getMonitor() {
        return this.monitor;
    }

    private void setMonitor(CraftingCPUCluster monitor) {
        this.monitor = monitor;
    }

    IGrid getNetwork() {
        return this.network;
    }

    private void setNetwork(IGrid network) {
        this.network = network;
    }

    public void togglePlayerFollowStatus(String name) {
        if (this.getMonitor() != null) {
            this.getMonitor().togglePlayerFollowStatus(name);
        }
    }

    public List<String> getPlayersFollowingCurrentCraft() {
        if (this.getMonitor() != null) {
            return this.getMonitor().getPlayersFollowingCurrentCraft();
        }
        return null;
    }

    public void changeAllowMode(String msg) {
        if (this.getMonitor() != null) {
            CraftingAllow newAllowMode = CraftingAllow.values()[Integer.valueOf(msg)].next();
            this.getMonitor().changeCraftingAllowMode(newAllowMode);
            this.allow = newAllowMode.ordinal();
        }
    }

    public CraftingAllow getAllowMode() {
        if (this.getMonitor() != null) {
            return this.getMonitor().getCraftingAllowMode();
        }
        return null;
    }
}

