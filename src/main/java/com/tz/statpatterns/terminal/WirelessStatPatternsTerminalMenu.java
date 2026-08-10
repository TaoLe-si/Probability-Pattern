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
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.helpers.IPatternTerminalMenuHost;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wut.ItemWUT;

/**
 * Wireless variant of the probability pattern terminal menu.
 * Includes singularity slot for Quantum Bridge Card and WUT support.
 */
public class WirelessStatPatternsTerminalMenu extends StatPatternsTerminalMenu {

    private final StatPatternsTerminalMenuHost wtHost;

    public WirelessStatPatternsTerminalMenu(int containerId, Inventory playerInventory,
            @Nullable IPatternTerminalMenuHost host) {
        this(StatPatternsMenus.WIRELESS_STAT_PATTERN_TERMINAL, containerId, playerInventory, host);
    }

    public WirelessStatPatternsTerminalMenu(MenuType<?> menuType, int containerId,
            Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host);

        this.wtHost = (StatPatternsTerminalMenuHost) host;

        // Singularity slot for Quantum Bridge Card
        this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                wtHost.getSingularityInventory(), 0), AE2wtlibSlotSemantics.SINGULARITY);
    }

    public IGridNode getGridNode() {
        return wtHost.getActionableNode();
    }

    public boolean isWUT() {
        return wtHost.getItemStack().getItem() instanceof ItemWUT;
    }

    public StatPatternsTerminalMenuHost getWTHost() {
        return wtHost;
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack stack) {
        if (stack.is(AEItems.QUANTUM_ENTANGLED_SINGULARITY.stack().getItem())
                || stack.is(AEItems.SINGULARITY.stack().getItem())) {
            for (var slot : slots) {
                if (slot.mayPlace(stack)) {
                    slot.safeInsert(stack);
                    return ItemStack.EMPTY;
                }
            }
            return ItemStack.EMPTY;
        }
        return super.transferStackToMenu(stack);
    }
}
