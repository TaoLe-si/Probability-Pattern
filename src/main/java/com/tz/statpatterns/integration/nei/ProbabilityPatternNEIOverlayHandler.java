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
package com.tz.statpatterns.integration.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.tz.statpatterns.network.ProbabilityPatternNetwork;
import com.tz.statpatterns.network.ProbabilityPatternPacket;
import com.tz.statpatterns.network.ProbabilityPatternPacket.Action;

import appeng.client.gui.implementations.GuiProbabilityPatternTerm;
import appeng.container.slot.SlotFakeCraftingMatrix;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * NEI {@link IOverlayHandler} for the probability pattern terminal. When the NEI
 * "?" button is pressed on a recipe while this terminal is open, it packs the
 * recipe's 3x3 inputs and its result and sends them to the server to fill the
 * terminal, exactly like the 1.21.1 original's JEI/EMI/REI recipe transfer.
 * <p>
 * The slot math mirrors GTNH AE2's {@code NEICraftingHandler.packIngredients}: NEI
 * crafting recipes lay out the 3x3 grid starting at (25, 6) with 18px cells, and the
 * target slot is located by walking {@code gui.inventorySlots} for a
 * {@link SlotFakeCraftingMatrix} whose {@code getSlotIndex()} matches
 * {@code col + row * 3}. This is coordinate-independent, so it keeps working no matter
 * how the terminal is resized/repositioned.
 */
public class ProbabilityPatternNEIOverlayHandler implements IOverlayHandler {

    @Override
    public void overlayRecipe(final GuiContainer gui, final IRecipeHandler recipe, final int recipeIndex,
        final boolean shift) {
        if (!(gui instanceof GuiProbabilityPatternTerm)) {
            return;
        }
        try {
            final ItemStack[] inputs = new ItemStack[9];
            final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
            if (ingredients != null) {
                for (final PositionedStack ps : ingredients) {
                    if (ps == null || ps.items == null || ps.items.length == 0) {
                        continue;
                    }
                    final int col = (ps.relx - 25) / 18;
                    final int row = (ps.rely - 6) / 18;
                    final int idx = col + row * 3;
                    if (idx < 0 || idx >= 9) {
                        continue;
                    }
                    for (final Object o : gui.inventorySlots.inventorySlots) {
                        if (o instanceof Slot) {
                            final Slot slot = (Slot) o;
                            if (slot instanceof SlotFakeCraftingMatrix && slot.getSlotIndex() == idx) {
                                inputs[idx] = ps.items[0];
                                break;
                            }
                        }
                    }
                }
            }

            final PositionedStack result = recipe.getResultStack(recipeIndex);
            final ItemStack output = result != null ? result.item : null;

            ProbabilityPatternNetwork.CHANNEL
                .sendToServer(new ProbabilityPatternPacket(Action.NEI_RECIPE, inputs, output));
        } catch (final Exception ignored) {
            // NEI malformed recipe — ignore.
        }
    }
}
