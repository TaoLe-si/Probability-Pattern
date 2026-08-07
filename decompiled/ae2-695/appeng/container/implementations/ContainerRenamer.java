/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.tileentity.TileEntity
 */
package appeng.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.parts.IPart;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.helpers.ICustomNameObject;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerRenamer
extends AEBaseContainer {
    private final ICustomNameObject namedObject;
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField textField;

    public ContainerRenamer(InventoryPlayer ip, ICustomNameObject obj) {
        super(ip, obj instanceof TileEntity ? (TileEntity)obj : null, obj instanceof IPart ? (IPart)((Object)obj) : null);
        this.namedObject = obj;
    }

    @SideOnly(value=Side.CLIENT)
    public void setTextField(MEGuiTextField name) {
        this.textField = name;
        if (this.getCustomName() != null) {
            this.textField.setText(this.getCustomName());
        }
    }

    public void setNewName(String newValue) {
        this.namedObject.setCustomName(newValue);
    }

    @Override
    public void setCustomName(String customName) {
        super.setCustomName(customName);
        if (!Platform.isServer() && customName != null) {
            this.textField.setText(customName);
        }
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        super.detectAndSendChanges();
        if (!Platform.isServer() && this.getCustomName() != null) {
            this.textField.setText(this.getCustomName());
        }
    }
}

