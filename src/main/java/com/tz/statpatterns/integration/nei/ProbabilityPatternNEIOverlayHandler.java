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
import net.minecraft.item.ItemStack;

import com.tz.statpatterns.network.ProbabilityPatternNetwork;
import com.tz.statpatterns.network.ProbabilityPatternPacket;
import com.tz.statpatterns.network.ProbabilityPatternPacket.Action;

import appeng.client.gui.implementations.GuiProbabilityPatternTerm;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * NEI {@link IOverlayHandler} for the probability pattern terminal. When the NEI
 * "?" button is pressed on a recipe while this terminal is open, it packs the
 * recipe's 3x3 inputs and its result and sends them to the server to fill the
 * terminal, exactly like the 1.21.1 original's JEI/EMI/REI recipe transfer.
 * <p>
 * The GUI is registered with {@link codechicken.nei.api.API#registerGuiOverlay}
 * using AE2's {@code TerminalCraftingSlotFinder}, so {@link PositionedStack}s are
 * repositioned onto this terminal's 3x3 grid before this handler sees them. The
 * grid starts at GUI-internal (18, 93) because the terminal is pinned to 3 ME
 * rows (ySize = 250, first input row renders at ySize - 157 = 93).
 */
public class ProbabilityPatternNEIOverlayHandler implements IOverlayHandler {

    private static final int INPUT_X = 18;
    private static final int INPUT_Y = 93;
    private static final int SLOT_SIZE = 18;

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
                    if (ps == null || ps.item == null) {
                        continue;
                    }
                    final int col = (ps.relx - INPUT_X + SLOT_SIZE / 2) / SLOT_SIZE;
                    final int row = (ps.rely - INPUT_Y + SLOT_SIZE / 2) / SLOT_SIZE;
                    final int idx = col + row * 3;
                    if (idx >= 0 && idx < 9) {
                        inputs[idx] = ps.item;
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
