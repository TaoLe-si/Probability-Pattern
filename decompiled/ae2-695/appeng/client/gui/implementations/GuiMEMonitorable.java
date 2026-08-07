/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.eventhandler.Event
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.client.event.GuiScreenEvent$InitGuiEvent$Post
 *  net.minecraftforge.client.event.GuiScreenEvent$InitGuiEvent$Pre
 *  net.minecraftforge.common.MinecraftForge
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.CraftingStatus;
import appeng.api.config.PinsState;
import appeng.api.config.SearchBoxFocusPriority;
import appeng.api.config.SearchBoxMode;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.config.YesNo;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.implementations.tiles.IMEChest;
import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.ITerminalPins;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.client.ActionKey;
import appeng.client.gui.AEBaseMEGui;
import appeng.client.gui.implementations.GuiMEPortableCell;
import appeng.client.gui.implementations.GuiSecurity;
import appeng.client.gui.implementations.GuiWirelessTerm;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.me.InternalSlotME;
import appeng.client.me.ItemRepo;
import appeng.client.me.PinSlotME;
import appeng.client.me.SlotME;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.CommonHelper;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketPinsUpdate;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IPinsHandler;
import appeng.helpers.InventoryAction;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.modules.NEI;
import appeng.items.storage.ItemViewCell;
import appeng.parts.reporting.AbstractPartTerminal;
import appeng.tile.misc.TileSecurity;
import appeng.util.IConfigManagerHost;
import appeng.util.Platform;
import cpw.mods.fml.common.eventhandler.Event;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiMEMonitorable
extends AEBaseMEGui
implements ISortSource,
IConfigManagerHost,
IDropToFillTextField,
IPinsHandler {
    public static int craftingGridOffsetX;
    public static int craftingGridOffsetY;
    private static String memoryText;
    private final IDisplayRepo repo;
    private final int offsetX = 9;
    private final int MAGIC_HEIGHT_NUMBER = 115;
    private final int lowerTextureOffset = 0;
    private final IConfigManager configSrc;
    private final boolean viewCell;
    private final ItemStack[] myCurrentViewCells = new ItemStack[5];
    private final ContainerMEMonitorable monitorableContainer;
    private GuiTabButton craftingStatusBtn;
    private GuiImgButton craftingStatusImgBtn;
    private final MEGuiTextField searchField;
    private GuiText myName;
    private int perRow = 9;
    private int reservedSpace = 0;
    private boolean customSortOrder = true;
    private int rows = 0;
    private int standardSize;
    private GuiImgButton ViewBox;
    private GuiImgButton SortByBox;
    private GuiImgButton SortDirBox;
    private GuiImgButton searchBoxSettings;
    private GuiImgButton terminalStyleBox;
    private GuiImgButton searchStringSave;
    private GuiImgButton typeFilter;
    private GuiImgButton pinsStateButton;
    private boolean canBeAutoFocused = false;
    private boolean isAutoFocused = false;
    private int currentMouseX = 0;
    private int currentMouseY = 0;
    private PinsState pinsState;
    public final boolean hasPinHost;

    public GuiMEMonitorable(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        this(inventoryPlayer, te, new ContainerMEMonitorable(inventoryPlayer, te));
    }

    public GuiMEMonitorable(InventoryPlayer inventoryPlayer, ITerminalHost te, ContainerMEMonitorable c) {
        super(c);
        GuiScrollbar scrollbar = new GuiScrollbar();
        this.setScrollBar(scrollbar);
        this.repo = new ItemRepo(scrollbar, this);
        this.xSize = 195;
        this.ySize = 204;
        this.standardSize = this.xSize;
        this.configSrc = ((IConfigurableObject)this.inventorySlots).getConfigManager();
        this.pinsState = (PinsState)this.configSrc.getSetting(Settings.PINS_STATE);
        this.monitorableContainer = (ContainerMEMonitorable)this.inventorySlots;
        this.monitorableContainer.setGui(this);
        this.viewCell = te instanceof IViewCellStorage;
        if (te instanceof TileSecurity) {
            this.myName = GuiText.Security;
        } else if (te instanceof WirelessTerminalGuiObject) {
            this.myName = GuiText.WirelessTerminal;
        } else if (te instanceof IPortableCell) {
            this.myName = GuiText.PortableCell;
        } else if (te instanceof IMEChest) {
            this.myName = GuiText.Chest;
        } else if (te instanceof AbstractPartTerminal) {
            this.myName = GuiText.Terminal;
        }
        this.hasPinHost = te instanceof ITerminalPins;
        this.searchField = new MEGuiTextField(90, 12, ButtonToolTips.SearchStringTooltip.getLocal()){

            @Override
            public void onTextChange(String oldText) {
                String text = this.getText();
                GuiMEMonitorable.this.repo.setSearchString(text);
                GuiMEMonitorable.this.repo.updateView();
                GuiMEMonitorable.this.setScrollBar();
            }
        };
        NEI.searchField.putFormatter(this.searchField);
    }

    public void postUpdate(List<IAEItemStack> list) {
        for (IAEItemStack is : list) {
            this.repo.postUpdate(is);
        }
        this.repo.updateView();
        this.setScrollBar();
    }

    private void setScrollBar() {
        this.getScrollBar().setTop(18).setLeft(175).setHeight(this.rows * 18 - 2);
        this.getScrollBar().setRange(0, (this.repo.size() + this.pinsState.ordinal() * 9 + this.perRow - 1) / this.perRow - this.rows, Math.max(1, this.rows / 6));
    }

    protected void actionPerformed(GuiButton btn) {
        GuiImgButton iBtn;
        if (btn == this.craftingStatusBtn || btn == this.craftingStatusImgBtn) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_CRAFTING_STATUS));
        }
        if (!(btn instanceof GuiImgButton) || (iBtn = (GuiImgButton)btn).getSetting() == Settings.ACTIONS) {
            return;
        }
        Enum cv = iBtn.getCurrentValue();
        boolean backwards = Mouse.isButtonDown((int)1);
        Enum next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
        if (btn == this.terminalStyleBox) {
            AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
        } else if (btn == this.searchBoxSettings) {
            AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
        } else if (btn == this.searchStringSave) {
            AEConfig.instance.preserveSearchBar = next == YesNo.YES;
        } else if (btn == this.pinsStateButton) {
            try {
                if (next.ordinal() >= this.rows) {
                    return;
                }
                PacketPinsUpdate p = new PacketPinsUpdate((PinsState)next);
                NetworkHandler.instance.sendToServer(p);
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        } else {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig(iBtn.getSetting().name(), next.name()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
        iBtn.set(next);
        if (next.getClass() == SearchBoxMode.class || next.getClass() == TerminalStyle.class) {
            this.reinitalize();
        }
    }

    private void adjustPinsSize() {
        int pinMaxSize = this.rows - 1;
        if (this.pinsState.ordinal() <= pinMaxSize) {
            return;
        }
        try {
            PinsState newState = PinsState.fromOrdinal(pinMaxSize);
            PacketPinsUpdate p = new PacketPinsUpdate(newState);
            NetworkHandler.instance.sendToServer(p);
        }
        catch (IOException e) {
            AELog.debug(e);
        }
    }

    private void reinitalize() {
        memoryText = this.searchField.getText();
        if (!MinecraftForge.EVENT_BUS.post((Event)new GuiScreenEvent.InitGuiEvent.Pre((GuiScreen)this, this.buttonList))) {
            this.buttonList.clear();
            this.initGui();
        }
        MinecraftForge.EVENT_BUS.post((Event)new GuiScreenEvent.InitGuiEvent.Post((GuiScreen)this, this.buttonList));
    }

    @Override
    public void initGui() {
        Enum<?> searchMode;
        int x;
        int y;
        Keyboard.enableRepeatEvents((boolean)true);
        this.perRow = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.FULL ? 9 : 9 + (this.width - this.standardSize) / 18;
        this.rows = this.calculateRowsCount();
        this.getMeSlots().clear();
        this.adjustPinsSize();
        int pinsRows = this.pinsState.ordinal();
        for (y = 0; y < pinsRows; ++y) {
            for (x = 0; x < this.perRow; ++x) {
                this.getMeSlots().add(new PinSlotME(this.repo, x + y * this.perRow, this.offsetX + x * 18, y * 18 + 18));
            }
        }
        for (y = 0; y < this.rows - pinsRows; ++y) {
            for (x = 0; x < this.perRow; ++x) {
                this.getMeSlots().add(new InternalSlotME(this.repo, x + y * this.perRow, this.offsetX + x * 18, 18 + y * 18 + pinsRows * 18));
            }
        }
        this.xSize = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.FULL ? this.standardSize + (this.perRow - 9) * 18 : this.standardSize;
        super.initGui();
        this.ySize = 115 + this.rows * 18 + this.reservedSpace;
        int unusedSpace = this.height - this.ySize;
        this.guiTop = (int)Math.floor((float)unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));
        int offset = this.guiTop + 8;
        this.buttonList.clear();
        if (this.customSortOrder) {
            this.SortByBox = new GuiImgButton(this.guiLeft - 18, offset, Settings.SORT_BY, this.configSrc.getSetting(Settings.SORT_BY));
            this.buttonList.add(this.SortByBox);
            offset += 20;
        }
        if (this.viewCell || this instanceof GuiWirelessTerm) {
            this.ViewBox = new GuiImgButton(this.guiLeft - 18, offset, Settings.VIEW_MODE, this.configSrc.getSetting(Settings.VIEW_MODE));
            this.buttonList.add(this.ViewBox);
            offset += 20;
        }
        if (!AEApi.instance().registries().itemDisplay().getItemFilters().isEmpty()) {
            this.typeFilter = new GuiImgButton(this.guiLeft - 18, offset, Settings.TYPE_FILTER, this.configSrc.getSetting(Settings.TYPE_FILTER));
            this.buttonList.add(this.typeFilter);
            offset += 20;
        }
        this.SortDirBox = new GuiImgButton(this.guiLeft - 18, offset, Settings.SORT_DIRECTION, this.configSrc.getSetting(Settings.SORT_DIRECTION));
        this.buttonList.add(this.SortDirBox);
        this.searchBoxSettings = new GuiImgButton(this.guiLeft - 18, offset += 20, Settings.SEARCH_MODE, AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE));
        this.buttonList.add(this.searchBoxSettings);
        this.searchStringSave = new GuiImgButton(this.guiLeft - 18, offset += 20, Settings.SAVE_SEARCH, AEConfig.instance.preserveSearchBar ? YesNo.YES : YesNo.NO);
        this.buttonList.add(this.searchStringSave);
        offset += 20;
        if (!(this instanceof GuiMEPortableCell) || this instanceof GuiWirelessTerm) {
            this.terminalStyleBox = new GuiImgButton(this.guiLeft - 18, offset, Settings.TERMINAL_STYLE, AEConfig.instance.settings.getSetting(Settings.TERMINAL_STYLE));
            this.buttonList.add(this.terminalStyleBox);
            offset += 20;
        }
        if (this.viewCell || this instanceof GuiWirelessTerm) {
            if (AEConfig.instance.getConfigManager().getSetting(Settings.CRAFTING_STATUS).equals((Object)CraftingStatus.BUTTON)) {
                this.craftingStatusImgBtn = new GuiImgButton(this.guiLeft - 18, offset, Settings.CRAFTING_STATUS, AEConfig.instance.settings.getSetting(Settings.CRAFTING_STATUS));
                this.buttonList.add(this.craftingStatusImgBtn);
            } else {
                this.craftingStatusBtn = new GuiTabButton(this.guiLeft + 170, this.guiTop - 4, 178, GuiText.CraftingStatus.getLocal(), itemRender);
                this.buttonList.add(this.craftingStatusBtn);
                this.craftingStatusBtn.setHideEdge(13);
            }
        }
        if (this.hasPinHost) {
            this.pinsStateButton = new GuiImgButton(this.guiLeft + 178, this.guiTop + 18 + this.rows * 18 + 25, Settings.PINS_STATE, this.configSrc.getSetting(Settings.PINS_STATE));
            this.buttonList.add(this.pinsStateButton);
        }
        this.canBeAutoFocused = SearchBoxMode.AUTOSEARCH == (searchMode = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE)) || SearchBoxMode.NEI_AUTOSEARCH == searchMode;
        this.searchField.x = this.guiLeft + Math.max(80, this.offsetX);
        this.searchField.y = this.guiTop + 4;
        this.searchField.setFocused(this.canBeAutoFocused);
        this.isAutoFocused = this.canBeAutoFocused;
        if (this.isSubGui()) {
            this.searchField.setText(memoryText);
        } else if (AEConfig.instance.preserveSearchBar) {
            this.searchField.setText(memoryText, true);
            this.repo.setSearchString(memoryText);
        }
        this.searchField.setCursorPositionEnd();
        this.setScrollBar();
        craftingGridOffsetX = Integer.MAX_VALUE;
        craftingGridOffsetY = Integer.MAX_VALUE;
        for (Object s : this.inventorySlots.inventorySlots) {
            if (s instanceof AppEngSlot && ((Slot)s).xDisplayPosition < 197) {
                this.repositionSlot((AppEngSlot)((Object)s));
            }
            if (!(s instanceof SlotCraftingMatrix) && !(s instanceof SlotFakeCraftingMatrix)) continue;
            Slot g = (Slot)s;
            if (g.xDisplayPosition <= 0 || g.yDisplayPosition <= 0) continue;
            craftingGridOffsetX = Math.min(craftingGridOffsetX, g.xDisplayPosition);
            craftingGridOffsetY = Math.min(craftingGridOffsetY, g.yDisplayPosition);
        }
        craftingGridOffsetX -= 25;
        craftingGridOffsetY -= 6;
    }

    protected int calculateRowsCount() {
        boolean hasNEI = IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI);
        int NEIPadding = hasNEI ? 42 : 0;
        int extraSpace = this.height - 115 - NEIPadding - this.reservedSpace;
        return Math.max(3, Math.min(this.getMaxRows(), (int)Math.floor(extraSpace / 18)));
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(this.myName.getLocal()), 8, 6, GuiColors.MEMonitorableTitle.getColor());
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, GuiColors.MEMonitorableInventory.getColor());
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.searchField.mouseClicked(xCoord, yCoord, btn);
        this.isAutoFocused = false;
        if (this.handleViewCellClick(xCoord, yCoord, btn)) {
            return;
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    private boolean handleViewCellClick(int xCoord, int yCoord, int btn) {
        Slot slot;
        if (this.viewCell && this.monitorableContainer.canAccessViewCells && btn == 1 && (slot = this.getSlot(xCoord, yCoord)) instanceof SlotRestrictedInput) {
            SlotRestrictedInput cvs = (SlotRestrictedInput)slot;
            if (!cvs.getHasStack()) {
                return false;
            }
            if (!(cvs.getStack().getItem() instanceof ItemViewCell)) {
                return false;
            }
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("Terminal.UpdateViewCell", Integer.toString(cvs.getSlotIndex())));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents((boolean)false);
        memoryText = this.searchField.getText();
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture(this.getBackground());
        int x_width = 195;
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, 195, 18);
        if (this.viewCell || this instanceof GuiSecurity) {
            this.drawTexturedModalRect(offsetX + 195, offsetY, 195, 0, 46, 128);
        }
        for (int x = 0; x < this.rows; ++x) {
            this.drawTexturedModalRect(offsetX, offsetY + 18 + x * 18, 0, 18, 195, 18);
        }
        this.drawTexturedModalRect(offsetX, offsetY + 16 + this.rows * 18 + this.lowerTextureOffset, 0, 70, 195, 99 + this.reservedSpace - this.lowerTextureOffset);
        if (this.viewCell) {
            boolean update = false;
            for (int i = 0; i < 5; ++i) {
                if (this.myCurrentViewCells[i] == this.monitorableContainer.getCellViewSlot(i).getStack()) continue;
                update = true;
                this.myCurrentViewCells[i] = this.monitorableContainer.getCellViewSlot(i).getStack();
            }
            if (update) {
                this.repo.setViewCell(this.myCurrentViewCells);
            }
        }
        this.searchField.drawTextBox();
    }

    protected String getBackground() {
        return "guis/terminal.png";
    }

    @Override
    protected boolean isPowered() {
        return this.repo.hasPower();
    }

    int getMaxRows() {
        return AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.SMALL ? AEConfig.instance.MEMonitorableSmallSize : Integer.MAX_VALUE;
    }

    protected void repositionSlot(AppEngSlot s) {
        s.yDisplayPosition = s.getY() + this.ySize - 78 - 5;
    }

    protected void keyTyped(char character, int key) {
        boolean skipHotbarCheck;
        if (NEI.searchField.existsSearchField()) {
            Slot slot;
            boolean mouseInGui;
            if ((NEI.searchField.focused() || this.searchField.isFocused()) && CommonHelper.proxy.isActionKey(ActionKey.TOGGLE_FOCUS, key)) {
                boolean focused = this.searchField.isFocused();
                this.searchField.setFocused(!focused);
                NEI.searchField.setFocus(focused);
                this.isAutoFocused = false;
                return;
            }
            if (CommonHelper.proxy.isActionKey(ActionKey.SEARCH_CONNECTED_INVENTORIES, key) && !NEI.searchField.focused() && !this.searchField.isFocused() && (mouseInGui = this.isPointInRegion(0, 0, this.xSize, this.ySize, this.currentMouseX, this.currentMouseY)) && (slot = this.getSlot(this.currentMouseX, this.currentMouseY)) instanceof SlotME) {
                SlotME sme = (SlotME)slot;
                IAEItemStack stack = sme.getAEStack();
                this.monitorableContainer.setTargetStack(stack);
                if (stack != null) {
                    PacketInventoryAction p = new PacketInventoryAction(InventoryAction.FIND_ITEMS, this.getInventorySlots().size(), 0L);
                    NetworkHandler.instance.sendToServer(p);
                    this.mc.thePlayer.closeScreen();
                    return;
                }
            }
            if (NEI.searchField.focused()) {
                return;
            }
        }
        if (this.searchField.isFocused() && key == 28) {
            this.searchField.setFocused(false);
            this.isAutoFocused = false;
            return;
        }
        if (character == ' ' && this.searchField.getText().isEmpty()) {
            return;
        }
        boolean bl = skipHotbarCheck = this.searchField.isFocused() && (AEConfig.instance.searchBoxFocusPriority == SearchBoxFocusPriority.ALWAYS || AEConfig.instance.searchBoxFocusPriority == SearchBoxFocusPriority.NO_AUTOSEARCH && !this.isAutoFocused);
        if (!skipHotbarCheck && this.checkHotbarKeys(key)) {
            return;
        }
        boolean mouseInGui = this.isPointInRegion(0, 0, this.xSize, this.ySize, this.currentMouseX, this.currentMouseY);
        if (this.canBeAutoFocused && !this.searchField.isFocused() && mouseInGui) {
            this.searchField.setFocused(true);
            this.isAutoFocused = true;
        }
        if (!this.searchField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    public void updateScreen() {
        this.repo.setPowered(this.monitorableContainer.isPowered());
        super.updateScreen();
    }

    @Override
    public Enum getSortBy() {
        return this.configSrc.getSetting(Settings.SORT_BY);
    }

    @Override
    public Enum getSortDir() {
        return this.configSrc.getSetting(Settings.SORT_DIRECTION);
    }

    @Override
    public Enum getTypeFilter() {
        return this.configSrc.getSetting(Settings.TYPE_FILTER);
    }

    @Override
    public Enum getSortDisplay() {
        return this.configSrc.getSetting(Settings.VIEW_MODE);
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        if (this.SortByBox != null) {
            this.SortByBox.set(this.configSrc.getSetting(Settings.SORT_BY));
        }
        if (this.SortDirBox != null) {
            this.SortDirBox.set(this.configSrc.getSetting(Settings.SORT_DIRECTION));
        }
        if (this.ViewBox != null) {
            this.ViewBox.set(this.configSrc.getSetting(Settings.VIEW_MODE));
        }
        if (this.typeFilter != null) {
            this.typeFilter.set(this.configSrc.getSetting(Settings.TYPE_FILTER));
        }
        if (this.pinsStateButton != null) {
            this.pinsState = (PinsState)this.configSrc.getSetting(Settings.PINS_STATE);
            this.pinsStateButton.set(this.pinsState);
            this.reinitalize();
        }
        this.repo.updateView();
    }

    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return (pointX -= this.guiLeft) >= rectX - 1 && pointX < rectX + rectWidth + 1 && (pointY -= this.guiTop) >= rectY - 1 && pointY < rectY + rectHeight + 1;
    }

    int getReservedSpace() {
        return this.reservedSpace;
    }

    void setReservedSpace(int reservedSpace) {
        this.reservedSpace = reservedSpace;
    }

    public boolean isCustomSortOrder() {
        return this.customSortOrder;
    }

    void setCustomSortOrder(boolean customSortOrder) {
        this.customSortOrder = customSortOrder;
    }

    public int getStandardSize() {
        return this.standardSize;
    }

    void setStandardSize(int standardSize) {
        this.standardSize = standardSize;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.handleTooltip(mouseX, mouseY, this.searchField);
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        return this.searchField.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        this.searchField.setText(NEI.searchField.getEscapedSearchText(displayName));
    }

    @Override
    protected void handleMouseClick(Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_) {
        if (p_146984_1_ != null && p_146984_4_ == 4 && p_146984_1_.xDisplayPosition > this.xSize) {
            p_146984_4_ = 0;
        }
        super.handleMouseClick(p_146984_1_, p_146984_2_, p_146984_3_, p_146984_4_);
    }

    public void handleKeyboardInput() {
        super.handleKeyboardInput();
        this.repo.setPaused(this.hasShiftDown());
    }

    public boolean hideItemPanelSlot(int tx, int ty, int tw, int th) {
        if (this.viewCell) {
            int rw = 33;
            int rh = 14 + this.myCurrentViewCells.length * 18;
            if (this.monitorableContainer.isAPatternTerminal()) {
                rh += 21;
            }
            if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
                return false;
            }
            int rx = this.guiLeft + this.xSize;
            int ry = this.guiTop + 0;
            rh += ry;
            tw += tx;
            th += ty;
            return !((rw += rx) >= rx && rw <= tx || rh >= ry && rh <= ty || tw >= tx && tw <= rx || th >= ty && th <= ry);
        }
        return false;
    }

    private boolean hasShiftDown() {
        return Keyboard.isKeyDown((int)42) || Keyboard.isKeyDown((int)54);
    }

    @Override
    public void setAEPins(IAEItemStack[] pins) {
        this.repo.setAEPins(pins);
    }

    @Override
    public void setPinsState(PinsState state) {
        this.configSrc.putSetting(Settings.PINS_STATE, state);
    }

    public IItemList<IAEItemStack> getAvaibleItems() {
        return this.repo.getAvailableItems();
    }

    @Override
    protected boolean checkHotbarKeys(int keyCode) {
        if (this.theSlot instanceof SlotME) {
            return false;
        }
        return super.checkHotbarKeys(keyCode);
    }

    static {
        memoryText = "";
    }
}

