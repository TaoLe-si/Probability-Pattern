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
package com.zincglux.statpatterns.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.tile.inventory.AppEngInternalInventory;

/**
 * Container for the self-implemented "set pattern value amount" dialog. Holds a reference
 * to the {@link ProbabilityPatternTerminalPart} and the encoding-grid slot index whose
 * quantity is being edited. Used instead of AE's {@code ContainerPatternValueAmount} so
 * that confirming the amount reopens OUR terminal rather than the vanilla one.
 * <p>
 * Mirrors the vanilla dialog's {@code patternValue} read-only slot so the item being
 * adjusted is shown in the GUI (the "vanilla feature" the old version was missing).
 */
public class ContainerProbabilityPatternValueAmount extends AEBaseContainer {

    private final ProbabilityPatternTerminalPart part;
    private final int valueIndex;
    private final Slot patternValue = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);

    public ContainerProbabilityPatternValueAmount(final InventoryPlayer ip, final ProbabilityPatternTerminalPart part) {
        super(ip, part);
        this.part = part;
        this.valueIndex = part.getPendingValueIndex();
        this.addSlotToContainer(this.patternValue);
        // Show the item being adjusted. The OPEN packet caches it on the part via the
        // ORIGINAL terminal's slot (getSlot), so it works for both the encoding grid
        // (0-8) and the output fake slots (10-12) without guessing the inventory here.
        this.patternValue.putStack(
            part.getPendingValueStack() == null ? null
                : part.getPendingValueStack()
                    .copy());
    }

    public ProbabilityPatternTerminalPart getPart() {
        return this.part;
    }

    public int getValueIndex() {
        return this.valueIndex;
    }

    public Slot getPatternValue() {
        return this.patternValue;
    }
}
