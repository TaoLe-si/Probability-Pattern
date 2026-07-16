package com.tz.statpatterns.menu;

import java.util.ArrayList;
import java.util.Objects;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.core.definition.SPMenus;
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

import com.tz.statpatterns.crafting.StatisticalPatternDetails;

public class ProbabilityPatternTerminalMenu extends PatternEncodingTermMenu {
    private static final String ACTION_SET_PROBABILITY = "setProbability";

    private double probability = 0.8;
    private final IPatternTerminalMenuHost patternHost;

    public ProbabilityPatternTerminalMenu(int containerId, Inventory playerInventory,
                                          @Nullable IPatternTerminalMenuHost host) {
        this(SPMenus.PROBABILITY_PATTERN_TERMINAL.get(), containerId, playerInventory, host);
    }

    public ProbabilityPatternTerminalMenu(MenuType<?> menuType, int containerId, Inventory playerInventory,
                                          @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host, true);
        this.patternHost = Objects.requireNonNull(host, "host");
        registerClientAction(ACTION_SET_PROBABILITY, Double.class, this::setProbability);
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = Math.max(0.01, Math.min(0.9999, probability));
        if (isClientSide()) {
            sendClientAction(ACTION_SET_PROBABILITY, this.probability);
        }
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        var encodedStack = patternHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
        var encoded = encodedStack.get(Components.ENCODED_STATISTICAL_PATTERN.get());
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
            return;
        }

        var sparseOutputs = new ArrayList<GenericStack>(outputsInv.size());
        for (int i = 0; i < outputsInv.size(); i++) {
            sparseOutputs.add(outputsInv.getStack(i));
        }
        if (sparseOutputs.isEmpty() || sparseOutputs.get(0) == null) {
            return;
        }

        var encodedPattern = StatisticalPatternDetails.encode(sparseInputs, sparseOutputs, probability, 0.05);
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