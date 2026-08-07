/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.inventory.Slot
 *  net.minecraft.inventory.SlotCrafting
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.CraftingManager
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IDefinitions;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternOutputs;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.sync.packets.PacketPatternSlot;
import appeng.helpers.IContainerCraftingPacket;
import appeng.items.storage.ItemViewCell;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorPlayerHand;
import appeng.util.item.AEItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

public class ContainerPatternTerm
extends ContainerMEMonitorable
implements IAEAppEngInventory,
IOptionalSlotHost,
IContainerCraftingPacket {
    public static final int MULTIPLE_OF_BUTTON_CLICK = 2;
    public static final int MULTIPLE_OF_BUTTON_CLICK_ON_SHIFT = 8;
    private final PartPatternTerminal patternTerminal;
    private final AppEngInternalInventory cOut = new AppEngInternalInventory(null, 1);
    private final IInventory crafting;
    private final SlotFakeCraftingMatrix[] craftingSlots = new SlotFakeCraftingMatrix[9];
    private final OptionalSlotFake[] outputSlots = new OptionalSlotFake[3];
    private final SlotPatternTerm craftSlot;
    private final SlotRestrictedInput patternSlotIN;
    private final SlotRestrictedInput patternSlotOUT;
    @GuiSync(value=97)
    public boolean craftingMode = true;
    @GuiSync(value=96)
    public boolean substitute = false;
    @GuiSync(value=95)
    public boolean beSubstitute = true;

    public ContainerPatternTerm(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable, false);
        int y;
        this.patternTerminal = (PartPatternTerminal)monitorable;
        IInventory patternInv = this.getPatternTerminal().getInventoryByName("pattern");
        IInventory output = this.getPatternTerminal().getInventoryByName("output");
        this.crafting = this.getPatternTerminal().getInventoryByName("crafting");
        for (y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                SlotFakeCraftingMatrix slotFakeCraftingMatrix = new SlotFakeCraftingMatrix(this.crafting, x + y * 3, 18 + x * 18, -76 + y * 18);
                this.craftingSlots[x + y * 3] = slotFakeCraftingMatrix;
                this.addSlotToContainer(slotFakeCraftingMatrix);
            }
        }
        this.craftSlot = new SlotPatternTerm(ip.player, this.getActionSource(), this.getPowerSource(), monitorable, this.crafting, patternInv, this.cOut, 110, -58, this, 2, this);
        this.addSlotToContainer(this.craftSlot);
        this.craftSlot.setIIcon(-1);
        for (y = 0; y < 3; ++y) {
            this.outputSlots[y] = new SlotPatternOutputs(output, this, y, 110, -76 + y * 18, 0, 0, 1);
            this.addSlotToContainer(this.outputSlots[y]);
            this.outputSlots[y].setRenderDisabled(false);
        }
        this.patternSlotIN = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.BLANK_PATTERN, patternInv, 0, 147, -81, this.getInventoryPlayer());
        this.addSlotToContainer(this.patternSlotIN);
        this.patternSlotOUT = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, patternInv, 1, 147, -38, this.getInventoryPlayer());
        this.addSlotToContainer(this.patternSlotOUT);
        this.patternSlotOUT.setStackLimit(1);
        this.bindPlayerInventory(ip, 0, 0);
        this.updateOrderOfOutputSlots();
        if (this.getPatternTerminal().hasRefillerUpgrade()) {
            this.refillBlankPatterns(this.patternSlotIN);
        }
    }

    private void updateOrderOfOutputSlots() {
        if (!this.isCraftingMode()) {
            this.craftSlot.xDisplayPosition = -9000;
            for (int y = 0; y < 3; ++y) {
                this.outputSlots[y].xDisplayPosition = this.outputSlots[y].getX();
            }
        } else {
            this.craftSlot.xDisplayPosition = this.craftSlot.getX();
            for (int y = 0; y < 3; ++y) {
                this.outputSlots[y].xDisplayPosition = -9000;
            }
        }
    }

    public void putStackInSlot(int par1, ItemStack par2ItemStack) {
        super.putStackInSlot(par1, par2ItemStack);
        this.getAndUpdateOutput();
    }

    public void putStacksInSlots(ItemStack[] par1ArrayOfItemStack) {
        super.putStacksInSlots(par1ArrayOfItemStack);
        this.getAndUpdateOutput();
    }

    private ItemStack getAndUpdateOutput() {
        InventoryCrafting ic = new InventoryCrafting((Container)this, 3, 3);
        for (int x = 0; x < ic.getSizeInventory(); ++x) {
            ic.setInventorySlotContents(x, this.crafting.getStackInSlot(x));
        }
        ItemStack is = CraftingManager.getInstance().findMatchingRecipe(ic, this.getPlayerInv().player.worldObj);
        this.cOut.setInventorySlotContents(0, is);
        return is;
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
    }

    public void encodeAndMoveToInventory(boolean encodeWholeStack) {
        this.encode();
        ItemStack output = this.patternSlotOUT.getStack();
        if (output != null) {
            if (encodeWholeStack) {
                ItemStack blanks = this.patternSlotIN.getStack();
                this.patternSlotIN.putStack(null);
                if (blanks != null) {
                    output.stackSize += blanks.stackSize;
                }
            }
            if (!this.getPlayerInv().addItemStackToInventory(output)) {
                this.getPlayerInv().player.entityDropItem(output, 0.0f);
            }
            this.patternSlotOUT.putStack(null);
            if (this.getPatternTerminal().hasRefillerUpgrade()) {
                this.refillBlankPatterns(this.patternSlotIN);
            }
        }
    }

    public void encode() {
        ItemStack output = this.patternSlotOUT.getStack();
        ItemStack[] in = this.getInputs();
        ItemStack[] out = this.getOutputs();
        if (in == null || out == null) {
            return;
        }
        if (output != null && !this.isPattern(output)) {
            return;
        }
        if (output == null) {
            output = this.patternSlotIN.getStack();
            if (!this.isPattern(output)) {
                return;
            }
            --output.stackSize;
            if (output.stackSize == 0) {
                this.patternSlotIN.putStack(null);
            }
            Iterator iterator = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack encodedPatternStack;
                output = encodedPatternStack = (ItemStack)iterator.next();
                this.patternSlotOUT.putStack(output);
            }
            if (this.getPatternTerminal().hasRefillerUpgrade()) {
                this.refillBlankPatterns(this.patternSlotIN);
            }
        }
        NBTTagCompound encodedValue = new NBTTagCompound();
        NBTTagList tagIn = new NBTTagList();
        NBTTagList tagOut = new NBTTagList();
        for (ItemStack i : in) {
            tagIn.appendTag(this.createItemTag(i));
        }
        for (ItemStack i : out) {
            tagOut.appendTag(this.createItemTag(i));
        }
        encodedValue.setTag("in", (NBTBase)tagIn);
        encodedValue.setTag("out", (NBTBase)tagOut);
        encodedValue.setBoolean("crafting", this.isCraftingMode());
        encodedValue.setBoolean("substitute", this.isSubstitute());
        encodedValue.setBoolean("beSubstitute", this.canBeSubstitute());
        encodedValue.setString("author", this.getPlayerInv().player.getCommandSenderName());
        output.setTagCompound(encodedValue);
    }

    private ItemStack[] getInputs() {
        ItemStack[] input = new ItemStack[9];
        boolean hasValue = false;
        for (int x = 0; x < this.craftingSlots.length; ++x) {
            input[x] = this.craftingSlots[x].getStack();
            if (input[x] == null) continue;
            hasValue = true;
        }
        if (hasValue) {
            return input;
        }
        return null;
    }

    private ItemStack[] getOutputs() {
        if (this.isCraftingMode()) {
            ItemStack out = this.getAndUpdateOutput();
            if (out != null && out.stackSize > 0) {
                return new ItemStack[]{out};
            }
        } else {
            ArrayList<ItemStack> list = new ArrayList<ItemStack>(3);
            boolean hasValue = false;
            for (OptionalSlotFake outputSlot : this.outputSlots) {
                ItemStack out = outputSlot.getStack();
                if (out == null || out.stackSize <= 0) continue;
                list.add(out);
                hasValue = true;
            }
            if (hasValue) {
                return list.toArray(new ItemStack[0]);
            }
        }
        return null;
    }

    private boolean isPattern(ItemStack output) {
        if (output == null) {
            return false;
        }
        IDefinitions definitions = AEApi.instance().definitions();
        boolean isPattern = definitions.items().encodedPattern().isSameAs(output);
        return isPattern |= definitions.materials().blankPattern().isSameAs(output);
    }

    private NBTBase createItemTag(ItemStack i) {
        NBTTagCompound c = new NBTTagCompound();
        if (i != null) {
            i.writeToNBT(c);
            c.setInteger("Count", i.stackSize);
        }
        return c;
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        if (idx == 1) {
            return Platform.isServer() ? !this.getPatternTerminal().isCraftingRecipe() : !this.isCraftingMode();
        }
        if (idx == 2) {
            return Platform.isServer() ? this.getPatternTerminal().isCraftingRecipe() : this.isCraftingMode();
        }
        return false;
    }

    public void craftOrGetItem(PacketPatternSlot packetPatternSlot) {
        if (packetPatternSlot.slotItem != null && this.getCellInventory() != null) {
            IAEItemStack out = packetPatternSlot.slotItem.copy();
            InventoryAdaptor inv = new AdaptorPlayerHand(this.getPlayerInv().player);
            InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(this.getPlayerInv().player, ForgeDirection.UNKNOWN);
            if (packetPatternSlot.shift) {
                inv = playerInv;
            }
            if (inv.simulateAdd(out.getItemStack()) != null) {
                return;
            }
            IAEItemStack extracted = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), out, this.getActionSource());
            EntityPlayer p = this.getPlayerInv().player;
            if (extracted != null) {
                inv.addItems(extracted.getItemStack());
                if (p instanceof EntityPlayerMP) {
                    this.updateHeld((EntityPlayerMP)p);
                }
                this.detectAndSendChanges();
                return;
            }
            InventoryCrafting ic = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
            InventoryCrafting real = new InventoryCrafting((Container)new ContainerNull(), 3, 3);
            for (int x = 0; x < 9; ++x) {
                ic.setInventorySlotContents(x, packetPatternSlot.pattern[x] == null ? null : packetPatternSlot.pattern[x].getItemStack());
            }
            IRecipe r = Platform.findMatchingRecipe(ic, p.worldObj);
            if (r == null) {
                return;
            }
            IMEMonitor<IAEItemStack> storage = this.getPatternTerminal().getItemInventory();
            IItemList<IAEItemStack> all = storage.getStorageList();
            ItemStack is = r.getCraftingResult(ic);
            for (int x = 0; x < ic.getSizeInventory(); ++x) {
                if (ic.getStackInSlot(x) == null) continue;
                ItemStack pulled = Platform.extractItemsByRecipe(this.getPowerSource(), this.getActionSource(), storage, p.worldObj, r, is, ic, ic.getStackInSlot(x), x, all, Actionable.MODULATE, ItemViewCell.createFilter(this.getViewCells()));
                real.setInventorySlotContents(x, pulled);
            }
            IRecipe rr = Platform.findMatchingRecipe(real, p.worldObj);
            if (rr == r && Platform.isSameItemPrecise(rr.getCraftingResult(real), is)) {
                SlotCrafting sc = new SlotCrafting(p, (IInventory)real, (IInventory)this.cOut, 0, 0, 0);
                sc.onPickupFromSlot(p, is);
                for (int x = 0; x < real.getSizeInventory(); ++x) {
                    ItemStack failed = playerInv.addItems(real.getStackInSlot(x));
                    if (failed == null) continue;
                    p.dropPlayerItemWithRandomChoice(failed, false);
                }
                inv.addItems(is);
                if (p instanceof EntityPlayerMP) {
                    this.updateHeld((EntityPlayerMP)p);
                }
                this.detectAndSendChanges();
            } else {
                for (int x = 0; x < real.getSizeInventory(); ++x) {
                    ItemStack failed = real.getStackInSlot(x);
                    if (failed == null) continue;
                    this.getCellInventory().injectItems(AEItemStack.create(failed), Actionable.MODULATE, new MachineSource(this.getPatternTerminal()));
                }
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (Platform.isServer()) {
            if (this.isCraftingMode() != this.getPatternTerminal().isCraftingRecipe()) {
                this.setCraftingMode(this.getPatternTerminal().isCraftingRecipe());
                this.updateOrderOfOutputSlots();
            }
            this.substitute = this.patternTerminal.isSubstitution();
            this.beSubstitute = this.patternTerminal.canBeSubstitution();
        }
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        super.onUpdate(field, oldValue, newValue);
        if (field.equals("craftingMode")) {
            this.getAndUpdateOutput();
            this.updateOrderOfOutputSlots();
        }
    }

    @Override
    public void onSlotChange(Slot s) {
        if (!Platform.isServer()) {
            return;
        }
        if (s == this.patternSlotOUT) {
            for (Object crafter : this.crafters) {
                ICrafting icrafting = (ICrafting)crafter;
                for (Object g : this.inventorySlots) {
                    if (!(g instanceof OptionalSlotFake) && !(g instanceof SlotFakeCraftingMatrix)) continue;
                    Slot sri = (Slot)g;
                    icrafting.sendSlotContents((Container)this, sri.slotNumber, sri.getStack());
                }
                ((EntityPlayerMP)icrafting).isChangingQuantityOnly = false;
            }
            this.detectAndSendChanges();
        } else if (s == this.patternRefiller && this.patternRefiller.getStack() != null) {
            this.refillBlankPatterns(this.patternSlotIN);
            this.detectAndSendChanges();
        }
    }

    public void clear() {
        for (SlotFakeCraftingMatrix slotFakeCraftingMatrix : this.craftingSlots) {
            slotFakeCraftingMatrix.putStack(null);
        }
        for (SlotFake slotFake : this.outputSlots) {
            slotFake.putStack(null);
        }
        this.detectAndSendChanges();
        this.getAndUpdateOutput();
    }

    @Override
    public IInventory getInventoryByName(String name) {
        if (name.equals("player")) {
            return this.getInventoryPlayer();
        }
        return this.getPatternTerminal().getInventoryByName(name);
    }

    @Override
    public boolean useRealItems() {
        return false;
    }

    public void toggleSubstitute() {
        this.substitute = !this.substitute;
        this.detectAndSendChanges();
        this.getAndUpdateOutput();
    }

    public boolean isCraftingMode() {
        return this.craftingMode;
    }

    private void setCraftingMode(boolean craftingMode) {
        this.craftingMode = craftingMode;
    }

    public PartPatternTerminal getPatternTerminal() {
        return this.patternTerminal;
    }

    private boolean isSubstitute() {
        return this.substitute;
    }

    private boolean canBeSubstitute() {
        return this.beSubstitute;
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

    public void setCanBeSubstitute(boolean beSubstitute) {
        this.beSubstitute = beSubstitute;
    }

    public void doubleStacks(int val) {
        this.multiplyOrDivideStacks(((val & 1) != 0 ? 8 : 2) * ((val & 2) != 0 ? -1 : 1));
    }

    static boolean canMultiplyOrDivide(SlotFake[] slots, int mult) {
        if (mult > 0) {
            for (SlotFake s : slots) {
                long val;
                if (s.getStack() == null || (val = (long)s.getStack().stackSize * (long)mult) <= Integer.MAX_VALUE) continue;
                return false;
            }
            return true;
        }
        if (mult < 0) {
            mult = -mult;
            for (SlotFake s : slots) {
                if (s.getStack() == null || s.getStack().stackSize % mult == 0) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    static void multiplyOrDivideStacksInternal(SlotFake[] slots, int mult) {
        block3: {
            List enabledSlots;
            block2: {
                enabledSlots = Arrays.stream(slots).filter(AppEngSlot::isEnabled).collect(Collectors.toList());
                if (mult <= 0) break block2;
                for (Slot s : enabledSlots) {
                    ItemStack st = s.getStack();
                    if (st == null) continue;
                    st.stackSize *= mult;
                    s.putStack(st);
                }
                break block3;
            }
            if (mult >= 0) break block3;
            mult = -mult;
            for (Slot s : enabledSlots) {
                ItemStack st = s.getStack();
                if (st == null) continue;
                st.stackSize /= mult;
                s.putStack(st);
            }
        }
    }

    public void multiplyOrDivideStacks(int multi) {
        if (!this.isCraftingMode()) {
            if (ContainerPatternTerm.canMultiplyOrDivide(this.craftingSlots, multi) && ContainerPatternTerm.canMultiplyOrDivide(this.outputSlots, multi)) {
                ContainerPatternTerm.multiplyOrDivideStacksInternal(this.craftingSlots, multi);
                ContainerPatternTerm.multiplyOrDivideStacksInternal(this.outputSlots, multi);
            }
            this.detectAndSendChanges();
        }
    }

    @Override
    public boolean isAPatternTerminal() {
        return true;
    }
}

