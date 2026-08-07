/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.SlotFake;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class OptionalSlotFake
extends SlotFake {
    private final int srcX;
    private final int srcY;
    private final int groupNum;
    private final IOptionalSlotHost host;
    private boolean renderDisabled = true;

    public OptionalSlotFake(IInventory inv, IOptionalSlotHost containerBus, int idx, int x, int y, int groupNum) {
        super(inv, idx, x, y);
        this.srcX = x;
        this.srcY = y;
        this.groupNum = groupNum;
        this.host = containerBus;
    }

    public OptionalSlotFake(IInventory inv, IOptionalSlotHost containerBus, int idx, int x, int y, int offX, int offY, int groupNum) {
        super(inv, idx, x + offX * 18, y + offY * 18);
        this.srcX = x;
        this.srcY = y;
        this.groupNum = groupNum;
        this.host = containerBus;
    }

    @Override
    public ItemStack getStack() {
        if (!this.isEnabled() && this.getDisplayStack() != null) {
            this.clearStack();
        }
        return super.getStack();
    }

    @Override
    public boolean isEnabled() {
        if (this.host == null) {
            return false;
        }
        return this.host.isSlotEnabled(this.groupNum);
    }

    public boolean renderDisabled() {
        return this.isRenderDisabled();
    }

    private boolean isRenderDisabled() {
        return this.renderDisabled;
    }

    public void setRenderDisabled(boolean renderDisabled) {
        this.renderDisabled = renderDisabled;
    }

    public int getSourceX() {
        return this.srcX;
    }

    public int getSourceY() {
        return this.srcY;
    }
}

