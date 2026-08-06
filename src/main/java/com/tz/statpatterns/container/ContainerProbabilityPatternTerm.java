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
package com.tz.statpatterns.container;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.tz.statpatterns.crafting.EncodedStatisticalPattern;
import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import com.tz.statpatterns.crafting.StatisticalPatternDetails;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.storage.ITerminalHost;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternOutputs;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

/**
 * Container for the ME Probability Pattern Encoding Terminal.
 * <p>
 * Reimplements the processing-pattern terminal layout (9 per-attempt inputs, 3 target
 * output slots, blank + encoded pattern slots) on top of {@link ContainerMEMonitorable}
 * so it can customise the pattern slots (accept the Probability Pattern item) and bake
 * the probability parameters into the encoded pattern.
 */
public class ContainerProbabilityPatternTerm extends ContainerMEMonitorable implements IOptionalSlotHost {

    private static final int GUI_SYNC_PROBABILITY = 96;
    private static final int GUI_SYNC_ALPHA95 = 97;

    private final ProbabilityPatternTerminalPart patternTerminal;
    private final SlotFakeCraftingMatrix[] craftingSlots = new SlotFakeCraftingMatrix[9];
    private final SlotPatternOutputs[] outputSlots = new SlotPatternOutputs[3];
    private final SlotRestrictedInput patternSlotIN;
    private final SlotRestrictedInput patternSlotOUT;

    @GuiSync(GUI_SYNC_PROBABILITY)
    public int probabilityScaled = 8000;

    @GuiSync(GUI_SYNC_ALPHA95)
    public int alpha95Flag = 1;

    public ContainerProbabilityPatternTerm(final InventoryPlayer ip, final ITerminalHost monitorable) {
        super(ip, monitorable, false);

        if (!(monitorable instanceof ProbabilityPatternTerminalPart)) {
            throw new IllegalArgumentException(
                "Probability Pattern Terminal container requires a ProbabilityPatternTerminalPart host.");
        }
        this.patternTerminal = (ProbabilityPatternTerminalPart) monitorable;

        final IInventory patternInv = this.patternTerminal.getInventoryByName("pattern");
        final IInventory outputInv = this.patternTerminal.getInventoryByName("output");
        final IInventory craftingInv = this.patternTerminal.getInventoryByName("crafting");

        // 3x3 per-attempt input grid
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.craftingSlots[x
                    + y * 3] = new SlotFakeCraftingMatrix(craftingInv, x + y * 3, 18 + x * 18, -76 + y * 18);
                this.addSlotToContainer(this.craftingSlots[x + y * 3]);
            }
        }

        // 3 target output slots
        for (int y = 0; y < 3; y++) {
            this.outputSlots[y] = new SlotPatternOutputs(outputInv, this, y, 110, -76 + y * 18, 0, 0, 1);
            this.outputSlots[y].setRenderDisabled(false);
            this.outputSlots[y].setIIcon(-1);
            this.addSlotToContainer(this.outputSlots[y]);
        }

        // blank probability pattern in, encoded probability pattern out.
        // PATTERN accepts any ICraftingPatternItem (our ProbabilityPatternItem blank included)
        // plus the vanilla AE2 blank pattern.
        this.patternSlotIN = new SlotRestrictedInput(
            SlotRestrictedInput.PlacableItemType.PATTERN,
            patternInv,
            0,
            147,
            -72 - 9,
            this.getInventoryPlayer());
        this.patternSlotOUT = new SlotRestrictedInput(
            SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
            patternInv,
            1,
            147,
            -72 + 34,
            this.getInventoryPlayer());
        this.patternSlotOUT.setStackLimit(1);
        this.addSlotToContainer(this.patternSlotIN);
        this.addSlotToContainer(this.patternSlotOUT);

        this.bindPlayerInventory(ip, 0, 0);
    }

    public ProbabilityPatternTerminalPart getProbabilityTerminal() {
        return this.patternTerminal;
    }

    @Override
    public boolean isSlotEnabled(final int idx) {
        // Processing-only terminal: all output slots are always enabled.
        return true;
    }

    public double getProbability() {
        return this.patternTerminal.getProbability();
    }

    public boolean isAlpha95() {
        return this.patternTerminal.isAlpha95();
    }

    public void setProbability(final double probability) {
        this.patternTerminal.setProbability(probability);
    }

    public void setAlpha95(final boolean alpha95) {
        this.patternTerminal.setAlpha95(alpha95);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (Platform.isServer()) {
            this.probabilityScaled = (int) Math.round(this.patternTerminal.getProbability() * 10000.0);
            this.alpha95Flag = this.patternTerminal.isAlpha95() ? 1 : 0;
        }
    }

    @Override
    public void onSlotChange(final Slot s) {
        super.onSlotChange(s);
        if (Platform.isServer() && s == this.patternSlotOUT) {
            // Loading an existing encoded probability pattern restores its parameters.
            final ItemStack encoded = this.patternSlotOUT.getStack();
            if (encoded != null && encoded.getItem() instanceof ProbabilityPatternItem) {
                final EncodedStatisticalPattern pattern = EncodedStatisticalPattern.decode(encoded.getTagCompound());
                if (pattern != null) {
                    this.patternTerminal.setProbability(pattern.successProbability());
                    this.patternTerminal.setAlpha95(pattern.isAlpha95());
                }
            }
        }
    }

    public void encode() {
        final IInventory patternInv = this.patternTerminal.getInventoryByName("pattern");
        final IInventory craftingInv = this.patternTerminal.getInventoryByName("crafting");
        final IInventory outputInv = this.patternTerminal.getInventoryByName("output");

        final ItemStack existingEncoded = patternInv.getStackInSlot(1);
        if (existingEncoded != null && !(existingEncoded.getItem() instanceof ProbabilityPatternItem)) {
            return; // only our own probability patterns may occupy the encoded slot
        }

        // collect per-attempt inputs
        final List<ItemStack> inputs = new ArrayList<ItemStack>();
        for (int i = 0; i < craftingInv.getSizeInventory(); i++) {
            final ItemStack s = craftingInv.getStackInSlot(i);
            if (s != null) {
                inputs.add(s);
            }
        }
        if (inputs.isEmpty()) {
            return;
        }

        // collect target output (first non-null)
        ItemStack output = null;
        for (int i = 0; i < outputInv.getSizeInventory(); i++) {
            final ItemStack s = outputInv.getStackInSlot(i);
            if (s != null) {
                output = s;
                break;
            }
        }
        if (output == null) {
            return;
        }

        if (existingEncoded == null) {
            final ItemStack blank = patternInv.getStackInSlot(0);
            if (blank == null || !(blank.getItem() instanceof ProbabilityPatternItem)) {
                return; // no blank probability pattern
            }
            blank.stackSize--;
            if (blank.stackSize <= 0) {
                patternInv.setInventorySlotContents(0, null);
            }
        }

        final double probability = this.patternTerminal.getProbability();
        final double alpha = this.patternTerminal.isAlpha95() ? 0.05 : 0.01;
        final ItemStack encoded = StatisticalPatternDetails
            .encode(inputs, output, probability, alpha, this.patternTerminal.isAlpha95());
        patternInv.setInventorySlotContents(1, encoded);
        this.patternTerminal.saveChanges();
        this.detectAndSendChanges();
    }

    public void clear() {
        for (final SlotFakeCraftingMatrix slot : this.craftingSlots) {
            slot.putStack(null);
        }
        for (final SlotPatternOutputs slot : this.outputSlots) {
            slot.putStack(null);
        }
        this.detectAndSendChanges();
    }
}
