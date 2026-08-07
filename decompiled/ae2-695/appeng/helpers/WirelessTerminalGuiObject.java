/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.World
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.ITerminalPins;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IConfigManager;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.items.contents.PinsHandler;
import appeng.items.contents.PinsHolder;
import appeng.items.contents.WirelessTerminalViewCells;
import appeng.tile.networking.TileWireless;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class WirelessTerminalGuiObject
implements IPortableCell,
IActionHost,
IInventorySlotAware,
IViewCellStorage,
ITerminalPins {
    private final ItemStack effectiveItem;
    private final IWirelessTermHandler wth;
    private final String encryptionKey;
    private final EntityPlayer myPlayer;
    private IGrid targetGrid;
    private IStorageGrid sg;
    private IMEMonitor<IAEItemStack> itemStorage;
    private IWirelessAccessPoint myWap;
    private double sqRange = Double.MAX_VALUE;
    private double myRange = Double.MAX_VALUE;
    private final int inventorySlot;
    private final WirelessTerminalViewCells viewCells;
    private final PinsHolder pinsInv;

    public WirelessTerminalGuiObject(IWirelessTermHandler wh, ItemStack is, EntityPlayer ep, World w, int x, int y, int z) {
        IGridNode n;
        this.encryptionKey = wh.getEncryptionKey(is);
        this.effectiveItem = is;
        this.myPlayer = ep;
        this.wth = wh;
        this.inventorySlot = x;
        this.viewCells = new WirelessTerminalViewCells(is);
        this.pinsInv = new PinsHolder(is);
        ILocatable obj = null;
        try {
            long encKey = Long.parseLong(this.encryptionKey);
            obj = AEApi.instance().registries().locatable().getLocatableBy(encKey);
        }
        catch (NumberFormatException encKey) {
            // empty catch block
        }
        if (obj instanceof IGridHost && (n = ((IGridHost)((Object)obj)).getGridNode(ForgeDirection.UNKNOWN)) != null) {
            this.targetGrid = n.getGrid();
            if (this.targetGrid != null) {
                this.sg = (IStorageGrid)this.targetGrid.getCache(IStorageGrid.class);
                if (this.sg != null) {
                    this.itemStorage = this.sg.getItemInventory();
                }
            }
        }
    }

    public double getRange() {
        return this.myRange;
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        if (this.sg == null) {
            return null;
        }
        return this.sg.getItemInventory();
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        if (this.sg == null) {
            return null;
        }
        return this.sg.getFluidInventory();
    }

    @Override
    public void addListener(IMEMonitorHandlerReceiver<IAEItemStack> l, Object verificationToken) {
        if (this.itemStorage != null) {
            this.itemStorage.addListener(l, verificationToken);
        }
    }

    @Override
    public void removeListener(IMEMonitorHandlerReceiver<IAEItemStack> l) {
        if (this.itemStorage != null) {
            this.itemStorage.removeListener(l);
        }
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList out, int iteration) {
        if (this.itemStorage != null) {
            return this.itemStorage.getAvailableItems(out, iteration);
        }
        return out;
    }

    @Override
    public IAEItemStack getAvailableItem(@Nonnull IAEItemStack request, int iteration) {
        if (this.itemStorage != null) {
            return this.itemStorage.getAvailableItem(request, iteration);
        }
        return null;
    }

    @Override
    public IItemList<IAEItemStack> getStorageList() {
        if (this.itemStorage != null) {
            return this.itemStorage.getStorageList();
        }
        return null;
    }

    @Override
    public AccessRestriction getAccess() {
        if (this.itemStorage != null) {
            return this.itemStorage.getAccess();
        }
        return AccessRestriction.NO_ACCESS;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        if (this.itemStorage != null) {
            return this.itemStorage.isPrioritized(input);
        }
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (this.itemStorage != null) {
            return this.itemStorage.canAccept(input);
        }
        return false;
    }

    @Override
    public int getPriority() {
        if (this.itemStorage != null) {
            return this.itemStorage.getPriority();
        }
        return 0;
    }

    @Override
    public int getSlot() {
        if (this.itemStorage != null) {
            return this.itemStorage.getSlot();
        }
        return 0;
    }

    @Override
    public boolean validForPass(int i) {
        return this.itemStorage.validForPass(i);
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, BaseActionSource src) {
        if (this.itemStorage != null) {
            return this.itemStorage.injectItems(input, type, src);
        }
        return input;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (this.itemStorage != null) {
            return this.itemStorage.extractItems(request, mode, src);
        }
        return null;
    }

    @Override
    public StorageChannel getChannel() {
        if (this.itemStorage != null) {
            return this.itemStorage.getChannel();
        }
        return StorageChannel.ITEMS;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        if (this.wth != null && this.effectiveItem != null) {
            if (mode == Actionable.SIMULATE) {
                return this.wth.hasPower(this.myPlayer, amt, this.effectiveItem) ? amt : 0.0;
            }
            return this.wth.usePower(this.myPlayer, amt, this.effectiveItem) ? amt : 0.0;
        }
        return 0.0;
    }

    @Override
    public ItemStack getItemStack() {
        return this.effectiveItem;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.wth.getConfigManager(this.effectiveItem);
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return this.getActionableNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.NONE;
    }

    @Override
    public void securityBreak() {
    }

    @Override
    public IGridNode getActionableNode() {
        this.rangeCheck();
        if (this.myWap != null) {
            return this.myWap.getActionableNode();
        }
        return null;
    }

    public boolean rangeCheck() {
        this.myRange = Double.MAX_VALUE;
        this.sqRange = Double.MAX_VALUE;
        if (this.targetGrid != null && this.itemStorage != null) {
            if (this.myWap != null) {
                return this.myWap.getGrid() == this.targetGrid && this.testWap(this.myWap);
            }
            IMachineSet tw = this.targetGrid.getMachines(TileWireless.class);
            this.myWap = null;
            for (IGridNode n : tw) {
                IWirelessAccessPoint wap = (IWirelessAccessPoint)n.getMachine();
                if (!this.testWap(wap)) continue;
                this.myWap = wap;
            }
            return this.myWap != null;
        }
        return false;
    }

    private boolean testWap(IWirelessAccessPoint wap) {
        double offZ;
        double offY;
        double offX;
        double r;
        double rangeLimit = wap.getRange();
        rangeLimit *= rangeLimit;
        DimensionalCoord dc = wap.getLocation();
        if (dc.getWorld() == this.myPlayer.worldObj && (r = (offX = (double)dc.x - this.myPlayer.posX) * offX + (offY = (double)dc.y - this.myPlayer.posY) * offY + (offZ = (double)dc.z - this.myPlayer.posZ) * offZ) < rangeLimit && this.sqRange > r && wap.isActive()) {
            this.sqRange = r;
            this.myRange = Math.sqrt(r);
            return true;
        }
        return false;
    }

    @Override
    public int getInventorySlot() {
        return this.inventorySlot;
    }

    @Override
    public IInventory getViewCellStorage() {
        return this.viewCells;
    }

    @Override
    public PinsHandler getPinsHandler(EntityPlayer player) {
        return this.pinsInv.getHandler(this.myPlayer);
    }

    @Override
    public IGrid getGrid() {
        return this.targetGrid;
    }
}

