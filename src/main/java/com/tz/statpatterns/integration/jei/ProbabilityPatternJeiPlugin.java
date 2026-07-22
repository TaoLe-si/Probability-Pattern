
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
import java.util.Optional;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.client.ProbabilityPatternTerminalScreen;
import com.tz.statpatterns.client.WirelessProbabilityPatternTerminalScreen;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;
import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

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

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.DropTargets;
import appeng.integration.modules.itemlists.EncodingHelper;


@JeiPlugin
public class ProbabilityPatternJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = ProbabilityPatternMod.id("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        var ghostHandler = new PatternTerminalGhostHandler();
        registration.addGhostIngredientHandler(ProbabilityPatternTerminalScreen.class, ghostHandler);
        // Cast to raw type to register for wireless subclass (JEI matches by class hierarchy at runtime)
        IGhostIngredientHandler rawHandler = ghostHandler;
        registration.addGhostIngredientHandler(WirelessProbabilityPatternTerminalScreen.class, rawHandler);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new PatternTerminalTransferHandler(
                ProbabilityPatternTerminalMenu.class, SPMenus.PROBABILITY_PATTERN_TERMINAL.get()));
        registration.addUniversalRecipeTransferHandler(new PatternTerminalTransferHandler(
                WirelessProbabilityPatternTerminalMenu.class, SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL.get()));
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
            var inputs = collectInputs(recipeSlots);
            var outputs = collectOutputs(recipeSlots);
            if (inputs.isEmpty() || outputs.isEmpty()) {
                return null;
            }

            if (doTransfer) {
                extractProbability(recipe).ifPresent(menu::setProbability);
                EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
                menu.encode();
            }
            return null;
        }

        private static List<List<GenericStack>> collectInputs(IRecipeSlotsView recipeSlots) {
            var result = new ArrayList<List<GenericStack>>();
            for (var slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                var alternatives = new ArrayList<GenericStack>();
                for (var ingredient : slot.getAllIngredientsList()) {
                    ingredient.getItemStack()
                            .filter(stack -> !stack.isEmpty())
                            .map(GenericStack::fromItemStack)
                            .ifPresent(alternatives::add);
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
                slot.getDisplayedItemStack()
                        .or(() -> slot.getItemStacks().findFirst())
                        .filter(stack -> !stack.isEmpty())
                        .map(GenericStack::fromItemStack)
                        .ifPresent(result::add);
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

            var itemStack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (itemStack.isEmpty()) {
                return List.of();
            }

            var genericStack = GenericStack.fromItemStack(itemStack);
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