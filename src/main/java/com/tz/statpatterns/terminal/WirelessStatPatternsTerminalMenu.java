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
package com.tz.statpatterns.terminal;

import appeng.api.networking.IGridNode;
import appeng.core.definitions.AEItems;
import appeng.menu.slot.RestrictedInputSlot;
import com.tz.statpatterns.core.definition.StatPatternsMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.helpers.IPatternTerminalMenuHost;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;

/**
 * Wireless variant of the probability pattern terminal menu.
 * Adds a singularity slot for quantum bridge functionality.
 * Follows the same pattern as ae2wtlib's WETMenu.
 */
public class WirelessStatPatternsTerminalMenu extends StatPatternsTerminalMenu {

    private final StatPatternsTerminalMenuHost wtHost;

    public WirelessStatPatternsTerminalMenu(int containerId, Inventory playerInventory,
            @Nullable IPatternTerminalMenuHost host) {
        this(StatPatternsMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL.get(), containerId, playerInventory, host);
    }

    public WirelessStatPatternsTerminalMenu(MenuType<?> menuType, int containerId,
            Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host);

        // Store the wtHost reference for getGridNode()/isWUT()
        this.wtHost = (StatPatternsTerminalMenuHost) host;

        // Add singularity slot for quantum bridge card (same as WETMenu)
        this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY, wtHost.getSingularityInventory(), 0), AE2wtlibSlotSemantics.SINGULARITY);
    }

    /**
     * Get the grid node for ae2wtlib network access.
     * Delegates to WTMenuHost.getActionableNode() which handles quantum bridge.
     */
    public IGridNode getGridNode() {
        return wtHost.getActionableNode();
    }

    /**
     * Check if this terminal is part of a Universal Wireless Terminal.
     */
    public boolean isWUT() {
        return wtHost.getItemStack().getItem() instanceof ItemWUT;
    }

    /**
     * Get the WTMenuHost for IUniversalTerminalCapable.
     */
    public StatPatternsTerminalMenuHost getWTHost() {
        return wtHost;
    }

    @Override
    protected int transferStackToMenu(ItemStack stack) {
        // Route singularities to the dedicated QE_SINGULARITY slot
        // (the parent's transferStackToMenu only checks blank/encoded pattern slots)
        if (stack.is(AEItems.QUANTUM_ENTANGLED_SINGULARITY.asItem())
                || stack.is(AEItems.SINGULARITY.asItem())) {
            var count = stack.getCount();
            for (var slot : slots) {
                if (slot.mayPlace(stack)) {
                    var remainder = slot.safeInsert(stack);
                    if (remainder.isEmpty()) {
                        return count;
                    }
                }
            }
            return 0;
        }
        return super.transferStackToMenu(stack);
    }
}
