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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;

import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;

/**
 * Lets REI recipes be pulled into the probability pattern terminal.
 * <p>
 * This is the REI counterpart of the JEI {@code PatternTerminalTransferHandler}
 * and the EMI {@code ProbabilityPatternEmiRecipeHandler}. Like vanilla AE2 it
 * auto-selects the pattern type: crafting recipes that fit the 3x3 grid become
 * crafting patterns, everything else becomes a processing (probability) pattern.
 */
public class ProbabilityPatternTransferHandler<T extends ProbabilityPatternTerminalMenu>
        implements TransferHandler {

    private static final CategoryIdentifier<?> CRAFTING = CategoryIdentifier.of("minecraft", "plugins/crafting");

    private final Class<T> containerClass;

    public ProbabilityPatternTransferHandler(Class<T> containerClass) {
        this.containerClass = containerClass;
    }

    @Override
    public Result handle(Context context) {
        if (!containerClass.isInstance(context.getMenu())) {
            return Result.createNotApplicable();
        }
        T menu = containerClass.cast(context.getMenu());
        var display = context.getDisplay();

        var holder = getRecipeHolder(display);
        var vanillaRecipe = holder != null ? holder.value() : null;

        var inputs = ofInputs(display);
        var outputs = ofOutputs(display);
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return Result.createNotApplicable();
        }

        // Auto-select the pattern type just like vanilla AE2: crafting recipes that
        // fit the 3x3 grid become crafting patterns, everything else becomes a
        // processing (probability) pattern.
        boolean craftingRecipe = isCraftingRecipe(vanillaRecipe, display);
        if (craftingRecipe && !fitsIn3x3Grid(vanillaRecipe, display)) {
            return Result.createNotApplicable(); // too large for the 3x3 crafting grid
        }

        if (context.isActuallyCrafting()) {
            if (craftingRecipe && holder != null) {
                EncodingHelper.encodeCraftingRecipe(menu, holder, getGuiIngredientsForCrafting(display), stack -> true);
            } else {
                extractProbability(display).ifPresent(menu::setProbability);
                EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
            }
            menu.encode();
        }
        return Result.createSuccessful();
    }

    private static boolean isCraftingRecipe(@Nullable Recipe<?> recipe, Display display) {
        return EncodingHelper.isSupportedCraftingRecipe(recipe)
                || display.getCategoryIdentifier().equals(CRAFTING);
    }

    private static boolean fitsIn3x3Grid(@Nullable Recipe<?> recipe, Display display) {
        return recipe == null || recipe.canCraftInDimensions(3, 3);
    }

    @Nullable
    private static RecipeHolder<?> getRecipeHolder(Display display) {
        var origin = DisplayRegistry.getInstance().getDisplayOrigin(display);
        return origin instanceof RecipeHolder<?> holder ? holder : null;
    }

    private static List<List<GenericStack>> getGuiIngredientsForCrafting(Display display) {
        var result = new ArrayList<List<GenericStack>>(9);
        for (int i = 0; i < 9; i++) {
            var stacks = new ArrayList<GenericStack>();
            if (i < display.getInputEntries().size()) {
                for (EntryStack<?> entryStack : display.getInputEntries().get(i)) {
                    if (entryStack.getType() == VanillaEntryTypes.ITEM) {
                        var generic = GenericStack.fromItemStack(entryStack.castValue());
                        if (generic != null) {
                            stacks.add(generic);
                        }
                    }
                }
            }
            result.add(stacks);
        }
        return result;
    }

    private static List<List<GenericStack>> ofInputs(Display display) {
        return display.getInputEntries().stream().map(ProbabilityPatternTransferHandler::of).toList();
    }

    private static List<GenericStack> ofOutputs(Display display) {
        var result = new ArrayList<GenericStack>();
        for (EntryIngredient entryIngredient : display.getOutputEntries()) {
            var stack = entryIngredient.stream()
                    .map(ProbabilityPatternTransferHandler::toGenericStack)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    private static List<GenericStack> of(EntryIngredient entryIngredient) {
        if (entryIngredient.isEmpty()) {
            return Collections.emptyList();
        }
        return entryIngredient.stream()
                .map(ProbabilityPatternTransferHandler::toGenericStack)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    private static GenericStack toGenericStack(EntryStack<?> entryStack) {
        if (entryStack.getType() == VanillaEntryTypes.ITEM) {
            return GenericStack.fromItemStack(entryStack.castValue());
        }
        // Fluids
        if (entryStack.getType() == VanillaEntryTypes.FLUID
                && entryStack.getValue() instanceof FluidStack fluidStack) {
            return GenericStack.fromFluidStack(FluidStackHooksForge.toForge(fluidStack));
        }
        return null;
    }

    private static Optional<Double> extractProbability(Display display) {
        return extractProbability(DisplayRegistry.getInstance().getDisplayOrigin(display), 0);
    }

    private static Optional<Double> extractProbability(Object value, int depth) {
        if (value == null || depth > 2) {
            return Optional.empty();
        }
        if (value instanceof RecipeHolder<?> holder) {
            return extractProbability(holder.value(), depth + 1);
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
