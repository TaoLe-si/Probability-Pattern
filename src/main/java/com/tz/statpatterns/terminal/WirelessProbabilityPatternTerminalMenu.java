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
import appeng.menu.slot.RestrictedInputSlot;
import com.tz.statpatterns.core.definition.SPMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import org.jetbrains.annotations.Nullable;

import appeng.helpers.IPatternTerminalMenuHost;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;

/**
 * Wireless variant of the probability pattern terminal menu.
 * Adds a singularity slot for quantum bridge functionality.
 * Follows the same pattern as ae2wtlib's WETMenu.
 */
public class WirelessProbabilityPatternTerminalMenu extends ProbabilityPatternTerminalMenu {

    private final ProbabilityPatternTerminalMenuHost wtHost;

    public WirelessProbabilityPatternTerminalMenu(int containerId, Inventory playerInventory,
            @Nullable IPatternTerminalMenuHost host) {
        this(SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL.get(), containerId, playerInventory, host);
    }

    public WirelessProbabilityPatternTerminalMenu(MenuType<?> menuType, int containerId,
            Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host);

        // Store the wtHost reference for getGridNode()/isWUT()
        this.wtHost = (ProbabilityPatternTerminalMenuHost) host;

        // Add singularity slot for quantum bridge card (same as WETMenu)
        this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        wtHost.getSingularityInventory(), 0),
                AE2wtlibSlotSemantics.SINGULARITY);
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
    public ProbabilityPatternTerminalMenuHost getWTHost() {
        return wtHost;
    }
}
