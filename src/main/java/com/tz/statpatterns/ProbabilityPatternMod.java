/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import com.tz.statpatterns.handler.ProbabilityPatternGuiHandler;
import com.tz.statpatterns.item.ItemProbabilityPatternTerminal;
import com.tz.statpatterns.network.ProbabilityPatternNetwork;

import appeng.api.AEApi;
import appeng.client.gui.implementations.GuiProbabilityPatternTerm;
import appeng.integration.modules.NEIHelpers.NEICraftingHandler;
import appeng.integration.modules.NEIHelpers.TerminalCraftingSlotFinder;
import codechicken.nei.api.API;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Probability Pattern for AE2 — 1.7.10 port.
 * <p>
 * Adds probabilistic (statistical) crafting patterns to Applied Energistics 2.
 * A probability pattern encodes per-attempt inputs, a target output and a single
 * attempt success probability p. When the ME crafting tree requests N output items,
 * the crafting interception coremod makes it run the machine enough times so that
 * P(total_produced >= N) >= 1 - alpha.
 */
@Mod(
    modid = ProbabilityPatternMod.MOD_ID,
    name = ProbabilityPatternMod.MOD_NAME,
    version = ProbabilityPatternMod.MOD_VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = ProbabilityPatternMod.DEPENDENCIES)
public final class ProbabilityPatternMod {

    public static final String MOD_ID = "probabilitypattern";
    public static final String MOD_NAME = "Probability Pattern for AE2";
    public static final String MOD_VERSION = "@version@";
    public static final String DEPENDENCIES = "required-after:appliedenergistics2;required-after:Forge@[10.13.4.1448,)";

    @Instance(MOD_ID)
    public static ProbabilityPatternMod instance;

    public static Item probabilityPatternItem;
    public static Item probabilityPatternTerminalItem;
    public static CreativeTabs creativeTab;

    @EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        creativeTab = new CreativeTabs("probabilitypattern") {

            @Override
            public Item getTabIconItem() {
                return probabilityPatternTerminalItem;
            }
        };

        probabilityPatternItem = new ProbabilityPatternItem().setCreativeTab(creativeTab);
        GameRegistry.registerItem(probabilityPatternItem, "probability_pattern");

        probabilityPatternTerminalItem = new ItemProbabilityPatternTerminal().setCreativeTab(creativeTab);
        GameRegistry.registerItem(probabilityPatternTerminalItem, "probability_pattern_terminal");

        ProbabilityPatternNetwork.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ProbabilityPatternGuiHandler());
    }

    @EventHandler
    public void init(final FMLInitializationEvent event) {
        final ItemStack patternTerminal = AEApi.instance()
            .definitions()
            .parts()
            .patternTerminal()
            .maybeStack(1)
            .orNull();
        final ItemStack calcProcessor = AEApi.instance()
            .definitions()
            .materials()
            .calcProcessor()
            .maybeStack(1)
            .orNull();

        if (patternTerminal != null && calcProcessor != null) {
            GameRegistry.addRecipe(
                new ItemStack(probabilityPatternTerminalItem),
                "PC",
                'P',
                patternTerminal,
                'C',
                calcProcessor);
        }

        // Encoding consumes the vanilla AE2 blank pattern directly (1.7.10 GTNH behaviour);
        // there is intentionally no separate "blank probability pattern" crafting recipe.
        // The ProbabilityPatternItem's blank (no-NBT) form is only available from the
        // creative tab / via the terminal mechanics.

        if (event.getSide()
            .isClient()) {
            this.registerNEI();
        }
    }

    /**
     * Register NEI integration (client-only): reuse AE2 GTNH's {@link NEICraftingHandler}
     * for the "?" recipe-transfer button. Because {@link GuiProbabilityPatternTerm}
     * extends {@code GuiPatternTerm}, NEICraftingHandler recognises it and transfers the
     * recipe through {@code PacketNEIRecipe} -> {@code IContainerCraftingPacket} exactly
     * like the vanilla processing pattern terminal.
     */
    @SideOnly(Side.CLIENT)
    private void registerNEI() {
        try {
            if (Loader.isModLoaded("NotEnoughItems")) {
                API.registerGuiOverlay(GuiProbabilityPatternTerm.class, "crafting", new TerminalCraftingSlotFinder());
                API.registerGuiOverlayHandler(
                    GuiProbabilityPatternTerm.class,
                    new NEICraftingHandler(0, 0),
                    "crafting");
            }
        } catch (final Throwable ignored) {
            // NEI not present / optional dependency — never crash.
        }
    }
}
