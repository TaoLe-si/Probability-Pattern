/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.util.inv;

import appeng.api.config.Actionable;
import appeng.api.config.AdvancedBlockingMode;
import appeng.api.config.InsertionMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.features.registries.BlockingModeIgnoreItemRegistry;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.item.AEItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class AdaptorDualityInterface
extends AdaptorIInventory {
    public final IInterfaceHost interfaceHost;

    public AdaptorDualityInterface(IInventory s, IInterfaceHost interfaceHost) {
        super(s);
        this.interfaceHost = interfaceHost;
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded, InsertionMode insertionMode) {
        IMEMonitor<IAEItemStack> monitor = this.interfaceHost.getInterfaceDuality().getItemInventory();
        IAEItemStack result = monitor.injectItems(AEItemStack.create(toBeAdded), Actionable.MODULATE, this.interfaceHost.getInterfaceDuality().getActionSource());
        return result == null ? null : result.getItemStack();
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated, InsertionMode insertionMode) {
        IMEMonitor<IAEItemStack> monitor = this.interfaceHost.getInterfaceDuality().getItemInventory();
        IAEItemStack result = monitor.injectItems(AEItemStack.create(toBeSimulated), Actionable.SIMULATE, this.interfaceHost.getInterfaceDuality().getActionSource());
        return result == null ? null : result.getItemStack();
    }

    @Override
    public boolean containsItems() {
        DualityInterface dual = this.interfaceHost.getInterfaceDuality();
        boolean hasMEItems = false;
        if (dual.getInstalledUpgrades(Upgrades.ADVANCED_BLOCKING) > 0) {
            IMEMonitor<IAEFluidStack> dualFluidInventory;
            if (dual.getConfigManager().getSetting(Settings.ADVANCED_BLOCKING_MODE) == AdvancedBlockingMode.DEFAULT) {
                IItemList<IAEItemStack> itemList = dual.getItemInventory().getStorageList();
                for (IAEItemStack stack : itemList) {
                    if (BlockingModeIgnoreItemRegistry.instance().isIgnored(stack.getItemStack())) continue;
                    hasMEItems = true;
                    break;
                }
            } else {
                hasMEItems = !dual.getItemInventory().getStorageList().isEmpty();
            }
            if ((dualFluidInventory = dual.getFluidInventory()) != null) {
                hasMEItems |= !dualFluidInventory.getStorageList().isEmpty();
            }
        }
        return hasMEItems || super.containsItems();
    }
}

