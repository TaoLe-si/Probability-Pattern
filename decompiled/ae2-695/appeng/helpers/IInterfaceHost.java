/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.helpers;

import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.util.IInterfaceViewable;
import appeng.helpers.DualityInterface;
import java.util.EnumSet;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public interface IInterfaceHost
extends ICraftingProvider,
IUpgradeableHost,
ICraftingRequester,
IInterfaceViewable {
    public DualityInterface getInterfaceDuality();

    public EnumSet<ForgeDirection> getTargets();

    @Override
    public TileEntity getTileEntity();

    public void saveChanges();

    @Override
    default public IInventory getPatterns() {
        return this.getInterfaceDuality().getPatterns();
    }

    @Override
    default public boolean shouldDisplay() {
        return this.getInterfaceDuality().getConfigManager().getSetting(Settings.INTERFACE_TERMINAL) == YesNo.YES;
    }

    @Override
    default public boolean allowsPatternOptimization() {
        return this.getInterfaceDuality().getConfigManager().getSetting(Settings.PATTERN_OPTIMIZATION) == YesNo.YES;
    }

    @Override
    default public String getName() {
        return this.getInterfaceDuality().getTermName();
    }

    @Override
    default public int rows() {
        return this.getInterfaceDuality().getInstalledUpgrades(Upgrades.PATTERN_CAPACITY) + 1;
    }

    @Override
    default public int rowSize() {
        return 9;
    }

    @Override
    default public ItemStack getDisplayRep() {
        return this.getInterfaceDuality().getCrafterIcon();
    }
}

