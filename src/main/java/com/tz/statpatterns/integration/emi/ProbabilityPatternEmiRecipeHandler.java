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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.SlotSemantics;

import com.tz.statpatterns.terminal.StatPatternsTerminalMenu;

import com.tz.statpatterns.util.StatPatternsExtractor;

/**
 * Lets EMI recipes be pulled into the probability pattern terminal.
 * <p>
 * This is the EMI counterpart of the JEI {@code PatternTerminalTransferHandler}:
 * clicking the fill button on an EMI recipe collects the recipe's inputs/outputs,
 * extracts a success probability (when the recipe exposes one) and encodes a
 * processing pattern. Drag-and-drop of individual stacks is already provided by
 * AE2's generic EMI drag-drop handler, so it is not repeated here.
 */
public class ProbabilityPatternEmiRecipeHandler<T extends StatPatternsTerminalMenu>
        implements StandardRecipeHandler<T> {

    private final Class<T> containerClass;

    public ProbabilityPatternEmiRecipeHandler(Class<T> containerClass) {
        this.containerClass = containerClass;
    }

    @Override
    public List<Slot> getInputSources(T menu) {
        var slots = new ArrayList<Slot>();
        slots.addAll(menu.getSlots(SlotSemantics.PLAYER_INVENTORY));
        slots.addAll(menu.getSlots(SlotSemantics.PLAYER_HOTBAR));
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(T menu) {
        // Pattern encoding reads inputs directly from the recipe — no grid fill.
        return List.of();
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return true;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return context.getType() == EmiCraftContext.Type.FILL_BUTTON;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (context.getType() != EmiCraftContext.Type.FILL_BUTTON
                || !containerClass.isInstance(context.getScreenHandler())) {
            return false;
        }
        T menu = containerClass.cast(context.getScreenHandler());

        var holder = recipe.getBackingRecipe();
        var vanillaRecipe = holder != null ? holder.value() : null;

        // Auto-select the pattern type just like vanilla AE2's EmiEncodePatternHandler:
        // crafting recipes that fit the 3x3 grid become crafting patterns (CRAFTING /
        // STONECUTTING / SMITHING_TABLE mode), everything else becomes a processing
        // (probability) pattern.
        boolean craftingRecipe = isCraftingRecipe(vanillaRecipe, recipe);
        if (craftingRecipe && !fitsIn3x3Grid(vanillaRecipe)) {
            return false; // too large for the 3x3 crafting grid
        }

        if (craftingRecipe && holder != null) {
            EncodingHelper.encodeCraftingRecipe(menu, holder, collectInputs(recipe), stack -> true);
        } else {
            var inputs = collectInputs(recipe);
            var outputs = collectOutputs(recipe);
            if (inputs.isEmpty() || outputs.isEmpty()) {
                return false;
            }
            StatPatternsExtractor.extract(recipe.getBackingRecipe()).ifPresent(menu::setProbability);
            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        }

        menu.encode();
        return true;
    }

    private static boolean isCraftingRecipe(@Nullable Recipe<?> recipe, EmiRecipe emiRecipe) {
        return EncodingHelper.isSupportedCraftingRecipe(recipe)
                || emiRecipe.getCategory().equals(VanillaEmiRecipeCategories.CRAFTING);
    }

    private static boolean fitsIn3x3Grid(@Nullable Recipe<?> recipe) {
        return recipe == null || recipe.canCraftInDimensions(3, 3);
    }

    private static List<List<GenericStack>> collectInputs(EmiRecipe recipe) {
        var result = new ArrayList<List<GenericStack>>();
        for (EmiIngredient ingredient : recipe.getInputs()) {
            var alternatives = new ArrayList<GenericStack>();
            for (var emiStack : ingredient.getEmiStacks()) {
                var generic = toGenericStack(emiStack);
                if (generic != null) {
                    long amount = ingredient.getAmount();
                    alternatives.add(amount > 0 ? new GenericStack(generic.what(), amount) : generic);
                }
            }
            if (!alternatives.isEmpty()) {
                result.add(alternatives);
            }
        }
        return result;
    }

    private static List<GenericStack> collectOutputs(EmiRecipe recipe) {
        var result = new ArrayList<GenericStack>();
        for (var emiStack : recipe.getOutputs()) {
            var generic = toGenericStack(emiStack);
            if (generic != null) {
                result.add(generic);
            }
        }
        return result;
    }

    /**
     * Converts an EMI stack (either an item or a fluid) into an AE2
     * {@link GenericStack}. Fluids are converted through {@link AEFluidKey}.
     */
    @Nullable
    private static GenericStack toGenericStack(EmiStack emiStack) {
        // Items
        var itemStack = emiStack.getItemStack();
        if (!itemStack.isEmpty()) {
            return GenericStack.fromItemStack(itemStack);
        }
        // Fluids
        var fluid = emiStack.getKeyOfType(Fluid.class);
        if (fluid != null && fluid != Fluids.EMPTY) {
            var fluidStack = new FluidStack(fluid.builtInRegistryHolder(), 1, emiStack.getComponentChanges());
            var key = AEFluidKey.of(fluidStack);
            return new GenericStack(key, emiStack.getAmount());
        }
        return null;
    }
}
