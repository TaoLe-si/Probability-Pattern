/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Optional
 *  cpw.mods.fml.common.Loader
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.StatCollector
 *  net.minecraft.world.World
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.StringOrder;
import appeng.api.config.TerminalStyle;
import appeng.api.config.YesNo;
import appeng.api.util.DimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IGuiTooltipHandler;
import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.AppEngSlot;
import appeng.core.AEConfig;
import appeng.core.CommonHelper;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.helpers.PatternHelper;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.modules.NEI;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import com.google.common.base.Optional;
import cpw.mods.fml.common.Loader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiInterfaceTerminal
extends AEBaseGui
implements IDropToFillTextField,
IGuiTooltipHandler,
IInterfaceTerminalPostUpdate {
    public static final int HEADER_HEIGHT = 52;
    public static final int INV_HEIGHT = 98;
    public static final int VIEW_WIDTH = 174;
    public static final int VIEW_LEFT = 10;
    protected static final ResourceLocation BACKGROUND = new ResourceLocation("appliedenergistics2", "textures/guis/newinterfaceterminal.png");
    private final InterfaceTerminalList masterList;
    private final MEGuiTextField searchFieldOutputs;
    private final MEGuiTextField searchFieldInputs;
    private final MEGuiTextField searchFieldNames;
    private final GuiImgButton guiButtonHideFull;
    private final GuiImgButton guiButtonAssemblersOnly;
    private final GuiImgButton guiButtonBrokenRecipes;
    private final GuiImgButton guiButtonUseSubstitute;
    protected final GuiImgButton terminalStyleBox;
    private final GuiImgButton searchStringSave;
    private final GuiImgButton guiButtonSectionOrder;
    private boolean onlyMolecularAssemblers;
    private boolean onlyBrokenRecipes;
    private boolean onlySubstitute;
    private boolean online;
    private int viewHeight;
    private final List<String> extraOptionsText;
    private ItemStack tooltipStack;
    private final boolean neiPresent;
    protected static String searchFieldInputsText = "";
    protected static String searchFieldOutputsText = "";
    protected static String searchFieldNamesText = "";
    protected int offsetY;
    private static final float ITEM_STACK_Z = 100.0f;
    private static final float SLOT_Z = 0.5f;
    private static final float ITEM_STACK_OVERLAY_Z = 200.0f;
    private static final float SLOT_HOVER_Z = 310.0f;
    private static final float TOOLTIP_Z = 410.0f;
    private static final float STEP_Z = 10.0f;
    private static final float MAGIC_RENDER_ITEM_Z = 50.0f;

    public GuiInterfaceTerminal(InventoryPlayer inventoryPlayer, PartInterfaceTerminal te) {
        this(new ContainerInterfaceTerminal(inventoryPlayer, te));
    }

    public GuiInterfaceTerminal(Container cont) {
        super(cont);
        this.masterList = new InterfaceTerminalList(((StringOrder)AEConfig.instance.settings.getSetting((Settings)Settings.INTERFACE_TERMINAL_SECTION_ORDER)).comparator);
        this.onlyMolecularAssemblers = false;
        this.onlyBrokenRecipes = false;
        this.onlySubstitute = false;
        this.setScrollBar(new GuiScrollbar());
        this.xSize = 208;
        this.ySize = 255;
        this.neiPresent = Loader.isModLoaded((String)"NotEnoughItems");
        this.searchFieldInputs = new MEGuiTextField(86, 12, ButtonToolTips.SearchFieldInputs.getLocal()){

            @Override
            public void onTextChange(String oldText) {
                GuiInterfaceTerminal.this.masterList.markDirty();
            }
        };
        this.searchFieldOutputs = new MEGuiTextField(86, 12, ButtonToolTips.SearchFieldOutputs.getLocal()){

            @Override
            public void onTextChange(String oldText) {
                GuiInterfaceTerminal.this.masterList.markDirty();
            }
        };
        this.searchFieldNames = new MEGuiTextField(71, 12, ButtonToolTips.SearchFieldNames.getLocal()){

            @Override
            public void onTextChange(String oldText) {
                GuiInterfaceTerminal.this.masterList.markDirty();
            }
        };
        this.searchFieldNames.setFocused(true);
        this.searchStringSave = new GuiImgButton(0, 0, Settings.SAVE_SEARCH, AEConfig.instance.preserveSearchBar ? YesNo.YES : YesNo.NO);
        this.guiButtonAssemblersOnly = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.guiButtonHideFull = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.guiButtonBrokenRecipes = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.guiButtonUseSubstitute = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.guiButtonSectionOrder = new GuiImgButton(0, 0, Settings.INTERFACE_TERMINAL_SECTION_ORDER, StringOrder.NATURAL);
        this.terminalStyleBox = new GuiImgButton(0, 0, Settings.TERMINAL_STYLE, null);
        this.extraOptionsText = new ArrayList<String>(2);
        this.extraOptionsText.add(ButtonToolTips.HighlightInterface.getLocal());
        NEI.searchField.putFormatter(this.searchFieldInputs);
        NEI.searchField.putFormatter(this.searchFieldOutputs);
    }

    private void setScrollBar() {
        int maxScroll = this.masterList.getHeight() - this.viewHeight - 1;
        if (maxScroll <= 0) {
            this.getScrollBar().setTop(52).setLeft(189).setHeight(this.viewHeight).setRange(0, 0, 1);
        } else {
            this.getScrollBar().setTop(52).setLeft(189).setHeight(this.viewHeight).setRange(0, maxScroll, 12);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.viewHeight = this.calculateViewHeight();
        this.ySize = 150 + this.viewHeight;
        int unusedSpace = this.height - this.ySize;
        this.guiTop = (int)Math.floor((float)unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));
        this.searchFieldInputs.x = this.guiLeft + Math.max(32, 10);
        this.searchFieldInputs.y = this.guiTop + 25;
        this.searchFieldOutputs.x = this.guiLeft + Math.max(32, 10);
        this.searchFieldOutputs.y = this.guiTop + 38;
        this.searchFieldNames.x = this.guiLeft + Math.max(32, 10) + 99;
        this.searchFieldNames.y = this.guiTop + 38;
        this.terminalStyleBox.xPosition = this.guiLeft - 18;
        this.terminalStyleBox.yPosition = this.guiTop + 8;
        this.searchStringSave.xPosition = this.guiLeft - 18;
        this.searchStringSave.yPosition = this.terminalStyleBox.yPosition + 18;
        this.guiButtonSectionOrder.xPosition = this.guiLeft - 18;
        this.guiButtonSectionOrder.yPosition = this.searchStringSave.yPosition + 18;
        this.guiButtonBrokenRecipes.xPosition = this.guiLeft - 18;
        this.guiButtonBrokenRecipes.yPosition = this.guiButtonSectionOrder.yPosition + 18;
        this.guiButtonHideFull.xPosition = this.guiLeft - 18;
        this.guiButtonHideFull.yPosition = this.guiButtonBrokenRecipes.yPosition + 18;
        this.guiButtonAssemblersOnly.xPosition = this.guiLeft - 18;
        this.guiButtonAssemblersOnly.yPosition = this.guiButtonHideFull.yPosition + 18;
        this.guiButtonUseSubstitute.xPosition = this.guiLeft - 18;
        this.offsetY = this.guiButtonUseSubstitute.yPosition = this.guiButtonAssemblersOnly.yPosition + 18;
        this.setSearchString();
        this.setScrollBar();
        this.repositionSlots();
        this.buttonList.add(this.guiButtonAssemblersOnly);
        this.buttonList.add(this.guiButtonHideFull);
        this.buttonList.add(this.guiButtonBrokenRecipes);
        this.buttonList.add(this.guiButtonSectionOrder);
        this.buttonList.add(this.searchStringSave);
        this.buttonList.add(this.terminalStyleBox);
        this.buttonList.add(this.guiButtonUseSubstitute);
    }

    protected void repositionSlots() {
        for (Object obj : this.inventorySlots.inventorySlots) {
            if (!(obj instanceof AppEngSlot)) continue;
            AppEngSlot slot = (AppEngSlot)((Object)obj);
            slot.yDisplayPosition = this.ySize + slot.getY() - 78 - 7;
        }
    }

    protected int calculateViewHeight() {
        int maxViewHeight = this.getMaxViewHeight();
        boolean hasNEI = IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI);
        int NEIPadding = hasNEI ? 40 : 0;
        int availableSpace = this.height - 52 - 98 - NEIPadding;
        return Math.min((int)((double)availableSpace * 0.95), maxViewHeight);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.InterfaceTerminal.getLocal()), 8, 6, GuiColors.InterfaceTerminalTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 12, this.ySize - 96, GuiColors.InterfaceTerminalInventory.getColor());
        if (!this.neiPresent && this.tooltipStack != null) {
            this.renderToolTip(this.tooltipStack, mouseX, mouseY);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.guiButtonAssemblersOnly.set(this.onlyMolecularAssemblers ? ActionItems.MOLECULAR_ASSEMBLEERS_ON : ActionItems.MOLECULAR_ASSEMBLEERS_OFF);
        this.guiButtonHideFull.set(AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal ? ActionItems.TOGGLE_SHOW_FULL_INTERFACES_OFF : ActionItems.TOGGLE_SHOW_FULL_INTERFACES_ON);
        this.guiButtonBrokenRecipes.set(this.onlyBrokenRecipes ? ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERN_OFF : ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERN_ON);
        this.guiButtonUseSubstitute.set(this.onlySubstitute ? ActionItems.TOGGLE_SHOW_ONLY_SUBSTITUTE_OFF : ActionItems.TOGGLE_SHOW_ONLY_SUBSTITUTE_ON);
        this.guiButtonSectionOrder.set(AEConfig.instance.settings.getSetting(Settings.INTERFACE_TERMINAL_SECTION_ORDER));
        this.terminalStyleBox.set(AEConfig.instance.settings.getSetting(Settings.TERMINAL_STYLE));
        this.handleTooltip(mouseX, mouseY, this.searchFieldInputs);
        this.handleTooltip(mouseX, mouseY, this.searchFieldOutputs);
        this.handleTooltip(mouseX, mouseY, this.searchFieldNames);
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.searchFieldInputs.mouseClicked(xCoord, yCoord, btn);
        this.searchFieldOutputs.mouseClicked(xCoord, yCoord, btn);
        this.searchFieldNames.mouseClicked(xCoord, yCoord, btn);
        if (this.masterList.mouseClicked(xCoord - this.guiLeft - 10, yCoord - this.guiTop - 52, btn)) {
            return;
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void actionPerformed(GuiButton btn) {
        GuiImgButton iBtn;
        if (btn == this.guiButtonAssemblersOnly) {
            this.onlyMolecularAssemblers = !this.onlyMolecularAssemblers;
            this.masterList.markDirty();
        } else if (btn == this.guiButtonHideFull) {
            AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = !AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal;
            this.masterList.markDirty();
        } else if (btn == this.guiButtonBrokenRecipes) {
            this.onlyBrokenRecipes = !this.onlyBrokenRecipes;
            this.masterList.markDirty();
        } else if (btn == this.guiButtonUseSubstitute) {
            this.onlySubstitute = !this.onlySubstitute;
            this.masterList.markDirty();
        } else if (btn instanceof GuiImgButton && (iBtn = (GuiImgButton)btn).getSetting() != Settings.ACTIONS) {
            Enum cv = iBtn.getCurrentValue();
            boolean backwards = Mouse.isButtonDown((int)1);
            Enum next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
            if (btn == this.terminalStyleBox) {
                AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                this.initGui();
            } else if (btn == this.searchStringSave) {
                AEConfig.instance.preserveSearchBar = next == YesNo.YES;
            } else if (btn == this.guiButtonSectionOrder) {
                AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                this.masterList.changeSectionComparator(((StringOrder)next).comparator);
                this.masterList.markDirty();
            }
            iBtn.set(next);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        searchFieldInputsText = this.searchFieldInputs.getText();
        searchFieldOutputsText = this.searchFieldOutputs.getText();
        searchFieldNamesText = this.searchFieldNames.getText();
    }

    public void setSearchString() {
        boolean setString;
        boolean bl = setString = AEConfig.instance.preserveSearchBar || this.isSubGui();
        if (this.searchFieldInputs.getText().isEmpty() && setString) {
            this.searchFieldInputs.setText(searchFieldInputsText);
        }
        if (this.searchFieldOutputs.getText().isEmpty() && setString) {
            this.searchFieldOutputs.setText(searchFieldOutputsText);
        }
        if (this.searchFieldNames.getText().isEmpty() && setString) {
            this.searchFieldNames.setText(searchFieldNamesText);
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        Tessellator tessellator = Tessellator.instance;
        this.bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 52);
        tessellator.startDrawingQuads();
        this.addTexturedRectToTesselator(offsetX, offsetY + 52, offsetX + this.xSize, offsetY + 52 + this.viewHeight + 1, 0.0f, 0.0f, 0.25390625f, (float)this.xSize / 256.0f, 0.6171875f);
        tessellator.draw();
        this.drawTexturedModalRect(offsetX, offsetY + 52 + this.viewHeight, 0, 158, this.xSize, 98);
        if (this.online) {
            GL11.glPushAttrib((int)1048575);
            GL11.glEnable((int)3042);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(offsetX + 10), (float)(offsetY + 52), (float)0.0f);
            this.tooltipStack = null;
            this.masterList.hoveredEntry = null;
            this.drawViewport(mouseX - offsetX - 10, mouseY - offsetY - 52 - 1);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        this.searchFieldInputs.drawTextBox();
        this.searchFieldOutputs.drawTextBox();
        this.searchFieldNames.drawTextBox();
    }

    private void drawViewport(int relMouseX, int relMouseY) {
        int scroll = this.getScrollBar().getCurrentScroll();
        int viewY = -scroll;
        int entryIdx = 0;
        List<InterfaceSection> visibleSections = this.masterList.getVisibleSections();
        float guiScaleX = (float)this.mc.displayWidth / (float)this.width;
        float guiScaleY = (float)this.mc.displayHeight / (float)this.height;
        GL11.glScissor((int)((int)((float)(this.guiLeft + 10) * guiScaleX)), (int)((int)((float)(this.height - (this.guiTop + 52 + this.viewHeight)) * guiScaleY)), (int)((int)(174.0f * guiScaleX)), (int)((int)((float)this.viewHeight * guiScaleY)));
        GL11.glEnable((int)3089);
        while (viewY < this.viewHeight && entryIdx < visibleSections.size()) {
            InterfaceSection section = visibleSections.get(entryIdx);
            int sectionHeight = section.getHeight();
            if (viewY + sectionHeight < 0) {
                ++entryIdx;
                viewY += sectionHeight;
                section.visible = false;
                continue;
            }
            section.visible = true;
            int advanceY = this.drawSection(section, viewY, relMouseX, relMouseY);
            viewY += advanceY;
            ++entryIdx;
        }
    }

    private int drawSection(InterfaceSection section, int viewY, int relMouseX, int relMouseY) {
        int renderY = 0;
        int sectionBottom = viewY + section.getHeight() - 1;
        int fontColor = GuiColors.InterfaceTerminalInventory.getColor();
        this.bindTexture(BACKGROUND);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)310.0f);
        int title = sectionBottom > 0 && sectionBottom < 12 ? sectionBottom : (viewY < 0 ? 0 : 0);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-310.0f);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        Iterator<InterfaceTerminalEntry> visible = section.getVisible();
        while (visible.hasNext()) {
            InterfaceTerminalEntry entry = visible.next();
            if (viewY + renderY + entry.rows * 18 + 1 > 0 && viewY + renderY < this.viewHeight) {
                renderY += this.drawEntry(entry, viewY + 12 + renderY, title, relMouseX, relMouseY);
                continue;
            }
            entry.dispY = -9999;
            entry.optionsButton.yPosition = -1;
            renderY += entry.rows * 18 + 1;
        }
        this.bindTexture(BACKGROUND);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)310.0f);
        if (sectionBottom > 0 && sectionBottom < 12) {
            this.drawTexturedModalRect(0, 0, 10, 64 - sectionBottom, 174, sectionBottom);
            this.fontRendererObj.drawString(section.name, 2, sectionBottom - 12 + 2, fontColor);
        } else if (viewY < 0) {
            GL11.glDisable((int)2929);
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)100.0f);
            this.drawTexturedModalRect(0, 0, 10, 52, 174, 12);
            this.fontRendererObj.drawString(section.name, 2, 2, fontColor);
            GL11.glEnable((int)2929);
        } else {
            this.drawTexturedModalRect(0, viewY, 10, 52, 174, 12);
            this.fontRendererObj.drawString(section.name, 2, viewY + 2, fontColor);
        }
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-310.0f);
        return 12 + renderY;
    }

    private int drawEntry(InterfaceTerminalEntry entry, int viewY, int titleBottom, int relMouseX, int relMouseY) {
        int rowYBot;
        int rowYTop;
        int row;
        this.bindTexture(BACKGROUND);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        int relY = 0;
        int slotLeftMargin = 174 - entry.rowSize * 18;
        entry.dispY = viewY;
        for (row = 0; row < entry.rows; ++row) {
            rowYTop = row * 18;
            rowYBot = rowYTop + 18;
            relY += 18;
            if (viewY + rowYBot <= titleBottom) continue;
            for (int col = 0; col < entry.rowSize; ++col) {
                this.addTexturedRectToTesselator(col * 18 + slotLeftMargin, viewY + rowYTop, 18 * col + 18 + slotLeftMargin, viewY + rowYBot, 0.0f, 0.08203125f, 0.67578125f, 0.15234375f, 0.74609375f);
            }
        }
        tessellator.draw();
        if (viewY + entry.optionsButton.height > 0 && viewY < this.viewHeight) {
            entry.optionsButton.yPosition = viewY + 5;
            entry.optionsButton.drawButton(this.mc, relMouseX, relMouseY);
            if (entry.optionsButton.getMouseIn() && relMouseY >= Math.max(12, entry.optionsButton.yPosition)) {
                GL11.glTranslatef((float)0.0f, (float)0.0f, (float)410.0f);
                GL11.glDisable((int)3089);
                this.drawHoveringText(this.extraOptionsText, relMouseX, relMouseY);
                GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-410.0f);
                GL11.glEnable((int)3089);
            }
        } else {
            entry.optionsButton.yPosition = -1;
        }
        for (row = 0; row < entry.rows; ++row) {
            rowYTop = row * 18;
            rowYBot = rowYTop + 18;
            if (viewY + rowYBot <= titleBottom) continue;
            AppEngInternalInventory inv = entry.getInventory();
            for (int col = 0; col < entry.rowSize; ++col) {
                boolean tooltip;
                int colLeft = col * 18 + slotLeftMargin + 1;
                int colRight = colLeft + 18 + 1;
                int slotIdx = row * entry.rowSize + col;
                ItemStack stack = inv.getStackInSlot(slotIdx);
                boolean bl = tooltip = relMouseX > colLeft - 1 && relMouseX < colRight - 1 && relMouseY >= Math.max(viewY + rowYTop, 12) && relMouseY < Math.min(viewY + rowYBot, this.viewHeight);
                if (stack != null) {
                    ItemEncodedPattern iep = (ItemEncodedPattern)stack.getItem();
                    ItemStack toRender = iep.getOutput(stack);
                    GL11.glPushMatrix();
                    GL11.glTranslatef((float)colLeft, (float)(viewY + rowYTop + 1), (float)100.0f);
                    GL11.glEnable((int)32826);
                    RenderHelper.enableGUIStandardItemLighting();
                    GuiInterfaceTerminal.translatedRenderItem.zLevel = 50.0f;
                    translatedRenderItem.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), toRender, 0, 0);
                    GL11.glTranslatef((float)0.0f, (float)0.0f, (float)200.0f);
                    aeRenderItem.setAeStack(AEItemStack.create(toRender));
                    aeRenderItem.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), toRender, 0, 0);
                    GuiInterfaceTerminal.aeRenderItem.zLevel = 0.0f;
                    RenderHelper.disableStandardItemLighting();
                    if (!tooltip) {
                        if (entry.slotIsBroken(slotIdx)) {
                            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-199.5f);
                            GuiInterfaceTerminal.drawRect((int)0, (int)0, (int)16, (int)16, (int)GuiColors.ItemSlotOverlayInvalid.getColor());
                        } else if (entry.filteredRecipes[slotIdx]) {
                            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)200.0f);
                            GuiInterfaceTerminal.drawRect((int)0, (int)0, (int)16, (int)16, (int)GuiColors.ItemSlotOverlayUnpowered.getColor());
                        }
                    } else {
                        this.tooltipStack = stack;
                    }
                    GL11.glPopMatrix();
                } else if (entry.filteredRecipes[slotIdx]) {
                    GL11.glPushMatrix();
                    GL11.glTranslatef((float)colLeft, (float)(viewY + rowYTop + 1), (float)200.0f);
                    GuiInterfaceTerminal.drawRect((int)0, (int)0, (int)16, (int)16, (int)GuiColors.ItemSlotOverlayUnpowered.getColor());
                    GL11.glPopMatrix();
                }
                if (tooltip) {
                    GL11.glDisable((int)2896);
                    GL11.glTranslatef((float)0.0f, (float)0.0f, (float)310.0f);
                    GuiInterfaceTerminal.drawRect((int)colLeft, (int)(viewY + 1 + rowYTop), (int)(-2 + colRight), (int)(viewY - 1 + rowYBot), (int)0x77FFFFFF);
                    GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-310.0f);
                    this.masterList.hoveredEntry = entry;
                    entry.hoveredSlotIdx = slotIdx;
                }
                GL11.glDisable((int)2896);
            }
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        return relY + 1;
    }

    @Override
    public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> currentToolTip) {
        return currentToolTip;
    }

    @Override
    public ItemStack getHoveredStack() {
        return this.tooltipStack;
    }

    @Override
    public void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        if (!textLines.isEmpty()) {
            GL11.glDisable((int)32826);
            RenderHelper.disableStandardItemLighting();
            int maxStrWidth = 0;
            for (String s : textLines) {
                int width = font.getStringWidth(s);
                if (width <= maxStrWidth) continue;
                maxStrWidth = width;
            }
            int curX = x + 12;
            int curY = y - 12;
            int totalHeight = 8;
            if (textLines.size() > 1) {
                totalHeight += 2 + (textLines.size() - 1) * 10;
            }
            if (curX + maxStrWidth > this.width) {
                curX -= 28 + maxStrWidth;
            }
            if (curY + totalHeight + 6 > this.height) {
                curY = this.height - totalHeight - 6;
            }
            int borderColor = -267386864;
            this.drawGradientRect(curX - 3, curY - 4, curX + maxStrWidth + 3, curY - 3, borderColor, borderColor);
            this.drawGradientRect(curX - 3, curY + totalHeight + 3, curX + maxStrWidth + 3, curY + totalHeight + 4, borderColor, borderColor);
            this.drawGradientRect(curX - 3, curY - 3, curX + maxStrWidth + 3, curY + totalHeight + 3, borderColor, borderColor);
            this.drawGradientRect(curX - 4, curY - 3, curX - 3, curY + totalHeight + 3, borderColor, borderColor);
            this.drawGradientRect(curX + maxStrWidth + 3, curY - 3, curX + maxStrWidth + 4, curY + totalHeight + 3, borderColor, borderColor);
            int color1 = 0x505000FF;
            int color2 = (color1 & 0xFEFEFE) >> 1 | color1 & 0xFF000000;
            this.drawGradientRect(curX - 3, curY - 3 + 1, curX - 3 + 1, curY + totalHeight + 3 - 1, color1, color2);
            this.drawGradientRect(curX + maxStrWidth + 2, curY - 3 + 1, curX + maxStrWidth + 3, curY + totalHeight + 3 - 1, color1, color2);
            this.drawGradientRect(curX - 3, curY - 3, curX + maxStrWidth + 3, curY - 3 + 1, color1, color1);
            this.drawGradientRect(curX - 3, curY + totalHeight + 2, curX + maxStrWidth + 3, curY + totalHeight + 3, color2, color2);
            for (int i = 0; i < textLines.size(); ++i) {
                String line = textLines.get(i);
                font.drawStringWithShadow(line, curX, curY, -1);
                if (i == 0) {
                    curY += 2;
                }
                curY += 10;
            }
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable((int)32826);
        }
    }

    protected void keyTyped(char character, int key) {
        if (!this.checkHotbarKeys(key)) {
            if (character == ' ' ? this.searchFieldInputs.getText().isEmpty() && this.searchFieldInputs.isFocused() || this.searchFieldOutputs.getText().isEmpty() && this.searchFieldOutputs.isFocused() || this.searchFieldNames.getText().isEmpty() && this.searchFieldNames.isFocused() : character == '\t' && this.handleTab()) {
                return;
            }
            if (this.searchFieldInputs.textboxKeyTyped(character, key) || this.searchFieldOutputs.textboxKeyTyped(character, key) || this.searchFieldNames.textboxKeyTyped(character, key)) {
                return;
            }
            super.keyTyped(character, key);
        }
    }

    @Override
    protected boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        boolean isMouseInViewport = this.isMouseInViewport(mouseX, mouseY);
        GuiScrollbar scrollbar = this.getScrollBar();
        if (isMouseInViewport && GuiInterfaceTerminal.isCtrlKeyDown()) {
            if (wheel < 0) {
                scrollbar.setCurrentScroll(this.masterList.getHeight());
            } else {
                this.getScrollBar().setCurrentScroll(0);
            }
            return true;
        }
        if (isMouseInViewport && GuiInterfaceTerminal.isShiftKeyDown()) {
            return this.masterList.scrollNextSection(wheel > 0);
        }
        return super.mouseWheelEvent(mouseX, mouseY, wheel);
    }

    private boolean isMouseInViewport(int mouseX, int mouseY) {
        return mouseX > this.guiLeft + 10 && mouseX < this.guiLeft + 10 + 174 && mouseY > this.guiTop + 52 && mouseY < this.guiTop + 52 + this.viewHeight;
    }

    private boolean handleTab() {
        if (this.searchFieldInputs.isFocused()) {
            this.searchFieldInputs.setFocused(false);
            if (GuiInterfaceTerminal.isShiftKeyDown()) {
                this.searchFieldNames.setFocused(true);
            } else {
                this.searchFieldOutputs.setFocused(true);
            }
            return true;
        }
        if (this.searchFieldOutputs.isFocused()) {
            this.searchFieldOutputs.setFocused(false);
            if (GuiInterfaceTerminal.isShiftKeyDown()) {
                this.searchFieldInputs.setFocused(true);
            } else {
                this.searchFieldNames.setFocused(true);
            }
            return true;
        }
        if (this.searchFieldNames.isFocused()) {
            this.searchFieldNames.setFocused(false);
            if (GuiInterfaceTerminal.isShiftKeyDown()) {
                this.searchFieldOutputs.setFocused(true);
            } else {
                this.searchFieldInputs.setFocused(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void postUpdate(List<PacketInterfaceTerminalUpdate.PacketEntry> updates, int statusFlags) {
        if ((statusFlags & 1) == 1) {
            this.masterList.list.clear();
        }
        this.online = (statusFlags & 2) != 2;
        for (PacketInterfaceTerminalUpdate.PacketEntry cmd : updates) {
            this.parsePacketCmd(cmd);
        }
        this.masterList.markDirty();
    }

    private void parsePacketCmd(PacketInterfaceTerminalUpdate.PacketEntry cmd) {
        long id = cmd.entryId;
        if (cmd instanceof PacketInterfaceTerminalUpdate.PacketAdd) {
            PacketInterfaceTerminalUpdate.PacketAdd addCmd = (PacketInterfaceTerminalUpdate.PacketAdd)cmd;
            InterfaceTerminalEntry entry = new InterfaceTerminalEntry(id, addCmd.name, addCmd.rows, addCmd.rowSize, addCmd.online, addCmd.p2pOutput).setLocation(addCmd.x, addCmd.y, addCmd.z, addCmd.dim).setIcons(addCmd.selfRep, addCmd.dispRep).setItems(addCmd.items);
            this.masterList.addEntry(entry);
        } else if (cmd instanceof PacketInterfaceTerminalUpdate.PacketRemove) {
            this.masterList.removeEntry(id);
        } else if (cmd instanceof PacketInterfaceTerminalUpdate.PacketOverwrite) {
            PacketInterfaceTerminalUpdate.PacketOverwrite owCmd = (PacketInterfaceTerminalUpdate.PacketOverwrite)cmd;
            InterfaceTerminalEntry entry = (InterfaceTerminalEntry)this.masterList.list.get(id);
            if (entry == null) {
                return;
            }
            if (owCmd.onlineValid) {
                entry.online = owCmd.online;
            }
            if (owCmd.itemsValid) {
                if (owCmd.allItemUpdate) {
                    entry.fullItemUpdate(owCmd.items, owCmd.items.tagCount());
                } else {
                    entry.partialItemUpdate(owCmd.items, owCmd.validIndices);
                }
            }
            this.masterList.isDirty = true;
        } else if (cmd instanceof PacketInterfaceTerminalUpdate.PacketRename) {
            PacketInterfaceTerminalUpdate.PacketRename renameCmd = (PacketInterfaceTerminalUpdate.PacketRename)cmd;
            InterfaceTerminalEntry entry = (InterfaceTerminalEntry)this.masterList.list.get(id);
            if (entry != null) {
                entry.dispName = StatCollector.canTranslate((String)renameCmd.newName) ? StatCollector.translateToLocal((String)renameCmd.newName) : StatCollector.translateToFallback((String)renameCmd.newName);
            }
            this.masterList.isDirty = true;
        }
    }

    private static boolean itemStackMatchesSearchTerm(ItemStack itemStack, String searchTerm, boolean in) {
        if (itemStack == null) {
            return false;
        }
        NBTTagCompound encodedValue = itemStack.getTagCompound();
        if (encodedValue == null) {
            return false;
        }
        NBTTagList tags = encodedValue.getTagList(in ? "in" : "out", 10);
        boolean containsInvalidDisplayName = GuiText.UnknownItem.getLocal().toLowerCase().contains(searchTerm);
        Predicate<ItemStack> itemFilter = NEI.searchField.existsSearchField() ? NEI.searchField.getFilter(searchTerm) : is -> Platform.getItemDisplayName(AEApi.instance().storage().createItemStack((ItemStack)is)).toLowerCase().contains(searchTerm);
        for (int i = 0; i < tags.tagCount(); ++i) {
            NBTTagCompound tag = tags.getCompoundTagAt(i);
            ItemStack parsedItemStack = ItemStack.loadItemStackFromNBT((NBTTagCompound)tag);
            if (!(parsedItemStack != null ? itemFilter.test(parsedItemStack) : containsInvalidDisplayName && !tag.hasNoTags())) continue;
            return true;
        }
        return false;
    }

    private static boolean interfaceSectionMatchesSearchTerm(InterfaceSection section, String searchTerm) {
        if (searchTerm.isEmpty()) {
            return true;
        }
        String sectionName = section.name.toLowerCase();
        if (searchTerm.length() >= 2 && searchTerm.startsWith("\"") && searchTerm.endsWith("\"")) {
            return sectionName.contains(searchTerm.substring(1, searchTerm.length() - 1).toLowerCase());
        }
        String[] terms = searchTerm.toLowerCase().split(" +");
        for (int i = 0; i < terms.length; ++i) {
            if (sectionName.contains(terms[i])) continue;
            return false;
        }
        return true;
    }

    private boolean recipeIsBroken(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        NBTTagCompound encodedValue = itemStack.getTagCompound();
        if (encodedValue == null) {
            return true;
        }
        World w = CommonHelper.proxy.getWorld();
        if (w == null) {
            return false;
        }
        try {
            new PatternHelper(itemStack, w);
            return false;
        }
        catch (Throwable t) {
            return true;
        }
    }

    private boolean isUseSubstitute(ItemStack is) {
        if (is == null) {
            return false;
        }
        NBTTagCompound encodedValue = is.getTagCompound();
        if (encodedValue == null) {
            return false;
        }
        return encodedValue.getBoolean("substitute") || encodedValue.getBoolean("beSubstitute");
    }

    private int getMaxViewHeight() {
        return AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.SMALL ? AEConfig.instance.InterfaceTerminalSmallSize * 18 : Integer.MAX_VALUE;
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        return this.searchFieldInputs.isMouseIn(mousex, mousey) || this.searchFieldOutputs.isMouseIn(mousex, mousey) || this.searchFieldNames.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        if (this.searchFieldInputs.isMouseIn(mousex, mousey)) {
            this.searchFieldInputs.setText(NEI.searchField.getEscapedSearchText(displayName));
        } else if (this.searchFieldOutputs.isMouseIn(mousex, mousey)) {
            this.searchFieldOutputs.setText(NEI.searchField.getEscapedSearchText(displayName));
        } else if (this.searchFieldNames.isMouseIn(mousex, mousey)) {
            this.searchFieldNames.setText(displayName);
        }
    }

    private class InterfaceSection {
        public static final int TITLE_HEIGHT = 12;
        String name;
        List<InterfaceTerminalEntry> entries = new ArrayList<InterfaceTerminalEntry>();
        Set<InterfaceTerminalEntry> visibleEntries = new TreeSet<InterfaceTerminalEntry>(Comparator.comparing(e -> {
            if (e.dispRep != null) {
                return e.dispRep.getDisplayName() + e.id;
            }
            return String.valueOf(e.id);
        }));
        int height;
        private boolean isDirty = true;
        boolean visible = false;

        InterfaceSection(String name) {
            this.name = name;
        }

        public int getHeight() {
            if (this.isDirty) {
                this.update();
            }
            return this.height;
        }

        private void update() {
            this.refreshVisible();
            if (this.visibleEntries.isEmpty()) {
                this.height = 0;
            } else {
                this.height = 12;
                for (InterfaceTerminalEntry entry : this.visibleEntries) {
                    this.height += entry.guiHeight;
                }
            }
            this.isDirty = false;
        }

        public void refreshVisible() {
            this.visibleEntries.clear();
            String input = GuiInterfaceTerminal.this.searchFieldInputs.getText().toLowerCase();
            String output = GuiInterfaceTerminal.this.searchFieldOutputs.getText().toLowerCase();
            for (InterfaceTerminalEntry entry : this.entries) {
                if (!entry.online || entry.p2pOutput) continue;
                Optional<ItemStack> moleAss = AEApi.instance().definitions().blocks().molecularAssembler().maybeStack(1);
                entry.dispY = -9999;
                if (GuiInterfaceTerminal.this.onlyMolecularAssemblers && (!moleAss.isPresent() || !Platform.isSameItem((ItemStack)moleAss.get(), entry.dispRep)) || AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal && entry.numItems == entry.rows * entry.rowSize || GuiInterfaceTerminal.this.onlyBrokenRecipes && !entry.hasBrokenSlot() || GuiInterfaceTerminal.this.onlySubstitute && !entry.hasUseSubstitute()) continue;
                if (!input.isEmpty() || !output.isEmpty()) {
                    AppEngInternalInventory inv = entry.inv;
                    boolean shouldAdd = false;
                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (GuiInterfaceTerminal.itemStackMatchesSearchTerm(stack, input, true) && GuiInterfaceTerminal.itemStackMatchesSearchTerm(stack, output, false)) {
                            shouldAdd = true;
                            entry.filteredRecipes[i] = false;
                            continue;
                        }
                        entry.filteredRecipes[i] = true;
                    }
                    if (!shouldAdd) {
                        continue;
                    }
                } else {
                    Arrays.fill(entry.filteredRecipes, false);
                }
                this.visibleEntries.add(entry);
            }
        }

        public void addEntry(InterfaceTerminalEntry entry) {
            this.entries.add(entry);
            entry.section = this;
            this.isDirty = true;
        }

        public void removeEntry(InterfaceTerminalEntry entry) {
            this.entries.remove(entry);
            entry.section = null;
            this.isDirty = true;
        }

        public Iterator<InterfaceTerminalEntry> getVisible() {
            if (this.isDirty) {
                this.update();
            }
            return this.visibleEntries.iterator();
        }

        public boolean mouseClicked(int relMouseX, int relMouseY, int btn) {
            Iterator<InterfaceTerminalEntry> it = this.getVisible();
            boolean ret = false;
            while (it.hasNext() && !ret) {
                ret = it.next().mouseClicked(relMouseX, relMouseY, btn);
            }
            return ret;
        }
    }

    private class InterfaceTerminalList {
        private final Map<Long, InterfaceTerminalEntry> list = new HashMap<Long, InterfaceTerminalEntry>();
        private Map<String, InterfaceSection> sections;
        private final List<InterfaceSection> visibleSections = new ArrayList<InterfaceSection>();
        private boolean isDirty;
        private int height;
        private InterfaceTerminalEntry hoveredEntry;

        InterfaceTerminalList(Comparator<String> comparator) {
            this.sections = comparator == null ? new TreeMap<String, InterfaceSection>() : new TreeMap(comparator);
            this.isDirty = true;
        }

        void changeSectionComparator(Comparator<String> comparator) {
            TreeMap t;
            Map<String, InterfaceSection> map = this.sections;
            if (map instanceof TreeMap && !Objects.equals(comparator, (t = (TreeMap)map).comparator())) {
                TreeMap<String, InterfaceSection> map2 = comparator == null ? new TreeMap<String, InterfaceSection>() : new TreeMap(comparator);
                map2.putAll(this.sections);
                this.sections = map2;
            }
        }

        private void update() {
            this.height = 0;
            this.visibleSections.clear();
            for (InterfaceSection section : this.sections.values()) {
                String query;
                if (!GuiInterfaceTerminal.interfaceSectionMatchesSearchTerm(section, query = GuiInterfaceTerminal.this.searchFieldNames.getText())) continue;
                section.isDirty = true;
                if (!section.getVisible().hasNext()) continue;
                this.height += section.getHeight();
                this.visibleSections.add(section);
            }
            this.isDirty = false;
        }

        public void markDirty() {
            this.isDirty = true;
            GuiInterfaceTerminal.this.setScrollBar();
        }

        public int getHeight() {
            if (this.isDirty) {
                this.update();
            }
            return this.height;
        }

        private boolean scrollNextSection(boolean up) {
            GuiScrollbar scrollbar = GuiInterfaceTerminal.this.getScrollBar();
            int viewY = scrollbar.getCurrentScroll();
            List<InterfaceSection> sections = this.getVisibleSections();
            boolean result = false;
            if (up) {
                int y = GuiInterfaceTerminal.this.masterList.getHeight();
                int i = sections.size() - 1;
                while (y > 0 && i >= 0) {
                    if ((y -= sections.get(--i).getHeight()) >= viewY) continue;
                    result = true;
                    scrollbar.setCurrentScroll(y);
                    break;
                }
            } else {
                int y = 0;
                for (InterfaceSection section : sections) {
                    if (y > viewY) {
                        result = true;
                        scrollbar.setCurrentScroll(y);
                        break;
                    }
                    y += section.getHeight();
                }
            }
            return result;
        }

        public void addEntry(InterfaceTerminalEntry entry) {
            InterfaceSection section = this.sections.get(entry.dispName);
            if (section == null) {
                section = new InterfaceSection(entry.dispName);
                this.sections.put(entry.dispName, section);
            }
            section.addEntry(entry);
            this.list.put(entry.id, entry);
            this.isDirty = true;
        }

        public void removeEntry(long id) {
            InterfaceTerminalEntry entry = this.list.remove(id);
            if (entry != null) {
                entry.section.removeEntry(entry);
            }
        }

        public List<InterfaceSection> getVisibleSections() {
            if (this.isDirty) {
                this.update();
            }
            return this.visibleSections;
        }

        public boolean mouseClicked(int relMouseX, int relMouseY, int btn) {
            if (relMouseX < 0 || relMouseX >= 174 || relMouseY < 0 || relMouseY >= GuiInterfaceTerminal.this.viewHeight) {
                return false;
            }
            for (InterfaceSection section : this.getVisibleSections()) {
                if (!section.mouseClicked(relMouseX, relMouseY, btn)) continue;
                return true;
            }
            return false;
        }
    }

    private class InterfaceTerminalEntry {
        String dispName;
        AppEngInternalInventory inv;
        GuiImgButton optionsButton;
        ItemStack selfRep;
        ItemStack dispRep;
        InterfaceSection section;
        long id;
        int x;
        int y;
        int z;
        int dim;
        int rows;
        int rowSize;
        int guiHeight;
        int dispY = -9999;
        boolean online;
        boolean p2pOutput;
        private Boolean[] brokenRecipes;
        int numItems = 0;
        boolean[] filteredRecipes;
        Boolean[] useSubstitute;
        private int hoveredSlotIdx = -1;

        InterfaceTerminalEntry(long id, String name, int rows, int rowSize, boolean online, boolean p2pOutput) {
            this.id = id;
            this.dispName = CraftingCPUCluster.translateFromNetwork(name);
            this.inv = new AppEngInternalInventory(null, rows * rowSize, 1);
            this.rows = rows;
            this.rowSize = rowSize;
            this.online = online;
            this.p2pOutput = p2pOutput;
            this.optionsButton = new GuiImgButton(2, 0, Settings.ACTIONS, ActionItems.HIGHLIGHT_INTERFACE);
            this.optionsButton.setHalfSize(true);
            this.guiHeight = 18 * rows + 1;
            this.brokenRecipes = new Boolean[rows * rowSize];
            this.useSubstitute = new Boolean[rows * rowSize];
            this.filteredRecipes = new boolean[rows * rowSize];
        }

        InterfaceTerminalEntry setLocation(int x, int y, int z, int dim) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            return this;
        }

        InterfaceTerminalEntry setIcons(ItemStack selfRep, ItemStack dispRep) {
            this.selfRep = selfRep;
            this.dispRep = dispRep;
            return this;
        }

        public void fullItemUpdate(NBTTagList items, int newSize) {
            this.inv = new AppEngInternalInventory(null, newSize);
            this.rows = newSize / this.rowSize;
            this.brokenRecipes = new Boolean[newSize];
            this.numItems = 0;
            for (int i = 0; i < this.inv.getSizeInventory(); ++i) {
                this.setItemInSlot(ItemStack.loadItemStackFromNBT((NBTTagCompound)items.getCompoundTagAt(i)), i);
            }
            this.guiHeight = 18 * this.rows + 4;
        }

        InterfaceTerminalEntry setItems(NBTTagList items) {
            assert (items.tagCount() == this.inv.getSizeInventory());
            for (int i = 0; i < items.tagCount(); ++i) {
                this.setItemInSlot(ItemStack.loadItemStackFromNBT((NBTTagCompound)items.getCompoundTagAt(i)), i);
            }
            return this;
        }

        public void partialItemUpdate(NBTTagList items, int[] validIndices) {
            for (int i = 0; i < validIndices.length; ++i) {
                this.setItemInSlot(ItemStack.loadItemStackFromNBT((NBTTagCompound)items.getCompoundTagAt(i)), validIndices[i]);
            }
        }

        private void setItemInSlot(ItemStack stack, int idx) {
            int oldHasItem = this.inv.getStackInSlot(idx) != null ? 1 : 0;
            int newHasItem = stack != null ? 1 : 0;
            this.inv.setInventorySlotContents(idx, stack);
            this.numItems += newHasItem - oldHasItem;
            assert (this.numItems >= 0);
        }

        public boolean hasBrokenSlot() {
            int idx;
            boolean existsUnknown = false;
            for (idx = 0; idx < this.brokenRecipes.length; ++idx) {
                if (this.brokenRecipes[idx] == null) {
                    existsUnknown = true;
                    continue;
                }
                if (!this.brokenRecipes[idx].booleanValue()) continue;
                return true;
            }
            if (existsUnknown) {
                for (idx = 0; idx < this.brokenRecipes.length; ++idx) {
                    if (!this.slotIsBroken(idx)) continue;
                    return true;
                }
            }
            return false;
        }

        public boolean hasUseSubstitute() {
            boolean existsUnknown = false;
            for (Boolean aBoolean : this.useSubstitute) {
                if (aBoolean == null) {
                    existsUnknown = true;
                    continue;
                }
                if (!aBoolean.booleanValue()) continue;
                return true;
            }
            if (existsUnknown) {
                for (int idx = 0; idx < this.useSubstitute.length; ++idx) {
                    if (!this.slotIsUseSubstitute(idx)) continue;
                    return true;
                }
            }
            return false;
        }

        public boolean slotIsBroken(int idx) {
            if (this.brokenRecipes[idx] == null) {
                this.brokenRecipes[idx] = GuiInterfaceTerminal.this.recipeIsBroken(this.inv.getStackInSlot(idx));
            }
            return this.brokenRecipes[idx];
        }

        public boolean slotIsUseSubstitute(int idx) {
            if (this.useSubstitute[idx] == null) {
                this.useSubstitute[idx] = GuiInterfaceTerminal.this.isUseSubstitute(this.inv.getStackInSlot(idx));
            }
            return this.useSubstitute[idx];
        }

        public AppEngInternalInventory getInventory() {
            return this.inv;
        }

        public boolean mouseClicked(int mouseX, int mouseY, int btn) {
            if (!this.section.visible || btn < 0 || btn > 2) {
                return false;
            }
            if (mouseX >= this.optionsButton.xPosition && mouseX < 2 + this.optionsButton.width && mouseY > Math.max(this.optionsButton.yPosition, 12) && mouseY <= Math.min(this.optionsButton.yPosition + this.optionsButton.height, GuiInterfaceTerminal.this.viewHeight)) {
                this.optionsButton.func_146113_a(GuiInterfaceTerminal.this.mc.getSoundHandler());
                BlockPosHighlighter.highlightBlocks((EntityPlayer)GuiInterfaceTerminal.this.mc.thePlayer, Collections.singletonList(new DimensionalCoord(this.x, this.y, this.z, this.dim)), PlayerMessages.InterfaceHighlighted.getUnlocalized(), PlayerMessages.InterfaceInOtherDim.getUnlocalized());
                GuiInterfaceTerminal.this.mc.thePlayer.closeScreen();
                return true;
            }
            int offsetY = mouseY - this.dispY - 1;
            int offsetX = mouseX - (174 - this.rowSize * 18) - 1;
            if (offsetX >= 0 && offsetX < this.rowSize * 18 && mouseY > Math.max(this.dispY, 12) && offsetY < Math.min(GuiInterfaceTerminal.this.viewHeight - this.dispY, this.guiHeight - 1)) {
                int col = offsetX / 18;
                int row = offsetY / 18;
                int slotIdx = row * this.rowSize + col;
                PacketInventoryAction packet = Keyboard.isKeyDown((int)57) ? new PacketInventoryAction(InventoryAction.MOVE_REGION, 0, this.id) : (GuiScreen.isShiftKeyDown() && (btn == 0 || btn == 1) ? new PacketInventoryAction(InventoryAction.SHIFT_CLICK, slotIdx, this.id) : (btn == 0 || btn == 1 ? new PacketInventoryAction(InventoryAction.PICKUP_OR_SET_DOWN, slotIdx, this.id) : new PacketInventoryAction(InventoryAction.CREATIVE_DUPLICATE, slotIdx, this.id)));
                NetworkHandler.instance.sendToServer(packet);
                return true;
            }
            return false;
        }
    }
}

