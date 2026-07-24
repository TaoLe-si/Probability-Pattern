package com.tz.statpatterns.integration.ae2wtlib;

import com.tz.statpatterns.core.definition.SPItems;

import de.mari_023.ae2wtlib.UpgradeHelper;

import net.minecraftforge.fml.ModList;

/**
 * Integration with AE2 Wireless Terminal Library (ae2wtlib) for 1.20.1 Forge.
 * Follows WCWT pattern: uses GridLinkables + Upgrades, no WUTHandler.
 */
public final class AE2WTLibIntegration {
    private static final String AE2WTLIB_MOD_ID = "ae2wtlib";

    private AE2WTLibIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(AE2WTLIB_MOD_ID);
    }

    public static void registerUpgrades() {
        if (!isLoaded()) {
            return;
        }
        var terminalDef = SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
        if (terminalDef == null) {
            return;
        }
        UpgradeHelper.addUpgradeToAllTerminals(terminalDef.stack().getItem(), 0);
    }
}
