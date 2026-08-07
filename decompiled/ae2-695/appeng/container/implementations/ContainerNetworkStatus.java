/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gtnewhorizon.gtnhlib.util.map.ItemStackMap
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.CellType;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.guiobjects.INetworkTool;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.NamedDimensionalCoord;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.core.AEConfig;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.helpers.ICustomNameObject;
import appeng.me.cache.GridStorageCache;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public class ContainerNetworkStatus
extends AEBaseContainer {
    @GuiSync(value=0)
    public long avgAddition;
    @GuiSync(value=1)
    public long powerUsage;
    @GuiSync(value=2)
    public long currentPower;
    @GuiSync(value=3)
    public long maxPower;
    @GuiSync(value=4)
    public long itemBytesTotal;
    @GuiSync(value=5)
    public long itemBytesUsed;
    @GuiSync(value=6)
    public long itemTypesTotal;
    @GuiSync(value=7)
    public long itemTypesUsed;
    @GuiSync(value=8)
    public long itemCellG;
    @GuiSync(value=9)
    public long itemCellB;
    @GuiSync(value=10)
    public long itemCellO;
    @GuiSync(value=11)
    public long itemCellR;
    @GuiSync(value=12)
    public long fluidBytesTotal;
    @GuiSync(value=13)
    public long fluidBytesUsed;
    @GuiSync(value=14)
    public long fluidTypesTotal;
    @GuiSync(value=15)
    public long fluidTypesUsed;
    @GuiSync(value=16)
    public long fluidCellG;
    @GuiSync(value=17)
    public long fluidCellB;
    @GuiSync(value=18)
    public long fluidCellO;
    @GuiSync(value=19)
    public long fluidCellR;
    @GuiSync(value=20)
    public long essentiaBytesTotal;
    @GuiSync(value=21)
    public long essentiaBytesUsed;
    @GuiSync(value=22)
    public long essentiaTypesTotal;
    @GuiSync(value=23)
    public long essentiaTypesUsed;
    @GuiSync(value=24)
    public long essentiaCellG;
    @GuiSync(value=25)
    public long essentiaCellB;
    @GuiSync(value=26)
    public long essentiaCellO;
    @GuiSync(value=27)
    public long essentiaCellR;
    @GuiSync(value=28)
    public long itemCellCount;
    @GuiSync(value=29)
    public long fluidCellCount;
    @GuiSync(value=30)
    public long essentiaCellCount;
    @GuiSync(value=31)
    public boolean powerInfinite;
    private IGrid network;
    private int delay = 40;
    private boolean isConsume = true;

    public ContainerNetworkStatus(InventoryPlayer ip, INetworkTool te) {
        super(ip, null, null);
        IGridHost host = te.getGridHost();
        if (host != null) {
            this.findNode(host, ForgeDirection.UNKNOWN);
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                this.findNode(host, d);
            }
        }
        if (this.network == null && Platform.isServer()) {
            this.setValidContainer(false);
        }
    }

    private void findNode(IGridHost host, ForgeDirection d) {
        IGridNode node;
        if (this.network == null && (node = host.getGridNode(d)) != null) {
            this.network = node.getGrid();
        }
    }

    @Override
    public void detectAndSendChanges() {
        ++this.delay;
        if (Platform.isServer() && this.delay > 15 && this.network != null) {
            GridStorageCache sg;
            this.delay = 0;
            IEnergyGrid eg = (IEnergyGrid)this.network.getCache(IEnergyGrid.class);
            if (eg != null) {
                this.setAverageAddition((long)(100.0 * eg.getAvgPowerInjection()));
                this.setPowerUsage((long)(100.0 * eg.getAvgPowerUsage()));
                this.setCurrentPower((long)(100.0 * eg.getStoredPower()));
                this.setMaxPower((long)(100.0 * eg.getMaxStoredPower()));
                this.setPowerInfinite(eg.getHasInfiniteStore());
            }
            try {
                PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();
                IItemList<IAEItemStack> list = AEApi.instance().storage().createItemList();
                HashMap<AEItemStack, ArrayList> dcMap = new HashMap<AEItemStack, ArrayList>();
                if (this.isConsume) {
                    for (Class clazz : this.network.getMachinesClasses()) {
                        for (Object machine : this.network.getMachines(clazz)) {
                            ArrayList dcList;
                            ICustomNameObject ico;
                            IGridBlock blk = machine.getGridBlock();
                            ItemStack is = blk.getMachineRepresentation();
                            if (is == null || is.getItem() == null) continue;
                            AEItemStack ais = AEItemStack.create(is);
                            ais.setStackSize(1L);
                            ais.setCountRequestable((long)PowerMultiplier.CONFIG.multiply(blk.getIdlePowerUsage() * 100.0));
                            list.add(ais);
                            String customName = "";
                            IGridHost iGridHost = blk.getMachine();
                            if (iGridHost instanceof ICustomNameObject && (ico = (ICustomNameObject)((Object)iGridHost)).hasCustomName()) {
                                customName = ico.getCustomName();
                            }
                            if (dcMap.containsKey(ais)) {
                                dcList = (ArrayList)dcMap.get(ais);
                                dcList.add(new NamedDimensionalCoord(blk.getLocation(), customName));
                                continue;
                            }
                            dcList = new ArrayList();
                            dcList.add(new NamedDimensionalCoord(blk.getLocation(), customName));
                            dcMap.put(ais, dcList);
                        }
                    }
                } else {
                    ItemStackMap<Integer> itemStackMap;
                    GridStorageCache sg2 = (GridStorageCache)this.network.getCache(IStorageGrid.class);
                    CellType cellType = AEConfig.instance.selectedCellType();
                    switch (cellType) {
                        default: {
                            throw new IncompatibleClassChangeError();
                        }
                        case ITEM: {
                            itemStackMap = sg2.getItemCells();
                            break;
                        }
                        case FLUID: {
                            itemStackMap = sg2.getFluidCells();
                            break;
                        }
                        case ESSENTIA: {
                            itemStackMap = sg2.getEssentiaCells();
                        }
                    }
                    ItemStackMap<Integer> cells = itemStackMap;
                    for (Map.Entry set : cells.entrySet()) {
                        AEItemStack ais = AEItemStack.create((ItemStack)set.getKey());
                        ais.setStackSize(((Integer)set.getValue()).intValue());
                        list.add(ais);
                    }
                }
                for (IAEItemStack iAEItemStack : list) {
                    ArrayList dcl = (ArrayList)dcMap.get(iAEItemStack);
                    if (dcl != null) {
                        ItemStack is = iAEItemStack.getItemStack();
                        NBTTagCompound tag = new NBTTagCompound();
                        NamedDimensionalCoord.writeListToNBTNamed(tag, dcl);
                        is.setTagCompound(tag);
                        piu.appendItem((IAEItemStack)AEItemStack.create(is).setCountRequestable(iAEItemStack.getCountRequestable()));
                        continue;
                    }
                    piu.appendItem(iAEItemStack);
                }
                for (Object object : this.crafters) {
                    if (!(object instanceof EntityPlayer)) continue;
                    NetworkHandler.instance.sendTo(piu, (EntityPlayerMP)object);
                }
            }
            catch (IOException piu) {
                // empty catch block
            }
            if ((sg = (GridStorageCache)this.network.getCache(IStorageGrid.class)) != null) {
                this.itemBytesUsed = Double.doubleToLongBits(sg.getItemBytesUsed());
                this.itemBytesTotal = Double.doubleToLongBits(sg.getItemBytesTotal());
                this.itemCellG = sg.getItemCellG();
                this.itemCellB = sg.getItemCellB();
                this.itemCellO = sg.getItemCellO();
                this.itemCellR = sg.getItemCellR();
                this.itemCellCount = sg.getItemCellCount();
                this.itemTypesUsed = sg.getItemTypesUsed();
                this.itemTypesTotal = sg.getItemTypesTotal();
                this.fluidBytesUsed = Double.doubleToLongBits(sg.getFluidBytesUsed());
                this.fluidBytesTotal = Double.doubleToLongBits(sg.getFluidBytesTotal());
                this.fluidCellG = sg.getFluidCellG();
                this.fluidCellB = sg.getFluidCellB();
                this.fluidCellO = sg.getFluidCellO();
                this.fluidCellR = sg.getFluidCellR();
                this.fluidCellCount = sg.getFluidCellCount();
                this.fluidTypesUsed = sg.getFluidTypesUsed();
                this.fluidTypesTotal = sg.getFluidTypesTotal();
                this.essentiaBytesUsed = Double.doubleToLongBits(sg.getEssentiaBytesUsed());
                this.essentiaBytesTotal = Double.doubleToLongBits(sg.getEssentiaBytesTotal());
                this.essentiaCellG = sg.getEssentiaCellG();
                this.essentiaCellB = sg.getEssentiaCellB();
                this.essentiaCellO = sg.getEssentiaCellO();
                this.essentiaCellR = sg.getEssentiaCellR();
                this.essentiaCellCount = sg.getEssentiaCellCount();
                this.essentiaTypesUsed = sg.getEssentiaTypesUsed();
                this.essentiaTypesTotal = sg.getEssentiaTypesTotal();
            }
        }
        super.detectAndSendChanges();
    }

    public void setConsume(boolean isConsume) {
        this.isConsume = isConsume;
    }

    public long getCurrentPower() {
        return this.currentPower;
    }

    private void setCurrentPower(long currentPower) {
        this.currentPower = currentPower;
    }

    public long getMaxPower() {
        return this.maxPower;
    }

    private void setMaxPower(long maxPower) {
        this.maxPower = maxPower;
    }

    public long getAverageAddition() {
        return this.avgAddition;
    }

    private void setAverageAddition(long avgAddition) {
        this.avgAddition = avgAddition;
    }

    public long getPowerUsage() {
        return this.powerUsage;
    }

    private void setPowerUsage(long powerUsage) {
        this.powerUsage = powerUsage;
    }

    public long getItemBytesTotal() {
        return this.itemBytesTotal;
    }

    public long getItemBytesUsed() {
        return this.itemBytesUsed;
    }

    public long getItemTypesTotal() {
        return this.itemTypesTotal;
    }

    public long getItemTypesUsed() {
        return this.itemTypesUsed;
    }

    public long getItemCellG() {
        return this.itemCellG;
    }

    public long getItemCellB() {
        return this.itemCellB;
    }

    public long getItemCellO() {
        return this.itemCellO;
    }

    public long getItemCellR() {
        return this.itemCellR;
    }

    public long getFluidBytesTotal() {
        return this.fluidBytesTotal;
    }

    public long getFluidBytesUsed() {
        return this.fluidBytesUsed;
    }

    public long getFluidTypesTotal() {
        return this.fluidTypesTotal;
    }

    public long getFluidTypesUsed() {
        return this.fluidTypesUsed;
    }

    public long getFluidCellG() {
        return this.fluidCellG;
    }

    public long getFluidCellB() {
        return this.fluidCellB;
    }

    public long getFluidCellO() {
        return this.fluidCellO;
    }

    public long getFluidCellR() {
        return this.fluidCellR;
    }

    public long getEssentiaBytesTotal() {
        return this.essentiaBytesTotal;
    }

    public long getEssentiaBytesUsed() {
        return this.essentiaBytesUsed;
    }

    public long getEssentiaTypesTotal() {
        return this.essentiaTypesTotal;
    }

    public long getEssentiaTypesUsed() {
        return this.essentiaTypesUsed;
    }

    public long getEssentiaCellG() {
        return this.essentiaCellG;
    }

    public long getEssentiaCellB() {
        return this.essentiaCellB;
    }

    public long getEssentiaCellO() {
        return this.essentiaCellO;
    }

    public long getEssentiaCellR() {
        return this.essentiaCellR;
    }

    public long getItemCellCount() {
        return this.itemCellCount;
    }

    public long getFluidCellCount() {
        return this.fluidCellCount;
    }

    public long getEssentiaCellCount() {
        return this.essentiaCellCount;
    }

    public boolean isPowerInfinite() {
        return this.powerInfinite;
    }

    public void setPowerInfinite(boolean powerInfinite) {
        this.powerInfinite = powerInfinite;
    }
}

