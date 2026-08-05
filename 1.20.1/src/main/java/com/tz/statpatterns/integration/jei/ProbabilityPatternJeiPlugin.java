package com.tz.statpatterns.integration.jei;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.integration.modules.jeirei.EncodingHelper;
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
import net.minecraftforge.fml.ModList;

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
import appeng.integration.modules.jeirei.EncodingHelper;
// TODO: 1.20.1 AE2 does not have DropTargets class - ghost handler disabled

@JeiPlugin
public class ProbabilityPatternJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = ProbabilityPatternMod.id("jei");

    @Override
    public ResourceLocation getPluginUid() { return ID; }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        var ghostHandler = new PatternTerminalGhostHandler();
        registration.addGhostIngredientHandler(ProbabilityPatternTerminalScreen.class, ghostHandler);
        if (ModList.get().isLoaded("ae2wtlib")) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            var rawHandler = (IGhostIngredientHandler) ghostHandler;
            registration.addGhostIngredientHandler(
                    com.tz.statpatterns.client.WirelessProbabilityPatternTerminalScreen.class, rawHandler);
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new PatternTerminalTransferHandler(
                ProbabilityPatternTerminalMenu.class, SPMenus.PROBABILITY_PATTERN_TERMINAL));
        if (ModList.get().isLoaded("ae2wtlib")) {
            registration.addUniversalRecipeTransferHandler(new PatternTerminalTransferHandler(
                    com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu.class,
                    SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL));
        }
    }

    private static final class PatternTerminalTransferHandler implements IUniversalRecipeTransferHandler<ProbabilityPatternTerminalMenu> {
        private final Class<? extends ProbabilityPatternTerminalMenu> containerClass;
        private final MenuType<?> menuType;

        PatternTerminalTransferHandler(Class<? extends ProbabilityPatternTerminalMenu> containerClass, MenuType<?> menuType) {
            this.containerClass = containerClass;
            this.menuType = menuType;
        }

        @Override public Class<? extends ProbabilityPatternTerminalMenu> getContainerClass() { return containerClass; }

        @Override
        public Optional<MenuType<ProbabilityPatternTerminalMenu>> getMenuType() {
            return Optional.of((MenuType<ProbabilityPatternTerminalMenu>) menuType);
        }

        @Override
        public IRecipeTransferError transferRecipe(ProbabilityPatternTerminalMenu menu, Object recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
            var vanillaRecipe = recipe instanceof Recipe<?> r ? r : null;

            var inputs = collectInputs(recipeSlots);
            var outputs = collectOutputs(recipeSlots);
            if (inputs.isEmpty() || outputs.isEmpty()) return null;

            // Auto-select the pattern type just like vanilla AE2: crafting recipes
            // that fit the 3x3 grid become crafting patterns, everything else becomes
            // a processing (probability) pattern.
            boolean craftingRecipe = EncodingHelper.isSupportedCraftingRecipe(vanillaRecipe);
            if (craftingRecipe && vanillaRecipe != null && !vanillaRecipe.canCraftInDimensions(3, 3)) {
                return null; // too large for the 3x3 crafting grid
            }

            if (doTransfer) {
                if (craftingRecipe && vanillaRecipe != null) {
                    EncodingHelper.encodeCraftingRecipe(menu, vanillaRecipe, inputs, stack -> true);
                } else {
                    extractProbability(recipe).ifPresent(menu::setProbability);
                    EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
                }
                menu.encode();
            }
            return null;
        }

        private static List<List<GenericStack>> collectInputs(IRecipeSlotsView recipeSlots) {
            var result = new ArrayList<List<GenericStack>>();
            for (var slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                var alternatives = new ArrayList<GenericStack>();
                for (var ingredient : slot.getAllIngredients().toList()) {
                    ingredient.getItemStack().filter(stack -> !stack.isEmpty()).map(GenericStack::fromItemStack).ifPresent(alternatives::add);
                }
                if (!alternatives.isEmpty()) result.add(alternatives);
            }
            return result;
        }

        private static List<GenericStack> collectOutputs(IRecipeSlotsView recipeSlots) {
            var result = new ArrayList<GenericStack>();
            for (var slot : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
                slot.getDisplayedItemStack().or(() -> slot.getItemStacks().findFirst()).filter(stack -> !stack.isEmpty()).map(GenericStack::fromItemStack).ifPresent(result::add);
            }
            return result;
        }

        private static Optional<Double> extractProbability(Object recipe) {
            return extractProbability(recipe, 0);
        }

        private static Optional<Double> extractProbability(Object value, int depth) {
            if (value == null || depth > 2) return Optional.empty();
            if (value instanceof Number number) return normalizeProbability(number.doubleValue());

            for (var methodName : List.of("successProbability", "getSuccessProbability", "probability", "getProbability", "chance", "getChance")) {
                try {
                    Method method = value.getClass().getMethod(methodName);
                    if (method.getParameterCount() == 0 && Number.class.isAssignableFrom(wrap(method.getReturnType()))) {
                        return normalizeProbability(((Number) method.invoke(value)).doubleValue());
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {}
            }

            for (var field : value.getClass().getDeclaredFields()) {
                var name = field.getName().toLowerCase(Locale.ROOT);
                if (name.contains("probability") || name.contains("chance")) {
                    var found = readProbabilityField(value, field, depth);
                    if (found.isPresent()) return found;
                }
            }
            return Optional.empty();
        }

        private static Optional<Double> readProbabilityField(Object owner, Field field, int depth) {
            try {
                field.setAccessible(true);
                return extractProbability(field.get(owner), depth + 1);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return Optional.empty();
            }
        }

        private static Optional<Double> normalizeProbability(double probability) {
            if (probability > 1.0 && probability <= 100.0) probability /= 100.0;
            if (probability > 0.0 && probability <= 1.0) return Optional.of(probability);
            return Optional.empty();
        }

        private static Class<?> wrap(Class<?> type) {
            if (!type.isPrimitive()) return type;
            if (type == double.class) return Double.class;
            if (type == float.class) return Float.class;
            if (type == int.class) return Integer.class;
            if (type == long.class) return Long.class;
            if (type == short.class) return Short.class;
            if (type == byte.class) return Byte.class;
            return type;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class PatternTerminalGhostHandler implements IGhostIngredientHandler<ProbabilityPatternTerminalScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(ProbabilityPatternTerminalScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
            // TODO: 1.20.1 AE2 does not have DropTargets - return empty targets for now
            return List.of();
        }

        @Override public void onComplete() {}
    }
}
