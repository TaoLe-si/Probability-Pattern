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
package com.tz.statpatterns.crafting;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.tz.statpatterns.ProbabilityPatternMod;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Probability Pattern item.
 * <p>
 * Acts as both the blank pattern (no NBT) and the encoded probability pattern
 * (carries the AE2 "in"/"out" tags plus the "sp_*" probability tags).
 * Implements {@link ICraftingPatternItem} so AE2's crafting grid decodes it through
 * {@link StatisticalPatternDetails} and the probability sizing coremod kicks in.
 */
public class ProbabilityPatternItem extends Item implements ICraftingPatternItem {

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    public ProbabilityPatternItem() {
        this.setMaxStackSize(1);
        this.setUnlocalizedName("probabilitypattern.probability_pattern");
        this.setTextureName("probabilitypattern:probability_pattern");
    }

    @Override
    public ICraftingPatternDetails getPatternForItem(final ItemStack is, final World w) {
        if (is == null || is.getItem() != this || !is.hasTagCompound()) {
            return null; // blank pattern
        }
        if (EncodedStatisticalPattern.decode(is.getTagCompound()) == null) {
            return null;
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
            && is.getTagCompound()
                .hasKey(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(final IIconRegister iconRegister) {
        this.icon = iconRegister.registerIcon("probabilitypattern:probability_pattern");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(final int meta) {
        return this.icon;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void getSubItems(final Item item, final CreativeTabs tab, final List list) {
        if (tab == ProbabilityPatternMod.creativeTab) {
            list.add(new ItemStack(item, 1, 0));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List lines,
        final boolean advanced) {
        if (!isEncoded(stack)) {
            return;
        }

        // Show the encoded recipe the same way GTNH's ItemEncodedPattern does: outputs
        // always, inputs while holding Shift.
        try {
            final ICraftingPatternDetails details = this.getPatternForItem(stack, player.worldObj);
            if (details != null) {
                final IAEItemStack[] out = details.getCondensedOutputs();
                if (out != null && out.length > 0) {
                    lines.add(
                        EnumChatFormatting.DARK_AQUA
                            + StatCollector.translateToLocal("probabilitypattern.tooltip.result")
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
                                + StatCollector.translateToLocal("probabilitypattern.tooltip.ingredients")
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
                            EnumChatFormatting.GRAY
                                + StatCollector.translateToLocal("probabilitypattern.tooltip.hold_shift")
                                + EnumChatFormatting.RESET);
                    }
                }
            }
        } catch (final Throwable ignored) {
            // tooltip rendering must never crash
        }

        final NBTTagCompound tag = stack.getTagCompound();
        final double p = tag.getDouble(EncodedStatisticalPattern.TAG_SUCCESS_PROBABILITY);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "probabilitypattern.tooltip.success_probability",
                String.format("%.0f%%", p * 100.0)));
        final double alpha = tag.getDouble(EncodedStatisticalPattern.TAG_ALPHA);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "probabilitypattern.tooltip.alpha",
                String.format("%.0f%%", (1.0 - alpha) * 100.0)));
    }
}
