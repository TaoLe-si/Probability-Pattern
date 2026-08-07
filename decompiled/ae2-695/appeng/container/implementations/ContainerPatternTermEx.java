/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.storage.ITerminalHost;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.util.Platform;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class ContainerPatternTermEx
extends ContainerMEMonitorable
implements IOptionalSlotHost,
IContainerCraftingPacket {
    private static final int CRAFTING_GRID_PAGES = 2;
    private static final int CRAFTING_GRID_WIDTH = 4;
    private static final int CRAFTING_GRID_HEIGHT = 4;
    private static final int CRAFTING_GRID_SLOTS = 16;
    private final PartPatternTerminalEx patternTerminal;
    private final ProcessingSlotFake[] craftingSlots = new ProcessingSlotFake[32];
    private final ProcessingSlotFake[] outputSlots = new ProcessingSlotFake[32];
    private final SlotRestrictedInput patternSlotIN;
    private final SlotRestrictedInput patternSlotOUT;
    @GuiSync(value=116)
    public boolean substitute = false;
    @GuiSync(value=117)
    public boolean beSubstitute = false;
    @GuiSync(value=120)
    public boolean inverted;
    @GuiSync(value=121)
    public int activePage = 0;

    public ContainerPatternTermEx(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable, false);
        this.patternTerminal = (PartPatternTerminalEx)monitorable;
        this.inverted = this.patternTerminal.isInverted();
        IInventory patternInv = this.getPatternTerminal().getInventoryByName("pattern");
        IInventory output = this.getPatternTerminal().getInventoryByName("output");
        IInventory crafting = this.getPatternTerminal().getInventoryByName("crafting");
        for (int page = 0; page < 2; ++page) {
            for (int y = 0; y < 4; ++y) {
                for (int x = 0; x < 4; ++x) {
                    ProcessingSlotFake processingSlotFake = new ProcessingSlotFake(crafting, this, x + y * 4 + page * 16, 15, -83, x, y, x + 4);
                    this.craftingSlots[x + y * 4 + page * 16] = processingSlotFake;
                    this.addSlotToContainer(processingSlotFake);
                }
            }
            for (int x = 0; x < 4; ++x) {
                for (int y = 0; y < 4; ++y) {
                    ProcessingSlotFake processingSlotFake = new ProcessingSlotFake(output, this, x * 4 + y + page * 16, 112, -83, -x, y, x);
                    this.outputSlots[x * 4 + y + page * 16] = processingSlotFake;
                    this.addSlotToContainer(processingSlotFake);
                }
            }
        }
        this.patternSlotIN = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.BLANK_PATTERN, patternInv, 0, 147, -81, this.getInventoryPlayer());
        this.addSlotToContainer(this.patternSlotIN);
        this.patternSlotOUT = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, patternInv, 1, 147, -38, this.getInventoryPlayer());
        this.addSlotToContainer(this.patternSlotOUT);
        this.patternSlotOUT.setStackLimit(1);
        this.bindPlayerInventory(ip, 0, 0);
        if (this.getPatternTerminal().hasRefillerUpgrade()) {
            this.refillBlankPatterns(this.patternSlotIN);
        }
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
        if (output != null && this.isNotPattern(output)) {
            return;
        }
        if (output == null) {
            output = this.patternSlotIN.getStack();
            if (this.isNotPattern(output)) {
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
        encodedValue.setBoolean("crafting", false);
        encodedValue.setBoolean("substitute", this.isSubstitute());
        encodedValue.setBoolean("beSubstitute", this.canBeSubstitute());
        encodedValue.setString("author", this.getPlayerInv().player.getCommandSenderName());
        output.setTagCompound(encodedValue);
    }

    private ItemStack[] getInputs() {
        ItemStack[] input = new ItemStack[32];
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
        ArrayList<ItemStack> list = new ArrayList<ItemStack>(32);
        boolean hasValue = false;
        for (ProcessingSlotFake outputSlot : this.outputSlots) {
            ItemStack out = outputSlot.getStack();
            if (out == null || out.stackSize <= 0) continue;
            list.add(out);
            hasValue = true;
        }
        if (hasValue) {
            return list.toArray(new ItemStack[0]);
        }
        return null;
    }

    private boolean isNotPattern(ItemStack output) {
        if (output == null) {
            return true;
        }
        IDefinitions definitions = AEApi.instance().definitions();
        boolean isPattern = definitions.items().encodedPattern().isSameAs(output);
        return !(isPattern |= definitions.materials().blankPattern().isSameAs(output));
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
        if (idx < 4) {
            return this.inverted || idx == 0;
        }
        return !this.inverted || idx == 4;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (Platform.isServer()) {
            this.substitute = this.patternTerminal.isSubstitution();
            this.beSubstitute = this.patternTerminal.canBeSubstitution();
            if (this.inverted != this.patternTerminal.isInverted() || this.activePage != this.patternTerminal.getActivePage()) {
                this.inverted = this.patternTerminal.isInverted();
                this.activePage = this.patternTerminal.getActivePage();
                this.offsetSlots();
            }
        }
    }

    private void offsetSlots() {
        for (int page = 0; page < 2; ++page) {
            for (int y = 0; y < 4; ++y) {
                for (int x = 0; x < 4; ++x) {
                    this.craftingSlots[x + y * 4 + page * 16].setHidden(page != this.activePage || x > 0 && this.inverted);
                    this.outputSlots[x * 4 + y + page * 16].setHidden(page != this.activePage || x > 0 && !this.inverted);
                }
            }
        }
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        super.onUpdate(field, oldValue, newValue);
        if (field.equals("inverted") || field.equals("activePage")) {
            this.offsetSlots();
        }
    }

    @Override
    public void onSlotChange(Slot s) {
        if (!Platform.isServer()) {
            return;
        }
        if (s == this.patternSlotOUT) {
            this.inverted = this.patternTerminal.isInverted();
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
        for (ProcessingSlotFake s : this.craftingSlots) {
            s.putStack(null);
        }
        for (ProcessingSlotFake s : this.outputSlots) {
            s.putStack(null);
        }
        this.detectAndSendChanges();
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

    public PartPatternTerminalEx getPatternTerminal() {
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

    public void setActivePage(int activePage) {
        this.activePage = activePage;
    }

    public int getActivePage() {
        return this.activePage;
    }

    public void doubleStacks(int val) {
        this.multiplyOrDivideStacks(((val & 1) != 0 ? 8 : 2) * ((val & 2) != 0 ? -1 : 1));
    }

    public void multiplyOrDivideStacks(int multi) {
        if (ContainerPatternTerm.canMultiplyOrDivide(this.craftingSlots, multi) && ContainerPatternTerm.canMultiplyOrDivide(this.outputSlots, multi)) {
            ContainerPatternTerm.multiplyOrDivideStacksInternal(this.craftingSlots, multi);
            ContainerPatternTerm.multiplyOrDivideStacksInternal(this.outputSlots, multi);
        }
        this.detectAndSendChanges();
    }

    @Override
    public boolean isAPatternTerminal() {
        return true;
    }

    private static class ProcessingSlotFake
    extends OptionalSlotFake {
        private static final int POSITION_SHIFT = 9000;
        private boolean hidden = false;

        public ProcessingSlotFake(IInventory inv, IOptionalSlotHost containerBus, int idx, int x, int y, int offX, int offY, int groupNum) {
            super(inv, containerBus, idx, x, y, offX, offY, groupNum);
            this.setRenderDisabled(false);
        }

        public void setHidden(boolean hide) {
            if (this.hidden != hide) {
                this.hidden = hide;
                this.xDisplayPosition += (hide ? -1 : 1) * 9000;
            }
        }
    }
}

