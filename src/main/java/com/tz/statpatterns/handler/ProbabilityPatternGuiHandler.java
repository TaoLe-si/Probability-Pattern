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
package com.tz.statpatterns.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.IGuiHandler;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.ITerminalHost;

import appeng.client.gui.implementations.GuiProbabilityPatternTerm;
import com.tz.statpatterns.container.ContainerProbabilityPatternTerm;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;

/**
 * Forge GUI handler. The gui id encodes the part side on the cable bus so the
 * exact {@link ProbabilityPatternTerminalPart} can be resolved from the tile.
 */
public class ProbabilityPatternGuiHandler implements IGuiHandler
{
	@Override
	public Object getServerGuiElement( final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z )
	{
		final ProbabilityPatternTerminalPart part = findPart( world, x, y, z, ID );
		if( part != null )
		{
			return new ContainerProbabilityPatternTerm( player.inventory, (ITerminalHost) part );
		}
		return null;
	}

	@Override
	public Object getClientGuiElement( final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z )
	{
		final ProbabilityPatternTerminalPart part = findPart( world, x, y, z, ID );
		if( part != null )
		{
			return new GuiProbabilityPatternTerm( player.inventory, (ITerminalHost) part );
		}
		return null;
	}

	private ProbabilityPatternTerminalPart findPart( final World world, final int x, final int y, final int z, final int sideId )
	{
		final TileEntity te = world.getTileEntity( x, y, z );
		if( te instanceof IPartHost )
		{
			final IPart part = ( (IPartHost) te ).getPart( ForgeDirection.getOrientation( sideId ) );
			if( part instanceof ProbabilityPatternTerminalPart )
			{
				return (ProbabilityPatternTerminalPart) part;
			}
		}
		return null;
	}
}
