/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  codechicken.nei.VisiblityData
 *  codechicken.nei.api.INEIGuiHandler
 *  codechicken.nei.api.TaggedInventoryArea
 *  cpw.mods.fml.common.Optional$Interface
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  org.apache.commons.lang3.tuple.Pair
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.config.ActionItems;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.implementations.IUpgradeableHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.SlotFake;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketNEIDragClick;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.parts.automation.PartExportBus;
import appeng.parts.automation.PartImportBus;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Optional;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Mouse;

@Optional.Interface(modid="NotEnoughItems", iface="codechicken.nei.api.INEIGuiHandler")
public class GuiUpgradeable
extends AEBaseGui
implements INEIGuiHandler {
    protected final ContainerUpgradeable cvb;
    protected final IUpgradeableHost bc;
    protected GuiImgButton redstoneMode;
    protected GuiImgButton fuzzyMode;
    protected GuiImgButton craftMode;
    protected GuiImgButton schedulingMode;
    protected GuiImgButton oreFilter;

    public GuiUpgradeable(InventoryPlayer inventoryPlayer, IUpgradeableHost te) {
        this(new ContainerUpgradeable(inventoryPlayer, te));
    }

    public GuiUpgradeable(ContainerUpgradeable te) {
        super(te);
        this.cvb = te;
        this.bc = (IUpgradeableHost)te.getTarget();
        if (this.hasToolbox()) {
            int n;
            switch (this.getToolboxSize()) {
                case 3: {
                    n = 246;
                    break;
                }
                case 5: {
                    n = 290;
                    break;
                }
                default: {
                    n = 246;
                }
            }
            this.xSize = n;
        } else {
            this.xSize = 211;
        }
        this.ySize = 184;
    }

    protected boolean hasToolbox() {
        return ((ContainerUpgradeable)this.inventorySlots).hasToolbox();
    }

    protected int getToolboxSize() {
        return ((ContainerUpgradeable)this.inventorySlots).getToolboxSize();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addButtons();
    }

    protected void addButtons() {
        this.redstoneMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.fuzzyMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.craftMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 48, Settings.CRAFT_ONLY, YesNo.NO);
        this.schedulingMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 68, Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT);
        this.oreFilter = new GuiImgButton(this.guiLeft - 18, this.guiTop + 28, Settings.ACTIONS, ActionItems.ORE_FILTER);
        this.buttonList.add(this.craftMode);
        this.buttonList.add(this.redstoneMode);
        this.buttonList.add(this.fuzzyMode);
        this.buttonList.add(this.schedulingMode);
        this.buttonList.add(this.oreFilter);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(this.getName().getLocal()), 8, 6, GuiColors.UpgradableTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.UpgradableInventory.getColor());
        if (this.redstoneMode != null) {
            this.redstoneMode.set(this.cvb.getRedStoneMode());
        }
        if (this.fuzzyMode != null) {
            this.fuzzyMode.set(this.cvb.getFuzzyMode());
        }
        if (this.craftMode != null) {
            this.craftMode.set(this.cvb.getCraftingMode());
        }
        if (this.schedulingMode != null) {
            this.schedulingMode.set(this.cvb.getSchedulingMode());
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.handleButtonVisibility();
        this.bindTexture(this.getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, 177, this.ySize);
        if (this.drawUpgrades()) {
            this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35, 14 + this.cvb.availableUpgrades() * 18);
        }
        if (this.hasToolbox()) {
            switch (this.getToolboxSize()) {
                case 3: {
                    this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90, 178, this.ySize - 90, 68, 68);
                    break;
                }
                case 5: {
                    this.bindTexture(this.getAdvancedBackground());
                    this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90 - 7, 0, 0, 104, 104);
                    this.bindTexture(this.getBackground());
                    break;
                }
                default: {
                    this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90, 178, this.ySize - 90, 68, 68);
                }
            }
        }
    }

    protected void handleButtonVisibility() {
        if (this.redstoneMode != null) {
            this.redstoneMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.REDSTONE) > 0);
        }
        if (this.fuzzyMode != null) {
            this.fuzzyMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.FUZZY) > 0 && this.bc.getInstalledUpgrades(Upgrades.ORE_FILTER) == 0);
        }
        if (this.craftMode != null) {
            this.craftMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.CRAFTING) > 0);
        }
        if (this.schedulingMode != null) {
            this.schedulingMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.CAPACITY) > 0 && this.bc instanceof PartExportBus);
        }
        if (this.oreFilter != null) {
            this.oreFilter.setVisibility(this.bc.getInstalledUpgrades(Upgrades.ORE_FILTER) > 0);
        }
    }

    protected String getBackground() {
        return "guis/bus.png";
    }

    protected String getAdvancedBackground() {
        return "guis/advanced_toolbox.png";
    }

    protected boolean drawUpgrades() {
        return true;
    }

    protected GuiText getName() {
        return this.bc instanceof PartImportBus ? GuiText.ImportBus : GuiText.ExportBus;
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.redstoneMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.redstoneMode.getSetting(), backwards));
        }
        if (btn == this.craftMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.craftMode.getSetting(), backwards));
        }
        if (btn == this.fuzzyMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.fuzzyMode.getSetting(), backwards));
        }
        if (btn == this.schedulingMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.schedulingMode.getSetting(), backwards));
        }
        if (btn == this.oreFilter) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_ORE_FILTER));
        }
    }

    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return currentVisibility;
    }

    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return Collections.emptyList();
    }

    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        ArrayList<Pair> slots = new ArrayList<Pair>();
        if (this.inventorySlots.inventorySlots.size() > 0) {
            for (int i = 0; i < this.inventorySlots.inventorySlots.size(); ++i) {
                Object slot = this.inventorySlots.inventorySlots.get(i);
                if (!(slot instanceof SlotFake)) continue;
                slots.add(Pair.of((Object)((Object)((SlotFake)((Object)slot))), (Object)i));
            }
        }
        for (Pair fakeSlotPair : slots) {
            SlotFake fakeSlot = (SlotFake)((Object)fakeSlotPair.getKey());
            if (!fakeSlot.isEnabled() || !this.getSlotArea(fakeSlot).contains(mouseX, mouseY)) continue;
            fakeSlot.putStack(draggedStack);
            NetworkHandler.instance.sendToServer(new PacketNEIDragClick(draggedStack, (Integer)fakeSlotPair.getValue()));
            if (draggedStack != null) {
                draggedStack.stackSize = 0;
            }
            return true;
        }
        return false;
    }

    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }

    private Rectangle getSlotArea(SlotFake slot) {
        return new Rectangle(this.guiLeft + slot.getX(), this.guiTop + slot.getY(), 16, 16);
    }
}

