/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.zincglux.statpatterns.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.zincglux.statpatterns.container.ContainerProbabilityPatternTerm;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/** Server-side bridge from the probability terminal to AE2's craft amount GUI. */
public class PacketProbabilityPatternAutoCraftHandler
    implements IMessageHandler<PacketProbabilityPatternAutoCraft, IMessage> {

    @Override
    public IMessage onMessage(final PacketProbabilityPatternAutoCraft message, final MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        ProbabilityPatternServerTaskQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                PacketProbabilityPatternAutoCraftHandler.this.openCraftAmount(player, message.getTarget());
            }
        });
        return null;
    }

    private void openCraftAmount(final EntityPlayerMP player, final ItemStack requestedStack) {
        if (player == null || requestedStack == null
            || !(player.openContainer instanceof ContainerProbabilityPatternTerm)) {
            return;
        }

        final ContainerProbabilityPatternTerm terminal = (ContainerProbabilityPatternTerm) player.openContainer;
        final ProbabilityPatternTerminalPart part = (ProbabilityPatternTerminalPart) terminal.getPatternTerminal();
        final TileEntity tile = part.getTile();
        if (tile == null) {
            return;
        }

        final ItemStack targetStack = requestedStack.copy();
        targetStack.stackSize = Math.max(1, targetStack.stackSize);
        final IAEItemStack target = AEApi.instance()
            .storage()
            .createItemStack(targetStack);
        if (target == null) {
            return;
        }

        Platform.openGUI(player, tile, part.getSide(), GuiBridge.GUI_CRAFTING_AMOUNT);
        final Container openContainer = player.openContainer;
        if (!(openContainer instanceof ContainerCraftAmount)) {
            return;
        }

        final ContainerCraftAmount craftAmount = (ContainerCraftAmount) openContainer;
        craftAmount.getCraftingItem()
            .putStack(targetStack);
        craftAmount.setItemToCraft(target);
        craftAmount.setInitialCraftAmount(1L);
        craftAmount.detectAndSendChanges();
    }
}
