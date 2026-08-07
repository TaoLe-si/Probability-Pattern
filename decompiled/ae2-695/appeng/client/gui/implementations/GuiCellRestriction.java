/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerCellRestriction;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.ICellRestriction;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import java.io.IOException;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

public class GuiCellRestriction
extends AEBaseGui {
    private MEGuiTextField amountField;
    private MEGuiTextField typesField;
    private ContainerCellRestriction.CellData cellData;

    public GuiCellRestriction(InventoryPlayer ip, ICellRestriction obj) {
        super(new ContainerCellRestriction(ip, obj));
        this.xSize = 256;
        this.amountField = new MEGuiTextField(85, 12);
        this.typesField = new MEGuiTextField(30, 12);
        this.cellData = new ContainerCellRestriction.CellData();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.amountField.x = this.guiLeft + 64;
        this.amountField.y = this.guiTop + 32;
        this.typesField.x = this.guiLeft + 162;
        this.typesField.y = this.guiTop + 32;
        Container container = this.inventorySlots;
        if (container instanceof ContainerCellRestriction) {
            ContainerCellRestriction ccr = (ContainerCellRestriction)container;
            ccr.setAmountField(this.amountField);
            ccr.setTypesField(this.typesField);
            ccr.setCellData(this.cellData);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    public String filterCellRestriction() {
        String types = this.typesField.getText();
        long amount = this.getAmount();
        int restrictionTypes = 0;
        long restrictionAmount = 0L;
        try {
            restrictionTypes = Math.min(Integer.parseInt(types), this.cellData.getTotalTypes());
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            restrictionAmount = Math.min(amount, (this.cellData.getTotalBytes() - (long)this.cellData.getPerType().intValue()) * (long)this.cellData.getPerByte().intValue());
        }
        catch (Exception exception) {
            // empty catch block
        }
        return restrictionTypes + "," + restrictionAmount;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        String type;
        this.fontRendererObj.drawString(GuiText.CellRestriction.getLocal(), 58, 6, GuiColors.DefaultBlack.getColor());
        switch (type = this.cellData.getCellType()) {
            case "item": {
                this.fontRendererObj.drawString(GuiText.NumberOfItems.getLocal(), 64, 23, GuiColors.DefaultBlack.getColor());
                break;
            }
            case "fluid": {
                this.fontRendererObj.drawString(GuiText.NumberOfFluids.getLocal(), 64, 23, GuiColors.DefaultBlack.getColor());
            }
        }
        this.fontRendererObj.drawString(GuiText.Types.getLocal(), 162, 23, GuiColors.DefaultBlack.getColor());
        this.fontRendererObj.drawString(GuiText.CellRestrictionTips.getLocal(), 64, 50, GuiColors.DefaultBlack.getColor());
        switch (type) {
            case "item": {
                this.fontRendererObj.drawString(GuiText.ItemsPerByte.getLocal() + " " + this.cellData.getPerByte(), 64, 60, GuiColors.DefaultBlack.getColor());
                break;
            }
            case "fluid": {
                this.fontRendererObj.drawString(GuiText.FluidsPerByte.getLocal() + " " + this.cellData.getPerByte() + " mB", 64, 60, GuiColors.DefaultBlack.getColor());
            }
        }
        this.fontRendererObj.drawString(GuiText.BytesPerType.getLocal() + " " + this.cellData.getPerType(), 64, 70, GuiColors.DefaultBlack.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/cellRestriction.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.amountField.drawTextBox();
        this.typesField.drawTextBox();
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.amountField.mouseClicked(xCoord, yCoord, btn);
        this.typesField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void keyTyped(char character, int key) {
        if (key == 28 || key == 156) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("cellRestriction", this.filterCellRestriction()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_CELL_WORKBENCH));
        } else if (!this.amountField.textboxKeyTyped(character, key) && !this.typesField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    private long getAmount() {
        String out = this.amountField.getText();
        double resultD = Calculator.conversion(out);
        if (resultD <= 0.0 || Double.isNaN(resultD)) {
            return 0L;
        }
        return (long)ArithHelper.round(resultD, 0);
    }
}

