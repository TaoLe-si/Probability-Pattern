/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.INetworkEncodable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.items.IBiometricCard;
import appeng.api.storage.ITerminalHost;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.tile.misc.TileSecurity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ContainerSecurity
extends ContainerMEMonitorable
implements IAEAppEngInventory {
    private final SlotRestrictedInput configSlot;
    private final AppEngInternalInventory wirelessEncoder = new AppEngInternalInventory(this, 2);
    private final SlotRestrictedInput wirelessIn;
    private final SlotOutput wirelessOut;
    private final TileSecurity securityBox;
    @GuiSync(value=0)
    public int permissionMode = 0;

    public ContainerSecurity(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable, false);
        this.securityBox = (TileSecurity)monitorable;
        this.configSlot = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.BIOMETRIC_CARD, this.securityBox.getConfigSlot(), 0, 37, -33, ip);
        this.addSlotToContainer(this.configSlot);
        this.wirelessIn = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODABLE_ITEM, this.wirelessEncoder, 0, 212, 10, ip);
        this.addSlotToContainer(this.wirelessIn);
        this.wirelessOut = new SlotOutput(this.wirelessEncoder, 1, 212, 68, -1);
        this.addSlotToContainer(this.wirelessOut);
        this.bindPlayerInventory(ip, 0, 0);
    }

    public void toggleSetting(String value, EntityPlayer player) {
        try {
            Item item;
            SecurityPermissions permission = SecurityPermissions.valueOf(value);
            ItemStack a = this.configSlot.getStack();
            if (a != null && (item = a.getItem()) instanceof IBiometricCard) {
                IBiometricCard bc = (IBiometricCard)item;
                if (bc.hasPermission(a, permission)) {
                    bc.removePermission(a, permission);
                } else {
                    bc.addPermission(a, permission);
                }
            }
        }
        catch (EnumConstantNotPresentException enumConstantNotPresentException) {
            // empty catch block
        }
    }

    @Override
    public void detectAndSendChanges() {
        Object object;
        this.verifyPermissions(SecurityPermissions.SECURITY, false);
        this.setPermissionMode(0);
        ItemStack a = this.configSlot.getStack();
        if (a != null && (object = a.getItem()) instanceof IBiometricCard) {
            IBiometricCard bc = (IBiometricCard)object;
            for (SecurityPermissions sp : bc.getPermissions(a)) {
                this.setPermissionMode(this.getPermissionMode() | 1 << sp.ordinal());
            }
        }
        this.updatePowerStatus();
        super.detectAndSendChanges();
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (this.wirelessIn.getHasStack()) {
            player.dropPlayerItemWithRandomChoice(this.wirelessIn.getStack(), false);
        }
        if (this.wirelessOut.getHasStack()) {
            player.dropPlayerItemWithRandomChoice(this.wirelessOut.getStack(), false);
        }
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
        if (!this.wirelessOut.getHasStack() && this.wirelessIn.getHasStack()) {
            IWirelessTermHandler wTermHandler;
            ItemStack term = this.wirelessIn.getStack().copy();
            INetworkEncodable networkEncodable = null;
            if (term.getItem() instanceof INetworkEncodable) {
                networkEncodable = (INetworkEncodable)term.getItem();
            }
            if ((wTermHandler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(term)) != null) {
                networkEncodable = wTermHandler;
            }
            if (networkEncodable != null) {
                networkEncodable.setEncryptionKey(term, String.valueOf(this.securityBox.getSecurityKey()), "");
                this.wirelessIn.putStack(null);
                this.wirelessOut.putStack(term);
                for (Object crafter : this.crafters) {
                    ICrafting icrafting = (ICrafting)crafter;
                    icrafting.sendSlotContents((Container)this, this.wirelessIn.slotNumber, this.wirelessIn.getStack());
                    icrafting.sendSlotContents((Container)this, this.wirelessOut.slotNumber, this.wirelessOut.getStack());
                }
            }
        }
    }

    public int getPermissionMode() {
        return this.permissionMode;
    }

    private void setPermissionMode(int permissionMode) {
        this.permissionMode = permissionMode;
    }
}

