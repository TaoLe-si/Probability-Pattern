/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.eventhandler.Event
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.container.AEBaseContainer;
import appeng.container.slot.QuartzKnifeOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.items.contents.QuartzKnifeObj;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.Platform;
import cpw.mods.fml.common.eventhandler.Event;
import java.util.Iterator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

public class ContainerQuartzKnife
extends AEBaseContainer
implements IAEAppEngInventory,
IInventory {
    private final QuartzKnifeObj toolInv;
    private final AppEngInternalInventory inSlot = new AppEngInternalInventory(this, 1);
    private final SlotRestrictedInput metals;
    private final QuartzKnifeOutput output;
    private String myName = "";

    public ContainerQuartzKnife(InventoryPlayer ip, QuartzKnifeObj te) {
        super(ip, null, null);
        this.toolInv = te;
        this.metals = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.METAL_INGOTS, this.inSlot, 0, 94, 44, ip);
        this.addSlotToContainer(this.metals);
        this.output = new QuartzKnifeOutput(this, 0, 134, 44, -1);
        this.addSlotToContainer(this.output);
        this.lockPlayerInventorySlot(ip.currentItem);
        this.bindPlayerInventory(ip, 0, 102);
    }

    public void setName(String value) {
        this.myName = value;
    }

    @Override
    public void detectAndSendChanges() {
        ItemStack currentItem = this.getPlayerInv().getCurrentItem();
        if (currentItem != this.toolInv.getItemStack()) {
            if (currentItem != null) {
                if (Platform.isSameItem(this.toolInv.getItemStack(), currentItem)) {
                    this.getPlayerInv().setInventorySlotContents(this.getPlayerInv().currentItem, this.toolInv.getItemStack());
                } else {
                    this.setValidContainer(false);
                }
            } else {
                this.setValidContainer(false);
            }
        }
        super.detectAndSendChanges();
    }

    public void onContainerClosed(EntityPlayer par1EntityPlayer) {
        if (this.inSlot.getStackInSlot(0) != null) {
            par1EntityPlayer.dropPlayerItemWithRandomChoice(this.inSlot.getStackInSlot(0), false);
        }
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
    }

    public int getSizeInventory() {
        return 1;
    }

    public ItemStack getStackInSlot(int var1) {
        Iterator iterator;
        ItemStack input = this.inSlot.getStackInSlot(0);
        if (input == null) {
            return null;
        }
        if (SlotRestrictedInput.isMetalIngot(input) && this.myName.length() > 0 && (iterator = AEApi.instance().definitions().materials().namePress().maybeStack(1).asSet().iterator()).hasNext()) {
            ItemStack namePressStack = (ItemStack)iterator.next();
            NBTTagCompound compound = Platform.openNbtData(namePressStack);
            compound.setString("InscribeName", this.myName);
            return namePressStack;
        }
        return null;
    }

    public ItemStack decrStackSize(int var1, int var2) {
        ItemStack is = this.getStackInSlot(0);
        if (is != null && this.makePlate()) {
            return is;
        }
        return null;
    }

    private boolean makePlate() {
        if (this.inSlot.decrStackSize(0, 1) != null) {
            ItemStack item = this.toolInv.getItemStack();
            item.damageItem(1, (EntityLivingBase)this.getPlayerInv().player);
            if (item.stackSize == 0) {
                this.getPlayerInv().mainInventory[this.getPlayerInv().currentItem] = null;
                MinecraftForge.EVENT_BUS.post((Event)new PlayerDestroyItemEvent(this.getPlayerInv().player, item));
            }
            return true;
        }
        return false;
    }

    public ItemStack getStackInSlotOnClosing(int var1) {
        return null;
    }

    public void setInventorySlotContents(int var1, ItemStack var2) {
        if (var2 == null && Platform.isServer()) {
            this.makePlate();
        }
    }

    public String getInventoryName() {
        return "Quartz Knife Output";
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    public int getInventoryStackLimit() {
        return 1;
    }

    public void markDirty() {
    }

    public boolean isUseableByPlayer(EntityPlayer var1) {
        return false;
    }

    public void openInventory() {
    }

    public void closeInventory() {
    }

    public boolean isItemValidForSlot(int var1, ItemStack var2) {
        return false;
    }
}

