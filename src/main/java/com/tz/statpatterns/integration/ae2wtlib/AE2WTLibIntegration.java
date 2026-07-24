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
package com.tz.statpatterns.integration.ae2wtlib;

import appeng.api.upgrades.Upgrades;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.core.definition.SPItems;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenuHost;

import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

/**
 * Integration with AE2 Wireless Terminal Library (ae2wtlib).
 * <p>
 * This class handles optional integration when ae2wtlib is installed:
 * <ul>
 *   <li>Registers the terminal with ae2wtlib's Universal Terminal system</li>
 *   <li>Registers Quantum Bridge Card support (infinite range ME network access)</li>
 *   <li>Registers Magnet Card support (automatic item pickup)</li>
 * </ul>
 * <p>
 * All methods in this class are safe to call even if ae2wtlib is not installed,
 * but the actual registration only happens when the mod is present.
 */
public final class AE2WTLibIntegration {
    private static final String AE2WTLIB_MOD_ID = "ae2wtlib";
    private static final ResourceLocation QUANTUM_BRIDGE_CARD_ID = ResourceLocation.fromNamespaceAndPath(AE2WTLIB_MOD_ID, "quantum_bridge_card");
    private static final ResourceLocation MAGNET_CARD_ID = ResourceLocation.fromNamespaceAndPath(AE2WTLIB_MOD_ID, "magnet_card");

    // Lazy-initialized to avoid loading ae2wtlib classes when the mod is absent
    private static Icon probabilityPatternIcon;

    private static Icon getProbabilityPatternIcon() {
        if (probabilityPatternIcon == null) {
            probabilityPatternIcon = new Icon(
                    0, 0, 16, 16,
                    new Icon.Texture(ResourceLocation.fromNamespaceAndPath("probabilitypattern", "textures/gui/icons.png"), 16, 16)
            );
        }
        return probabilityPatternIcon;
    }

    private AE2WTLibIntegration() {
    }

    /**
     * Check if AE2WTLib is installed.
     */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(AE2WTLIB_MOD_ID);
    }

    /**
     * Register the probability pattern terminal with ae2wtlib's Universal Terminal system.
     * <p>
     * This should be called during mod initialization (FMLCommonSetupEvent).
     * Only registers if ae2wtlib is actually installed.
     */
    public static void registerTerminal() {
        if (!isLoaded()) {
            return;
        }

        var menuType = SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL_TYPE;
        var terminalItem = SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
        if (menuType == null || terminalItem == null) {
            return;
        }

        AddTerminalEvent.register(event -> {
            event.builder(
                    "probability_pattern",
                    ProbabilityPatternTerminalMenuHost::new,
                    menuType,
                    terminalItem.asItem(),
                    getProbabilityPatternIcon()
            ).addTerminal();
        });
    }

    /**
     * Register ae2wtlib upgrade cards for the portable probability pattern terminal.
     * <p>
     * This should be called during mod initialization (FMLCommonSetupEvent).
     * Only registers upgrades if ae2wtlib is actually installed.
     */
    public static void registerUpgrades() {
        if (!isLoaded()) {
            return;
        }

        var terminalItem = SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
        if (terminalItem == null) {
            return;
        }

        // Quantum Bridge Card: allows infinite range access to ME network
        // Max 1 card (same as ae2wtlib's own terminals)
        var quantumBridgeCard = getItem(QUANTUM_BRIDGE_CARD_ID);
        if (quantumBridgeCard != null) {
            Upgrades.add(quantumBridgeCard, terminalItem, 1);
        }

        // Magnet Card: automatically picks up nearby items
        // Max 1 card
        var magnetCard = getItem(MAGNET_CARD_ID);
        if (magnetCard != null) {
            Upgrades.add(magnetCard, terminalItem, 1);
        }
    }

    private static Item getItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }
}
