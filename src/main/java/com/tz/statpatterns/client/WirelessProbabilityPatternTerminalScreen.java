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
package com.tz.statpatterns.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.menu.AEBaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;

import de.mari_023.ae2wtlib.api.gui.ScrollingUpgradesPanel;
import de.mari_023.ae2wtlib.api.terminal.IUniversalTerminalCapable;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;

/**
 * Wireless variant of the probability pattern terminal screen.
 * Implements IUniversalTerminalCapable for ae2wtlib Universal Terminal support:
 * - ScrollingUpgradesPanel for upgrade cards and singularity slot
 * - Terminal cycling button for switching between terminals in UUT
 * - Keyboard shortcuts for terminal switching
 */
public class WirelessProbabilityPatternTerminalScreen
        extends ProbabilityPatternTerminalScreen<WirelessProbabilityPatternTerminalMenu>
        implements IUniversalTerminalCapable {

    private final ScrollingUpgradesPanel upgradesPanel;

    public WirelessProbabilityPatternTerminalScreen(WirelessProbabilityPatternTerminalMenu menu,
            Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        // Add cycle terminal button if this is a Universal Wireless Terminal
        if (menu.isWUT()) {
            addToLeftToolbar(cycleTerminalButton());
        }

        // Add the scrolling upgrades panel (manages upgrade slots + singularity slot)
        this.upgradesPanel = addUpgradePanel(widgets, menu);
    }

    @Override
    public void init() {
        super.init();
        // Set max visible rows for the scrolling upgrades panel
        upgradesPanel.setMaxRows(Math.max(2, getVisibleRows()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (!handled) {
            handled = checkForTerminalKeys(keyCode, scanCode);
        }
        return handled;
    }

    @Override
    public WTMenuHost getHost() {
        return getMenu().getWTHost();
    }
}
