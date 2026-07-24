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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;

/**
 * Wireless variant of the probability pattern terminal screen.
 * <p>
 * When ae2wtlib is present, the actual screen is provided by ae2wtlib's
 * terminal registration system with full upgrade panel and terminal cycling.
 * This class exists as a fallback and to satisfy NeoForge's class scanning
 * without requiring ae2wtlib at class-load time.
 */
public class WirelessProbabilityPatternTerminalScreen
        extends ProbabilityPatternTerminalScreen<WirelessProbabilityPatternTerminalMenu> {

    public WirelessProbabilityPatternTerminalScreen(WirelessProbabilityPatternTerminalMenu menu,
            Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
