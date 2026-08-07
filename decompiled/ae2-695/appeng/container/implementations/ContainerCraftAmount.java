/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.Slot
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.world.World
 */
package appeng.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.slot.SlotInaccessible;
import appeng.core.sync.GuiBridge;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ContainerCraftAmount
extends AEBaseContainer {
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField amountField;
    @GuiSync(value=1)
    public long initialCraftAmount = -1L;
    private final Slot craftingItem = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);
    private IAEItemStack itemToCreate;

    public ContainerCraftAmount(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
        this.addSlotToContainer(this.getCraftingItem());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.verifyPermissions(SecurityPermissions.CRAFT, false);
    }

    public IGrid getGrid() {
        IActionHost h = (IActionHost)this.getTarget();
        return h.getActionableNode().getGrid();
    }

    public World getWorld() {
        return this.getPlayerInv().player.worldObj;
    }

    public BaseActionSource getActionSrc() {
        return new PlayerSource(this.getPlayerInv().player, (IActionHost)this.getTarget());
    }

    public Slot getCraftingItem() {
        return this.craftingItem;
    }

    public IAEItemStack getItemToCraft() {
        return this.itemToCreate;
    }

    public void setItemToCraft(@Nonnull IAEItemStack itemToCreate) {
        this.itemToCreate = itemToCreate;
    }

    public void setInitialCraftAmount(long initialCraftAmount) {
        this.initialCraftAmount = initialCraftAmount;
    }

    @SideOnly(value=Side.CLIENT)
    public void setAmountField(MEGuiTextField amountField) {
        this.amountField = amountField;
        this.amountField.setText(String.valueOf(Math.max(1L, this.initialCraftAmount)));
        this.amountField.setCursorPositionEnd();
        this.amountField.setSelectionPos(0);
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("initialCraftAmount") && this.amountField != null) {
            this.amountField.setText(String.valueOf(Math.max(1L, this.initialCraftAmount)));
            this.amountField.setCursorPositionEnd();
            this.amountField.setSelectionPos(0);
        }
        super.onUpdate(field, oldValue, newValue);
    }

    public void openConfirmationGUI(EntityPlayer player, TileEntity te) {
        Platform.openGUI(player, te, this.getOpenContext().getSide(), GuiBridge.GUI_CRAFTING_CONFIRM);
        this.setupConfirmationGUI(player);
    }

    public void setupConfirmationGUI(EntityPlayer player) {
        Container container = player.openContainer;
        if (container instanceof ContainerCraftConfirm) {
            ContainerCraftConfirm ccc = (ContainerCraftConfirm)container;
            ccc.setItemToCraft(this.itemToCreate);
        }
    }
}

