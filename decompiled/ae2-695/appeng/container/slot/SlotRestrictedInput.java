/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntityFurnace
 *  net.minecraft.world.World
 *  net.minecraftforge.oredict.OreDictionary
 */
package appeng.container.slot;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IItems;
import appeng.api.definitions.IMaterials;
import appeng.api.features.INetworkEncodable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.items.IBiometricCard;
import appeng.api.implementations.items.ISpatialStorageCell;
import appeng.api.implementations.items.IStorageComponent;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.container.slot.AppEngSlot;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class SlotRestrictedInput
extends AppEngSlot {
    private final PlacableItemType which;
    private final InventoryPlayer p;
    private boolean allowEdit = true;
    private int stackLimit = -1;

    public SlotRestrictedInput(PlacableItemType valid, IInventory i, int slotIndex, int x, int y, InventoryPlayer p) {
        super(i, slotIndex, x, y);
        this.which = valid;
        this.setIIcon(valid.IIcon);
        this.p = p;
    }

    public int getSlotStackLimit() {
        if (this.stackLimit != -1) {
            return this.stackLimit;
        }
        return super.getSlotStackLimit();
    }

    public boolean isValid(ItemStack is, World theWorld) {
        if (this.which == PlacableItemType.VALID_ENCODED_PATTERN_W_OUTPUT) {
            ICraftingPatternDetails ap = is.getItem() instanceof ICraftingPatternItem ? ((ICraftingPatternItem)is.getItem()).getPatternForItem(is, theWorld) : null;
            return ap != null;
        }
        return true;
    }

    public Slot setStackLimit(int i) {
        this.stackLimit = i;
        return this;
    }

    @Override
    public boolean isItemValid(ItemStack i) {
        if (!this.isEnabled()) {
            return false;
        }
        if (!this.getContainer().isValidForSlot(this, i)) {
            return false;
        }
        if (i == null) {
            return false;
        }
        if (i.getItem() == null) {
            return false;
        }
        if (!this.inventory.isItemValidForSlot(this.getSlotIndex(), i)) {
            return false;
        }
        if (!this.isAllowEdit()) {
            return false;
        }
        IDefinitions definitions = AEApi.instance().definitions();
        IMaterials materials = definitions.materials();
        IItems items = definitions.items();
        switch (this.which) {
            case ENCODED_CRAFTING_PATTERN: {
                ICraftingPatternItem b;
                ICraftingPatternDetails de;
                Item item = i.getItem();
                if (item instanceof ICraftingPatternItem && (de = (b = (ICraftingPatternItem)item).getPatternForItem(i, this.p.player.worldObj)) != null) {
                    return de.isCraftable();
                }
                return false;
            }
            case VALID_ENCODED_PATTERN_W_OUTPUT: 
            case ENCODED_PATTERN_W_OUTPUT: 
            case ENCODED_PATTERN: {
                return i.getItem() instanceof ICraftingPatternItem;
            }
            case BLANK_PATTERN: {
                return materials.blankPattern().isSameAs(i);
            }
            case PATTERN: {
                if (i.getItem() instanceof ICraftingPatternItem) {
                    return true;
                }
                return materials.blankPattern().isSameAs(i);
            }
            case INSCRIBER_PLATE: {
                if (materials.namePress().isSameAs(i)) {
                    return true;
                }
                for (ItemStack optional : AEApi.instance().registries().inscriber().getOptionals()) {
                    if (!Platform.isSameItemPrecise(optional, i)) continue;
                    return true;
                }
                return false;
            }
            case INSCRIBER_INPUT: {
                return true;
            }
            case METAL_INGOTS: {
                return SlotRestrictedInput.isMetalIngot(i);
            }
            case VIEW_CELL: {
                return items.viewCell().isSameAs(i);
            }
            case ORE: {
                return AEApi.instance().registries().grinder().getRecipeForInput(i) != null;
            }
            case FUEL: {
                return TileEntityFurnace.getItemBurnTime((ItemStack)i) > 0;
            }
            case POWERED_TOOL: {
                return Platform.isChargeable(i);
            }
            case QE_SINGULARITY: {
                return materials.qESingularity().isSameAs(i);
            }
            case RANGE_BOOSTER: {
                return materials.wirelessBooster().isSameAs(i);
            }
            case SPATIAL_STORAGE_CELLS: {
                return i.getItem() instanceof ISpatialStorageCell && ((ISpatialStorageCell)i.getItem()).isSpatialStorage(i);
            }
            case STORAGE_CELLS: {
                return AEApi.instance().registries().cell().isCellHandled(i);
            }
            case WORKBENCH_CELL: {
                return i.getItem() instanceof ICellWorkbenchItem && ((ICellWorkbenchItem)i.getItem()).isEditable(i);
            }
            case STORAGE_COMPONENT: {
                return i.getItem() instanceof IStorageComponent && ((IStorageComponent)i.getItem()).isStorageComponent(i);
            }
            case TRASH: {
                if (AEApi.instance().registries().cell().isCellHandled(i)) {
                    return false;
                }
                return !(i.getItem() instanceof IStorageComponent) || !((IStorageComponent)i.getItem()).isStorageComponent(i);
            }
            case ENCODABLE_ITEM: {
                return i.getItem() instanceof INetworkEncodable || AEApi.instance().registries().wireless().isWirelessTerminal(i);
            }
            case BIOMETRIC_CARD: {
                return i.getItem() instanceof IBiometricCard;
            }
            case UPGRADES: {
                return i.getItem() instanceof IUpgradeModule && ((IUpgradeModule)i.getItem()).getType(i) != null;
            }
        }
        return false;
    }

    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
        return this.isAllowEdit();
    }

    @Override
    public ItemStack getDisplayStack() {
        ItemEncodedPattern iep;
        ItemStack out;
        Item item;
        ItemStack is;
        if (Platform.isClient() && this.which == PlacableItemType.ENCODED_PATTERN && (is = super.getStack()) != null && (item = is.getItem()) instanceof ItemEncodedPattern && (out = (iep = (ItemEncodedPattern)item).getOutput(is)) != null) {
            return out;
        }
        return super.getStack();
    }

    public static boolean isMetalIngot(ItemStack i) {
        if (Platform.isSameItemPrecise(i, new ItemStack(Items.iron_ingot))) {
            return true;
        }
        for (String name : new String[]{"Copper", "Tin", "Obsidian", "Iron", "Lead", "Bronze", "Brass", "Nickel", "Aluminium"}) {
            for (ItemStack ingot : OreDictionary.getOres((String)("ingot" + name))) {
                if (!Platform.isSameItemPrecise(i, ingot)) continue;
                return true;
            }
        }
        return false;
    }

    private boolean isAllowEdit() {
        return this.allowEdit;
    }

    public void setAllowEdit(boolean allowEdit) {
        this.allowEdit = allowEdit;
    }

    public static enum PlacableItemType {
        STORAGE_CELLS(15),
        ORE(31),
        STORAGE_COMPONENT(63),
        ENCODABLE_ITEM(79),
        TRASH(95),
        VALID_ENCODED_PATTERN_W_OUTPUT(127),
        ENCODED_PATTERN_W_OUTPUT(127),
        ENCODED_CRAFTING_PATTERN(127),
        ENCODED_PATTERN(127),
        PATTERN(143),
        BLANK_PATTERN(143),
        POWERED_TOOL(159),
        RANGE_BOOSTER(111),
        QE_SINGULARITY(175),
        SPATIAL_STORAGE_CELLS(191),
        FUEL(207),
        UPGRADES(223),
        WORKBENCH_CELL(15),
        BIOMETRIC_CARD(239),
        VIEW_CELL(78),
        INSCRIBER_PLATE(46),
        INSCRIBER_INPUT(62),
        METAL_INGOTS(62);

        public final int IIcon;

        private PlacableItemType(int o) {
            this.IIcon = o;
        }
    }
}

