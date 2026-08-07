/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.CraftingManager
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.world.World
 */
package appeng.helpers;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerNull;
import appeng.util.ItemSorters;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

public class PatternHelper
implements ICraftingPatternDetails,
Comparable<PatternHelper> {
    private final ItemStack patternItem;
    private final InventoryCrafting crafting = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
    private final InventoryCrafting testFrame = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
    private final ItemStack correctOutput;
    private final IRecipe standardRecipe;
    private final IAEItemStack[] condensedInputs;
    private final IAEItemStack[] condensedOutputs;
    private final IAEItemStack[] inputs;
    private final IAEItemStack[] outputs;
    private final boolean isCrafting;
    private final boolean canSubstitute;
    private final boolean canBeSubstitute;
    private final Set<TestLookup> failCache = new HashSet<TestLookup>();
    private final Set<TestLookup> passCache = new HashSet<TestLookup>();
    private final IAEItemStack pattern;
    private int priority = 0;

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public PatternHelper(ItemStack is, World w) {
        ItemStack gs;
        NBTTagCompound tag;
        int x;
        NBTTagCompound encodedValue = is.getTagCompound();
        if (encodedValue == null) {
            throw new IllegalArgumentException("No pattern here!");
        }
        NBTTagList inTag = encodedValue.getTagList("in", 10);
        NBTTagList outTag = encodedValue.getTagList("out", 10);
        this.isCrafting = encodedValue.getBoolean("crafting");
        this.canSubstitute = encodedValue.getBoolean("substitute");
        this.canBeSubstitute = encodedValue.getBoolean("beSubstitute");
        this.patternItem = is;
        if (encodedValue.hasKey("author")) {
            ItemStack forComparison = this.patternItem.copy();
            forComparison.stackTagCompound.removeTag("author");
            this.pattern = AEItemStack.create(forComparison);
        } else {
            this.pattern = AEItemStack.create(is);
        }
        ArrayList<IAEItemStack> in = new ArrayList<IAEItemStack>();
        ArrayList<IAEItemStack> out = new ArrayList<IAEItemStack>();
        for (x = 0; x < inTag.tagCount(); ++x) {
            tag = inTag.getCompoundTagAt(x);
            gs = Platform.loadItemStackFromNBT(tag);
            if (gs == null && !tag.hasNoTags()) {
                throw new IllegalStateException("No pattern here!");
            }
            if (this.isCrafting) {
                this.crafting.setInventorySlotContents(x, gs);
            }
            if (!(gs == null || this.isCrafting && gs.hasTagCompound())) {
                this.markItemAs(x, gs, TestStatus.ACCEPT);
            }
            in.add(AEApi.instance().storage().createItemStack(gs));
            if (!this.isCrafting) continue;
            this.testFrame.setInventorySlotContents(x, gs);
        }
        if (this.isCrafting) {
            this.standardRecipe = Platform.findMatchingRecipe(this.crafting, w);
            if (this.standardRecipe == null) throw new IllegalStateException("No pattern here!");
            this.correctOutput = this.standardRecipe.getCraftingResult(this.crafting);
            out.add(AEApi.instance().storage().createItemStack(this.correctOutput));
        } else {
            this.standardRecipe = null;
            this.correctOutput = null;
            for (x = 0; x < outTag.tagCount(); ++x) {
                tag = outTag.getCompoundTagAt(x);
                gs = Platform.loadItemStackFromNBT(tag);
                if (gs != null) {
                    out.add(AEApi.instance().storage().createItemStack(gs));
                    continue;
                }
                if (tag.hasNoTags()) continue;
                throw new IllegalStateException("No pattern here!");
            }
        }
        this.outputs = out.toArray(new IAEItemStack[0]);
        this.inputs = in.toArray(new IAEItemStack[0]);
        this.condensedInputs = PatternHelper.convertToCondensedList(this.inputs);
        this.condensedOutputs = PatternHelper.convertToCondensedList(this.outputs);
        if (this.condensedInputs.length != 0 && this.condensedOutputs.length != 0) return;
        throw new IllegalStateException("No pattern here!");
    }

    private void markItemAs(int slotIndex, ItemStack i, TestStatus b) {
        if (b == TestStatus.TEST || i.hasTagCompound()) {
            return;
        }
        (b == TestStatus.ACCEPT ? this.passCache : this.failCache).add(new TestLookup(slotIndex, i));
    }

    @Override
    public ItemStack getPattern() {
        return this.patternItem;
    }

    @Override
    public synchronized boolean isValidItemForSlot(int slotIndex, ItemStack i, World w) {
        if (!this.isCrafting) {
            throw new IllegalStateException("Only crafting recipes supported.");
        }
        TestStatus result = this.getStatus(slotIndex, i);
        switch (result) {
            case ACCEPT: {
                return true;
            }
            case DECLINE: {
                return false;
            }
        }
        for (int x = 0; x < this.crafting.getSizeInventory(); ++x) {
            this.testFrame.setInventorySlotContents(x, this.crafting.getStackInSlot(x));
        }
        this.testFrame.setInventorySlotContents(slotIndex, i);
        if (this.standardRecipe.matches(this.testFrame, w)) {
            ItemStack testOutput = this.standardRecipe.getCraftingResult(this.testFrame);
            if (Platform.isSameItemPrecise(this.correctOutput, testOutput)) {
                this.testFrame.setInventorySlotContents(slotIndex, this.crafting.getStackInSlot(slotIndex));
                this.markItemAs(slotIndex, i, TestStatus.ACCEPT);
                return true;
            }
        } else {
            ItemStack testOutput = CraftingManager.getInstance().findMatchingRecipe(this.testFrame, w);
            if (Platform.isSameItemPrecise(this.correctOutput, testOutput)) {
                this.testFrame.setInventorySlotContents(slotIndex, this.crafting.getStackInSlot(slotIndex));
                this.markItemAs(slotIndex, i, TestStatus.ACCEPT);
                return true;
            }
        }
        this.markItemAs(slotIndex, i, TestStatus.DECLINE);
        return false;
    }

    @Override
    public boolean isCraftable() {
        return this.isCrafting;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return this.inputs;
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return this.condensedInputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return this.condensedOutputs;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return this.outputs;
    }

    @Override
    public boolean canSubstitute() {
        return this.canSubstitute;
    }

    @Override
    public boolean canBeSubstitute() {
        return this.canBeSubstitute;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World w) {
        if (!this.isCrafting) {
            throw new IllegalStateException("Only crafting recipes supported.");
        }
        for (int x = 0; x < craftingInv.getSizeInventory(); ++x) {
            if (this.isValidItemForSlot(x, craftingInv.getStackInSlot(x), w)) continue;
            return null;
        }
        if (this.outputs != null && this.outputs.length > 0) {
            return this.outputs[0].getItemStack();
        }
        return null;
    }

    private TestStatus getStatus(int slotIndex, ItemStack i) {
        if (this.crafting.getStackInSlot(slotIndex) == null) {
            return i == null ? TestStatus.ACCEPT : TestStatus.DECLINE;
        }
        if (i == null) {
            return TestStatus.DECLINE;
        }
        if (i.hasTagCompound()) {
            return TestStatus.TEST;
        }
        if (this.passCache.contains(new TestLookup(slotIndex, i))) {
            return TestStatus.ACCEPT;
        }
        if (this.failCache.contains(new TestLookup(slotIndex, i))) {
            return TestStatus.DECLINE;
        }
        return TestStatus.TEST;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(PatternHelper o) {
        return ItemSorters.compareInt(o.priority, this.priority);
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        PatternHelper other = (PatternHelper)obj;
        if (this.pattern != null && other.pattern != null) {
            return this.pattern.equals(other.pattern);
        }
        return false;
    }

    public static IAEItemStack[] loadIAEItemStackFromNBT(NBTTagList tags, boolean saveOrder, ItemStack unknownItem) {
        ArrayList<IAEItemStack> items = new ArrayList<IAEItemStack>();
        for (int x = 0; x < tags.tagCount(); ++x) {
            IAEItemStack ae;
            NBTTagCompound tag = tags.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;
            ItemStack gs = Platform.loadItemStackFromNBT(tag);
            if (gs == null && unknownItem != null) {
                gs = unknownItem.copy();
            }
            if ((ae = AEApi.instance().storage().createItemStack(gs)) == null && !saveOrder) continue;
            items.add(ae);
        }
        return items.toArray(new IAEItemStack[0]);
    }

    public static IAEItemStack[] convertToCondensedList(IAEItemStack[] items) {
        LinkedHashMap<IAEItemStack, IAEItemStack> tmp = new LinkedHashMap<IAEItemStack, IAEItemStack>();
        for (IAEItemStack io : items) {
            if (io == null) continue;
            IAEItemStack g = (IAEItemStack)tmp.get(io);
            if (g == null) {
                tmp.put(io, io.copy());
                continue;
            }
            g.add(io);
        }
        return tmp.values().toArray(new IAEItemStack[0]);
    }

    private static enum TestStatus {
        ACCEPT,
        DECLINE,
        TEST;

    }

    private static final class TestLookup {
        private final int slot;
        private final int ref;
        private final int hash;

        public TestLookup(int slot, ItemStack i) {
            this(slot, i.getItem(), i.getItemDamage());
        }

        public TestLookup(int slot, Item item, int dmg) {
            this.slot = slot;
            this.ref = dmg << 16 | Item.getIdFromItem((Item)item) & 0xFFFF;
            int offset = 3 * slot;
            this.hash = this.ref << offset | this.ref >> offset + 32;
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            boolean equality;
            if (obj instanceof TestLookup) {
                TestLookup b = (TestLookup)obj;
                equality = b.slot == this.slot && b.ref == this.ref;
            } else {
                equality = false;
            }
            return equality;
        }
    }
}

