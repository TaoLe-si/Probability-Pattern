
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
package com.tz.statpatterns.terminal;

import java.util.ArrayList;
import java.util.Objects;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.crafting.StatisticalPatternDetails;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;


public class ProbabilityPatternTerminalMenu extends PatternEncodingTermMenu {
    private static final String ACTION_SET_PROBABILITY = "setProbability";
    private static final String ACTION_SET_ALPHA95 = "setAlpha95";

    private double probability = 0.8;
    private boolean alpha95 = true;
    private final IPatternTerminalMenuHost patternHost;

    public ProbabilityPatternTerminalMenu(int containerId, Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        this(SPMenus.PROBABILITY_PATTERN_TERMINAL.get(), containerId, playerInventory, host);
        // Note: wireless subclass overrides MenuType via its own constructor
    }

    public ProbabilityPatternTerminalMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host, true);
        this.patternHost = Objects.requireNonNull(host, "host");
        registerClientAction(ACTION_SET_PROBABILITY, Double.class, this::setProbability);
        registerClientAction(ACTION_SET_ALPHA95, Boolean.class, this::setAlpha95);
        // Note: upgrade slots are handled by MEStorageMenu base class via host.getUpgrades()
    }

    public double getProbability() {
        return probability;
    }

    public boolean isAlpha95() {
        return alpha95;
    }

    public void setProbability(double probability) {
        this.probability = Math.max(0.01, Math.min(0.9999, probability));
        if (isClientSide()) {
            sendClientAction(ACTION_SET_PROBABILITY, this.probability);
        }
    }
    public void setAlpha95(boolean value) {
        this.alpha95 = value;
        // 客户端点击时发送同步包到服务端
        if (isClientSide()) {
            sendClientAction(ACTION_SET_ALPHA95, value);
        }
        // 同步所有打开此Menu的客户端界面
        broadcastChanges();
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        var encodedStack = patternHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
        var encoded = encodedStack.get(Components.ENCODED_STATISTICAL_PATTERN);
        if (encoded != null) {
            this.probability = encoded.successProbability();
        }
    }

    @Override
    public void encode() {
        if (isClientSide()) {
            sendClientAction("encode");
            return;
        }

        if (getMode() != EncodingMode.PROCESSING) {
            super.encode();
            return;
        }

        encodeProbabilityProcessingPattern();
    }

    private void encodeProbabilityProcessingPattern() {
        var logic = patternHost.getLogic();
        var inputsInv = logic.getEncodedInputInv();
        var outputsInv = logic.getEncodedOutputInv();

        var sparseInputs = new ArrayList<GenericStack>(inputsInv.size());
        var hasInput = false;
        for (int i = 0; i < inputsInv.size(); i++) {
            var stack = inputsInv.getStack(i);
            sparseInputs.add(stack);
            hasInput |= stack != null;
        }
        if (!hasInput) {
            super.encode();
            broadcastChanges();
            return;
        }

        var sparseOutputs = new ArrayList<GenericStack>(outputsInv.size());
        for (int i = 0; i < outputsInv.size(); i++) {
            sparseOutputs.add(outputsInv.getStack(i));
        }
        if (sparseOutputs.isEmpty() || sparseOutputs.get(0) == null) {
            super.encode();
            broadcastChanges();
            return;
        }

        var encodedPattern = StatisticalPatternDetails.encode(sparseInputs, sparseOutputs, probability, isAlpha95() ? 0.05 : 0.01, isAlpha95());

        var encodedInv = logic.getEncodedPatternInv();
        var blankInv = logic.getBlankPatternInv();
        var existingEncoded = encodedInv.getStackInSlot(0);

        if (!existingEncoded.isEmpty()) {
            if (!PatternDetailsHelper.isEncodedPattern(existingEncoded)) {
                return;
            }
            encodedInv.setItemDirect(0, encodedPattern);
        } else {
            var blankPattern = blankInv.getStackInSlot(0);
            if (blankPattern.isEmpty()) {
                return;
            }
            blankPattern.shrink(1);
            blankInv.setItemDirect(0, blankPattern.isEmpty() ? ItemStack.EMPTY : blankPattern);
            encodedInv.setItemDirect(0, encodedPattern);
        }

        logic.saveChanges();
        broadcastChanges();
    }
}
