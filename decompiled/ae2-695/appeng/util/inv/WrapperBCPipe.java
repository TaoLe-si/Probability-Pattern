/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.util.inv;

import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.abstraction.IBuildCraftTransport;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class WrapperBCPipe
implements IInventory {
    private final IBuildCraftTransport bc = (IBuildCraftTransport)IntegrationRegistry.INSTANCE.getInstance(IntegrationType.BuildCraftTransport);
    private final TileEntity ad;
    private final ForgeDirection dir;

    public WrapperBCPipe(TileEntity te, ForgeDirection d) {
        this.ad = te;
        this.dir = d;
    }

    public int getSizeInventory() {
        return 1;
    }

    public ItemStack getStackInSlot(int i) {
        return null;
    }

    public ItemStack decrStackSize(int i, int j) {
        return null;
    }

    public ItemStack getStackInSlotOnClosing(int i) {
        return null;
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.BuildCraftTransport)) {
            this.bc.addItemsToPipe(this.ad, itemstack, this.dir);
        }
    }

    public String getInventoryName() {
        return "BC Pipe Wrapper";
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public void markDirty() {
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return false;
    }

    public void openInventory() {
    }

    public void closeInventory() {
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return this.bc.canAddItemsToPipe(this.ad, itemstack, this.dir);
    }
}

