
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

import appeng.api.storage.ITerminalHost;
import appeng.parts.encoding.PatternEncodingLogic;
import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.crafting.StatisticalPatternDetails;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.apache.commons.logging.impl.Log4JLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;

import com.tz.statpatterns.part.ProbabilityPatternEncodingLogic;


public class ProbabilityPatternTerminalMenu extends PatternEncodingTermMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbabilityPatternTerminalMenu.class);
    private static final String ACTION_SET_PROBABILITY = "setProbability";
    private static final String ACTION_SET_ALPHA95 = "setAlpha95";

    private double probability = 0.8;
    private boolean alpha95 = true;
    private final PatternEncodingLogic encodingLogic;

    public ProbabilityPatternTerminalMenu(int containerId, Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        this(SPMenus.PROBABILITY_PATTERN_TERMINAL.get(), containerId, playerInventory, host);
    }

    public ProbabilityPatternTerminalMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host, true);
        this.encodingLogic = host.getLogic();
        registerClientAction(ACTION_SET_PROBABILITY, Double.class, this::setProbability);
        registerClientAction(ACTION_SET_ALPHA95, Boolean.class, this::setAlpha95);

        // 从编码逻辑中恢复上次保存的概率值
        if (encodingLogic instanceof ProbabilityPatternEncodingLogic peLogic) {
            this.probability = peLogic.getProbability();
            this.alpha95 = peLogic.isAlpha95();
        }
    }

    @Override
    public void onServerDataSync(ShortSet updatedFields) {
        super.onServerDataSync(updatedFields);

    }

    public double getProbability() {
        return probability;
    }

    public boolean isAlpha95() {
        return alpha95;
    }

    public void setProbability(double probability) {
        this.probability = Math.max(0.01, Math.min(0.9999, probability));
        if (encodingLogic instanceof ProbabilityPatternEncodingLogic peLogic) {
            peLogic.setProbability(this.probability);
        }
        if (isClientSide()) {
            sendClientAction(ACTION_SET_PROBABILITY, this.probability);
        }
    }
    public void setAlpha95(boolean value) {
        this.alpha95 = value;
        if (encodingLogic instanceof ProbabilityPatternEncodingLogic peLogic) {
            peLogic.setAlpha95(value);
        }
        if (isClientSide()) {
            sendClientAction(ACTION_SET_ALPHA95, value);
        }
        broadcastChanges();
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        var encodedStack = encodingLogic.getEncodedPatternInv().getStackInSlot(0);
        var encoded = encodedStack.get(Components.ENCODED_STATISTICAL_PATTERN);
        if (encoded != null) {
            this.probability = encoded.successProbability();
            this.alpha95 = encoded.isAlpha95();
            if (encodingLogic instanceof ProbabilityPatternEncodingLogic peLogic) {
                peLogic.setProbability(this.probability);
                peLogic.setAlpha95(this.alpha95);
            }
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
        var logic = encodingLogic;
        var inputsInv = logic.getEncodedInputInv();
        var outputsInv = logic.getEncodedOutputInv();

        // TEMP DIAGNOSTIC — to be removed
        LOGGER.warn("[PP-MENU] encode server-side: mode={}, input0={}, output0={}, prob={}",
                getMode(),
                inputsInv.getStack(0),
                outputsInv.getStack(0),
                probability);

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
