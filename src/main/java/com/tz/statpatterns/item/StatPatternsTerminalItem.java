/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tz.statpatterns.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocator;

import com.tz.statpatterns.core.definition.StatPatternsMenus;
import com.tz.statpatterns.terminal.StatPatternsTerminalMenuHost;

import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;

/**
 * Handheld probability pattern terminal item.
 * Extends AE2's WirelessTerminalItem and implements ae2wtlib's
 * IUniversalWirelessTerminalItem for WUT/quantum bridge compatibility.
 */
public class StatPatternsTerminalItem extends WirelessTerminalItem
        implements IUniversalWirelessTerminalItem {

    public StatPatternsTerminalItem(Properties props) {
        super(() -> 1600000.0, props);
    }

    @Override
    public MenuType<?> getMenuType() {
        return StatPatternsMenus.WIRELESS_STAT_PATTERN_TERMINAL;
    }

    @Override
    public MenuType<?> getMenuType(ItemStack stack) {
        return getMenuType();
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int slot, ItemStack stack, net.minecraft.core.BlockPos pos) {
        return new StatPatternsTerminalMenuHost(player, slot, stack,
                (p, sm) -> openFromInventory(p, slot, true));
    }

    /**
     * Override to provide our own menu host directly, bypassing WUTHandler lookup.
     * This is the critical method that allows the terminal to open without
     * requiring the WUTHandler.wirelessTerminals map to be populated.
     */
    @Override
    public ItemMenuHost getMenuHost(Player player, MenuLocator locator, ItemStack stack) {
        return new StatPatternsTerminalMenuHost(player, null, stack,
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
