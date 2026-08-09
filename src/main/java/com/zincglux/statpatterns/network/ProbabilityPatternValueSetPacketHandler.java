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
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.zincglux.statpatterns.ProbabilityPatternMod;
import com.zincglux.statpatterns.container.ContainerProbabilityPatternTerm;
import com.zincglux.statpatterns.container.ContainerProbabilityPatternValueAmount;
import com.zincglux.statpatterns.network.ProbabilityPatternValueSetPacket.Action;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.container.implementations.ContainerPatternTerm;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link ProbabilityPatternValueSetPacket} — the self-implemented
 * replacement for AE's {@code PacketPatternValueSet} which would reopen the VANILLA
 * pattern terminal instead of our probability terminal.
 */
public class ProbabilityPatternValueSetPacketHandler
    implements IMessageHandler<ProbabilityPatternValueSetPacket, IMessage> {

    /**
     * GUI id offset used by {@link com.zincglux.statpatterns.handler.ProbabilityPatternGuiHandler}
     * to distinguish the amount dialog (id >= 8) from the terminal (id = side ordinal).
     */
    private static final int AMOUNT_GUI_BASE = 8;

    @Override
    public IMessage onMessage(final ProbabilityPatternValueSetPacket message, final MessageContext ctx) {
        final EntityPlayer player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }

        final Action action = message.getAction();
        if (action == Action.OPEN) {
            this.openAmountGui(player, message.getValueIndex());
        } else if (action == Action.SET) {
            this.applyAmountAndReopen(player, message.getAmount());
        }
        return null;
    }

    /** OPEN: remember which encoding-grid/output slot and open our own amount GUI. */
    private void openAmountGui(final EntityPlayer player, final int valueIndex) {
        if (!(player.openContainer instanceof ContainerProbabilityPatternTerm)) {
            return;
        }
        final ContainerProbabilityPatternTerm terminal = (ContainerProbabilityPatternTerm) player.openContainer;
        final ProbabilityPatternTerminalPart part = (ProbabilityPatternTerminalPart) terminal.getPatternTerminal();
        part.setPendingValueIndex(valueIndex);
        // Cache the item being adjusted from the ORIGINAL terminal's slot (works for both
        // the 3x3 encoding grid slots 0-8 and the output fake slots 10-12).
        final Slot slot = terminal.getSlot(valueIndex);
        part.setPendingValueStack(
            slot == null || !slot.getHasStack() ? null
                : slot.getStack()
                    .copy());

        final TileEntity te = part.getTile();
        player.openGui(
            ProbabilityPatternMod.instance,
            AMOUNT_GUI_BASE + part.getSide()
                .ordinal(),
            player.worldObj,
            te.xCoord,
            te.yCoord,
            te.zCoord);
    }

    /**
     * SET: reopen OUR terminal, then write the quantity THROUGH the terminal's encoding
     * slot (mirroring the vanilla {@code PacketPatternValueSet} flow). Going through
     * {@code getSlot(valueIndex).putStack(...)} reuses the vanilla encoding-slot
     * properties (SlotFakeCraftingMatrix / OptionalSlotFake) instead of hard-coding a
     * plain crafting-inventory write.
     */
    private void applyAmountAndReopen(final EntityPlayer player, final int amount) {
        if (!(player.openContainer instanceof ContainerProbabilityPatternValueAmount)) {
            return;
        }
        final ContainerProbabilityPatternValueAmount cpv = (ContainerProbabilityPatternValueAmount) player.openContainer;
        final ProbabilityPatternTerminalPart part = cpv.getPart();
        final int valueIndex = cpv.getValueIndex();

        // Reopen OUR terminal first so the open container is the encoding terminal again.
        final TileEntity te = part.getTile();
        player.openGui(
            ProbabilityPatternMod.instance,
            part.getSide()
                .ordinal(),
            player.worldObj,
            te.xCoord,
            te.yCoord,
            te.zCoord);

        // Set the quantity through the container slot to reuse the vanilla slot behaviour.
        if (player.openContainer instanceof ContainerPatternTerm) {
            final Slot slot = player.openContainer.getSlot(valueIndex);
            if (slot != null && slot.getHasStack()) {
                final ItemStack nextStack = slot.getStack()
                    .copy();
                nextStack.stackSize = Math.max(1, Math.min(nextStack.getMaxStackSize(), amount));
                slot.putStack(nextStack);
            }
        }
    }
}
