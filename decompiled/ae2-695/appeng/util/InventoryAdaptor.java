/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ISidedInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntityChest
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.util;

import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.parts.IPart;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.helpers.IInterfaceHost;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.abstraction.IBetterStorage;
import appeng.integration.abstraction.IThaumicTinkerer;
import appeng.parts.p2p.PartP2PItems;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;
import appeng.tile.storage.TileChest;
import appeng.util.Platform;
import appeng.util.inv.AdaptorDualityInterface;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.AdaptorList;
import appeng.util.inv.AdaptorMEChest;
import appeng.util.inv.AdaptorP2PItem;
import appeng.util.inv.AdaptorPlayerInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import appeng.util.inv.WrapperMCISidedInventory;
import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class InventoryAdaptor
implements Iterable<ItemSlot> {
    public static InventoryAdaptor getAdaptor(Object te, ForgeDirection d) {
        IInventory i;
        if (te == null) {
            return null;
        }
        IBetterStorage bs = (IBetterStorage)(IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.BetterStorage) ? IntegrationRegistry.INSTANCE.getInstance(IntegrationType.BetterStorage) : null);
        IThaumicTinkerer tt = (IThaumicTinkerer)(IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.ThaumicTinkerer) ? IntegrationRegistry.INSTANCE.getInstance(IntegrationType.ThaumicTinkerer) : null);
        if (tt != null && tt.isTransvectorInterface(te)) {
            te = tt.getTile(te);
        }
        if (te instanceof EntityPlayer) {
            return new AdaptorIInventory(new AdaptorPlayerInventory((IInventory)((EntityPlayer)te).inventory, false));
        }
        if (te instanceof ArrayList) {
            ArrayList list = (ArrayList)te;
            return new AdaptorList(list);
        }
        if (bs != null && bs.isStorageCrate(te)) {
            return bs.getAdaptor(te, d);
        }
        if (te instanceof TileEntityChest) {
            return new AdaptorIInventory(Platform.GetChestInv(te));
        }
        if (te instanceof ISidedInventory) {
            ISidedInventory si = (ISidedInventory)te;
            if (te instanceof TileInterface) {
                return new AdaptorDualityInterface((IInventory)new WrapperMCISidedInventory(si, d), (IInterfaceHost)te);
            }
            if (te instanceof TileCableBus) {
                IPart part = ((TileCableBus)te).getPart(d);
                if (part instanceof IInterfaceHost) {
                    IInterfaceHost host = (IInterfaceHost)((Object)part);
                    return new AdaptorDualityInterface((IInventory)new WrapperMCISidedInventory(si, d), host);
                }
                if (part instanceof PartP2PItems) {
                    PartP2PItems p2p = (PartP2PItems)part;
                    return new AdaptorP2PItem(p2p);
                }
            } else if (te instanceof TileChest) {
                return new AdaptorMEChest(new WrapperMCISidedInventory(si, d), (TileChest)te);
            }
            int[] slots = si.getAccessibleSlotsFromSide(d.ordinal());
            if (si.getSizeInventory() > 0 && slots != null && slots.length > 0) {
                return new AdaptorIInventory(new WrapperMCISidedInventory(si, d));
            }
        } else if (te instanceof IInventory && (i = (IInventory)te).getSizeInventory() > 0) {
            return new AdaptorIInventory(i);
        }
        return null;
    }

    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int iteration) {
        return out;
    }

    public abstract ItemStack removeItems(int var1, ItemStack var2, IInventoryDestination var3);

    public abstract ItemStack simulateRemove(int var1, ItemStack var2, IInventoryDestination var3);

    public abstract ItemStack removeSimilarItems(int var1, ItemStack var2, FuzzyMode var3, IInventoryDestination var4);

    public abstract ItemStack simulateSimilarRemove(int var1, ItemStack var2, FuzzyMode var3, IInventoryDestination var4);

    public abstract ItemStack addItems(ItemStack var1);

    public ItemStack addItems(ItemStack toBeAdded, InsertionMode insertionMode) {
        return this.addItems(toBeAdded);
    }

    public abstract ItemStack simulateAdd(ItemStack var1);

    public ItemStack simulateAdd(ItemStack toBeSimulated, InsertionMode insertionMode) {
        return this.simulateAdd(toBeSimulated);
    }

    public abstract boolean containsItems();
}

