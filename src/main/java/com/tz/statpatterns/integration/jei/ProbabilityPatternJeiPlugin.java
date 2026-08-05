
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
package com.tz.statpatterns.integration.jei;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.client.ProbabilityPatternTerminalScreen;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.DropTargets;
import appeng.integration.modules.itemlists.EncodingHelper;


@JeiPlugin
public class ProbabilityPatternJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = ProbabilityPatternMod.id("jei");
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbabilityPatternJeiPlugin.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        var ghostHandler = new PatternTerminalGhostHandler();
        registration.addGhostIngredientHandler(ProbabilityPatternTerminalScreen.class, ghostHandler);
        // Wireless variant — only available when ae2wtlib is present
        if (ModList.get().isLoaded("ae2wtlib")) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            var rawHandler = (IGhostIngredientHandler) ghostHandler;
            registration.addGhostIngredientHandler(
                    com.tz.statpatterns.client.WirelessProbabilityPatternTerminalScreen.class,
                    rawHandler);
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new PatternTerminalTransferHandler(
                ProbabilityPatternTerminalMenu.class, SPMenus.PROBABILITY_PATTERN_TERMINAL.get()));
        // Wireless variant — only available when ae2wtlib is present
        if (ModList.get().isLoaded("ae2wtlib")) {
            registration.addUniversalRecipeTransferHandler(new PatternTerminalTransferHandler(
                    com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu.class,
                    SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL.get()));
        }
    }

    /**
     * Converts a JEI typed ingredient (either an item or a fluid) into an AE2
     * {@link GenericStack}. Fluids are converted through {@link AEFluidKey}, so
     * they can be dropped into ghost slots and encoded into patterns.
     */
    @Nullable
    static GenericStack toGenericStack(ITypedIngredient<?> ingredient) {
        // Items — but do NOT abort on failure; a JEI slot may expose both an item
        // and a fluid representation, and we want to fall back to the fluid.
        var itemStack = ingredient.getItemStack();
        if (itemStack.isPresent() && !itemStack.get().isEmpty()) {
            var generic = GenericStack.fromItemStack(itemStack.get());
            if (generic != null) {
                return generic;
            }
        }
        var value = ingredient.getIngredient();
        // Fluids (as FluidStack)
        if (value instanceof FluidStack fluidStack && !fluidStack.isEmpty()) {
            return GenericStack.fromFluidStack(fluidStack);
        }
        // Fluids (as bare Fluid)
        if (value instanceof Fluid fluid && fluid != Fluids.EMPTY) {
            return new GenericStack(AEFluidKey.of(fluid), AEFluidKey.AMOUNT_BUCKET);
        }
        return null;
    }

    private static final class PatternTerminalTransferHandler implements IUniversalRecipeTransferHandler<ProbabilityPatternTerminalMenu> {
        private final Class<? extends ProbabilityPatternTerminalMenu> containerClass;
        private final MenuType<?> menuType;

        PatternTerminalTransferHandler(Class<? extends ProbabilityPatternTerminalMenu> containerClass, MenuType<?> menuType) {
            this.containerClass = containerClass;
            this.menuType = menuType;
        }

        @Override
        public Class<? extends ProbabilityPatternTerminalMenu> getContainerClass() {
            return containerClass;
        }

        @Override
        public Optional<MenuType<ProbabilityPatternTerminalMenu>> getMenuType() {
            return Optional.of((MenuType<ProbabilityPatternTerminalMenu>) menuType);
        }

        @Override
        public IRecipeTransferError transferRecipe(ProbabilityPatternTerminalMenu menu, Object recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
            var holder = getRecipeHolder(recipe);
            var vanillaRecipe = holder != null ? holder.value() : getRecipe(recipe);

            var inputs = collectInputs(recipeSlots);
            var outputs = collectOutputs(recipeSlots);
            // TEMP DIAGNOSTIC — to be removed
            LOGGER.warn("[PP-JEI] transferRecipe doTransfer={} inputs={} outputs={} recipeClass={}",
                    doTransfer, inputs.size(), outputs.size(),
                    recipe == null ? "null" : recipe.getClass().getName());
            if (inputs.isEmpty() || outputs.isEmpty()) {
                return null;
            }

            // Auto-select the pattern type just like vanilla AE2: crafting recipes
            // that fit the 3x3 grid become crafting patterns, everything else becomes
            // a processing (probability) pattern.
            boolean craftingRecipe = EncodingHelper.isSupportedCraftingRecipe(vanillaRecipe);
            if (craftingRecipe && vanillaRecipe != null && !vanillaRecipe.canCraftInDimensions(3, 3)) {
                return null; // too large for the 3x3 crafting grid
            }

            if (doTransfer) {
                if (craftingRecipe && holder != null) {
                    EncodingHelper.encodeCraftingRecipe(menu, holder, inputs, stack -> true);
                } else {
                    extractProbability(recipe).ifPresent(menu::setProbability);
                    EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
                }
                menu.encode();
            }
            return null;
        }

        private static @Nullable RecipeHolder<?> getRecipeHolder(Object recipe) {
            return recipe instanceof RecipeHolder<?> holder ? holder : null;
        }

        private static @Nullable Recipe<?> getRecipe(Object recipe) {
            return recipe instanceof Recipe<?> value ? value : null;
        }

        private static List<List<GenericStack>> collectInputs(IRecipeSlotsView recipeSlots) {
            var result = new ArrayList<List<GenericStack>>();
            for (var slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                var alternatives = new ArrayList<GenericStack>();
                for (var ingredient : slot.getAllIngredientsList()) {
                    var stack = toGenericStack(ingredient);
                    // TEMP DIAGNOSTIC — to be removed
                    if (stack == null && !(ingredient.getItemStack().isPresent())) {
                        LOGGER.warn("[PP-JEI] input ingredient unhandled: valueClass={}, typeClass={}",
                                ingredient.getIngredient() == null ? "null" : ingredient.getIngredient().getClass().getName(),
                                ingredient.getType() == null ? "null" : ingredient.getType().getIngredientClass().getName());
                    }
                    if (stack != null) {
                        alternatives.add(stack);
                    }
                }
                // Fall back to the displayed ingredient if the slot reports no ingredients
                if (alternatives.isEmpty()) {
                    var displayed = slot.getDisplayedIngredient()
                            .map(ProbabilityPatternJeiPlugin::toGenericStack)
                            .orElse(null);
                    if (displayed != null) {
                        alternatives.add(displayed);
                    }
                }
                if (!alternatives.isEmpty()) {
                    result.add(alternatives);
                }
            }
            return result;
        }

        private static List<GenericStack> collectOutputs(IRecipeSlotsView recipeSlots) {
            var result = new ArrayList<GenericStack>();
            for (var slot : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
                var stack = slot.getAllIngredientsList().stream()
                        .map(ProbabilityPatternJeiPlugin::toGenericStack)
                        .filter(Objects::nonNull)
                        .findFirst()
                        // Fall back to the displayed ingredient if the slot reports no ingredients
                        .or(() -> slot.getDisplayedIngredient()
                                .map(ProbabilityPatternJeiPlugin::toGenericStack))
                        .orElse(null);
                if (stack != null) {
                    result.add(stack);
                }
            }
            return result;
        }

        private static Optional<Double> extractProbability(Object recipe) {
            return extractProbability(recipe, 0);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class PatternTerminalGhostHandler
            implements IGhostIngredientHandler<ProbabilityPatternTerminalScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(
                ProbabilityPatternTerminalScreen screen,
                ITypedIngredient<I> ingredient,
                boolean doStart
        ) {
            // 内部强转为带泛型版本调用DropTargets
            ProbabilityPatternTerminalScreen<?> customScreen = (ProbabilityPatternTerminalScreen<?>) screen;

            // 同时支持物品和流体 ghost 拖放
            var genericStack = ProbabilityPatternJeiPlugin.toGenericStack(ingredient);
            if (genericStack == null) {
                return List.of();
            }

            var targets = new ArrayList<Target<I>>();
            for (var dropTarget : DropTargets.getTargets(customScreen)) {
                if (dropTarget.canDrop(genericStack)) {
                    targets.add(new Target<>() {
                        @Override
                        public Rect2i getArea() {
                            return dropTarget.area();
                        }

                        @Override
                        public void accept(I ignored) {
                            dropTarget.drop(genericStack);
                        }
                    });
                }
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}