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
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.crafting.EncodedStatisticalPattern;
import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.util.Platform;
import cpw.mods.fml.common.FMLLog;

/**
 * Container for the ME Probability Pattern Encoding Terminal.
 * <p>
 * Extends AE2 GTNH's {@link ContainerPatternTerm} so all vanilla pattern-terminal
 * behaviour (slot layout, NEI recipe transfer, stack doubling / halving, substitute
 * toggles, clear, buttons) is reused. {@link #encode()} is a faithful port of the
 * vanilla encode with the probability parameters baked into the
 * {@link ProbabilityPatternItem}, and the two probability controls are exposed.
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
     * Faithful port of GTNH AE2 695's {@code ContainerPatternTerm.encode()}: 9 fake
     * crafting inputs (incl. nulls), non-null outputs, {@code createItemTag} = writeToNBT
     * + integer {@code Count}, tags {@code in}/{@code out}/{@code crafting}/{@code substitute}/
     * {@code beSubstitute}/{@code author}. The output pattern is our own
     * {@link ProbabilityPatternItem} carrying the vanilla NBT plus the {@code sp_*} tags.
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

        final List<ItemStack> out = new ArrayList<ItemStack>();
        for (int i = 0; i < outputInv.getSizeInventory(); i++) {
            final ItemStack s = outputInv.getStackInSlot(i);
            if (s != null) {
                out.add(s);
            }
        }
        if (out.isEmpty()) {
            return;
        }

        if (output != null && !this.isPattern(output)) {
            return;
        }

        if (output == null) {
            final ItemStack blank = patternInv.getStackInSlot(0);
            if (blank == null || !this.isPattern(blank)) {
                return;
            }
            blank.stackSize--;
            if (blank.stackSize <= 0) {
                patternInv.setInventorySlotContents(0, null);
            }
            output = new ItemStack(ProbabilityPatternMod.probabilityPatternItem);
        }

        final NBTTagCompound encodedValue = new NBTTagCompound();
        final NBTTagList tagIn = new NBTTagList();
        for (final ItemStack i : in) {
            tagIn.appendTag(createItemTag(i));
        }
        final NBTTagList tagOut = new NBTTagList();
        for (final ItemStack i : out) {
            tagOut.appendTag(createItemTag(i));
        }
        encodedValue.setTag(EncodedStatisticalPattern.TAG_INPUTS, tagIn);
        encodedValue.setTag(EncodedStatisticalPattern.TAG_OUTPUT, tagOut);
        encodedValue.setBoolean(EncodedStatisticalPattern.TAG_CRAFTING, this.craftingMode);
        encodedValue.setBoolean(EncodedStatisticalPattern.TAG_SUBSTITUTE, this.substitute);
        encodedValue.setBoolean("beSubstitute", this.beSubstitute);
        encodedValue.setString("author", this.getPlayerInv().player.getCommandSenderName());

        encodedValue.setDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY, this.getProbability());
        encodedValue.setDouble(EncodedStatisticalPattern.TAG_ALPHA, this.isAlpha95() ? 0.05 : 0.01);
        encodedValue.setBoolean(EncodedStatisticalPattern.TAG_ALPHA95, this.isAlpha95());

        output.setTagCompound(encodedValue);
        patternInv.setInventorySlotContents(1, output);
        this.saveChanges();
        this.detectAndSendChanges();

        // Diagnostic: verify the freshly encoded pattern decodes through our getPatternForItem.
        try {
            final ICraftingPatternDetails d = ((ICraftingPatternItem) ProbabilityPatternMod.probabilityPatternItem)
                .getPatternForItem(
                    output,
                    this.getPatternTerminal()
                        .getTile()
                        .getWorldObj());
            if (d == null) {
                FMLLog.info("[ProbabilityPattern] encode: RESULT INVALID (getPatternForItem returned null)");
            } else {
                FMLLog.info(
                    "[ProbabilityPattern] encode: OK inputs=%d outputs=%d inStack=%d outStack=%d p=%.3f alpha=%.3f",
                    d.getInputs().length,
                    d.getOutputs().length,
                    d.getInputs().length > 0 && d.getInputs()[0] != null ? d.getInputs()[0].getStackSize() : -1L,
                    d.getOutputs().length > 0 && d.getOutputs()[0] != null ? d.getOutputs()[0].getStackSize() : -1L,
                    this.getProbability(),
                    this.isAlpha95() ? 0.05 : 0.01);
            }
        } catch (final Throwable t) {
            FMLLog.info("[ProbabilityPattern] encode: decode threw: %s", t.toString());
        }
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

    /**
     * Exactly GTNH 695's {@code ContainerPatternTerm.createItemTag}: write the stack, then
     * store the amount as an integer {@code Count} (read by
     * {@code Platform.loadItemStackFromNBT}). Empty slots become empty tags.
     */
    private static NBTBase createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();
        if (i != null) {
            Platform.writeItemStackToNBT(i, c);
        }
        return c;
    }
}
