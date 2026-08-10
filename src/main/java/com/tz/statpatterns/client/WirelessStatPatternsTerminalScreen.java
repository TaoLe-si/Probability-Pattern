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
import de.mari_023.ae2wtlib.api.gui.ScrollingUpgradesPanel;
import de.mari_023.ae2wtlib.api.terminal.IUniversalTerminalCapable;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.tz.statpatterns.terminal.WirelessStatPatternsTerminalMenu;

/**
 * Wireless variant of the probability pattern terminal screen.
 * Implements ae2wtlib's IUniversalTerminalCapable for WUT support:
 * - ScrollingUpgradesPanel for upgrade cards
 * - Terminal cycle button when part of a WUT
 * - Keyboard shortcuts for terminal switching
 */
public class WirelessStatPatternsTerminalScreen
        extends StatPatternsTerminalScreen<WirelessStatPatternsTerminalMenu>
        implements IUniversalTerminalCapable {

    private final ScrollingUpgradesPanel upgradesPanel;

    public WirelessStatPatternsTerminalScreen(WirelessStatPatternsTerminalMenu menu,
            Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        if (getMenu().isWUT()) {
            addToLeftToolbar(cycleTerminalButton());
        }
        this.upgradesPanel = addUpgradePanel(widgets, getMenu());
    }

    @Override
    public void init() {
        super.init();
        upgradesPanel.setMaxRows(Math.max(2, getVisibleRows()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return checkForTerminalKeys(keyCode, scanCode);
    }

    @Override
    public WTMenuHost getHost() {
        return getMenu().getWTHost();
    }
}
