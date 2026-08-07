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

import appeng.api.parts.IPart;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.helpers.IOreFilterable;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerOreFilter
extends AEBaseContainer {
    private final IOreFilterable filterHost;
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField textField;
    @GuiSync(value=2)
    public String filter = "";

    public ContainerOreFilter(InventoryPlayer ip, IOreFilterable te) {
        super(ip, (TileEntity)(te instanceof TileEntity ? te : null), (IPart)((Object)(te instanceof IPart ? te : null)));
        this.filterHost = te;
    }

    @SideOnly(value=Side.CLIENT)
    public void setTextField(MEGuiTextField f) {
        this.textField = f;
        this.textField.setText(this.filter);
    }

    public void setFilter(String newValue) {
        this.filterHost.setFilter(newValue);
        this.filter = newValue;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.filter = this.filterHost.getFilter();
        }
        super.detectAndSendChanges();
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("filter") && this.textField != null) {
            this.textField.setText(this.filter);
        }
        super.onUpdate(field, oldValue, newValue);
    }
}

