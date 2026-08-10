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
import appeng.client.gui.widgets.BackgroundPanel;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.tz.statpatterns.terminal.WirelessStatPatternsTerminalMenu;

/**
 * Wireless variant of the probability pattern terminal screen.
 * Implements ae2wtlib's IUniversalTerminalCapable for WUT support.
 */
public class WirelessStatPatternsTerminalScreen
        extends StatPatternsTerminalScreen<WirelessStatPatternsTerminalMenu>
        implements IUniversalTerminalCapable {

    public WirelessStatPatternsTerminalScreen(WirelessStatPatternsTerminalMenu menu,
            Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        if (getMenu().isWUT()) {
            addToLeftToolbar(new CycleTerminalButton(btn -> cycleTerminal()));
        }
        // Draw the quantum entangled singularity slot background border.
        // Matches ae2wtlib's WETScreen, which adds this panel from the
        // "singularityBackground" image in universal_terminal_*.json.
        widgets.add("singularityBackground", new BackgroundPanel(style.getImage("singularityBackground")));
    }

    @Override
    public boolean isHandlingRightClick() {
        return false;
    }

    @Override
    public void storeState() {
    }
}
