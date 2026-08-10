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

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.BlockHitResult;

import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.locator.ItemMenuHostLocator;

import com.tz.statpatterns.core.definition.StatPatternsMenus;
import com.tz.statpatterns.terminal.StatPatternsTerminalMenuHost;

import de.mari_023.ae2wtlib.api.terminal.ItemWT;

/**
 * Handheld probability pattern terminal item.
 * Extends ItemWT for ae2wtlib Universal Terminal compatibility.
 */
public class StatPatternsTerminalItem extends ItemWT {

    public StatPatternsTerminalItem() {
        super();
    }

    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player) {
        return StatPatternsMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL.get();
    }

    @Override
    public WirelessTerminalMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator locator,
            @Nullable BlockHitResult hitResult) {
        return new StatPatternsTerminalMenuHost(this, player, locator,
                (p, sm) -> openFromInventory(p, locator, true));
    }
}