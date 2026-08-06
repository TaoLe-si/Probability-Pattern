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

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * Client -> server packet for the probability pattern terminal.
 */
public class ProbabilityPatternPacket implements IMessage {

    public enum Action {
        SET_PROBABILITY,
        SET_ALPHA95,
        ENCODE,
        CLEAR
    }

    private Action action;
    private double value;

    public ProbabilityPatternPacket() {
        // required by SimpleNetworkWrapper
    }

    public ProbabilityPatternPacket(final Action action, final double value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return this.action;
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
        this.value = buf.readDouble();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(this.action.ordinal());
        buf.writeDouble(this.value);
    }
}
