/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.client.gui.implementations.GuiAmount;
import appeng.container.implementations.ContainerPriority;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IPriorityHost;
import appeng.parts.automation.PartFormationPlane;
import appeng.parts.misc.PartInterface;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.misc.TileInterface;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import java.io.IOException;
import java.util.Iterator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class GuiPriority
extends GuiAmount {
    public GuiPriority(InventoryPlayer inventoryPlayer, IPriorityHost te) {
        super(new ContainerPriority(inventoryPlayer, te));
    }

    protected GuiPriority(ContainerPriority container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.nextBtn.enabled = false;
        this.nextBtn.visible = false;
        this.buttonList.remove(this.nextBtn);
        ((ContainerPriority)this.inventorySlots).setTextField(this.amountTextField);
    }

    @Override
    protected void setOriginGUI(Object target) {
        ItemStack interfaceStack;
        Iterator iterator;
        IDefinitions definitions = AEApi.instance().definitions();
        IParts parts = definitions.parts();
        IBlocks blocks = definitions.blocks();
        if (target instanceof PartStorageBus) {
            iterator = parts.storageBus().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack storageBusStack;
                this.myIcon = storageBusStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_STORAGEBUS;
        }
        if (target instanceof PartFormationPlane) {
            iterator = parts.formationPlane().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack formationPlaneStack;
                this.myIcon = formationPlaneStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_FORMATION_PLANE;
        }
        if (target instanceof TileDrive) {
            iterator = blocks.drive().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack driveStack;
                this.myIcon = driveStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_DRIVE;
        }
        if (target instanceof TileChest) {
            iterator = blocks.chest().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack chestStack;
                this.myIcon = chestStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_CHEST;
        }
        if (target instanceof TileInterface) {
            iterator = blocks.iface().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = interfaceStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_INTERFACE;
        }
        if (target instanceof PartInterface) {
            iterator = parts.iface().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = interfaceStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_INTERFACE;
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.Priority.getLocal(), 8, 6, GuiColors.PriorityTitle.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.amountTextField.drawTextBox();
    }

    @Override
    protected void keyTyped(char character, int key) {
        super.keyTyped(character, key);
        try {
            NetworkHandler.instance.sendToServer(new PacketValueConfig("PriorityHost.Priority", String.valueOf(this.getAmount())));
        }
        catch (IOException e) {
            AELog.debug(e);
        }
    }

    @Override
    protected int getButtonQtyByIndex(int index) {
        return AEConfig.instance.priorityByStacksAmounts(index);
    }

    @Override
    protected void addAmount(int i) {
        String result = Long.toString(this.getAmount() + i);
        this.amountTextField.setText(result);
        this.amountTextField.setCursorPositionEnd();
        try {
            NetworkHandler.instance.sendToServer(new PacketValueConfig("PriorityHost.Priority", result));
        }
        catch (IOException e) {
            AELog.debug(e);
        }
    }

    @Override
    protected int getAmount() {
        try {
            String out = this.amountTextField.getText();
            double result = Calculator.conversion(out);
            if (Double.isNaN(result)) {
                return 0;
            }
            return (int)ArithHelper.round(result, 0);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected String getBackground() {
        return "guis/priority.png";
    }
}

