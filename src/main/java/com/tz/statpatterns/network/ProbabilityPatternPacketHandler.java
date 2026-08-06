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
package com.tz.statpatterns.network;

import net.minecraft.entity.player.EntityPlayer;

import com.tz.statpatterns.container.ContainerProbabilityPatternTerm;
import com.tz.statpatterns.network.ProbabilityPatternPacket.Action;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link ProbabilityPatternPacket}.
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
            container.setProbability(message.getValue());
        } else if (action == Action.SET_ALPHA95) {
            container.setAlpha95(message.getValue() != 0.0);
        } else if (action == Action.ENCODE) {
            container.encode();
        } else if (action == Action.CLEAR) {
            container.clear();
        }

        return null;
    }
}
