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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import com.tz.statpatterns.crafting.StatisticalPatternDetails;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerPatternTerm;
import cpw.mods.fml.common.FMLLog;

/**
 * Container for the ME Probability Pattern Encoding Terminal.
 * <p>
 * Extends AE2 GTNH's {@link ContainerPatternTerm} so every vanilla pattern-terminal
 * behaviour is reused verbatim: the 3x3 fake crafting grid + 3 output slots + blank /
 * encoded pattern slots, NEI recipe transfer through {@code IContainerCraftingPacket},
 * stack doubling / halving, substitute toggles, clear, and the terminal buttons
 * (crafting/processing tabs, encode, clear, double). Only {@link #encode()} is
 * overridden to bake the probability parameters into a {@link ProbabilityPatternItem}
 * instead of a vanilla encoded pattern, and the two probability controls are exposed.
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
     * Re-encode the pattern as a probability pattern: same inputs/outputs handling as the
     * vanilla terminal, but the output pattern is a {@link ProbabilityPatternItem} carrying
     * the per-attempt success probability and the confidence flag.
     */
    @Override
    public void encode() {
        final IInventory patternInv = this.getPatternTerminal()
            .getInventoryByName("pattern");
        final IInventory craftingInv = this.getPatternTerminal()
            .getInventoryByName("crafting");
        final IInventory outputInv = this.getPatternTerminal()
            .getInventoryByName("output");

        final ItemStack existingEncoded = patternInv.getStackInSlot(1);
        if (existingEncoded != null && !(existingEncoded.getItem() instanceof ProbabilityPatternItem)) {
            FMLLog.info("[ProbabilityPattern] encode: encoded slot holds a foreign item, aborting");
            return;
        }

        // Per-attempt inputs (fake slots may report stackSize 0; normalise to 1).
        final List<ItemStack> inputs = new ArrayList<ItemStack>();
        for (int i = 0; i < craftingInv.getSizeInventory(); i++) {
            final ItemStack s = craftingInv.getStackInSlot(i);
            if (s != null) {
                if (s.stackSize <= 0) {
                    s.stackSize = 1;
                }
                inputs.add(s);
            }
        }
        if (inputs.isEmpty()) {
            FMLLog.info("[ProbabilityPattern] encode: no crafting inputs");
            return;
        }

        ItemStack output = null;
        for (int i = 0; i < outputInv.getSizeInventory(); i++) {
            final ItemStack s = outputInv.getStackInSlot(i);
            if (s != null) {
                if (s.stackSize <= 0) {
                    s.stackSize = 1;
                }
                output = s;
                break;
            }
        }
        if (output == null) {
            FMLLog.info("[ProbabilityPattern] encode: no target output");
            return;
        }

        if (existingEncoded == null) {
            final ItemStack blank = patternInv.getStackInSlot(0);
            if (blank == null) {
                FMLLog.info("[ProbabilityPattern] encode: no blank pattern in slot");
                return;
            }
            final boolean isAE2Blank = AEApi.instance()
                .definitions()
                .materials()
                .blankPattern()
                .isSameAs(blank);
            final boolean isOurBlank = blank.getItem() instanceof ProbabilityPatternItem;
            if (!isAE2Blank && !isOurBlank) {
                FMLLog.info("[ProbabilityPattern] encode: blank slot holds %s, not a blank pattern", blank);
                return;
            }
            blank.stackSize--;
            if (blank.stackSize <= 0) {
                patternInv.setInventorySlotContents(0, null);
            }
        }

        final double probability = this.getProbability();
        final boolean alpha95 = this.isAlpha95();
        final double alpha = alpha95 ? 0.05 : 0.01;
        final ItemStack encoded = StatisticalPatternDetails.encode(inputs, output, probability, alpha, alpha95);
        patternInv.setInventorySlotContents(1, encoded);
        this.saveChanges();
        this.detectAndSendChanges();

        // Diagnostic: verify the freshly encoded pattern decodes through AE2's pattern path.
        try {
            final World w = this.getPatternTerminal()
                .getTile()
                .getWorldObj();
            final ICraftingPatternDetails d = ((ICraftingPatternItem) ProbabilityPatternMod.probabilityPatternItem)
                .getPatternForItem(encoded, w);
            if (d == null) {
                FMLLog.info("[ProbabilityPattern] encode: RESULT INVALID (getPatternForItem returned null)");
            } else {
                final IAEItemStack[] outs = d.getOutputs();
                long nbtOutCnt = -1L;
                try {
                    final NBTTagList outTag = encoded.getTagCompound()
                        .getTagList("out", 10);
                    if (outTag.tagCount() > 0) {
                        nbtOutCnt = outTag.getCompoundTagAt(0)
                            .getLong("Cnt");
                    }
                } catch (final Throwable ignored) {}
                FMLLog.info(
                    "[ProbabilityPattern] encode: OK inputs=%d outputs=%d outStackSize=%d nbtOutCnt=%d craftable=%s",
                    d.getInputs().length,
                    outs.length,
                    outs.length > 0 && outs[0] != null ? outs[0].getStackSize() : -1L,
                    nbtOutCnt,
                    d.isCraftable());
            }
        } catch (final Throwable t) {
            FMLLog.info("[ProbabilityPattern] encode: decode threw: %s", t.toString());
        }
    }
}
