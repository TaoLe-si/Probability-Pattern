/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockAir
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package appeng.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class InWorldToolOperationResult {
    private final ItemStack BlockItem;
    private final List<ItemStack> Drops;

    public InWorldToolOperationResult() {
        this.BlockItem = null;
        this.Drops = null;
    }

    public InWorldToolOperationResult(ItemStack block, List<ItemStack> drops) {
        this.BlockItem = block;
        this.Drops = drops;
    }

    public InWorldToolOperationResult(ItemStack block) {
        this.BlockItem = block;
        this.Drops = null;
    }

    public static InWorldToolOperationResult getBlockOperationResult(ItemStack[] items) {
        ArrayList<ItemStack> temp = new ArrayList<ItemStack>();
        ItemStack b = null;
        for (ItemStack l : items) {
            Block bl;
            if (b == null && (bl = Block.getBlockFromItem((Item)l.getItem())) != null && !(bl instanceof BlockAir)) {
                b = l;
                continue;
            }
            temp.add(l);
        }
        return new InWorldToolOperationResult(b, temp);
    }

    public ItemStack getBlockItem() {
        return this.BlockItem;
    }

    public List<ItemStack> getDrops() {
        return this.Drops;
    }
}

