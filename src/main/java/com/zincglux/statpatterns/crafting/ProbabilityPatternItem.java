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
package com.zincglux.statpatterns.crafting;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.zincglux.statpatterns.ProbabilityPatternMod;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Probability Pattern item (Spec v2.0 3.2.1).
 * <p>
 * Extends AE2's {@link ItemEncodedPattern} to reuse its public methods and machinery:
 * <ul>
 * <li>{@code getOutput(ItemStack)} — the finished-good stack used by AE2's
 * {@code ItemEncodedPatternRenderer} for the Shift-hold texture swap (our own renderer is
 * therefore unnecessary; being a subclass makes {@code instanceof ItemEncodedPattern}
 * true so AE2's renderer applies automatically).</li>
 * <li>{@code onItemRightClick}/{@code onItemUseFirst} — Shift-right-click clears the
 * pattern back to a blank pattern.</li>
 * <li>The client-only {@code MinecraftForgeClient.registerItemRenderer} call in the
 * superclass constructor.</li>
 * </ul>
 * The only behaviour overridden is {@link #getPatternForItem}, which decodes the
 * probability parameters into a {@link StatisticalPatternDetails} so the crafting-tree
 * mixin kicks in, plus the icon and tooltip so it renders/labels as a probability pattern.
 */
public class ProbabilityPatternItem extends ItemEncodedPattern {

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    public ProbabilityPatternItem() {
        super();
        this.setMaxStackSize(1);
        this.setUnlocalizedName("statpatterns.probability_pattern");
        this.setTextureName("statpatterns:probability_pattern");
    }

    @Override
    public ICraftingPatternDetails getPatternForItem(final ItemStack is, final World w) {
        if (is == null || !(is.getItem() instanceof ProbabilityPatternItem)) {
            return null; // not our item
        }
        if (!is.hasTagCompound() || !EncodedStatisticalPattern.isProbabilityPattern(is.getTagCompound())) {
            return null; // blank or invalid
        }
        try {
            return new StatisticalPatternDetails(is, w);
        } catch (final Throwable t) {
            return null;
        }
    }

    /**
     * @return true if this stack is an encoded (non-blank) probability pattern.
     */
    public static boolean isEncoded(final ItemStack is) {
        return is != null && is.getItem() instanceof ProbabilityPatternItem
            && is.hasTagCompound()
            && EncodedStatisticalPattern.isProbabilityPattern(is.getTagCompound());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(final IIconRegister iconRegister) {
        this.icon = iconRegister.registerIcon("statpatterns:probability_pattern");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(final int meta) {
        return this.icon;
    }

    @Override
    protected void getCheckedSubItems(final Item sameItem, final CreativeTabs creativeTab,
        final List<ItemStack> itemStacks) {
        if (creativeTab == ProbabilityPatternMod.creativeTab) {
            itemStacks.add(new ItemStack(sameItem, 1, 0));
        }
    }

    /**
     * Tooltip: outputs always, inputs while holding Shift, then probability / confidence
     * lines. AE2's {@code addInformation} is final, so we override {@code addCheckedInformation}.
     */
    @Override
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> lines,
        final boolean displayMoreInfo) {
        if (!isEncoded(stack)) {
            return;
        }

        // Outputs always, inputs while holding Shift (like GTNH's ItemEncodedPattern),
        // then the probability / confidence lines.
        try {
            final ICraftingPatternDetails details = this.getPatternForItem(stack, player.worldObj);
            if (details != null) {
                final IAEItemStack[] out = details.getCondensedOutputs();
                if (out != null && out.length > 0) {
                    lines.add(
                        EnumChatFormatting.DARK_AQUA + StatCollector.translateToLocal("statpatterns.tooltip.result")
                            + ":"
                            + EnumChatFormatting.RESET);
                    for (final IAEItemStack s : out) {
                        lines.add(
                            "   " + EnumChatFormatting.WHITE
                                + s.getStackSize()
                                + "x "
                                + EnumChatFormatting.RESET
                                + s.getItemStack()
                                    .getDisplayName());
                    }
                }

                final IAEItemStack[] in = details.getCondensedInputs();
                if (in != null && in.length > 0) {
                    if (GuiScreen.isShiftKeyDown()) {
                        lines.add(
                            EnumChatFormatting.DARK_GREEN
                                + StatCollector.translateToLocal("statpatterns.tooltip.ingredients")
                                + ": "
                                + EnumChatFormatting.RESET);
                        for (final IAEItemStack s : in) {
                            lines.add(
                                "   " + EnumChatFormatting.WHITE
                                    + s.getStackSize()
                                    + "x "
                                    + EnumChatFormatting.RESET
                                    + s.getItemStack()
                                        .getDisplayName());
                        }
                    } else {
                        lines.add(
                            EnumChatFormatting.GRAY + StatCollector.translateToLocal("statpatterns.tooltip.hold_shift")
                                + EnumChatFormatting.RESET);
                    }
                }
            }
        } catch (final Throwable ignored) {
            // tooltip rendering must never crash
        }

        final double p = stack.getTagCompound()
            .getDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "statpatterns.tooltip.success_probability",
                String.format("%.0f%%", p * 100.0)));
        final double alpha = stack.getTagCompound()
            .getDouble(EncodedStatisticalPattern.TAG_ALPHA);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "statpatterns.tooltip.alpha",
                String.format("%.0f%%", (1.0 - alpha) * 100.0)));
    }
}
