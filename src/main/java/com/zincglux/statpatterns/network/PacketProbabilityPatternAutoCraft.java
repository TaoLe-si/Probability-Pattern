/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.zincglux.statpatterns.network;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Client-to-server request to open AE2's crafting amount dialog from this terminal. */
public class PacketProbabilityPatternAutoCraft implements IMessage {

    private ItemStack target;

    public PacketProbabilityPatternAutoCraft() {}

    public PacketProbabilityPatternAutoCraft(final ItemStack target) {
        this.target = target;
    }

    public ItemStack getTarget() {
        return this.target;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.target = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, this.target);
    }
}
