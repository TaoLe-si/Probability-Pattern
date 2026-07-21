
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
package com.tz.statpatterns.client;

import appeng.client.gui.widgets.*;
import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.ScreenStyle;

import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenu;

public class ProbabilityPatternTerminalScreen<P extends ProbabilityPatternTerminalMenu> extends PatternEncodingTermScreen<P> {
    private static final int PROBABILITY_FIELD_WIDTH = 40;
    private static final int FIELD_HEIGHT = 14;
    private static final int TITLE_TO_PROBABILITY_GAP = 10;
    private static final int LABEL_TO_FIELD_GAP = 4;
    private static final int INVENTORY_TITLE_GAP = 12;

    private final Inventory playerInventory;
    private final AETextField probabilityField;
    private final AECheckbox alpha95;

    public ProbabilityPatternTerminalScreen(P menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.playerInventory = playerInventory;
        this.probabilityField = widgets.addTextField("probability");
        probabilityField.setHeight(FIELD_HEIGHT);
        probabilityField.setWidth(PROBABILITY_FIELD_WIDTH);
        probabilityField.setMaxLength(8);
        probabilityField.setMessage(Component.translatable("gui.probabilitypattern.probability"));
        probabilityField.setValue(formatProbability(menu.getProbability()));
        probabilityField.setResponder(value -> {
            var parsed = parseProbability(value);
            if (parsed != null) {
                menu.setProbability(parsed);
            }
        });
        probabilityField.setX(probabilityFieldX());
        probabilityField.setY(fieldY());

        this.alpha95 = widgets.addCheckbox("alpha", Component.translatable("gui.probabilitypattern.alpha95"), this::save);
        this.alpha95.setWidth(100);

        updateState();
    }

    private void updateState() {
        alpha95.setSelected(getMenu().isAlpha95());
    }

    private void save() {
        getMenu().setAlpha95(alpha95.isSelected());
        updateState();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        var probabilityLabel = Component.translatable("gui.probabilitypattern.probability");
        guiGraphics.drawString(font, probabilityLabel, probabilityLabelX(), labelY() + 1, 0x404040, false);

        probabilityField.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (!probabilityField.isFocused()) {
            var current = formatProbability(menu.getProbability());
            if (!current.equals(probabilityField.getValue())) {
                probabilityField.setValue(current);
            }
        }
    }

    private int probabilityLabelX() {
        return leftPos + playerInventoryLeftX() + font.width(playerInventoryTitle) + TITLE_TO_PROBABILITY_GAP - 8;
    }

    private int probabilityFieldX() {
        var label = Component.translatable("gui.probabilitypattern.probability");
        return probabilityLabelX() + font.width(label) + LABEL_TO_FIELD_GAP;
    }

    private int fieldY() {
        return labelY() - 3;
    }

    private int labelY() {
        return topPos + playerInventoryTopY() - INVENTORY_TITLE_GAP;
    }

    private int playerInventoryLeftX() {
        var minX = Integer.MAX_VALUE;
        for (var slot : menu.slots) {
            if (slot.container == playerInventory) {
                minX = Math.min(minX, slot.x);
            }
        }
        return minX == Integer.MAX_VALUE ? inventoryLabelX : minX;
    }

    private int playerInventoryTopY() {
        var minY = Integer.MAX_VALUE;
        for (var slot : menu.slots) {
            if (slot.container == playerInventory) {
                minY = Math.min(minY, slot.y);
            }
        }
        return minY == Integer.MAX_VALUE ? inventoryLabelY + 12 : minY;
    }

    private static String formatProbability(double probability) {
        return "%.4f".formatted(probability);
    }

    private static Double parseProbability(String value) {
        try {
            var parsed = Double.parseDouble(value.trim());
            if (parsed > 0.0 && parsed <= 1.0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }
}