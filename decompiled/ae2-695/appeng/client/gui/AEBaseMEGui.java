/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui;

import appeng.api.config.TerminalFontSize;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IGuiTooltipHandler;
import appeng.client.me.SlotME;
import appeng.container.slot.SlotFake;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.util.Platform;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public abstract class AEBaseMEGui
extends AEBaseGui
implements IGuiTooltipHandler {
    public AEBaseMEGui(Container container) {
        super(container);
    }

    @Override
    public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> currentToolTip) {
        Slot s;
        boolean isSlotME;
        if (stack != null && ((isSlotME = (s = this.getSlot(mouseX, mouseY)) instanceof SlotME) || s instanceof SlotFake)) {
            int BigNumber = AEConfig.instance.getTerminalFontSize() == TerminalFontSize.SMALL ? 9999 : 999;
            IAEItemStack myStack = null;
            try {
                myStack = Platform.getAEStackInSlot(s);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (myStack != null) {
                String format;
                String formattedAmount;
                String local;
                if (myStack.getStackSize() > (long)BigNumber || myStack.getStackSize() > 1L && stack.isItemDamaged()) {
                    local = isSlotME ? ButtonToolTips.ItemsStored.getLocal() : ButtonToolTips.ItemCount.getLocal();
                    formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(myStack.getStackSize());
                    format = String.format(local, formattedAmount);
                    currentToolTip.add("\u00a77" + format);
                }
                if (myStack.getCountRequestable() > 0L) {
                    local = ButtonToolTips.ItemsRequestable.getLocal();
                    formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(myStack.getCountRequestable());
                    format = String.format(local, formattedAmount);
                    currentToolTip.add("\u00a77" + format);
                }
            } else if (stack.stackSize > BigNumber || stack.stackSize > 1 && stack.isItemDamaged()) {
                String local = ButtonToolTips.ItemsStored.getLocal();
                String formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(stack.stackSize);
                String format = String.format(local, formattedAmount);
                currentToolTip.add("\u00a77" + format);
            }
        }
        return currentToolTip;
    }

    @Override
    public void renderToolTip(ItemStack stack, int x, int y) {
        Slot s = this.getSlot(x, y);
        if ((s instanceof SlotME || s instanceof SlotFake) && stack != null) {
            int BigNumber = AEConfig.instance.getTerminalFontSize() == TerminalFontSize.SMALL ? 9999 : 999;
            IAEItemStack myStack = null;
            try {
                myStack = Platform.getAEStackInSlot(s);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (myStack != null) {
                List currentToolTip = stack.getTooltip((EntityPlayer)this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
                if (myStack.getStackSize() > (long)BigNumber || myStack.getStackSize() > 1L && stack.isItemDamaged()) {
                    currentToolTip.add("Items Stored: " + NumberFormat.getNumberInstance(Locale.US).format(myStack.getStackSize()));
                }
                if (myStack.getCountRequestable() > 0L) {
                    currentToolTip.add("Items Requestable: " + NumberFormat.getNumberInstance(Locale.US).format(myStack.getCountRequestable()));
                }
                this.drawTooltip(x, y, currentToolTip.toArray(new String[0]));
            } else if (stack.stackSize > BigNumber) {
                List var4 = stack.getTooltip((EntityPlayer)this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
                var4.add("Items Stored: " + NumberFormat.getNumberInstance(Locale.US).format(stack.stackSize));
                this.drawTooltip(x, y, var4.toArray(new String[0]));
                return;
            }
        }
        super.renderToolTip(stack, x, y);
    }
}

