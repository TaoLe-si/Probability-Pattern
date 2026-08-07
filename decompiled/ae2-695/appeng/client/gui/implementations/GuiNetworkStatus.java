/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraftforge.common.util.ForgeDirection
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.implementations;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.guiobjects.INetworkTool;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.NamedDimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiContextMenu;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.me.ItemRepo;
import appeng.client.me.SlotME;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.container.implementations.ContainerNetworkStatus;
import appeng.core.AEConfig;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketClick;
import appeng.core.sync.packets.PacketNetworkStatusSelected;
import appeng.util.Platform;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiNetworkStatus
extends AEBaseGui
implements ISortSource {
    private final ItemRepo repo;
    private final int rows = 4;
    private GuiImgButton units;
    private GuiImgButton cell;
    private int tooltip = -1;
    private final DecimalFormat df;
    private final boolean isAdvanced;
    private boolean isConsume;
    private final StringBuilder sb;
    private final String Equal;
    private final String Minus;
    private double totalBytes;
    private double usedBytes;
    private final int counterNumberGap = 20;
    private GuiContextMenu menu;

    public GuiNetworkStatus(InventoryPlayer inventoryPlayer, INetworkTool te) {
        super(new ContainerNetworkStatus(inventoryPlayer, te));
        GuiScrollbar scrollbar = new GuiScrollbar();
        this.sb = new StringBuilder();
        this.df = new DecimalFormat("#.##");
        this.setScrollBar(scrollbar);
        this.repo = new ItemRepo(scrollbar, this);
        this.ySize = 183;
        this.xSize = 195;
        this.repo.setRowSize(5);
        this.isAdvanced = te.getSize() != 3;
        this.isConsume = true;
        this.Equal = "=";
        this.Minus = "-";
        this.menu = new GuiContextMenu(5){

            @Override
            public void action(int i) {
                NamedDimensionalCoord dc = (NamedDimensionalCoord)this.list.get(i);
                if (GuiScreen.isShiftKeyDown()) {
                    BlockPosHighlighter.highlightBlocks((EntityPlayer)GuiNetworkStatus.this.mc.thePlayer, Collections.singletonList(dc), dc.getCustomName(), PlayerMessages.MachineHighlighted.getUnlocalized(), PlayerMessages.MachineInOtherDim.getUnlocalized());
                    GuiNetworkStatus.this.mc.thePlayer.closeScreen();
                } else {
                    NetworkHandler.instance.sendToServer(new PacketClick(dc.x, dc.y, dc.z, ForgeDirection.UP.ordinal(), 0.0f, 0.0f, 0.0f));
                }
            }

            @Override
            public String getDrawText(int i) {
                NamedDimensionalCoord dc = (NamedDimensionalCoord)this.list.get(i);
                if (dc.getCustomName().isEmpty()) {
                    return "x:" + dc.x + " y:" + dc.y + " z:" + dc.z;
                }
                return dc.getCustomName();
            }
        };
    }

    @Override
    protected boolean mouseWheelEvent(int x, int y, int wheel) {
        if (this.menu.mouseWheelEvent(x, y, wheel)) {
            return true;
        }
        return super.mouseWheelEvent(x, y, wheel);
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        IAEItemStack aeStack;
        if (this.menu.mouseClick(xCoord, yCoord, btn)) {
            return;
        }
        ItemStack is = null;
        if (this.tooltip > -1 && (aeStack = this.repo.getReferenceItem(this.tooltip)) != null) {
            is = aeStack.getItemStack();
        }
        if (is == null || !is.hasTagCompound()) {
            super.mouseClicked(xCoord, yCoord, btn);
            return;
        }
        NBTTagCompound tag = is.getTagCompound();
        switch (btn) {
            case 0: {
                if (!GuiNetworkStatus.isShiftKeyDown()) break;
                List<DimensionalCoord> dcl = DimensionalCoord.readAsListFromNBT(tag);
                BlockPosHighlighter.highlightBlocks((EntityPlayer)this.mc.thePlayer, dcl, is.getDisplayName(), PlayerMessages.MachineHighlighted.getUnlocalized(), PlayerMessages.MachineInOtherDim.getUnlocalized());
                this.mc.thePlayer.closeScreen();
                break;
            }
            case 1: {
                List<NamedDimensionalCoord> dcl = NamedDimensionalCoord.readAsListFromNBTNamed(tag);
                this.menu.init(dcl, xCoord, yCoord);
            }
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        boolean oldConsume = this.isConsume;
        if (btn == this.units) {
            if (this.isConsume) {
                AEConfig.instance.nextPowerUnit(backwards);
                this.units.set(AEConfig.instance.selectedPowerUnit());
            } else {
                this.isConsume = !this.isConsume;
            }
        } else if (btn == this.cell) {
            if (!this.isConsume) {
                AEConfig.instance.nextCellType(backwards);
                this.cell.set(AEConfig.instance.selectedCellType());
            } else {
                boolean bl = this.isConsume = !this.isConsume;
            }
        }
        if (oldConsume != this.isConsume) {
            try {
                NetworkHandler.instance.sendToServer(new PacketNetworkStatusSelected(this.isConsume));
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.units = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.POWER_UNITS, AEConfig.instance.selectedPowerUnit());
        this.buttonList.add(this.units);
        if (this.isAdvanced) {
            this.cell = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.CELL_TYPE, AEConfig.instance.selectedCellType());
            this.buttonList.add(this.cell);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        int gx = (this.width - this.xSize) / 2;
        int gy = (this.height - this.ySize) / 2;
        this.tooltip = -1;
        int y = 0;
        int x = 0;
        int viewEnd = Math.min(20, this.repo.size());
        for (int z = 0; z < viewEnd; ++z) {
            int minX = gx + 14 + x * 31;
            int minY = gy + 41 + y * 18;
            if (!this.menu.isActive() && minX < mouseX && minX + 28 > mouseX && minY < mouseY && minY + 20 > mouseY) {
                this.tooltip = z;
                break;
            }
            if (++x <= 4) continue;
            ++y;
            x = 0;
        }
        super.drawScreen(mouseX, mouseY, btn);
        this.menu.draw(mouseX, mouseY);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        if (this.isConsume) {
            this.drawConsume();
        } else {
            switch (AEConfig.instance.selectedCellType()) {
                case ITEM: {
                    this.drawItemInfo();
                    break;
                }
                case FLUID: {
                    this.drawFluidInfo();
                    break;
                }
                case ESSENTIA: {
                    this.drawEssentiaInfo();
                }
            }
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/networkstatus.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    public void postUpdate(List<IAEItemStack> list) {
        this.repo.clear();
        for (IAEItemStack is : list) {
            this.repo.postUpdate(is);
        }
        this.repo.updateView();
        this.setScrollBar();
    }

    private void setScrollBar() {
        int size = this.repo.size();
        this.getScrollBar().setTop(39).setLeft(175).setHeight(78);
        this.getScrollBar().setRange(0, (size + 4) / 5 - this.rows, 1);
    }

    public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> currentToolTip) {
        Slot s;
        if (stack != null && (s = this.getSlot(mouseX, mouseY)) instanceof SlotME) {
            IAEItemStack myStack = null;
            try {
                SlotME theSlotField = (SlotME)s;
                myStack = theSlotField.getAEStack();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (myStack != null) {
                while (currentToolTip.size() > 1) {
                    currentToolTip.remove(1);
                }
            }
        }
        return currentToolTip;
    }

    protected void drawItemStackTooltip(ItemStack stack, int x, int y) {
        Slot s = this.getSlot(x, y);
        if (s instanceof SlotME && stack != null) {
            IAEItemStack myStack = null;
            try {
                SlotME theSlotField = (SlotME)s;
                myStack = theSlotField.getAEStack();
            }
            catch (Throwable theSlotField) {
                // empty catch block
            }
            if (myStack != null) {
                List currentToolTip = stack.getTooltip((EntityPlayer)this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
                while (currentToolTip.size() > 1) {
                    currentToolTip.remove(1);
                }
                currentToolTip.add(GuiText.Installed.getLocal() + ": " + NumberFormat.getNumberInstance(Locale.US).format(myStack.getStackSize()));
                currentToolTip.add(GuiText.EnergyDrain.getLocal() + ": " + Platform.formatPowerLong(myStack.getCountRequestable(), true));
                this.drawTooltip(x, y, currentToolTip.toArray(new String[0]));
            }
        }
    }

    @Override
    public Enum getSortBy() {
        return SortOrder.NAME;
    }

    @Override
    public Enum getSortDir() {
        return SortDir.ASCENDING;
    }

    @Override
    public Enum getSortDisplay() {
        return ViewItems.ALL;
    }

    private void drawConsume() {
        ContainerNetworkStatus ns = (ContainerNetworkStatus)this.inventorySlots;
        this.fontRendererObj.drawString(GuiText.NetworkDetails.getLocal(), 8, 6, GuiColors.NetworkStatusDetails.getColor());
        if (ns.isPowerInfinite()) {
            this.fontRendererObj.drawString(GuiText.StoredPower.getLocal() + ": \u221e", 13, 16, GuiColors.NetworkStatusStoredPower.getColor());
            this.fontRendererObj.drawString(GuiText.MaxPower.getLocal() + ": \u221e", 13, 26, GuiColors.NetworkStatusMaxPower.getColor());
        } else {
            this.fontRendererObj.drawString(GuiText.StoredPower.getLocal() + ": " + Platform.formatPowerLong(ns.getCurrentPower(), false), 13, 16, GuiColors.NetworkStatusStoredPower.getColor());
            this.fontRendererObj.drawString(GuiText.MaxPower.getLocal() + ": " + Platform.formatPowerLong(ns.getMaxPower(), false), 13, 26, GuiColors.NetworkStatusMaxPower.getColor());
        }
        this.fontRendererObj.drawString(GuiText.PowerInputRate.getLocal() + ": " + Platform.formatPowerLong(ns.getAverageAddition(), true), 13, 133, GuiColors.NetworkStatusPowerInputRate.getColor());
        this.fontRendererObj.drawString(GuiText.PowerUsageRate.getLocal() + ": " + Platform.formatPowerLong(ns.getPowerUsage(), true), 13, 123, GuiColors.NetworkStatusPowerUsageRate.getColor());
        this.totalBytes = Double.longBitsToDouble(ns.getItemBytesTotal());
        this.usedBytes = Double.longBitsToDouble(ns.getItemBytesUsed());
        String tempStr = this.totalBytes == 0.0 ? "" : " (" + this.df.format(this.usedBytes * 100.0 / this.totalBytes) + "%)";
        this.fontRendererObj.drawString(GuiText.Items.getLocal() + ": " + Platform.formatByteDouble(this.usedBytes) + " / " + Platform.formatByteDouble(this.totalBytes) + tempStr, 13, 143, GuiColors.DefaultBlack.getColor());
        this.totalBytes = Double.longBitsToDouble(ns.getFluidBytesTotal());
        this.usedBytes = Double.longBitsToDouble(ns.getFluidBytesUsed());
        tempStr = this.totalBytes == 0.0 ? "" : " (" + this.df.format(this.usedBytes * 100.0 / this.totalBytes) + "%)";
        this.fontRendererObj.drawString(GuiText.Fluids.getLocal() + ": " + Platform.formatByteDouble(this.usedBytes) + " / " + Platform.formatByteDouble(this.totalBytes) + tempStr, 13, 153, GuiColors.DefaultBlack.getColor());
        this.totalBytes = Double.longBitsToDouble(ns.getEssentiaBytesTotal());
        this.usedBytes = Double.longBitsToDouble(ns.getEssentiaBytesUsed());
        tempStr = this.totalBytes == 0.0 ? "" : " (" + this.df.format(this.usedBytes * 100.0 / this.totalBytes) + "%)";
        this.fontRendererObj.drawString(GuiText.Essentias.getLocal() + ": " + Platform.formatByteDouble(this.usedBytes) + " / " + Platform.formatByteDouble(this.totalBytes) + tempStr, 13, 163, GuiColors.DefaultBlack.getColor());
        this.drawItemRepo();
    }

    private void drawItemRepo() {
        int sectionLength = 30;
        int x = 0;
        int y = 0;
        int xo = 12;
        int yo = 42;
        boolean viewStart = false;
        int viewEnd = 20;
        String toolTip = "";
        int toolPosX = 0;
        int toolPosY = 0;
        for (int z = 0; z < Math.min(20, this.repo.size()); ++z) {
            IAEItemStack refStack = this.repo.getReferenceItem(z);
            if (refStack == null) continue;
            GL11.glPushMatrix();
            GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
            String str = Long.toString(refStack.getStackSize());
            if (refStack.getStackSize() >= 10000L) {
                str = Long.toString(refStack.getStackSize() / 1000L) + 'k';
            }
            int w = this.fontRendererObj.getStringWidth(str);
            this.fontRendererObj.drawString(str, (int)(((double)(x * 30 + 12 + 30 - 19) - (double)w * 0.5) * 2.0), (y * 18 + 42 + 6) * 2, GuiColors.NetworkStatusItemCount.getColor());
            GL11.glPopMatrix();
            int posX = x * 30 + 12 + 30 - 18;
            int posY = y * 18 + 42;
            if (this.tooltip == z - 0) {
                toolTip = Platform.getItemDisplayName(this.repo.getItem(z));
                toolTip = toolTip + '\n' + GuiText.Installed.getLocal() + ": " + NumberFormat.getNumberInstance(Locale.US).format(refStack.getStackSize());
                if (refStack.getCountRequestable() > 0L) {
                    toolTip = toolTip + '\n' + GuiText.EnergyDrain.getLocal() + ": " + Platform.formatPowerLong(refStack.getCountRequestable(), true);
                }
                toolPosX = x * 30 + 12 + 30 - 8;
                toolPosY = y * 18 + 42;
            }
            this.drawItem(posX, posY, this.repo.getItem(z));
            if (++x <= 4) continue;
            ++y;
            x = 0;
        }
        if (this.tooltip >= 0 && toolTip.length() > 0) {
            this.drawTooltip(toolPosX, toolPosY + 10, toolTip);
        }
    }

    private GuiColors getCorrespondingColor(double percentage) {
        if (Double.isNaN(percentage)) {
            return GuiColors.DefaultBlack;
        }
        if (percentage > 95.0) {
            return GuiColors.CellStatusRed;
        }
        if (percentage > 75.0) {
            return GuiColors.CellStatusOrange;
        }
        return GuiColors.DefaultBlack;
    }

    private String getProgressBar(double percentage) {
        int count = (int)Math.round(percentage / 5.0);
        this.sb.setLength(0);
        this.sb.append('<');
        for (int i = 0; i < 20; ++i) {
            if (i < count) {
                this.sb.append(this.Equal);
                continue;
            }
            this.sb.append(this.Minus);
        }
        this.sb.append('>');
        return this.sb.toString();
    }

    private void drawItemInfo() {
        ContainerNetworkStatus ns = (ContainerNetworkStatus)this.inventorySlots;
        this.fontRendererObj.drawString(GuiText.NetworkBytesDetails.getLocal(), 8, 6, GuiColors.DefaultBlack.getColor());
        this.fontRendererObj.drawString(GuiText.NetworkItemCellCount.getLocal() + " : " + ns.getItemCellCount(), 13, 16, GuiColors.DefaultBlack.getColor());
        this.drawAllCellCount(ns.getItemCellG(), ns.getItemCellB(), ns.getItemCellO(), ns.getItemCellR());
        this.totalBytes = Double.longBitsToDouble(ns.getItemBytesTotal());
        this.usedBytes = Double.longBitsToDouble(ns.getItemBytesUsed());
        double tempDouble = this.usedBytes * 100.0 / this.totalBytes;
        String tempStr = this.totalBytes == 0.0 ? " (0%)" : " (" + this.df.format(tempDouble) + "%)";
        this.fontRendererObj.drawString(GuiText.BytesInfo.getLocal() + ": " + Platform.formatByteDouble(this.usedBytes) + " / " + Platform.formatByteDouble(this.totalBytes), 13, 123, this.getCorrespondingColor(tempDouble).getColor());
        this.fontRendererObj.drawString(this.getProgressBar(tempDouble) + tempStr, 13, 133, this.getCorrespondingColor(tempDouble).getColor());
        tempDouble = (double)ns.getItemTypesUsed() * 100.0 / (double)ns.getItemTypesTotal();
        tempStr = ns.getItemTypesTotal() == 0L ? " (0%)" : " (" + this.df.format(tempDouble) + "%)";
        this.fontRendererObj.drawString(GuiText.TypesInfo.getLocal() + ": " + ns.getItemTypesUsed() + " / " + ns.getItemTypesTotal(), 13, 143, this.getCorrespondingColor(tempDouble).getColor());
        this.fontRendererObj.drawString(this.getProgressBar(tempDouble) + tempStr, 13, 153, this.getCorrespondingColor(tempDouble).getColor());
        this.drawItemRepo();
    }

    private void drawFluidInfo() {
        ContainerNetworkStatus ns = (ContainerNetworkStatus)this.inventorySlots;
        this.fontRendererObj.drawString(GuiText.NetworkBytesDetails.getLocal(), 8, 6, GuiColors.DefaultBlack.getColor());
        this.fontRendererObj.drawString(GuiText.NetworkFluidCellCount.getLocal() + " : " + ns.getFluidCellCount(), 13, 16, GuiColors.DefaultBlack.getColor());
        this.drawAllCellCount(ns.getFluidCellG(), ns.getFluidCellB(), ns.getFluidCellO(), ns.getFluidCellR());
        this.totalBytes = Double.longBitsToDouble(ns.getFluidBytesTotal());
        this.usedBytes = Double.longBitsToDouble(ns.getFluidBytesUsed());
        double tempDouble = this.usedBytes * 100.0 / this.totalBytes;
        String tempStr = this.totalBytes == 0.0 ? " (0%)" : " (" + this.df.format(tempDouble) + "%)";
        this.fontRendererObj.drawString(GuiText.BytesInfo.getLocal() + ": " + Platform.formatByteDouble(this.usedBytes) + " / " + Platform.formatByteDouble(this.totalBytes), 13, 123, this.getCorrespondingColor(tempDouble).getColor());
        this.fontRendererObj.drawString(this.getProgressBar(tempDouble) + tempStr, 13, 133, this.getCorrespondingColor(tempDouble).getColor());
        tempDouble = (double)ns.getFluidTypesUsed() * 100.0 / (double)ns.getFluidTypesTotal();
        tempStr = ns.getFluidTypesTotal() == 0L ? " (0%)" : " (" + this.df.format(tempDouble) + "%)";
        this.fontRendererObj.drawString(GuiText.TypesInfo.getLocal() + ": " + ns.getFluidTypesUsed() + " / " + ns.getFluidTypesTotal(), 13, 143, this.getCorrespondingColor(tempDouble).getColor());
        this.fontRendererObj.drawString(this.getProgressBar(tempDouble) + tempStr, 13, 153, this.getCorrespondingColor(tempDouble).getColor());
        this.drawItemRepo();
    }

    private void drawEssentiaInfo() {
        ContainerNetworkStatus ns = (ContainerNetworkStatus)this.inventorySlots;
        this.fontRendererObj.drawString(GuiText.NetworkBytesDetails.getLocal(), 8, 6, GuiColors.DefaultBlack.getColor());
        this.fontRendererObj.drawString(GuiText.NetworkEssentiaCellCount.getLocal() + " : " + ns.getEssentiaCellCount(), 13, 16, GuiColors.DefaultBlack.getColor());
        this.drawAllCellCount(ns.getEssentiaCellG(), ns.getEssentiaCellB(), ns.getEssentiaCellO(), ns.getEssentiaCellR());
        this.totalBytes = Double.longBitsToDouble(ns.getEssentiaBytesTotal());
        this.usedBytes = Double.longBitsToDouble(ns.getEssentiaBytesUsed());
        double tempDouble = this.usedBytes * 100.0 / this.totalBytes;
        String tempStr = this.totalBytes == 0.0 ? " (0%)" : " (" + this.df.format(tempDouble) + "%)";
        this.fontRendererObj.drawString(GuiText.BytesInfo.getLocal() + ": " + Platform.formatByteDouble(this.usedBytes) + " / " + Platform.formatByteDouble(this.totalBytes), 13, 123, this.getCorrespondingColor(tempDouble).getColor());
        this.fontRendererObj.drawString(this.getProgressBar(tempDouble) + tempStr, 13, 133, this.getCorrespondingColor(tempDouble).getColor());
        tempDouble = (double)ns.getEssentiaTypesUsed() * 100.0 / (double)ns.getEssentiaTypesTotal();
        tempStr = ns.getEssentiaTypesTotal() == 0L ? " (0%)" : " (" + this.df.format(tempDouble) + "%)";
        this.fontRendererObj.drawString(GuiText.TypesInfo.getLocal() + ": " + ns.getEssentiaTypesUsed() + " / " + ns.getEssentiaTypesTotal(), 13, 143, this.getCorrespondingColor(tempDouble).getColor());
        this.fontRendererObj.drawString(this.getProgressBar(tempDouble) + tempStr, 13, 153, this.getCorrespondingColor(tempDouble).getColor());
        this.drawItemRepo();
    }

    private void drawAllCellCount(long greenCellNum, long blueCellNum, long orangeCellNum, long redCellNum) {
        this.fontRendererObj.drawString(GuiText.NetworkCellStatus.getLocal() + ":", 13, 27, GuiColors.DefaultBlack.getColor());
        int numStartAt = this.fontRendererObj.getStringWidth(GuiText.NetworkCellStatus.getLocal() + ":") + 20;
        this.fontRendererObj.drawString(String.valueOf(greenCellNum), numStartAt + this.counterNumberGap * 0, 27, GuiColors.CellStatusGreen.getColor());
        this.fontRendererObj.drawString(String.valueOf(blueCellNum), numStartAt + this.counterNumberGap * 1, 27, GuiColors.CellStatusBlue.getColor());
        this.fontRendererObj.drawString(String.valueOf(orangeCellNum), numStartAt + this.counterNumberGap * 2, 27, GuiColors.CellStatusOrange.getColor());
        this.fontRendererObj.drawString(String.valueOf(redCellNum), numStartAt + this.counterNumberGap * 3, 27, GuiColors.CellStatusRed.getColor());
    }
}

