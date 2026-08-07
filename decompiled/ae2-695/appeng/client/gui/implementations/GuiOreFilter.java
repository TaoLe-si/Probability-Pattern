/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.oredict.OreDictionary
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerOreFilter;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IOreFilterable;
import appeng.integration.modules.NEI;
import appeng.parts.automation.PartSharedItemBus;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.misc.TileCellWorkbench;
import appeng.util.prioitylist.OreFilteredList;
import java.io.IOException;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Mouse;

public class GuiOreFilter
extends AEBaseGui
implements IDropToFillTextField {
    private MEGuiTextField textField;
    private boolean useNEIFilter = false;
    private long lastclicktime;

    public GuiOreFilter(InventoryPlayer ip, IOreFilterable obj) {
        super(new ContainerOreFilter(ip, obj));
        this.xSize = 256;
        this.textField = new MEGuiTextField(231, 12){

            @Override
            public void onTextChange(String oldText) {
                String text = this.getText();
                if (!text.equals(oldText)) {
                    ((ContainerOreFilter)GuiOreFilter.this.inventorySlots).setFilter(text);
                    if (GuiOreFilter.this.useNEIFilter) {
                        NEI.searchField.updateFilter();
                    }
                }
            }
        };
        if (NEI.searchField.existsSearchField()) {
            this.textField.setFormatter(new OreFilteredList.OreFilterTextFormatter());
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.textField.x = this.guiLeft + 12;
        this.textField.y = this.guiTop + 35;
        this.textField.setFocused(true);
        ((ContainerOreFilter)this.inventorySlots).setTextField(this.textField);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (this.useNEIFilter) {
            this.useNEIFilter = false;
            NEI.searchField.updateFilter();
        }
    }

    public String getText() {
        return this.textField.getText();
    }

    public boolean useNEIFilter() {
        return this.useNEIFilter;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.OreFilterLabel.getLocal(), 12, 8, GuiColors.OreFilterLabel.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/renamer.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.textField.drawTextBox();
        if (this.useNEIFilter) {
            GuiOreFilter.drawRect((int)(this.textField.x - 1), (int)(this.textField.y - 1), (int)(this.textField.x + this.textField.w), (int)this.textField.y, (int)-256);
            GuiOreFilter.drawRect((int)(this.textField.x - 1), (int)(this.textField.y + this.textField.h - 1), (int)(this.textField.x + this.textField.w), (int)(this.textField.y + this.textField.h), (int)-256);
            GuiOreFilter.drawRect((int)(this.textField.x - 1), (int)this.textField.y, (int)this.textField.x, (int)(this.textField.y + this.textField.h - 1), (int)-256);
            GuiOreFilter.drawRect((int)(this.textField.x + this.textField.w - 1), (int)this.textField.y, (int)(this.textField.x + this.textField.w), (int)(this.textField.y + this.textField.h - 1), (int)-256);
        }
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        if (btn == 0 && NEI.searchField.existsSearchField() && this.textField.isMouseIn(xCoord, yCoord)) {
            if (this.textField.isFocused() && System.currentTimeMillis() - this.lastclicktime < 400L) {
                this.useNEIFilter = !this.useNEIFilter;
                NEI.searchField.updateFilter();
            }
            this.lastclicktime = System.currentTimeMillis();
        }
        this.textField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void keyTyped(char character, int key) {
        if (key == 28 || key == 156) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("OreFilter", this.textField.getText()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
            Object target = ((AEBaseContainer)this.inventorySlots).getTarget();
            GuiBridge OriginalGui = null;
            if (target instanceof PartStorageBus) {
                OriginalGui = GuiBridge.GUI_STORAGEBUS;
            } else if (target instanceof PartSharedItemBus) {
                OriginalGui = GuiBridge.GUI_BUS;
            } else if (target instanceof TileCellWorkbench) {
                OriginalGui = GuiBridge.GUI_CELL_WORKBENCH;
            }
            if (OriginalGui != null) {
                NetworkHandler.instance.sendToServer(new PacketSwitchGuis(OriginalGui));
            } else {
                this.mc.thePlayer.closeScreen();
            }
        } else if (!this.textField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        return this.textField.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        int[] ores = OreDictionary.getOreIDs((ItemStack)stack);
        if (ores.length > 0) {
            if (Mouse.isButtonDown((int)0)) {
                String oldText = this.textField.getText();
                if (!oldText.isEmpty()) {
                    oldText = oldText + " | ";
                }
                this.textField.setText(oldText + OreDictionary.getOreName((int)ores[0]));
            } else if (Mouse.isButtonDown((int)1)) {
                this.textField.setText(OreDictionary.getOreName((int)ores[0]));
            }
            this.textField.setCursorPositionEnd();
        }
    }
}

