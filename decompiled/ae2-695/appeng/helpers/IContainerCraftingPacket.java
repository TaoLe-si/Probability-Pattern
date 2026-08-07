/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.helpers;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public interface IContainerCraftingPacket {
    public IGridNode getNetworkNode();

    public IInventory getInventoryByName(String var1);

    public BaseActionSource getActionSource();

    public boolean useRealItems();

    public ItemStack[] getViewCells();
}

