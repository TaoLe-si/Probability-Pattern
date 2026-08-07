/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.features.IInscriberRecipe;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.misc.TileInscriber;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerInscriber
extends ContainerUpgradeable
implements IProgressProvider {
    private final TileInscriber ti;
    private final Slot top;
    private final Slot middle;
    private final Slot bottom;
    @GuiSync(value=2)
    public int maxProcessingTime = -1;
    @GuiSync(value=3)
    public int processingTime = -1;

    public ContainerInscriber(InventoryPlayer ip, TileInscriber te) {
        super(ip, te);
        this.ti = te;
        this.top = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.INSCRIBER_PLATE, (IInventory)this.ti, 0, 45, 16, this.getInventoryPlayer());
        this.addSlotToContainer(this.top);
        this.bottom = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.INSCRIBER_PLATE, (IInventory)this.ti, 1, 45, 62, this.getInventoryPlayer());
        this.addSlotToContainer(this.bottom);
        this.middle = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.INSCRIBER_INPUT, (IInventory)this.ti, 2, 63, 39, this.getInventoryPlayer());
        this.addSlotToContainer(this.middle);
        this.addSlotToContainer(new SlotOutput((IInventory)this.ti, 3, 113, 40, -1));
    }

    @Override
    protected int getHeight() {
        return 176;
    }

    @Override
    protected void setupConfig() {
        this.setupUpgrades();
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 3;
    }

    @Override
    public void detectAndSendChanges() {
        this.standardDetectAndSendChanges();
        if (Platform.isServer()) {
            this.maxProcessingTime = this.ti.getMaxProcessingTime();
            this.processingTime = this.ti.getProcessingTime();
        }
    }

    @Override
    public boolean isValidForSlot(Slot s, ItemStack is) {
        ItemStack top = this.ti.getStackInSlot(0);
        ItemStack bot = this.ti.getStackInSlot(1);
        if (s == this.middle) {
            for (ItemStack optional : AEApi.instance().registries().inscriber().getOptionals()) {
                if (!Platform.isSameItemPrecise(optional, is)) continue;
                return false;
            }
            boolean matches = false;
            boolean found = false;
            for (IInscriberRecipe recipe : AEApi.instance().registries().inscriber().getRecipes()) {
                boolean matchB;
                boolean matchA;
                boolean bl = top == null && !recipe.getTopOptional().isPresent() || Platform.isSameItemPrecise(top, (ItemStack)recipe.getTopOptional().orNull()) && (bot == null && !recipe.getBottomOptional().isPresent()) | Platform.isSameItemPrecise(bot, (ItemStack)recipe.getBottomOptional().orNull()) ? true : (matchA = false);
                boolean bl2 = bot == null && !recipe.getTopOptional().isPresent() || Platform.isSameItemPrecise(bot, (ItemStack)recipe.getTopOptional().orNull()) && (top == null && !recipe.getBottomOptional().isPresent()) | Platform.isSameItemPrecise(top, (ItemStack)recipe.getBottomOptional().orNull()) ? true : (matchB = false);
                if (!matchA && !matchB) continue;
                matches = true;
                for (ItemStack option : recipe.getInputs()) {
                    if (!Platform.isSameItemPrecise(is, option)) continue;
                    found = true;
                }
            }
            if (matches && !found) {
                return false;
            }
        }
        if (s == this.top && bot != null || s == this.bottom && top != null) {
            ItemStack otherSlot = null;
            otherSlot = s == this.top ? this.bottom.getStack() : this.top.getStack();
            IItemDefinition namePress = AEApi.instance().definitions().materials().namePress();
            if (namePress.isSameAs(otherSlot)) {
                return namePress.isSameAs(is);
            }
            boolean isValid = false;
            for (IInscriberRecipe recipe : AEApi.instance().registries().inscriber().getRecipes()) {
                if (Platform.isSameItemPrecise((ItemStack)recipe.getTopOptional().orNull(), otherSlot)) {
                    isValid = Platform.isSameItemPrecise(is, (ItemStack)recipe.getBottomOptional().orNull());
                } else if (Platform.isSameItemPrecise((ItemStack)recipe.getBottomOptional().orNull(), otherSlot)) {
                    isValid = Platform.isSameItemPrecise(is, (ItemStack)recipe.getTopOptional().orNull());
                }
                if (!isValid) continue;
                break;
            }
            if (!isValid) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getCurrentProgress() {
        return this.processingTime;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProcessingTime;
    }
}

