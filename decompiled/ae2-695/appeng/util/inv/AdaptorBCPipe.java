/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.util.inv;

import appeng.api.config.FuzzyMode;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.abstraction.IBuildCraftTransport;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import appeng.util.iterators.NullIterator;
import java.util.Iterator;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class AdaptorBCPipe
extends InventoryAdaptor {
    private final IBuildCraftTransport buildCraft = (IBuildCraftTransport)IntegrationRegistry.INSTANCE.getInstance(IntegrationType.BuildCraftTransport);
    private final TileEntity i;
    private final ForgeDirection d;

    public AdaptorBCPipe(TileEntity s, ForgeDirection dd) {
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.BuildCraftTransport) && this.buildCraft.isPipe(s, dd)) {
            this.i = s;
            this.d = dd;
            return;
        }
        this.i = null;
        this.d = null;
    }

    @Override
    public ItemStack removeItems(int amount, ItemStack filter, IInventoryDestination destination) {
        return null;
    }

    @Override
    public ItemStack simulateRemove(int amount, ItemStack filter, IInventoryDestination destination) {
        return null;
    }

    @Override
    public ItemStack removeSimilarItems(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        return null;
    }

    @Override
    public ItemStack simulateSimilarRemove(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        return null;
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded) {
        if (this.i == null) {
            return toBeAdded;
        }
        if (toBeAdded == null) {
            return null;
        }
        if (toBeAdded.stackSize == 0) {
            return null;
        }
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.BuildCraftTransport) && this.buildCraft.addItemsToPipe(this.i, toBeAdded, this.d)) {
            return null;
        }
        return toBeAdded;
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated) {
        if (this.i == null) {
            return toBeSimulated;
        }
        return null;
    }

    @Override
    public boolean containsItems() {
        return false;
    }

    @Override
    public Iterator<ItemSlot> iterator() {
        return new NullIterator<ItemSlot>();
    }
}

