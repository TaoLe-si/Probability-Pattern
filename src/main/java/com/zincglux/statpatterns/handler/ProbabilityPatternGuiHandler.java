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
package com.zincglux.statpatterns.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.zincglux.statpatterns.container.ContainerProbabilityPatternTerm;
import com.zincglux.statpatterns.container.ContainerProbabilityPatternValueAmount;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiProbabilityPatternTerm;
import appeng.client.gui.implementations.GuiProbabilityPatternValueAmount;
import appeng.container.ContainerOpenContext;
import cpw.mods.fml.common.network.IGuiHandler;

/**
 * Forge GUI handler (Spec v2.0 3.1 / 5.1). The gui id encodes the part side on the
 * cable bus so the exact {@link ProbabilityPatternTerminalPart} can be resolved from
 * the tile. Ids 0-5 open the probability terminal, ids 8-13 open the self-implemented
 * "set pattern value amount" dialog for that side.
 */
public class ProbabilityPatternGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(final int ID, final EntityPlayer player, final World world, final int x,
        final int y, final int z) {
        final int sideId = ID & 7;
        final ProbabilityPatternTerminalPart part = findPart(world, x, y, z, sideId);
        if (part != null) {
            if (ID >= 8) {
                return new ContainerProbabilityPatternValueAmount(player.inventory, part);
            }
            final ContainerProbabilityPatternTerm container = new ContainerProbabilityPatternTerm(
                player.inventory,
                (ITerminalHost) part);
            // AE's GuiBridge normally calls setOpenContext(...) when a container is opened
            // via Platform.openGUI; our handler constructs the container directly, so we
            // must populate the open context ourselves - otherwise every feature that relies
            // on getOpenContext() (e.g. middle-click SET_PATTERN_VALUE quantity dialog)
            // silently breaks because the context stays null.
            final ForgeDirection side = ForgeDirection.getOrientation(sideId);
            container.setOpenContext(new ContainerOpenContext(part.getTile()));
            container.getOpenContext()
                .setWorld(world);
            container.getOpenContext()
                .setX(x);
            container.getOpenContext()
                .setY(y);
            container.getOpenContext()
                .setZ(z);
            container.getOpenContext()
                .setSide(side);
            return container;
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(final int ID, final EntityPlayer player, final World world, final int x,
        final int y, final int z) {
        final int sideId = ID & 7;
        final ProbabilityPatternTerminalPart part = findPart(world, x, y, z, sideId);
        if (part != null) {
            if (ID >= 8) {
                return new GuiProbabilityPatternValueAmount(player.inventory, (ITerminalHost) part);
            }
            return new GuiProbabilityPatternTerm(player.inventory, (ITerminalHost) part);
        }
        return null;
    }

    private ProbabilityPatternTerminalPart findPart(final World world, final int x, final int y, final int z,
        final int sideId) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IPartHost) {
            final IPart part = ((IPartHost) te).getPart(ForgeDirection.getOrientation(sideId));
            if (part instanceof ProbabilityPatternTerminalPart) {
                return (ProbabilityPatternTerminalPart) part;
            }
        }
        return null;
    }
}
