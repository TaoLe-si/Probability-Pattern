package com.tz.statpatterns.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocator;

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenuHost;

import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;

/**
 * Handheld probability pattern terminal item.
 * Extends AE2's WirelessTerminalItem and implements ae2wtlib's
 * IUniversalWirelessTerminalItem for WUT/quantum bridge compatibility.
 */
public class ProbabilityPatternTerminalItem extends WirelessTerminalItem
        implements IUniversalWirelessTerminalItem {

    public ProbabilityPatternTerminalItem(Properties props) {
        super(() -> 1600000.0, props);
    }

    @Override
    public MenuType<?> getMenuType() {
        return SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
    }

    @Override
    public MenuType<?> getMenuType(ItemStack stack) {
        return getMenuType();
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int slot, ItemStack stack, net.minecraft.core.BlockPos pos) {
        return new ProbabilityPatternTerminalMenuHost(player, slot, stack,
                (p, sm) -> openFromInventory(p, slot, true));
    }

    /**
     * Override to provide our own menu host directly, bypassing WUTHandler lookup.
     * This is the critical method that allows the terminal to open without
     * requiring the WUTHandler.wirelessTerminals map to be populated.
     */
    @Override
    public ItemMenuHost getMenuHost(Player player, MenuLocator locator, ItemStack stack) {
        return new ProbabilityPatternTerminalMenuHost(player, null, stack,
                (p, sm) -> tryOpen(p, locator, stack, true));
    }

    /**
     * Combined grid lookup: tries WAP first, falls back to quantum bridge.
     * Uses ItemWT's static getQuantumBridge for bridge support.
     */
    @Override
    public IGrid getLinkedGrid(ItemStack stack, Level level, Player player) {
        // Try standard WAP link first
        var wapGrid = super.getLinkedGrid(stack, level, player);
        if (wapGrid != null) {
            return wapGrid;
        }
        // Fall back to quantum bridge
        if (!level.isClientSide()) {
            var qb = de.mari_023.ae2wtlib.terminal.ItemWT.getQuantumBridge(stack, level, null, null);
            if (qb != null && qb.getActionableNode() != null) {
                return qb.getActionableNode().getGrid();
            }
        }
        return null;
    }
}
