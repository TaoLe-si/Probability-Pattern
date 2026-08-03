package com.tz.statpatterns.integration.ae2wtlib;

import appeng.api.upgrades.Upgrades;
import appeng.core.localization.GuiText;
import com.tz.statpatterns.core.definition.SPItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

/**
 * Integration with AE2 Wireless Terminal Library (ae2wtlib) for 1.20.1 Forge.
 */
public final class AE2WTLibIntegration {
    private static final String AE2WTLIB_MOD_ID = "ae2wtlib";
    private static final ResourceLocation QUANTUM_BRIDGE_CARD_ID =
            new ResourceLocation(AE2WTLIB_MOD_ID, "quantum_bridge_card");
    private static final ResourceLocation MAGNET_CARD_ID =
            new ResourceLocation(AE2WTLIB_MOD_ID, "magnet_card");

    private AE2WTLibIntegration() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(AE2WTLIB_MOD_ID);
    }

    /**
     * Register ae2wtlib upgrade cards FOR the PP wireless terminal.
     */
    public static void registerUpgrades() {
        if (!isLoaded()) return;

        var terminalDef = SPItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
        if (terminalDef == null) return;

        var terminal = terminalDef.stack().getItem();
        String groupKey = GuiText.WirelessTerminals.getTranslationKey();

        var quantumBridgeCard = getItem(QUANTUM_BRIDGE_CARD_ID);
        if (quantumBridgeCard != null) {
            Upgrades.add(quantumBridgeCard, terminal, 1, groupKey);
        }

        var magnetCard = getItem(MAGNET_CARD_ID);
        if (magnetCard != null) {
            Upgrades.add(magnetCard, terminal, 1, groupKey);
        }
    }

    private static Item getItem(ResourceLocation id) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
        return (item != null && item != Items.AIR) ? item : null;
    }
}
