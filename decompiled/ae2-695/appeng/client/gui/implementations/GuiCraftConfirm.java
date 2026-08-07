/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.StatCollector
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.CraftingSortOrder;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.TerminalStyle;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IGuiTooltipHandler;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingCPUTable;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiSimpleImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.ICraftingCPUTableHolder;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.crafting.v2.CraftingJobV2;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.integration.modules.NEI;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.parts.reporting.PartTerminal;
import appeng.util.ColorPickHelper;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.RoundHelper;
import appeng.util.item.AEItemStack;
import com.google.common.base.Joiner;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiCraftConfirm
extends AEBaseGui
implements ICraftingCPUTableHolder,
IGuiTooltipHandler {
    public static final int TREE_VIEW_TEXTURE_WIDTH = 238;
    public static final int TREE_VIEW_TEXTURE_HEIGHT = 238;
    public static final int TREE_VIEW_DEFAULT_CPU_SLOTS = 8;
    public static final float TERR_VIEW_MAX_WIDTH_RATIO = 0.5f;
    public static final int LIST_VIEW_TEXTURE_WIDTH = 238;
    public static final int LIST_VIEW_TEXTURE_HEIGHT = 206;
    public static final int LIST_VIEW_TEXTURE_BELOW_TOP_ROW_Y = 41;
    public static final int LIST_VIEW_TEXTURE_ABOVE_BOTTOM_ROW_Y = 110;
    public static final int LIST_VIEW_TEXTURE_ROW_HEIGHT = 23;
    public static final int LIST_VIEW_TEXTURE_NONROW_HEIGHT = 91;
    private final ContainerCraftConfirm ccc;
    private final GuiCraftingCPUTable cpuTable;
    private final GuiCraftingTree craftingTree;
    private int rows = 5;
    private final IItemList<IAEItemStack> storage = AEApi.instance().storage().createItemList();
    private final IItemList<IAEItemStack> pending = AEApi.instance().storage().createItemList();
    private final IItemList<IAEItemStack> missing = AEApi.instance().storage().createItemList();
    private CraftingJobV2 jobTree = null;
    private final List<IAEItemStack> visual = new ArrayList<IAEItemStack>();
    private DisplayMode displayMode = DisplayMode.LIST;
    private boolean tallMode;
    private CraftingSortOrder sortMode = CraftingSortOrder.NAME;
    private SortDir sortDir = SortDir.ASCENDING;
    private GuiBridge OriginalGui;
    private GuiButton cancel;
    private GuiButton start;
    private GuiButton startWithFollow;
    private GuiButton selectCPU;
    private GuiImgButton switchTallMode;
    private GuiSimpleImgButton takeScreenshot;
    private GuiTabButton switchDisplayMode;
    private GuiImgButton sortingModeButton;
    private GuiImgButton sortingDirectionButton;
    private GuiSimpleImgButton optimizeButton;
    private GuiAeButton findNext;
    private GuiAeButton findPrev;
    private MEGuiTextField searchField;
    private int tooltip = -1;
    private ItemStack hoveredStack;
    private ArrayList<Integer> goToData = new ArrayList();
    private int searchGotoIndex = -1;
    private IAEItemStack needHighlight;
    final GuiScrollbar scrollbar;
    Comparator<IAEItemStack> comparator = (i1, i2) -> {
        IAEItemStack storage1 = this.storage.findPrecise((IAEItemStack)i1);
        IAEItemStack storage2 = this.storage.findPrecise((IAEItemStack)i2);
        IAEItemStack pending1 = this.pending.findPrecise((IAEItemStack)i1);
        IAEItemStack pending2 = this.pending.findPrecise((IAEItemStack)i2);
        IAEItemStack missing1 = this.missing.findPrecise((IAEItemStack)i1);
        IAEItemStack missing2 = this.missing.findPrecise((IAEItemStack)i2);
        if (missing1 != null && missing2 == null) {
            return -1;
        }
        if (missing1 == null && missing2 != null) {
            return 1;
        }
        if (this.sortMode == CraftingSortOrder.CRAFTS) {
            long amount1 = pending1 != null ? pending1.getCountRequestableCrafts() : 0L;
            long amount2 = pending2 != null ? pending2.getCountRequestableCrafts() : 0L;
            return Long.compare(amount1, amount2) * this.sortDir.sortHint;
        }
        if (this.sortMode == CraftingSortOrder.AMOUNT) {
            long amount1 = (storage1 != null ? storage1.getStackSize() : 0L) + (pending1 != null ? pending1.getStackSize() : 0L) + (missing1 != null ? missing1.getStackSize() : 0L);
            long amount2 = (storage2 != null ? storage2.getStackSize() : 0L) + (pending2 != null ? pending2.getStackSize() : 0L) + (missing2 != null ? missing2.getStackSize() : 0L);
            return Long.compare(amount1, amount2) * this.sortDir.sortHint;
        }
        if (this.sortMode == CraftingSortOrder.NAME) {
            return ((AEItemStack)i1).getDisplayName().compareToIgnoreCase(((AEItemStack)i2).getDisplayName()) * this.sortDir.sortHint;
        }
        if (this.sortMode == CraftingSortOrder.MOD) {
            int v = ((AEItemStack)i1).getModID().compareToIgnoreCase(((AEItemStack)i2).getModID());
            return (v == 0 ? ((AEItemStack)i1).getDisplayName().compareToIgnoreCase(((AEItemStack)i2).getDisplayName()) : v) * this.sortDir.sortHint;
        }
        if (this.sortMode == CraftingSortOrder.PERCENT) {
            float percent1 = storage1 != null && pending1 == null && missing1 == null ? storage1.getUsedPercent() : -1.0f;
            float percent2 = storage2 != null && pending2 == null && missing2 == null ? storage2.getUsedPercent() : -1.0f;
            return Float.compare(percent1, percent2) * this.sortDir.sortHint;
        }
        return 0;
    };

    protected void recalculateScreenSize() {
        switch (this.displayMode) {
            case LIST: {
                int maxAvailableHeight = this.height - 64;
                this.xSize = 238;
                if (this.tallMode) {
                    this.rows = (maxAvailableHeight - 91) / 23;
                    this.ySize = 91 + this.rows * 23;
                    break;
                }
                this.rows = 5;
                this.ySize = 206;
                break;
            }
            case TREE: {
                this.xSize = this.tallMode ? Math.max(238, (int)((float)this.width * 0.5f)) : 238;
                this.ySize = this.tallMode ? this.height - 64 : 238;
                this.rows = this.tallMode ? (this.ySize - 46) / 23 : 8;
                this.craftingTree.widgetW = this.xSize - 35;
                this.craftingTree.widgetH = this.ySize - 46;
            }
        }
        GuiCraftingCPUTable.CPU_TABLE_SLOTS = this.rows;
        GuiCraftingCPUTable.CPU_TABLE_HEIGHT = this.rows * 23 + 27;
    }

    public GuiCraftConfirm(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerCraftConfirm(inventoryPlayer, te));
        this.craftingTree = new GuiCraftingTree(this, 9, 19, 203, 192);
        this.tallMode = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.TALL;
        this.recalculateScreenSize();
        this.scrollbar = new GuiScrollbar();
        this.setScrollBar(this.scrollbar);
        this.ccc = (ContainerCraftConfirm)this.inventorySlots;
        this.cpuTable = new GuiCraftingCPUTable(this, ((ContainerCraftConfirm)this.inventorySlots).cpuTable, c -> this.ccc.cpuCraftingSameItem((CraftingCPUStatus)c) && this.ccc.cpuMatches((CraftingCPUStatus)c));
        if (te instanceof WirelessTerminalGuiObject) {
            this.OriginalGui = GuiBridge.GUI_WIRELESS_TERM;
        }
        if (te instanceof PartTerminal) {
            this.OriginalGui = GuiBridge.GUI_ME;
        }
        if (te instanceof PartCraftingTerminal) {
            this.OriginalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
        }
        if (te instanceof PartPatternTerminal) {
            this.OriginalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }
        if (te instanceof PartPatternTerminalEx) {
            this.OriginalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }
    }

    @Override
    public GuiCraftingCPUTable getCPUTable() {
        return this.cpuTable;
    }

    boolean isAutoStart() {
        return ((ContainerCraftConfirm)this.inventorySlots).isAutoStart();
    }

    @Override
    public void initGui() {
        this.recalculateScreenSize();
        super.initGui();
        this.setScrollBar();
        this.start = new GuiButton(0, this.guiLeft + this.xSize - 78, this.guiTop + this.ySize - 25, 52, 20, GuiText.Start.getLocal());
        this.start.enabled = false;
        this.buttonList.add(this.start);
        this.startWithFollow = new GuiButton(0, this.guiLeft + 61, this.guiTop + this.ySize - 25, 96, 20, GuiText.StartWithFollow.getLocal());
        this.startWithFollow.enabled = false;
        this.buttonList.add(this.startWithFollow);
        this.selectCPU = new GuiButton(0, this.guiLeft + 19, this.guiTop + this.ySize - 68, 180, 20, GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic);
        this.selectCPU.enabled = false;
        this.buttonList.add(this.selectCPU);
        this.cancel = new GuiButton(0, this.guiLeft + 6, this.guiTop + this.ySize - 25, 52, 20, GuiText.Cancel.getLocal());
        this.buttonList.add(this.cancel);
        this.switchTallMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + this.ySize - 18, Settings.TERMINAL_STYLE, this.tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
        this.buttonList.add(this.switchTallMode);
        this.takeScreenshot = new GuiSimpleImgButton(this.guiLeft - 36, this.guiTop + this.ySize - 18, 144, ButtonToolTips.SaveAsImage.getLocal());
        this.buttonList.add(this.takeScreenshot);
        this.switchDisplayMode = new GuiTabButton(this.guiLeft + this.xSize - 25, this.guiTop - 4, 211, GuiText.SwitchCraftingSimulationDisplayMode.getLocal(), itemRender);
        this.switchDisplayMode.setHideEdge(1);
        this.buttonList.add(this.switchDisplayMode);
        this.sortMode = (CraftingSortOrder)AEConfig.instance.settings.getSetting(Settings.CRAFTING_SORT_BY);
        this.sortDir = (SortDir)AEConfig.instance.settings.getSetting(Settings.SORT_DIRECTION);
        this.sortingModeButton = new GuiImgButton(this.guiLeft + this.xSize + 2, this.guiTop + 8, Settings.CRAFTING_SORT_BY, this.sortMode);
        this.buttonList.add(this.sortingModeButton);
        this.sortingDirectionButton = new GuiImgButton(this.guiLeft + this.xSize + 2, this.guiTop + 8 + 20, Settings.SORT_DIRECTION, this.sortDir);
        this.buttonList.add(this.sortingDirectionButton);
        this.optimizeButton = new GuiSimpleImgButton(this.guiLeft + this.xSize + 2, this.guiTop + 8 + 40, 19, ButtonToolTips.OptimizePatterns.getLocal());
        this.optimizeButton.enabled = false;
        this.buttonList.add(this.optimizeButton);
        this.searchField = new MEGuiTextField(52, 12, "Search"){

            @Override
            public void onTextChange(String oldText) {
                super.onTextChange(oldText);
                switch (GuiCraftConfirm.this.displayMode) {
                    case LIST: {
                        GuiCraftConfirm.this.updateSearchGoToList();
                        break;
                    }
                    case TREE: {
                        GuiCraftConfirm.this.craftingTree.updateSearchGoToList(this.getText().toLowerCase());
                    }
                }
            }
        };
        this.searchField.x = this.guiLeft + this.xSize - 101;
        this.searchField.y = this.guiTop + 5;
        this.findPrev = new GuiAeButton(0, this.guiLeft + this.xSize - 48, this.guiTop + 6, 10, 10, "\u2191", ButtonToolTips.SearchGotoPrev.getLocal());
        this.buttonList.add(this.findPrev);
        this.findNext = new GuiAeButton(0, this.guiLeft + this.xSize - 36, this.guiTop + 6, 10, 10, "\u2193", ButtonToolTips.SearchGotoNext.getLocal());
        this.buttonList.add(this.findNext);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.updateCPUButtonText();
        this.updateCancelButtonText();
        this.cpuTable.drawScreen();
        boolean bl = this.start.enabled = !this.ccc.hasNoCPU() && !this.isSimulation();
        if (this.start.enabled) {
            CraftingCPUStatus selected = this.cpuTable.getContainer().getSelectedCPU();
            this.start.displayString = selected != null && this.ccc.cpuCraftingSameItem(selected) ? GuiText.Merge.getLocal() : GuiText.Start.getLocal();
            if (selected == null || !this.ccc.cpuMatches(selected)) {
                this.start.enabled = false;
            }
        }
        this.startWithFollow.enabled = this.start.enabled;
        this.selectCPU.enabled = this.displayMode == DisplayMode.LIST && !this.isSimulation();
        boolean bl2 = this.optimizeButton.enabled = this.displayMode == DisplayMode.LIST && !this.isSimulation() && this.ccc.isAllowedToRunPatternOptimization;
        if (!this.ccc.isAllowedToRunPatternOptimization) {
            this.optimizeButton.setTooltip(ButtonToolTips.OptimizePatterns.getLocal() + "\n" + ButtonToolTips.OptimizePatternsNoReq.getLocal());
        } else {
            this.optimizeButton.setTooltip(ButtonToolTips.OptimizePatterns.getLocal());
        }
        this.sortingDirectionButton.visible = this.displayMode == DisplayMode.LIST;
        this.sortingModeButton.visible = this.sortingDirectionButton.visible;
        this.optimizeButton.visible = this.sortingDirectionButton.visible;
        this.selectCPU.visible = this.sortingDirectionButton.visible;
        this.takeScreenshot.visible = this.displayMode == DisplayMode.TREE;
        switch (this.displayMode) {
            case LIST: {
                this.drawListScreen(mouseX, mouseY, btn);
                break;
            }
            case TREE: {
                this.drawTreeScreen(mouseX, mouseY, btn);
            }
        }
        super.drawScreen(mouseX, mouseY, btn);
    }

    private void drawListScreen(int mouseX, int mouseY, float btn) {
        int gx = (this.width - this.xSize) / 2;
        int gy = (this.height - this.ySize) / 2;
        this.tooltip = -1;
        int offY = 23;
        int y = 0;
        int x = 0;
        for (int z = 0; z <= 4 * this.rows; ++z) {
            int minX = gx + 9 + x * 67;
            int minY = gy + 22 + y * 23;
            if (minX < mouseX && minX + 67 > mouseX && minY < mouseY && minY + 23 - 2 > mouseY) {
                this.tooltip = z;
                break;
            }
            if (++x <= 2) continue;
            ++y;
            x = 0;
        }
    }

    private void drawTreeScreen(int mouseX, int mouseY, float btn) {
        this.craftingTree.drawTooltip(mouseX, mouseY);
    }

    private void updateCancelButtonText() {
        this.cancel.displayString = !this.missing.isEmpty() && GuiCraftConfirm.isShiftKeyDown() ? GuiText.AddToBookmark.getLocal() : GuiText.Cancel.getLocal();
    }

    private void updateCPUButtonText() {
        String btnTextText = GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic.getLocal();
        if (this.ccc.getSelectedCpu() >= 0) {
            if (!this.ccc.getName().isEmpty()) {
                String name = this.ccc.getName().substring(0, Math.min(20, this.ccc.getName().length()));
                btnTextText = GuiText.CraftingCPU.getLocal() + ": " + name;
            } else {
                btnTextText = GuiText.CraftingCPU.getLocal() + ": #" + this.ccc.getSelectedCpu();
            }
        }
        if (this.ccc.hasNoCPU()) {
            btnTextText = GuiText.NoCraftingCPUs.getLocal();
        }
        this.selectCPU.displayString = btnTextText;
    }

    private boolean isSimulation() {
        return ((ContainerCraftConfirm)this.inventorySlots).isSimulation();
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.cpuTable.drawFG(offsetX, offsetY, mouseX, mouseY, this.guiLeft, this.guiTop);
        long BytesUsed = this.ccc.getUsedBytes();
        String byteUsed = Platform.formatByteDouble(BytesUsed);
        String bannerText = this.jobTree != null && !this.jobTree.getErrorMessage().isEmpty() ? (this.jobTree.getErrorMessage().equals("java.lang.ArithmeticException: long overflow") ? GuiText.CraftingSizeLimitExceeded.getLocal() : StatCollector.translateToLocal((String)this.jobTree.getErrorMessage())) : (BytesUsed > 0L ? byteUsed : GuiText.CalculatingWait.getLocal());
        this.fontRendererObj.drawString(GuiText.CraftingPlan.getLocal() + " - " + bannerText, 8, 7, GuiColors.CraftConfirmCraftingPlan.getColor());
        switch (this.displayMode) {
            case LIST: {
                this.drawListFG(offsetX, offsetY, mouseX, mouseY);
                break;
            }
            case TREE: {
                this.drawTreeFG(offsetX, offsetY, mouseX, mouseY);
            }
        }
    }

    private void updateSearchGoToList() {
        this.needHighlight = null;
        this.searchGotoIndex = -1;
        this.goToData.clear();
        if (this.searchField.getText().isEmpty()) {
            return;
        }
        String s = this.searchField.getText().toLowerCase();
        int visCount = 0;
        for (IAEItemStack aeis : this.visual) {
            if (aeis != null && Platform.getItemDisplayName(aeis).toLowerCase().contains(s)) {
                this.goToData.add(visCount);
            }
            ++visCount;
        }
        this.searchGoTo(true);
    }

    private void searchGoTo(boolean forward) {
        String s = this.searchField.getText().toLowerCase();
        if (s.isEmpty() || this.goToData.isEmpty()) {
            return;
        }
        if (forward) {
            ++this.searchGotoIndex;
            if (this.searchGotoIndex >= this.goToData.size()) {
                this.searchGotoIndex = 0;
            }
        } else {
            if (this.searchGotoIndex <= 0) {
                this.searchGotoIndex = this.goToData.size();
            }
            --this.searchGotoIndex;
        }
        IAEItemStack aeis = this.visual.get(this.goToData.get(this.searchGotoIndex));
        this.getScrollBar().setCurrentScroll(this.goToData.get(this.searchGotoIndex) / 3 - this.rows / 2);
        this.needHighlight = aeis.copy();
    }

    private void drawListFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        String dsp = null;
        dsp = this.isSimulation() ? GuiText.Simulation.getLocal() : (this.ccc.getCpuAvailableBytes() > 0L ? GuiText.Bytes.getLocal() + ": " + Platform.formatByteDouble(this.ccc.getCpuAvailableBytes()) + " : " + GuiText.CoProcessors.getLocal() + ": " + NumberFormat.getInstance().format(this.ccc.getCpuCoProcessors()) : GuiText.Bytes.getLocal() + ": N/A : " + GuiText.CoProcessors.getLocal() + ": N/A");
        int offset = (219 - this.fontRendererObj.getStringWidth(dsp)) / 2;
        this.fontRendererObj.drawString(dsp, offset, this.ySize - 41, GuiColors.CraftConfirmSimulation.getColor());
        int sectionLength = 67;
        int x = 0;
        int y = 0;
        int xo = 9;
        int yo = 22;
        int viewStart = this.getScrollBar().getCurrentScroll() * 3;
        int viewEnd = viewStart + 3 * this.rows;
        String dspToolTip = "";
        LinkedList<String> lineList = new LinkedList<String>();
        int toolPosX = 0;
        int toolPosY = 0;
        this.hoveredStack = null;
        int offY = 23;
        for (int z = viewStart; z < Math.min(viewEnd, this.visual.size()); ++z) {
            int startY;
            int startX;
            int w;
            IAEItemStack refStack = this.visual.get(z);
            if (refStack == null) continue;
            GL11.glPushMatrix();
            GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
            IAEItemStack stored = this.storage.findPrecise(refStack);
            IAEItemStack pendingStack = this.pending.findPrecise(refStack);
            IAEItemStack missingStack = this.missing.findPrecise(refStack);
            int lines = 0;
            if (stored != null && stored.getStackSize() > 0L) {
                ++lines;
                if (missingStack == null && pendingStack == null) {
                    ++lines;
                }
            }
            if (missingStack != null && missingStack.getStackSize() > 0L) {
                ++lines;
            }
            if (pendingStack != null && pendingStack.getStackSize() > 0L) {
                lines += 2;
            }
            int negY = (lines - 1) * 5 / 2;
            int downY = 0;
            if (stored != null && stored.getStackSize() > 0L) {
                String str = GuiText.FromStorage.getLocal() + ": " + ReadableNumberConverter.INSTANCE.toWideReadableForm(stored.getStackSize());
                int w2 = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w2 * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftConfirmFromStorage.getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.FromStorage.getLocal() + ": " + NumberFormat.getInstance().format(stored.getStackSize()));
                }
                downY += 5;
            }
            boolean red = false;
            if (missingStack != null && missingStack.getStackSize() > 0L) {
                String str = GuiText.Missing.getLocal() + ": " + ReadableNumberConverter.INSTANCE.toWideReadableForm(missingStack.getStackSize());
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftConfirmMissing.getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.Missing.getLocal() + ": " + NumberFormat.getInstance().format(missingStack.getStackSize()));
                }
                red = true;
                downY += 5;
            }
            if (pendingStack != null && pendingStack.getStackSize() > 0L) {
                String str = GuiText.ToCraft.getLocal() + ": " + ReadableNumberConverter.INSTANCE.toWideReadableForm(pendingStack.getStackSize());
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftConfirmToCraft.getColor());
                str = GuiText.ToCraftRequests.getLocal() + ": " + ReadableNumberConverter.INSTANCE.toWideReadableForm(pendingStack.getCountRequestableCrafts());
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + (downY += 5)) * 2, GuiColors.CraftConfirmToCraft.getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.ToCraft.getLocal() + ": " + NumberFormat.getInstance().format(pendingStack.getStackSize()));
                    lineList.add(GuiText.ToCraftRequests.getLocal() + ": " + NumberFormat.getInstance().format(pendingStack.getCountRequestableCrafts()));
                }
            }
            if (stored != null && stored.getStackSize() > 0L && missingStack == null && pendingStack == null) {
                String str = GuiText.FromStoragePercent.getLocal() + ": " + RoundHelper.toRoundedFormattedForm(stored.getUsedPercent(), 2) + "%";
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, ColorPickHelper.selectColorFromThreshold(stored.getUsedPercent()).getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.FromStoragePercent.getLocal() + ": " + RoundHelper.toRoundedFormattedForm(stored.getUsedPercent(), 4) + "%");
                }
            }
            GL11.glPopMatrix();
            int posX = x * 68 + 9 + 67 - 19;
            int posY = y * 23 + 22;
            ItemStack is = refStack.copy().getItemStack();
            if (this.tooltip == z - viewStart) {
                dspToolTip = Platform.getItemDisplayName(is);
                if (!lineList.isEmpty()) {
                    this.addItemTooltip(is, lineList);
                    dspToolTip = dspToolTip + '\n' + Joiner.on((String)"\n").join(lineList);
                }
                toolPosX = x * 68 + 9 + 67 - 8;
                toolPosY = y * 23 + 22;
                this.hoveredStack = is;
            }
            this.drawItem(posX, posY, is);
            if (red) {
                startX = x * 68 + 9;
                startY = posY - 4;
                GuiCraftConfirm.drawRect((int)startX, (int)startY, (int)(startX + 67), (int)(startY + 23), (int)GuiColors.CraftConfirmMissingItem.getColor());
            }
            if (!this.searchField.getText().isEmpty() && this.goToData.contains(z)) {
                startX = x * 68 + 9;
                startY = posY - 4;
                int color = this.needHighlight != null && this.needHighlight.isSameType(refStack) ? GuiColors.SearchGoToHighlight.getColor() : GuiColors.SearchHighlight.getColor();
                this.drawVerticalLine(startX, startY, startY + 23, color);
                this.drawVerticalLine(startX + 67 - 1, startY, startY + 23, color);
                this.drawHorizontalLine(startX + 1, startX + 67 - 2, startY + 1, color);
                this.drawHorizontalLine(startX + 1, startX + 67 - 2, startY + 23 - 1, color);
            }
            if (++x <= 2) continue;
            ++y;
            x = 0;
        }
        if (this.tooltip >= 0 && !dspToolTip.isEmpty()) {
            this.drawTooltip(toolPosX, toolPosY + 10, dspToolTip);
        }
    }

    private void drawTreeFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        CraftingJobV2 jobTree = this.jobTree;
        if (jobTree == null) {
            this.drawTooltip(16, 48, GuiText.NoCraftingTreeReceived.getLocal());
            return;
        }
        if (jobTree.getOutput() == null) {
            this.drawTooltip(16, 48, GuiText.Nothing.getLocal());
            return;
        }
        this.craftingTree.setRequest(jobTree.originalRequest);
        this.craftingTree.draw(mouseX, mouseY);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.cpuTable.drawBG(offsetX, offsetY);
        this.setScrollBar();
        switch (this.displayMode) {
            case LIST: {
                this.bindTexture("guis/craftingreport.png");
                if (this.tallMode) {
                    this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 41);
                    int y = 41;
                    for (int row = 1; row < this.rows - 1; ++row) {
                        this.drawTexturedModalRect(offsetX, offsetY + y, 0, 41, this.xSize, 23);
                        y += 23;
                    }
                    this.drawTexturedModalRect(offsetX, offsetY + y, 0, 110, this.xSize, 96);
                    break;
                }
                this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
                break;
            }
            case TREE: {
                this.bindTexture("guis/craftingtree.png");
                this.drawTextured9PatchRect(offsetX, offsetY, this.xSize, this.ySize, 0, 0, 238, 238);
            }
        }
        this.bindTexture("guis/searchField.png");
        this.drawTexturedModalRect(this.guiLeft + this.xSize - 101, this.guiTop + 5, 0, 0, 52, 12);
        this.searchField.drawTextBox();
    }

    private void setScrollBar() {
        switch (this.displayMode) {
            case LIST: {
                if (this.getScrollBar() == null) {
                    this.setScrollBar(this.scrollbar);
                }
                int size = this.visual.size();
                this.getScrollBar().setTop(19).setLeft(218).setHeight(this.ySize - 92);
                this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
                break;
            }
            case TREE: {
                if (this.getScrollBar() == null) break;
                this.setScrollBar(null);
            }
        }
    }

    public void postUpdate(List<IAEItemStack> list, byte ref) {
        switch (ref) {
            case 0: {
                for (IAEItemStack l : list) {
                    this.handleInput(this.storage, l);
                }
                break;
            }
            case 1: {
                for (IAEItemStack l : list) {
                    this.handleInput(this.pending, l);
                }
                break;
            }
            case 2: {
                for (IAEItemStack l : list) {
                    this.handleInput(this.missing, l);
                }
                break;
            }
        }
        for (IAEItemStack l : list) {
            long amt = this.getTotal(l);
            if (amt <= 0L) {
                this.deleteVisualStack(l);
                continue;
            }
            IAEItemStack is = this.findVisualStack(l);
            is.setStackSize(amt);
        }
        this.sortItems();
        this.setScrollBar();
    }

    public void setJobTree(CraftingJobV2 jobTree) {
        this.jobTree = jobTree;
    }

    private void sortItems() {
        this.visual.sort(this.comparator);
    }

    private void handleInput(IItemList<IAEItemStack> s, IAEItemStack l) {
        IAEItemStack a = s.findPrecise(l);
        if (l.getStackSize() <= 0L) {
            if (a != null) {
                a.reset();
            }
        } else {
            if (a == null) {
                s.add(l.copy());
                a = s.findPrecise(l);
            }
            if (a != null) {
                a.setStackSize(l.getStackSize());
            }
        }
    }

    @Override
    protected boolean mouseWheelEvent(int x, int y, int wheel) {
        if (this.displayMode == DisplayMode.TREE && this.craftingTree != null && this.craftingTree.isPointInWidget(x - this.guiLeft, y - this.guiTop)) {
            this.craftingTree.onMouseWheel(x - this.guiLeft, y - this.guiTop, wheel);
            return true;
        }
        return super.mouseWheelEvent(x, y, wheel);
    }

    private long getTotal(IAEItemStack is) {
        IAEItemStack a = this.storage.findPrecise(is);
        IAEItemStack c = this.pending.findPrecise(is);
        IAEItemStack m = this.missing.findPrecise(is);
        long total = 0L;
        if (a != null) {
            total += a.getStackSize();
        }
        if (c != null) {
            total += c.getStackSize();
        }
        if (m != null) {
            total += m.getStackSize();
        }
        return total;
    }

    private void deleteVisualStack(IAEItemStack l) {
        Iterator<IAEItemStack> i = this.visual.iterator();
        while (i.hasNext()) {
            IAEItemStack o = i.next();
            if (!o.equals(l)) continue;
            i.remove();
            return;
        }
    }

    private IAEItemStack findVisualStack(IAEItemStack l) {
        for (IAEItemStack o : this.visual) {
            if (!o.equals(l)) continue;
            return o;
        }
        IAEItemStack stack = l.copy();
        this.visual.add(stack);
        return stack;
    }

    protected void keyTyped(char character, int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28 || key == 156) {
                this.actionPerformed(this.start);
            }
            if (!this.searchField.textboxKeyTyped(character, key)) {
                super.keyTyped(character, key);
            }
        }
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean backwards = Mouse.isButtonDown((int)1);
        if (btn == this.selectCPU) {
            this.cpuTable.cycleCPU(backwards);
        } else if (btn == this.cancel) {
            this.addMissingItemsToBookMark();
            this.switchToOriginalGUI();
        } else if (btn == this.switchDisplayMode) {
            this.displayMode = this.displayMode.next();
            this.recalculateScreenSize();
            this.setWorldAndResolution(this.mc, this.width, this.height);
            this.searchField.setText("");
        } else if (btn == this.takeScreenshot) {
            if (this.craftingTree != null) {
                this.craftingTree.saveScreenshot();
            }
        } else if (btn instanceof GuiImgButton) {
            GuiImgButton iBtn = (GuiImgButton)btn;
            Enum cv = iBtn.getCurrentValue();
            Enum next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
            if (btn == this.switchTallMode) {
                this.tallMode = next == TerminalStyle.TALL;
                this.recalculateScreenSize();
                this.setWorldAndResolution(this.mc, this.width, this.height);
            } else if (btn == this.sortingModeButton) {
                this.sortMode = (CraftingSortOrder)next;
                AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                this.sortItems();
            } else if (btn == this.sortingDirectionButton) {
                this.sortDir = (SortDir)next;
                AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                this.sortItems();
            }
            iBtn.set(next);
        } else if (btn == this.optimizeButton) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("Terminal.OptimizePatterns", "Patterns"));
            }
            catch (Throwable e) {
                AELog.debug(e);
            }
        } else if (btn == this.start) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("Terminal.Start", "Start"));
            }
            catch (Throwable e) {
                AELog.debug(e);
            }
        } else if (btn == this.startWithFollow) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("Terminal.StartWithFollow", "Start"));
            }
            catch (Throwable e) {
                AELog.debug(e);
            }
        } else if (btn == this.findNext) {
            switch (this.displayMode) {
                case LIST: {
                    this.searchGoTo(true);
                    break;
                }
                case TREE: {
                    this.craftingTree.searchGoTo(true);
                }
            }
        } else if (btn == this.findPrev) {
            switch (this.displayMode) {
                case LIST: {
                    this.searchGoTo(false);
                    break;
                }
                case TREE: {
                    this.craftingTree.searchGoTo(false);
                }
            }
        }
    }

    public void switchToOriginalGUI() {
        if (this.OriginalGui != null) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(this.OriginalGui));
        }
    }

    @Override
    public ItemStack getHoveredStack() {
        return this.hoveredStack;
    }

    public GuiButton getCancelButton() {
        return this.cancel;
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        super.mouseClicked(xCoord, yCoord, btn);
        this.cpuTable.mouseClicked(xCoord - this.guiLeft, yCoord - this.guiTop, btn);
        this.searchField.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void mouseClickMove(int x, int y, int c, long d) {
        super.mouseClickMove(x, y, c, d);
        this.cpuTable.mouseClickMove(x - this.guiLeft, y - this.guiTop);
    }

    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() {
        if (this.cpuTable.handleMouseInput(this.guiLeft, this.guiTop)) {
            return;
        }
        super.handleMouseInput();
    }

    public boolean hideItemPanelSlot(int x, int y, int w, int h) {
        if (this.cpuTable.hideItemPanelSlot(x - this.guiLeft, y - this.guiTop, w, h)) {
            return true;
        }
        int bruhx = x - this.guiLeft - this.xSize;
        int bruhy = y - this.guiTop;
        return bruhx >= -w && bruhx <= 22 && bruhy >= -h && bruhy <= 48;
    }

    protected void addMissingItemsToBookMark() {
        if (!this.missing.isEmpty() && GuiCraftConfirm.isShiftKeyDown()) {
            ArrayList<ItemStack> missing = new ArrayList<ItemStack>();
            for (IAEItemStack iaeItemStack : this.missing) {
                missing.add(iaeItemStack.getItemStack());
            }
            IAEItemStack outputStack = ((ContainerCraftConfirm)this.inventorySlots).getItemToCraft();
            if (outputStack != null) {
                NEI.instance.addToBookmark(outputStack.getItemStack(), missing);
            } else {
                NEI.instance.addToBookmark(null, missing);
            }
        }
    }

    public IItemList<IAEItemStack> getStorage() {
        return this.storage;
    }

    public IItemList<IAEItemStack> getPending() {
        return this.pending;
    }

    public IItemList<IAEItemStack> getMissing() {
        return this.missing;
    }

    public static enum DisplayMode {
        LIST,
        TREE;


        public DisplayMode next() {
            DisplayMode displayMode;
            switch (this) {
                case LIST: {
                    displayMode = TREE;
                    break;
                }
                case TREE: {
                    displayMode = LIST;
                    break;
                }
                default: {
                    throw new IllegalArgumentException(this.toString());
                }
            }
            return displayMode;
        }
    }
}

