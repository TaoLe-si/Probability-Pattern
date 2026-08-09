/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
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
package com.zincglux.statpatterns;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.zincglux.statpatterns.crafting.ProbabilityPatternItem;
import com.zincglux.statpatterns.handler.ProbabilityPatternGuiHandler;
import com.zincglux.statpatterns.item.ItemProbabilityPatternTerminal;
import com.zincglux.statpatterns.network.ProbabilityPatternNetwork;

import appeng.api.AEApi;
import appeng.client.gui.implementations.GuiProbabilityPatternTerm;
import appeng.integration.modules.NEIHelpers.NEIAETerminalBookmarkContainerHandler;
import appeng.integration.modules.NEIHelpers.NEICraftingHandler;
import appeng.integration.modules.NEIHelpers.TerminalCraftingSlotFinder;
import codechicken.nei.api.API;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Probability Pattern for AE2 — 1.7.10 port (Spec v2.0 1.1).
 * <p>
 * Adds probabilistic (statistical) crafting patterns to Applied Energistics 2.
 * A probability pattern encodes per-attempt inputs, a target output and a single
 * attempt success probability p. When the ME crafting tree requests N output items,
 * the crafting interception mixin makes it run the machine enough times so that
 * P(total_produced >= N) >= 1 - alpha.
 */
@Mod(
    modid = ProbabilityPatternMod.MOD_ID,
    name = ProbabilityPatternMod.MOD_NAME,
    version = ProbabilityPatternMod.MOD_VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = ProbabilityPatternMod.DEPENDENCIES)
public final class ProbabilityPatternMod {

    public static final String MOD_ID = "statpatterns";
    public static final String MOD_NAME = "AE2 Probability Pattern";
    public static final String MOD_VERSION = "@version@";
    public static final String DEPENDENCIES = "required-after:appliedenergistics2;required-after:Forge@[10.13.4.1448,)";

    @Instance(MOD_ID)
    public static ProbabilityPatternMod instance;

    public static Item probabilityPatternItem;
    public static Item probabilityPatternTerminalItem;
    public static CreativeTabs creativeTab;

    /** NEI overlay idents already registered for our terminal (client only). */
    @SideOnly(Side.CLIENT)
    private final Set<String> registeredOverlayIdents = new HashSet<>();

    @EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        creativeTab = new CreativeTabs("statpatterns") {

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
    }

    @EventHandler
    public void postInit(final FMLPostInitializationEvent event) {
        // NEI's RecipeInfo overlay maps are keyed by EXACT GUI class (HashMap lookup, not
        // instanceof), so our GuiProbabilityPatternTerm — a GuiPatternTerm subclass — must
        // be registered on its own for the NEI "?" transfer button to recognise it.
        // postInit guarantees AE2's NEI module has already registered the vanilla classes.
        if (event.getSide()
            .isClient()) {
            this.registerNEI();
        }
    }

    @SideOnly(Side.CLIENT)
    private void registerNEI() {
        if (!Loader.isModLoaded("NotEnoughItems")) {
            return;
        }
        try {
            // NEI's overlay maps are keyed by EXACT GUI class (HashMap lookup, not
            // instanceof), so our GuiProbabilityPatternTerm — a GuiPatternTerm subclass —
            // must be registered on its own for the NEI "?" transfer button to recognise it.
            API.registerGuiOverlay(GuiProbabilityPatternTerm.class, "crafting", new TerminalCraftingSlotFinder());
            API.registerGuiOverlayHandler(GuiProbabilityPatternTerm.class, new NEICraftingHandler(6, 75), "crafting");
            API.registerBookmarkContainerHandler(
                GuiProbabilityPatternTerm.class,
                new NEIAETerminalBookmarkContainerHandler());
            // NEI keeps a SECOND, independent mechanism for recipe transfer: the
            // RecipeTransferRectHandler.guiMap (exact GUI class -> transfer rects). The
            // RecipeInfo.overlayMap registration above only powers the "?" overlay button;
            // without guiMap the terminal's canHandle() stays false and recipes cannot be
            // filled (NEI falls back to treating the 3x3 grid like a vanilla workbench).
            TemplateRecipeHandler.RecipeTransferRectHandler.registerRectsToGuis(
                Arrays.asList(GuiProbabilityPatternTerm.class),
                Arrays.asList(new TemplateRecipeHandler.RecipeTransferRect(new Rectangle(84, 23, 24, 18), "crafting")));
            this.registerNEEFill();
        } catch (final Throwable t) {
            // Never crash the client; log the failure so it can be diagnosed.
            FMLLog.warning("[statpatterns] Failed to register NEI integration: %s", String.valueOf(t));
        }
    }

    /**
     * Fill recipes by delegating to NEE (NotEnoughEnergistics)'s own pattern-terminal fill
     * handler {@code NEEPatternTerminalHandler.instance}, registered on our
     * {@link GuiProbabilityPatternTerm} for every recipe ident.
     * <p>
     * NEE is the reference implementation for this feature: it bypasses AE's native
     * validation (no items needed), auto-distinguishes crafting/processing, resolves inputs
     * and outputs through per-mod {@code IRecipeProcessor}s (GT/TE/EnderIO/...), fills all
     * inputs and up to 3 outputs, and its server handler writes straight into the container
     * ("crafting"/"output" inventories). NEE is an optional dependency, so the handler is
     * fetched reflectively.
     * <p>
     * Idempotent; safe to call again later (e.g. every time the terminal GUI opens) to pick
     * up mods that registered their NEI handlers after our postInit.
     */
    @SideOnly(Side.CLIENT)
    public void registerNEEFill() {
        if (!Loader.isModLoaded("neenergistics")) {
            return; // NEE not installed — no processing-recipe fill
        }
        try {
            final Class<?> neeClz = Class.forName("com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler");
            final IOverlayHandler neeHandler = (IOverlayHandler) neeClz.getField("instance")
                .get(null);
            if (neeHandler == null) {
                FMLLog.warning("[statpatterns] NEEPatternTerminalHandler.instance is null");
                return;
            }

            final Set<String> idents = new LinkedHashSet<>();
            // Default NEI idents (crafting included - NEE handles it too) + every registered
            // recipe handler's ident (GT/TE/EnderIO/...) + GT recipe categories.
            idents.add("crafting");
            idents.add("crafting2x2");
            idents.add("smelting");
            idents.add("fuel");
            idents.add("brewing");
            try {
                this.collectOverlayIdents(GuiCraftingRecipe.craftinghandlers, idents);
                this.collectOverlayIdents(GuiCraftingRecipe.serialCraftingHandlers, idents);
            } catch (final Throwable t) {
                FMLLog.warning("[statpatterns] collecting NEI handler idents failed: %s", String.valueOf(t));
            }
            try {
                this.collectGTRecipeCategoryIdents(idents);
            } catch (final Throwable t) {
                FMLLog.warning("[statpatterns] collecting GT recipe category idents failed: %s", String.valueOf(t));
            }

            for (final String ident : idents) {
                if (ident == null || ident.isEmpty()) {
                    continue;
                }
                if (!this.registeredOverlayIdents.add(ident)) {
                    continue;
                }
                API.registerGuiOverlay(GuiProbabilityPatternTerm.class, ident);
                API.registerGuiOverlayHandler(GuiProbabilityPatternTerm.class, neeHandler, ident);
            }
        } catch (final Throwable t) {
            FMLLog.warning("[statpatterns] registerNEEFill failed: %s", String.valueOf(t));
        }
    }

    @SideOnly(Side.CLIENT)
    private void collectOverlayIdents(final List<ICraftingHandler> handlers, final Set<String> out) {
        if (handlers == null) {
            return;
        }
        for (final ICraftingHandler h : handlers) {
            if (h instanceof TemplateRecipeHandler) {
                final String ident = ((TemplateRecipeHandler) h).getOverlayIdentifier();
                if (ident != null && !ident.isEmpty()) {
                    out.add(ident);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void collectGTRecipeCategoryIdents(final Set<String> out) throws Exception {
        final Class<?> rcClass = Class.forName("gregtech.api.recipe.RecipeCategory");
        final java.lang.reflect.Field allField = rcClass.getDeclaredField("ALL_RECIPE_CATEGORIES");
        final java.util.Map<?, ?> all = (java.util.Map<?, ?>) allField.get(null);
        for (final Object category : all.values()) {
            final java.lang.reflect.Field nameField = category.getClass()
                .getDeclaredField("unlocalizedName");
            nameField.setAccessible(true);
            final String ident = (String) nameField.get(category);
            if (ident != null && !ident.isEmpty()) {
                out.add(ident);
            }
        }
    }
}
