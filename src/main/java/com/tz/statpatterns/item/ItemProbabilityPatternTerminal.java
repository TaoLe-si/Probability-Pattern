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
package com.tz.statpatterns.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.tz.statpatterns.ProbabilityPatternMod;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Item form of the ME Probability Pattern Encoding Terminal part.
 * <p>
 * Implements {@link IPartItem} and delegates placement to AE2's cable bus system
 * (the same mechanism AE2 uses for its own parts).
 */
public class ItemProbabilityPatternTerminal extends Item implements IPartItem {

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    public ItemProbabilityPatternTerminal() {
        this.setMaxStackSize(64);
        this.setUnlocalizedName("probabilitypattern.probability_pattern_terminal");
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer(this);
    }

    @Override
    public IPart createPartFromItemStack(final ItemStack is) {
        return new ProbabilityPatternTerminalPart(is);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0; // use the block texture atlas
    }

    @Override
    public boolean onItemUse(final ItemStack is, final EntityPlayer player, final World world, final int x, final int y,
        final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        return AEApi.instance()
            .partHelper()
            .placeBus(is, x, y, z, side, player, world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(final IIconRegister iconRegister) {
        this.icon = iconRegister.registerIcon("probabilitypattern:probability_pattern_terminal");
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
}
