/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
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
package com.zincglux.statpatterns.container;

import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.zincglux.statpatterns.ProbabilityPatternMod;
import com.zincglux.statpatterns.crafting.EncodedStatisticalPattern;
import com.zincglux.statpatterns.crafting.ProbabilityPatternItem;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.util.Platform;

/**
 * Container for the ME Probability Pattern Encoding Terminal (Spec v2.0 3.3.4).
 * <p>
 * Extends AE2 GTNH's {@link ContainerPatternTerm} so the vanilla pattern-terminal
 * slot layout and network plumbing are reused, and adds the probability controls
 * backed by the {@link ProbabilityPatternTerminalPart}. {@link #encode()} is an
 * independent implementation built on {@link EncodedStatisticalPattern#encode},
 * which bakes the probability parameters into the {@link ProbabilityPatternItem}.
 */
public class ContainerProbabilityPatternTerm extends ContainerPatternTerm {

    public ContainerProbabilityPatternTerm(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
        if (!(te instanceof ProbabilityPatternTerminalPart)) {
            throw new IllegalArgumentException(
                "Probability Pattern Terminal container requires a ProbabilityPatternTerminalPart host.");
        }
    }

    private ProbabilityPatternTerminalPart probabilityPart() {
        return (ProbabilityPatternTerminalPart) this.getPatternTerminal();
    }

    public double getProbability() {
        return this.probabilityPart()
            .getProbability();
    }

    public boolean isAlpha95() {
        return this.probabilityPart()
            .isAlpha95();
    }

    public void setProbability(final double probability) {
        this.probabilityPart()
            .setProbability(probability);
    }

    public void setAlpha95(final boolean alpha95) {
        this.probabilityPart()
            .setAlpha95(alpha95);
    }

    /**
     * Restore the probability parameters into the part when an already-encoded
     * probability pattern is loaded into the encoded-pattern slot.
     */
    @Override
    public void onSlotChange(final Slot s) {
        super.onSlotChange(s);
        if (Platform.isServer() && s.inventory == this.getPatternTerminal()
            .getInventoryByName("pattern") && s.getSlotIndex() == 1) {
            final ItemStack encoded = s.getStack();
            if (encoded != null && encoded.getTagCompound() != null
                && EncodedStatisticalPattern.isProbabilityPattern(encoded.getTagCompound())) {
                this.probabilityPart()
                    .setProbability(
                        encoded.getTagCompound()
                            .getDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY));
                this.probabilityPart()
                    .setAlpha95(
                        encoded.getTagCompound()
                            .getBoolean(EncodedStatisticalPattern.TAG_ALPHA95));
            }
        }
    }

    /**
     * Encode a probability pattern. Reads the 9 crafting-matrix inputs (nulls for
     * empty slots) and the non-null outputs, then delegates the NBT construction to
     * {@link EncodedStatisticalPattern#encode} which writes the vanilla-compatible
     * {@code in}/{@code out} tags plus the {@code sp_*} probability parameters.
     * The encoded pattern is a fresh {@link ProbabilityPatternItem}.
     */
    @Override
    public void encode() {
        final IInventory patternInv = this.getPatternTerminal()
            .getInventoryByName("pattern");
        final IInventory craftingInv = this.getPatternTerminal()
            .getInventoryByName("crafting");
        final IInventory outputInv = this.getPatternTerminal()
            .getInventoryByName("output");

        ItemStack output = patternInv.getStackInSlot(1);
        // If the encoded-pattern slot already holds something that is not a valid
        // (blank or probability) pattern source, bail out.
        if (output != null && !this.isPattern(output)) {
            return;
        }

        final ItemStack[] in = new ItemStack[9];
        boolean hasInput = false;
        for (int i = 0; i < 9; i++) {
            in[i] = craftingInv.getStackInSlot(i);
            if (in[i] != null) {
                hasInput = true;
            }
        }
        if (!hasInput) {
            return;
        }

        final List<ItemStack> out = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < outputInv.getSizeInventory(); i++) {
            final ItemStack s = outputInv.getStackInSlot(i);
            if (s != null) {
                out.add(s);
            }
        }
        if (out.isEmpty()) {
            return;
        }

        // Mirror AE2's ContainerPatternTerm.encode() semantics:
        // - Output slot EMPTY: require + consume ONE blank pattern, then create a fresh
        // probability pattern.
        // - Output slot already holds a probability pattern: re-encode it IN PLACE (update its
        // NBT, keep the same stack) — no blank is consumed and the existing pattern is not
        // overwritten by a new item.
        // - Output slot holds a vanilla/blank pattern (isPattern but not ours): AE2 would decode
        // it as a vanilla pattern (ignoring our probability NBT), so replace it with a fresh
        // probability pattern instead of reusing it.
        if (output == null) {
            final ItemStack blank = patternInv.getStackInSlot(0);
            if (blank == null || !this.isPattern(blank)) {
                return; // no blank pattern available to create a new pattern
            }
            blank.stackSize--;
            if (blank.stackSize <= 0) {
                patternInv.setInventorySlotContents(0, null);
            }
            output = new ItemStack(ProbabilityPatternMod.probabilityPatternItem);
        } else if (!(output.getItem() instanceof ProbabilityPatternItem)) {
            output = new ItemStack(ProbabilityPatternMod.probabilityPatternItem);
        }

        final NBTTagCompound encodedValue = EncodedStatisticalPattern.encode(
            in,
            out,
            this.substitute,
            this.beSubstitute,
            this.getPlayerInv().player.getCommandSenderName(),
            this.getProbability(),
            this.isAlpha95() ? 0.05 : 0.01,
            this.isAlpha95());

        output.setTagCompound(encodedValue);
        patternInv.setInventorySlotContents(1, output);
        this.saveChanges();
        this.detectAndSendChanges();
    }

    private boolean isPattern(final ItemStack is) {
        if (is == null) {
            return false;
        }
        final IDefinitions definitions = AEApi.instance()
            .definitions();
        boolean isPattern = definitions.items()
            .encodedPattern()
            .isSameAs(is);
        isPattern |= definitions.materials()
            .blankPattern()
            .isSameAs(is);
        // accept already-encoded probability patterns
        isPattern |= is.getItem() instanceof ProbabilityPatternItem;
        return isPattern;
    }
}
