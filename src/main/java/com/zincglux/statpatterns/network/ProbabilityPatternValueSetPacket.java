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
 * Client -> server packet for the self-implemented "set pattern value amount" flow
 * (middle-click on an encoding-grid fake slot).
 * <p>
 * AE's vanilla {@code GuiPatternValueAmount} reopens the parent GUI through
 * {@code GuiBridge.GUI_PATTERN_TERMINAL} (the vanilla terminal), so we deliberately do
 * NOT use it. Instead the client opens OUR own amount GUI:
 * <ul>
 * <li>{@link Action#OPEN}: client asks the server to open our amount GUI for the
 * encoding-grid slot {@code valueIndex}.</li>
 * <li>{@link Action#SET}: the amount GUI confirms a new quantity; the server writes it
 * back into the encoding grid and reopens OUR probability terminal.</li>
 * </ul>
 */
public class ProbabilityPatternValueSetPacket implements IMessage {

    public enum Action {
        OPEN,
        SET
    }

    private Action action;
    private int valueIndex;
    private int amount;

    public ProbabilityPatternValueSetPacket() {
        // required by SimpleNetworkWrapper
    }

    public ProbabilityPatternValueSetPacket(final Action action, final int valueIndex, final int amount) {
        this.action = action;
        this.valueIndex = valueIndex;
        this.amount = amount;
    }

    public Action getAction() {
        return this.action;
    }

    public int getValueIndex() {
        return this.valueIndex;
    }

    public int getAmount() {
        return this.amount;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
        this.valueIndex = buf.readInt();
        this.amount = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(this.action.ordinal());
        buf.writeInt(this.valueIndex);
        buf.writeInt(this.amount);
    }
}
