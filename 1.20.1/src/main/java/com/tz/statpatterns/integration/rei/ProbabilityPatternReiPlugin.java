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
package com.tz.statpatterns.integration.rei;

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;

import net.minecraftforge.fml.ModList;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * REI (Roughly Enough Items) integration for the probability pattern terminal.
 * <p>
 * Registers a transfer handler so players can pull any REI recipe into the
 * terminal: inputs/outputs are filled from the recipe, a success probability is
 * extracted (when the recipe exposes one) and a processing pattern is encoded.
 * <p>
 * This class is only loaded by REI's plugin discovery when REI is installed, so
 * it is safe to ship without REI present. Drag-and-drop of individual stacks is
 * already provided by AE2's own REI {@code GhostIngredientHandler}, so it is not
 * repeated here.
 */
@REIPluginClient
public class ProbabilityPatternReiPlugin implements REIClientPlugin {

    @Override
    public String getPluginProviderName() {
        return "Probability Pattern";
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new ProbabilityPatternTransferHandler<>(ProbabilityPatternTerminalMenu.class));

        // Wireless variant — only available when ae2wtlib is present
        if (ModList.get().isLoaded("ae2wtlib")) {
            var wirelessType = SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
            if (wirelessType != null) {
                registry.register(new ProbabilityPatternTransferHandler<>(WirelessProbabilityPatternTerminalMenu.class));
            }
        }
    }
}
