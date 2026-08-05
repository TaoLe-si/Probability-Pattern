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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.SlotSemantics;

import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;

/**
 * Lets EMI recipes be pulled into the probability pattern terminal.
 * <p>
 * This is the EMI counterpart of the JEI {@code PatternTerminalTransferHandler}:
 * clicking the fill button on an EMI recipe collects the recipe's inputs/outputs,
 * extracts a success probability (when the recipe exposes one) and encodes a
 * processing pattern. Drag-and-drop of individual stacks is already provided by
 * AE2's generic EMI drag-drop handler, so it is not repeated here.
 */
public class ProbabilityPatternEmiRecipeHandler<T extends ProbabilityPatternTerminalMenu>
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

        var backing = recipe.getBackingRecipe();
        var vanillaRecipe = resolveRecipe(backing);

        // Auto-select the pattern type just like vanilla AE2's EmiEncodePatternHandler:
        // crafting recipes that fit the 3x3 grid become crafting patterns (CRAFTING /
        // STONECUTTING / SMITHING_TABLE mode), everything else becomes a processing
        // (probability) pattern.
        boolean craftingRecipe = isCraftingRecipe(vanillaRecipe, recipe);
        if (craftingRecipe && !fitsIn3x3Grid(vanillaRecipe)) {
            return false; // too large for the 3x3 crafting grid
        }

        if (craftingRecipe && vanillaRecipe != null) {
            EncodingHelper.encodeCraftingRecipe(menu, vanillaRecipe, collectInputs(recipe), stack -> true);
        } else {
            var inputs = collectInputs(recipe);
            var outputs = collectOutputs(recipe);
            if (inputs.isEmpty() || outputs.isEmpty()) {
                return false;
            }
            extractProbability(backing).ifPresent(menu::setProbability);
            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        }

        menu.encode();
        return true;
    }

    @Nullable
    private static Recipe<?> resolveRecipe(@Nullable Object backing) {
        if (backing instanceof Recipe<?> r) {
            return r;
        }
        // For newer EMI versions that wrap in RecipeHolder
        try {
            if (backing != null) {
                var valueMethod = backing.getClass().getMethod("value");
                var value = valueMethod.invoke(backing);
                if (value instanceof Recipe<?> r) {
                    return r;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
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
                var stack = emiStack.getItemStack();
                if (!stack.isEmpty()) {
                    var generic = GenericStack.fromItemStack(stack);
                    if (generic != null) {
                        long amount = ingredient.getAmount();
                        alternatives.add(amount > 0 ? new GenericStack(generic.what(), amount) : generic);
                    }
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
            var stack = emiStack.getItemStack();
            if (!stack.isEmpty()) {
                var generic = GenericStack.fromItemStack(stack);
                if (generic != null) {
                    result.add(generic);
                }
            }
        }
        return result;
    }

    private static Optional<Double> extractProbability(@Nullable Object backing) {
        return extractProbability(backing, 0);
    }

    private static Optional<Double> extractProbability(Object value, int depth) {
        if (value == null || depth > 2) {
            return Optional.empty();
        }
        // Handle RecipeHolder-like wrapper from newer EMI versions
        try {
            if (value.getClass().getSimpleName().contains("Holder")) {
                var valueMethod = value.getClass().getMethod("value");
                return extractProbability(valueMethod.invoke(value), depth + 1);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        if (value instanceof Recipe<?> recipe) {
            return extractProbability(recipe, depth + 1);
        }
        if (value instanceof Number number) {
            return normalizeProbability(number.doubleValue());
        }

        for (var methodName : List.of("successProbability", "getSuccessProbability", "probability",
                "getProbability", "chance", "getChance")) {
            try {
                Method method = value.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0 && Number.class.isAssignableFrom(wrap(method.getReturnType()))) {
                    return normalizeProbability(((Number) method.invoke(value)).doubleValue());
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        for (var field : value.getClass().getDeclaredFields()) {
            var name = field.getName().toLowerCase(Locale.ROOT);
            if (name.contains("probability") || name.contains("chance")) {
                var found = readProbabilityField(value, field, depth);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Double> readProbabilityField(Object owner, Field field, int depth) {
        try {
            field.setAccessible(true);
            var fieldValue = field.get(owner);
            return extractProbability(fieldValue, depth + 1);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Double> normalizeProbability(double probability) {
        if (probability > 1.0 && probability <= 100.0) {
            probability /= 100.0;
        }
        if (probability > 0.0 && probability <= 1.0) {
            return Optional.of(probability);
        }
        return Optional.empty();
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        return type;
    }
}
