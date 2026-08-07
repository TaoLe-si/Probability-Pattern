/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.FMLCommonHandler
 *  cpw.mods.fml.common.eventhandler.Event
 *  net.minecraft.block.Block
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.Item$ToolMaterial
 *  net.minecraft.item.ItemHoe
 *  net.minecraft.item.ItemPickaxe
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.item.ItemTool
 *  net.minecraft.stats.AchievementList
 *  net.minecraft.stats.StatBase
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
 */
package appeng.container.slot;

import appeng.container.slot.AppEngSlot;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

public class AppEngCraftingSlot
extends AppEngSlot {
    private final IInventory craftMatrix;
    private final EntityPlayer thePlayer;
    private int amountCrafted;

    public AppEngCraftingSlot(EntityPlayer par1EntityPlayer, IInventory par2IInventory, IInventory par3IInventory, int par4, int par5, int par6) {
        super(par3IInventory, par4, par5, par6);
        this.thePlayer = par1EntityPlayer;
        this.craftMatrix = par2IInventory;
    }

    @Override
    public boolean isItemValid(ItemStack par1ItemStack) {
        return false;
    }

    protected void onCrafting(ItemStack par1ItemStack, int par2) {
        this.amountCrafted += par2;
        this.onCrafting(par1ItemStack);
    }

    protected void onCrafting(ItemStack par1ItemStack) {
        par1ItemStack.onCrafting(this.thePlayer.worldObj, this.thePlayer, this.amountCrafted);
        this.amountCrafted = 0;
        if (par1ItemStack.getItem() == Item.getItemFromBlock((Block)Blocks.crafting_table)) {
            this.thePlayer.addStat((StatBase)AchievementList.buildWorkBench, 1);
        }
        if (par1ItemStack.getItem() instanceof ItemPickaxe) {
            this.thePlayer.addStat((StatBase)AchievementList.buildPickaxe, 1);
        }
        if (par1ItemStack.getItem() == Item.getItemFromBlock((Block)Blocks.furnace)) {
            this.thePlayer.addStat((StatBase)AchievementList.buildFurnace, 1);
        }
        if (par1ItemStack.getItem() instanceof ItemHoe) {
            this.thePlayer.addStat((StatBase)AchievementList.buildHoe, 1);
        }
        if (par1ItemStack.getItem() == Items.bread) {
            this.thePlayer.addStat((StatBase)AchievementList.makeBread, 1);
        }
        if (par1ItemStack.getItem() == Items.cake) {
            this.thePlayer.addStat((StatBase)AchievementList.bakeCake, 1);
        }
        if (par1ItemStack.getItem() instanceof ItemPickaxe && ((ItemTool)par1ItemStack.getItem()).func_150913_i() != Item.ToolMaterial.WOOD) {
            this.thePlayer.addStat((StatBase)AchievementList.buildBetterPickaxe, 1);
        }
        if (par1ItemStack.getItem() instanceof ItemSword) {
            this.thePlayer.addStat((StatBase)AchievementList.buildSword, 1);
        }
        if (par1ItemStack.getItem() == Item.getItemFromBlock((Block)Blocks.enchanting_table)) {
            this.thePlayer.addStat((StatBase)AchievementList.enchantments, 1);
        }
        if (par1ItemStack.getItem() == Item.getItemFromBlock((Block)Blocks.bookshelf)) {
            this.thePlayer.addStat((StatBase)AchievementList.bookcase, 1);
        }
    }

    public void onPickupFromSlot(EntityPlayer par1EntityPlayer, ItemStack par2ItemStack) {
        FMLCommonHandler.instance().firePlayerCraftingEvent(par1EntityPlayer, par2ItemStack, this.craftMatrix);
        this.onCrafting(par2ItemStack);
        for (int i = 0; i < this.craftMatrix.getSizeInventory(); ++i) {
            ItemStack itemstack1 = this.craftMatrix.getStackInSlot(i);
            if (itemstack1 == null) continue;
            this.craftMatrix.decrStackSize(i, 1);
            if (!itemstack1.getItem().hasContainerItem(itemstack1)) continue;
            ItemStack itemstack2 = itemstack1.getItem().getContainerItem(itemstack1);
            if (itemstack2 != null && itemstack2.isItemStackDamageable() && itemstack2.getItemDamage() > itemstack2.getMaxDamage()) {
                MinecraftForge.EVENT_BUS.post((Event)new PlayerDestroyItemEvent(this.thePlayer, itemstack2));
                continue;
            }
            if (itemstack1.getItem().doesContainerItemLeaveCraftingGrid(itemstack1) && this.thePlayer.inventory.addItemStackToInventory(itemstack2)) continue;
            if (this.craftMatrix.getStackInSlot(i) == null) {
                this.craftMatrix.setInventorySlotContents(i, itemstack2);
                continue;
            }
            this.thePlayer.dropPlayerItemWithRandomChoice(itemstack2, false);
        }
    }

    public ItemStack decrStackSize(int par1) {
        if (this.getHasStack()) {
            this.amountCrafted += Math.min(par1, this.getStack().stackSize);
        }
        return super.decrStackSize(par1);
    }
}

