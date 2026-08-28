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
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import com.tz.statpatterns.core.definition.StatPatternsItems;
import com.tz.statpatterns.core.definition.StatPatternsMenus;
import com.tz.statpatterns.item.StatPatternsTerminalItem;
import com.tz.statpatterns.terminal.StatPatternsTerminalMenuHost;

import de.mari_023.ae2wtlib.wut.WTDefinition;
import de.mari_023.ae2wtlib.wut.WUTHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Actual ae2wtlib integration code. Every method here references ae2wtlib
 * types, so this class must ONLY be linked after {@link AE2WTLibIntegration#isLoaded()}
 * has confirmed that ae2wtlib is installed - otherwise verification of this
 * class resolves the missing ae2wtlib classes and crashes the game with
 * {@code NoClassDefFoundError}. Never call into this class directly from
 * mod setup code; go through the {@link AE2WTLibIntegration} facade instead.
 */
final class AE2WTLibCompat {
    static final String AE2WTLIB_MOD_ID = "ae2wtlib";
    private static final ResourceLocation QUANTUM_BRIDGE_CARD_ID =
            new ResourceLocation(AE2WTLIB_MOD_ID, "quantum_bridge_card");
    private static final ResourceLocation MAGNET_CARD_ID =
            new ResourceLocation(AE2WTLIB_MOD_ID, "magnet_card");
    private static final ResourceLocation WUT_ITEM_ID =
            new ResourceLocation(AE2WTLIB_MOD_ID, "wireless_universal_terminal");

    /** Translation key used for this terminal's display name inside a WUT. */
    static final String TERMINAL_NAME_TRANSLATION_KEY =
            "item.statpatterns.wireless_stat_pattern_terminal";

    private AE2WTLibCompat() {}

    /**
     * Register ae2wtlib upgrade cards FOR the PP wireless terminal.
     */
    static void registerUpgrades() {
        var terminalDef = StatPatternsItems.WIRELESS_STAT_PATTERN_TERMINAL;
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

    /**
     * Register the wireless stat pattern terminal with ae2wtlib's WUT (Wireless
     * Universal Terminal) system so it can be merged into a WUT together with
     * other wireless terminals and opened through it.
     * <p>
     * Called from FMLCommonSetupEvent. {@code WUTHandler.addTerminal()} internally
     * registers an AE2 hotkey, which throws on 1.20.1 once AE2 has finalized hotkey
     * registration (that happens before FMLCommonSetupEvent on the client). We
     * therefore try the proper API first and, if it throws, fall back to registering
     * the {@link WTDefinition} directly into {@link WUTHandler}'s maps (no hotkey,
     * but the terminal still works inside a WUT). WUT merge is driven by the
     * {@code ae2wtlib:upgrade} recipe shipped in this mod's data.
     */
    static void registerWithWUT() {
        var terminalDef = StatPatternsItems.WIRELESS_STAT_PATTERN_TERMINAL;
        if (terminalDef == null) return;

        var item = terminalDef.stack().getItem();
        if (!(item instanceof StatPatternsTerminalItem terminalItem)) return;

        String name = "stat_pattern";
        try {
            WUTHandler.addTerminal(
                    name,
                    terminalItem::tryOpen,
                    StatPatternsTerminalMenuHost::new,
                    StatPatternsMenus.WIRELESS_STAT_PATTERN_TERMINAL,
                    terminalItem,
                    TERMINAL_NAME_TRANSLATION_KEY);
        } catch (Throwable t) {
            // On 1.20.1 AE2 finalizes hotkey registration before FMLCommonSetupEvent,
            // so WUTHandler.addTerminal() throws. Register directly instead.
            AELog.warn("WUTHandler.addTerminal failed (%s) - falling back to direct WUT registration", t.getMessage());
            registerWithWUTDirect(name, terminalItem);
        }
    }

    private static void registerWithWUTDirect(String name, StatPatternsTerminalItem terminalItem) {
        if (WUTHandler.terminalNames.contains(name)) {
            return; // already registered
        }
        // Look the universal terminal up by registry id to avoid referencing
        // ItemWUT directly (its type hierarchy pulls in Curios at compile time).
        Item wutItem = ForgeRegistries.ITEMS.getValue(WUT_ITEM_ID);
        if (wutItem == null || wutItem == Items.AIR) {
            AELog.warn("ae2wtlib UNIVERSAL_TERMINAL not ready; skipping direct WUT registration");
            return;
        }

        // Mirror the universal-terminal template WUTHandler.addTerminal() builds.
        ItemStack wutTemplate = new ItemStack(wutItem);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(name, true);
        wutTemplate.setTag(tag);

        WTDefinition definition = new WTDefinition(
                terminalItem::tryOpen,
                StatPatternsTerminalMenuHost::new,
                StatPatternsMenus.WIRELESS_STAT_PATTERN_TERMINAL,
                terminalItem,
                wutTemplate,
                Component.translatable(TERMINAL_NAME_TRANSLATION_KEY));

        WUTHandler.wirelessTerminals.put(name, definition);
        WUTHandler.terminalNames.add(name);
    }

    private static Item getItem(ResourceLocation id) {
        var item = ForgeRegistries.ITEMS.getValue(id);
        return (item != null && item != Items.AIR) ? item : null;
    }
}
