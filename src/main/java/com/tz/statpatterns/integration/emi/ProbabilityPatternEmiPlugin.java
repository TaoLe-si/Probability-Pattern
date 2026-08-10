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
package com.tz.statpatterns.integration.emi;

import com.tz.statpatterns.core.definition.StatPatternsMenus;
import com.tz.statpatterns.terminal.StatPatternsTerminalMenu;
import com.tz.statpatterns.terminal.WirelessStatPatternsTerminalMenu;

import net.neoforged.fml.ModList;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * EMI integration for the probability pattern terminal.
 * <p>
 * Registers a recipe handler so players can pull any EMI recipe into the terminal:
 * inputs/outputs are filled from the recipe, a success probability is extracted
 * (when the recipe exposes one) and a processing pattern is encoded.
 * <p>
 * This class is only loaded by EMI's entrypoint discovery when EMI is installed,
 * so it is safe to ship without EMI present.
 */
@EmiEntrypoint
public class ProbabilityPatternEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(StatPatternsMenus.PROBABILITY_PATTERN_TERMINAL.get(),
                new ProbabilityPatternEmiRecipeHandler<>(StatPatternsTerminalMenu.class));

        // Wireless variant — only available when ae2wtlib is present
        if (ModList.get().isLoaded("ae2wtlib")) {
            var wirelessType = StatPatternsMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL.get();
            if (wirelessType != null) {
                registry.addRecipeHandler(wirelessType,
                        new ProbabilityPatternEmiRecipeHandler<>(WirelessStatPatternsTerminalMenu.class));
            }
        }
    }
}
