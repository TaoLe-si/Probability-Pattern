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
import appeng.helpers.ICellRestriction;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerCellRestriction
extends AEBaseContainer {
    private final ICellRestriction Host;
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField typesField;
    @SideOnly(value=Side.CLIENT)
    private MEGuiTextField amountField;
    @SideOnly(value=Side.CLIENT)
    private CellData cellData;
    @GuiSync(value=69)
    public String cellRestriction;

    public ContainerCellRestriction(InventoryPlayer ip, ICellRestriction te) {
        super(ip, (TileEntity)(te instanceof TileEntity ? te : null), (IPart)((Object)(te instanceof IPart ? te : null)));
        this.Host = te;
    }

    @SideOnly(value=Side.CLIENT)
    public void setAmountField(MEGuiTextField f) {
        this.amountField = f;
    }

    @SideOnly(value=Side.CLIENT)
    public void setTypesField(MEGuiTextField f) {
        this.typesField = f;
    }

    @SideOnly(value=Side.CLIENT)
    public void setCellData(CellData newCellData) {
        this.cellData = newCellData;
    }

    public void setCellRestriction(String data) {
        this.Host.setCellRestriction(null, data);
        this.cellRestriction = data;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.cellRestriction = this.Host.getCellData(null);
        }
        super.detectAndSendChanges();
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("cellRestriction") && this.amountField != null && this.typesField != null) {
            String[] newData = this.cellRestriction.split(",", 7);
            this.cellData.setTotalBytes(Long.parseLong(newData[0]));
            this.cellData.setTotalTypes(Integer.parseInt(newData[1]));
            this.cellData.setPerType(Integer.parseInt(newData[2]));
            this.cellData.setPerByte(Integer.parseInt(newData[3]));
            this.cellData.setCellType(newData[6]);
            this.typesField.setMaxStringLength(this.cellData.getTotalTypes().toString().length());
            this.amountField.setMaxStringLength(String.valueOf((this.cellData.getTotalBytes() - (long)this.cellData.getPerType().intValue()) * (long)this.cellData.getPerByte().intValue()).length());
            this.typesField.setText(newData[4]);
            this.amountField.setText(newData[5]);
        }
        super.onUpdate(field, oldValue, newValue);
    }

    public static class CellData {
        private Long totalBytes;
        private Integer totalTypes;
        private Integer perType;
        private Integer perByte;
        private String cellType;

        public void setPerType(Integer perType) {
            this.perType = perType;
        }

        public void setTotalBytes(Long totalBytes) {
            this.totalBytes = totalBytes;
        }

        public void setTotalTypes(Integer totalTypes) {
            this.totalTypes = totalTypes;
        }

        public void setPerByte(Integer perByte) {
            this.perByte = perByte;
        }

        public void setCellType(String cellType) {
            this.cellType = cellType;
        }

        public Integer getPerType() {
            return this.perType;
        }

        public Integer getTotalTypes() {
            return this.totalTypes;
        }

        public Long getTotalBytes() {
            return this.totalBytes;
        }

        public Integer getPerByte() {
            return this.perByte;
        }

        public String getCellType() {
            return this.cellType;
        }
    }
}

