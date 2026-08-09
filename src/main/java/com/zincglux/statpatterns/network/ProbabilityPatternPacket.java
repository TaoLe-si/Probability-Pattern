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

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * Client -> server packet for the probability pattern terminal (Spec v2.0 3.3.5).
 * <p>
 * Carries only the two probability controls:
 * <ul>
 * <li>{@link Action#SET_PROBABILITY}: sets the single-attempt success probability,
 * payload is a double in (0, 1].</li>
 * <li>{@link Action#SET_ALPHA95}: sets the confidence, payload is an int
 * (1 = 95% confidence, 0 = 99% confidence).</li>
 * </ul>
 * Every other terminal action (encode, clear, tabs, substitute, NEI transfer, stack
 * doubling) is handled by AE2 GTNH's own mechanisms on the inherited
 * {@code ContainerPatternTerm}.
 */
public class ProbabilityPatternPacket implements IMessage {

    public enum Action {
        SET_PROBABILITY,
        SET_ALPHA95
    }

    private Action action;
    private double probability;
    private int alpha95;

    public ProbabilityPatternPacket() {
        // required by SimpleNetworkWrapper
    }

    public ProbabilityPatternPacket(final Action action, final double probability, final int alpha95) {
        this.action = action;
        this.probability = probability;
        this.alpha95 = alpha95;
    }

    public Action getAction() {
        return this.action;
    }

    public double getProbability() {
        return this.probability;
    }

    public int getAlpha95() {
        return this.alpha95;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
        this.probability = buf.readDouble();
        this.alpha95 = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(this.action.ordinal());
        buf.writeDouble(this.probability);
        buf.writeInt(this.alpha95);
    }
}
