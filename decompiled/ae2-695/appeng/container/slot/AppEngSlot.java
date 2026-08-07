/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package appeng.container.slot;

import appeng.container.AEBaseContainer;
import appeng.tile.inventory.AppEngInternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class AppEngSlot
extends Slot {
    private final int defX;
    private final int defY;
    private boolean isDraggable = true;
    private boolean isPlayerSide = false;
    private AEBaseContainer myContainer = null;
    private int IIcon = -1;
    private hasCalculatedValidness isValid;
    private boolean isDisplay = false;

    public AppEngSlot(IInventory inv, int idx, int x, int y) {
        super(inv, idx, x, y);
        this.defX = x;
        this.defY = y;
        this.setIsValid(hasCalculatedValidness.NotAvailable);
    }

    public Slot setNotDraggable() {
        this.setDraggable(false);
        return this;
    }

    public Slot setPlayerSide() {
        this.isPlayerSide = true;
        return this;
    }

    public String getTooltip() {
        return null;
    }

    public void clearStack() {
        super.putStack(null);
    }

    public boolean isItemValid(ItemStack par1ItemStack) {
        if (this.isEnabled()) {
            return super.isItemValid(par1ItemStack);
        }
        return false;
    }

    public ItemStack getStack() {
        if (!this.isEnabled()) {
            return null;
        }
        if (this.inventory.getSizeInventory() <= this.getSlotIndex()) {
            return null;
        }
        if (this.isDisplay()) {
            this.setDisplay(false);
            return this.getDisplayStack();
        }
        return super.getStack();
    }

    public void putStack(ItemStack par1ItemStack) {
        if (this.isEnabled()) {
            super.putStack(par1ItemStack);
            if (this.getContainer() != null) {
                this.getContainer().onSlotChange(this);
            }
        }
    }

    public void onSlotChanged() {
        if (this.inventory instanceof AppEngInternalInventory) {
            ((AppEngInternalInventory)this.inventory).markDirty(this.getSlotIndex());
        } else {
            super.onSlotChanged();
        }
        this.setIsValid(hasCalculatedValidness.NotAvailable);
    }

    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        if (this.isEnabled()) {
            return super.canTakeStack(par1EntityPlayer);
        }
        return false;
    }

    public boolean func_111238_b() {
        return this.isEnabled();
    }

    public ItemStack getDisplayStack() {
        return super.getStack();
    }

    public boolean isEnabled() {
        return true;
    }

    public float getOpacityOfIcon() {
        return 0.4f;
    }

    public boolean renderIconWithItem() {
        return false;
    }

    public int getIcon() {
        return this.getIIcon();
    }

    public boolean isPlayerSide() {
        return this.isPlayerSide;
    }

    public boolean shouldDisplay() {
        return this.isEnabled();
    }

    public int getX() {
        return this.defX;
    }

    public int getY() {
        return this.defY;
    }

    private int getIIcon() {
        return this.IIcon;
    }

    public void setIIcon(int iIcon) {
        this.IIcon = iIcon;
    }

    private boolean isDisplay() {
        return this.isDisplay;
    }

    public void setDisplay(boolean isDisplay) {
        this.isDisplay = isDisplay;
    }

    public boolean isDraggable() {
        return this.isDraggable;
    }

    private void setDraggable(boolean isDraggable) {
        this.isDraggable = isDraggable;
    }

    void setPlayerSide(boolean isPlayerSide) {
        this.isPlayerSide = isPlayerSide;
    }

    public hasCalculatedValidness getIsValid() {
        return this.isValid;
    }

    public void setIsValid(hasCalculatedValidness isValid) {
        this.isValid = isValid;
    }

    AEBaseContainer getContainer() {
        return this.myContainer;
    }

    public void setContainer(AEBaseContainer myContainer) {
        this.myContainer = myContainer;
    }

    public static enum hasCalculatedValidness {
        NotAvailable,
        Valid,
        Invalid;

    }
}

