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
package com.zincglux.statpatterns.network;

import net.minecraft.entity.player.EntityPlayer;

import com.zincglux.statpatterns.container.ContainerProbabilityPatternTerm;
import com.zincglux.statpatterns.network.ProbabilityPatternPacket.Action;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link ProbabilityPatternPacket} (Spec v2.0 3.3.5): applies
 * the probability / confidence changes to the probability pattern terminal part.
 */
public class ProbabilityPatternPacketHandler implements IMessageHandler<ProbabilityPatternPacket, IMessage> {

    @Override
    public IMessage onMessage(final ProbabilityPatternPacket message, final MessageContext ctx) {
        final EntityPlayer player = ctx.getServerHandler().playerEntity;
        if (player == null || !(player.openContainer instanceof ContainerProbabilityPatternTerm)) {
            return null;
        }

        final ContainerProbabilityPatternTerm container = (ContainerProbabilityPatternTerm) player.openContainer;
        final Action action = message.getAction();

        if (action == Action.SET_PROBABILITY) {
            container.setProbability(message.getProbability());
        } else if (action == Action.SET_ALPHA95) {
            container.setAlpha95(message.getAlpha95() != 0);
        }

        return null;
    }
}
