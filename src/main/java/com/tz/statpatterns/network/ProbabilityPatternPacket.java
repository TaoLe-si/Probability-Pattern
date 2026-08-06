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

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * Client -> server packet for the probability pattern terminal.
 * <p>
 * {@link Action#NEI_RECIPE} carries a 3x3 per-attempt input grid plus one target
 * output, serialised as ItemStacks (may be null).
 */
public class ProbabilityPatternPacket implements IMessage {

    public enum Action {
        SET_PROBABILITY,
        SET_ALPHA95,
        ENCODE,
        CLEAR,
        NEI_RECIPE
    }

    private Action action;
    private double value;
    private ItemStack[] inputs;
    private ItemStack output;

    public ProbabilityPatternPacket() {
        // required by SimpleNetworkWrapper
    }

    public ProbabilityPatternPacket(final Action action, final double value) {
        this.action = action;
        this.value = value;
    }

    public ProbabilityPatternPacket(final Action action, final ItemStack[] inputs, final ItemStack output) {
        this.action = action;
        this.inputs = inputs;
        this.output = output;
    }

    public Action getAction() {
        return this.action;
    }

    public double getValue() {
        return this.value;
    }

    public ItemStack[] getInputs() {
        return this.inputs;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
        if (this.action == Action.NEI_RECIPE) {
            this.inputs = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                this.inputs[i] = ByteBufUtils.readItemStack(buf);
            }
            this.output = ByteBufUtils.readItemStack(buf);
        } else {
            this.value = buf.readDouble();
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(this.action.ordinal());
        if (this.action == Action.NEI_RECIPE) {
            for (int i = 0; i < 9; i++) {
                ByteBufUtils.writeItemStack(buf, this.inputs != null && i < this.inputs.length ? this.inputs[i] : null);
            }
            ByteBufUtils.writeItemStack(buf, this.output);
        } else {
            buf.writeDouble(this.value);
        }
    }
}
