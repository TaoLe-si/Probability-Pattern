/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.helpers.IPriorityHost;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerPriority
extends AEBaseContainer {
    private final IPriorityHost priHost;
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField priorityTextField;
    @SideOnly(value=Side.CLIENT)
    private boolean priorityTextInitialized;
    @GuiSync(value=2)
    public long priorityValue = -1L;

    public ContainerPriority(InventoryPlayer ip, IPriorityHost host) {
        super(ip, host);
        this.priHost = host;
    }

    @SideOnly(value=Side.CLIENT)
    public void setTextField(MEGuiTextField level) {
        this.priorityTextField = level;
        this.updatePriorityTextFieldValue();
    }

    public void setPriority(int newValue, EntityPlayer player) {
        this.priHost.setPriority(newValue);
        this.priorityValue = newValue;
    }

    @Override
    public void detectAndSendChanges() {
        if (!(this.priHost instanceof IGuiItemObject)) {
            this.verifyPermissions(SecurityPermissions.BUILD, false);
        }
        if (Platform.isServer()) {
            this.priorityValue = this.priHost.getPriority();
        }
        super.detectAndSendChanges();
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("priorityValue") && this.priorityTextField != null && !this.priorityTextInitialized) {
            this.updatePriorityTextFieldValue();
            this.priorityTextInitialized = true;
        }
        super.onUpdate(field, oldValue, newValue);
    }

    private void updatePriorityTextFieldValue() {
        this.priorityTextField.setText(String.valueOf(this.priorityValue));
        this.priorityTextField.setCursorPositionEnd();
        this.priorityTextField.setSelectionPos(0);
    }
}

