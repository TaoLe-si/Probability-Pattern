/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  org.apache.commons.lang3.time.DurationFormatUtils
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.CraftingAllow;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.config.YesNo;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.DimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IGuiTooltipHandler;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftingCPU;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCraftingItemInterface;
import appeng.core.sync.packets.PacketCraftingRemainingOperations;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.ScheduledReason;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.lwjgl.opengl.GL11;

public class GuiCraftingCPU
extends AEBaseGui
implements ISortSource,
IGuiTooltipHandler {
    protected static final int GUI_HEIGHT = 184;
    protected static final int GUI_WIDTH = 238;
    protected static final int TEXTURE_BELOW_TOP_ROW_Y = 41;
    protected static final int TEXTURE_ABOVE_BOTTOM_ROW_Y = 51;
    protected static final int DISPLAYED_ROWS = 6;
    protected static final int SECTION_LENGTH = 67;
    protected static final int SECTION_HEIGHT = 23;
    protected static final int SCROLLBAR_TOP = 19;
    protected static final int SCROLLBAR_LEFT = 218;
    protected static final int SCROLLBAR_HEIGHT = 137;
    private static final int CANCEL_LEFT_OFFSET = 163;
    private static final int CANCEL_TOP_OFFSET = 25;
    private static final int CANCEL_HEIGHT = 20;
    private static final int CANCEL_WIDTH = 50;
    private static final int TITLE_TOP_OFFSET = 7;
    private static final int TITLE_LEFT_OFFSET = 8;
    private static final int ITEMSTACK_LEFT_OFFSET = 9;
    private static final int ITEMSTACK_TOP_OFFSET = 22;
    private static final int ITEMS_PER_ROW = 3;
    private final ContainerCraftingCPU craftingCpu;
    protected IItemList<IAEItemStack> storage = AEApi.instance().storage().createItemList();
    protected IItemList<IAEItemStack> active = AEApi.instance().storage().createItemList();
    protected IItemList<IAEItemStack> pending = AEApi.instance().storage().createItemList();
    private IAEItemStack hoveredAEStack = null;
    protected int rows = 6;
    protected List<IAEItemStack> visual = new ArrayList<IAEItemStack>();
    private GuiButton cancel;
    protected List<IAEItemStack> visualHiddenStored = new ArrayList<IAEItemStack>();
    protected GuiImgButton toggleHideStored;
    protected boolean hideStored;
    private int tooltip = -1;
    private final RemainingOperations remainingOperations = new RemainingOperations();
    private ItemStack hoveredStack;
    private ItemStack hoveredNbtStack;
    private GuiAeButton findNext;
    private GuiAeButton findPrev;
    private GuiImgButton changeAllow;
    private MEGuiTextField searchField;
    private ArrayList<Integer> goToData = new ArrayList();
    private int searchGotoIndex = -1;
    private IAEItemStack needHighlight;

    public GuiCraftingCPU(InventoryPlayer inventoryPlayer, Object te) {
        this(new ContainerCraftingCPU(inventoryPlayer, te));
    }

    protected GuiCraftingCPU(ContainerCraftingCPU container) {
        super(container);
        this.craftingCpu = container;
        this.ySize = 184;
        this.xSize = 238;
        this.hideStored = AEConfig.instance.getConfigManager().getSetting(Settings.HIDE_STORED) == YesNo.YES;
        GuiScrollbar scrollbar = new GuiScrollbar();
        this.setScrollBar(scrollbar);
    }

    public void clearItems() {
        this.storage = AEApi.instance().storage().createItemList();
        this.active = AEApi.instance().storage().createItemList();
        this.pending = AEApi.instance().storage().createItemList();
        this.visual = new ArrayList<IAEItemStack>();
        this.visualHiddenStored = new ArrayList<IAEItemStack>();
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        if (this.cancel == btn) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("TileCrafting.Cancel", "Cancel"));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        } else if (this.toggleHideStored == btn) {
            this.hideStored ^= true;
            AEConfig.instance.getConfigManager().putSetting(Settings.HIDE_STORED, this.hideStored ? YesNo.YES : YesNo.NO);
            this.toggleHideStored.set(this.hideStored ? YesNo.YES : YesNo.NO);
            this.hideStoredSorting();
            this.setScrollBar();
            this.updateSearchGoToList(true);
        } else if (btn == this.findNext) {
            this.searchGoTo(true);
        } else if (btn == this.findPrev) {
            this.searchGoTo(false);
        } else if (btn == this.changeAllow) {
            String msg = String.valueOf(((CraftingAllow)this.changeAllow.getCurrentValue()).ordinal());
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("TileCrafting.Allow", msg));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
    }

    public IAEItemStack getHoveredAEStack() {
        return this.hoveredAEStack;
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        if (GuiCraftingCPU.isShiftKeyDown() && this.hoveredNbtStack != null) {
            NBTTagCompound data = Platform.openNbtData(this.hoveredNbtStack);
            BlockPosHighlighter.highlightBlocks((EntityPlayer)this.mc.thePlayer, DimensionalCoord.readAsListFromNBT(data), PlayerMessages.InterfaceHighlighted.getUnlocalized(), PlayerMessages.InterfaceInOtherDim.getUnlocalized());
            this.mc.thePlayer.closeScreen();
        } else if (this.hoveredAEStack != null && btn == 2) {
            ((AEBaseContainer)this.inventorySlots).setTargetStack(this.hoveredAEStack);
            PacketInventoryAction p = new PacketInventoryAction(InventoryAction.AUTO_CRAFT, this.inventorySlots.inventorySlots.size(), this.hoveredAEStack.getStackSize());
            NetworkHandler.instance.sendToServer(p);
        }
        super.mouseClicked(xCoord, yCoord, btn);
        this.searchField.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.setScrollBar();
        this.cancel = new GuiButton(0, this.guiLeft + 163, this.guiTop + this.ySize - 25, 50, 20, GuiText.Cancel.getLocal());
        this.toggleHideStored = new GuiImgButton(this.guiLeft + 221, this.guiTop + this.ySize - 19, Settings.HIDE_STORED, AEConfig.instance.getConfigManager().getSetting(Settings.HIDE_STORED));
        this.buttonList.add(this.toggleHideStored);
        this.buttonList.add(this.cancel);
        this.searchField = new MEGuiTextField(52, 12, "Search"){

            @Override
            public void onTextChange(String oldText) {
                super.onTextChange(oldText);
                GuiCraftingCPU.this.updateSearchGoToList(true);
            }
        };
        this.searchField.x = this.guiLeft + this.xSize - 101;
        this.searchField.y = this.guiTop + 5;
        this.findPrev = new GuiAeButton(0, this.guiLeft + this.xSize - 48, this.guiTop + 6, 10, 10, "\u2191", ButtonToolTips.SearchGotoPrev.getLocal());
        this.buttonList.add(this.findPrev);
        this.findNext = new GuiAeButton(0, this.guiLeft + this.xSize - 36, this.guiTop + 6, 10, 10, "\u2193", ButtonToolTips.SearchGotoNext.getLocal());
        this.buttonList.add(this.findNext);
        this.changeAllow = new GuiImgButton(this.guiLeft - 20, this.guiTop + 2, Settings.CRAFTING_ALLOW, CraftingAllow.ALLOW_ALL);
        this.buttonList.add(this.changeAllow);
    }

    private void setScrollBar() {
        int size = this.hideStored ? this.visualHiddenStored.size() : this.visual.size();
        this.getScrollBar().setTop(19).setLeft(218).setHeight(137);
        this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.cancel.enabled = !this.visual.isEmpty();
        this.changeAllow.set(CraftingAllow.values()[this.craftingCpu.allow]);
        int gx = (this.width - this.xSize) / 2;
        int gy = (this.height - this.ySize) / 2;
        this.tooltip = -1;
        int offY = 23;
        int y = 0;
        int x = 0;
        for (int z = 0; z <= 3 * this.rows; ++z) {
            int minX = gx + 9 + x * 67;
            int minY = gy + 22 + y * 23;
            if (minX < mouseX && minX + 67 > mouseX && minY < mouseY && minY + 23 > mouseY) {
                this.tooltip = z;
                break;
            }
            if (++x != 3) continue;
            ++y;
            x = 0;
        }
        this.handleTooltip(mouseX, mouseY, this.remainingOperations);
        super.drawScreen(mouseX, mouseY, btn);
    }

    private void updateSearchGoToList(boolean dropIndex) {
        this.needHighlight = null;
        this.goToData.clear();
        if (this.searchField.getText().isEmpty()) {
            return;
        }
        String s = this.searchField.getText().toLowerCase();
        int visCount = 0;
        for (IAEItemStack aeis : this.hideStored ? this.visualHiddenStored : this.visual) {
            if (aeis != null && Platform.getItemDisplayName(aeis).toLowerCase().contains(s)) {
                this.goToData.add(visCount);
            }
            ++visCount;
        }
        if (dropIndex) {
            this.searchGotoIndex = -1;
            this.searchGoTo(true);
        }
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
        List<IAEItemStack> visualTemp = this.hideStored ? this.visualHiddenStored : this.visual;
        IAEItemStack aeis = visualTemp.get(this.goToData.get(this.searchGotoIndex));
        this.getScrollBar().setCurrentScroll(this.goToData.get(this.searchGotoIndex) / 3 - this.rows / 2);
        this.needHighlight = aeis.copy();
    }

    private void updateRemainingOperations() {
        int interval = 1000;
        if (this.remainingOperations.getRefreshTick() >= this.remainingOperations.getLastWorkingTick() + (long)interval) {
            try {
                NetworkHandler.instance.sendToServer(new PacketCraftingRemainingOperations());
            }
            catch (IOException iOException) {
                // empty catch block
            }
            this.remainingOperations.setLastWorkingTick(this.remainingOperations.refreshTick);
        } else {
            this.remainingOperations.setRefreshTick(System.currentTimeMillis());
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        String title = this.getGuiDisplayName(GuiText.CraftingStatus.getLocal());
        if (this.craftingCpu.getElapsedTime() > 0L && !this.visual.isEmpty()) {
            long elapsedInMilliseconds = TimeUnit.MILLISECONDS.convert(this.craftingCpu.getElapsedTime(), TimeUnit.NANOSECONDS);
            String elapsedTimeText = DurationFormatUtils.formatDuration((long)elapsedInMilliseconds, (String)GuiText.ETAFormat.getLocal());
            title = title.length() == 0 ? elapsedTimeText : title + " - " + elapsedTimeText;
        }
        this.updateRemainingOperations();
        this.fontRendererObj.drawString(String.valueOf(this.remainingOperations.getRemainingOperations()), 136 - this.remainingOperations.getStringWidth(), 7, GuiColors.CraftingCPUTitle.getColor());
        this.fontRendererObj.drawString(title, 8, 7, GuiColors.CraftingCPUTitle.getColor());
        int x = 0;
        int y = 0;
        int viewStart = this.getScrollBar().getCurrentScroll() * 3;
        int viewEnd = viewStart + 3 * this.rows;
        String dspToolTip = "";
        LinkedList<String> lineList = new LinkedList<String>();
        int toolPosX = 0;
        int toolPosY = 0;
        this.hoveredStack = null;
        int offY = 23;
        ReadableNumberConverter converter = ReadableNumberConverter.INSTANCE;
        List<IAEItemStack> visualTemp = this.hideStored ? this.visualHiddenStored : this.visual;
        for (int z = viewStart; z < Math.min(viewEnd, visualTemp.size()); ++z) {
            int w;
            IAEItemStack refStack = visualTemp.get(z);
            if (refStack == null) continue;
            GL11.glPushMatrix();
            GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
            IAEItemStack stored = this.storage.findPrecise(refStack);
            IAEItemStack activeStack = this.active.findPrecise(refStack);
            IAEItemStack pendingStack = this.pending.findPrecise(refStack);
            int lines = 0;
            if (stored != null && stored.getStackSize() > 0L) {
                ++lines;
            }
            boolean active = false;
            if (activeStack != null && activeStack.getStackSize() > 0L) {
                ++lines;
                active = true;
            }
            boolean scheduled = false;
            if (pendingStack != null && pendingStack.getStackSize() > 0L) {
                ++lines;
                scheduled = true;
            }
            if (AEConfig.instance.useColoredCraftingStatus && (active || scheduled)) {
                int bgColor = active ? GuiColors.CraftingCPUActive.getColor() : GuiColors.CraftingCPUInactive.getColor();
                int startX = (x * 68 + 9) * 2;
                int startY = (y * 23 + 22 - 3) * 2;
                GuiCraftingCPU.drawRect((int)startX, (int)startY, (int)(startX + 134), (int)(startY + 46 - 2), (int)bgColor);
            }
            int negY = (lines - 1) * 5 / 2;
            int downY = 0;
            if (stored != null && stored.getStackSize() > 0L) {
                String str = GuiText.Stored.getLocal() + ": " + converter.toWideReadableForm(stored.getStackSize());
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftingCPUStored.getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.Stored.getLocal() + ": " + NumberFormat.getInstance().format(stored.getStackSize()));
                }
                downY += 5;
            }
            if (activeStack != null && activeStack.getStackSize() > 0L) {
                String str = GuiText.Crafting.getLocal() + ": " + converter.toWideReadableForm(activeStack.getStackSize());
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftingCPUAmount.getColor());
                if (this.tooltip == z - viewStart) {
                    this.hoveredAEStack = refStack;
                    lineList.add(GuiText.Crafting.getLocal() + ": " + NumberFormat.getInstance().format(activeStack.getStackSize()));
                }
                downY += 5;
            }
            if (pendingStack != null && pendingStack.getStackSize() > 0L) {
                String str = GuiText.Scheduled.getLocal() + ": " + converter.toWideReadableForm(pendingStack.getStackSize());
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftingCPUScheduled.getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.Scheduled.getLocal() + ": " + NumberFormat.getInstance().format(pendingStack.getStackSize()));
                }
            }
            GL11.glPopMatrix();
            int posX = x * 68 + 9 + 67 - 19;
            int posY = y * 23 + 22;
            ItemStack is = refStack.copy().getItemStack();
            if (this.tooltip == z - viewStart) {
                dspToolTip = Platform.getItemDisplayName(is);
                if (lineList.size() > 0) {
                    this.addItemTooltip(refStack, lineList);
                    dspToolTip = dspToolTip + '\n' + Joiner.on((String)"\n").join(lineList);
                }
                toolPosX = x * 68 + 9 + 67 - 8;
                toolPosY = y * 23 + 22;
                this.hoveredStack = is;
            }
            this.drawItem(posX, posY, is);
            if (!this.searchField.getText().isEmpty() && this.goToData.contains(z)) {
                int startX = x * 68 + 9;
                int startY = posY - 4;
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
        if (this.tooltip >= 0 && dspToolTip.length() > 0) {
            this.drawTooltip(toolPosX, toolPosY + 10, dspToolTip);
        }
    }

    protected void addItemTooltip(IAEItemStack refStack, List<String> lineList) {
        if (GuiCraftingCPU.isShiftKeyDown()) {
            ItemStack is = refStack.copy().getItemStack();
            List l = is.getTooltip((EntityPlayer)this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
            if (!l.isEmpty()) {
                l.remove(0);
            }
            lineList.addAll(l);
            if (this.hoveredNbtStack == null || this.hoveredNbtStack.getItem() != is.getItem()) {
                this.hoveredNbtStack = is;
                try {
                    NetworkHandler.instance.sendToServer(new PacketCraftingItemInterface(refStack.copy()));
                }
                catch (Exception exception) {}
            } else {
                NBTTagCompound data = Platform.openNbtData(this.hoveredNbtStack);
                List<DimensionalCoord> blocks = DimensionalCoord.readAsListFromNBT(data);
                ScheduledReason sr = ScheduledReason.values()[data.getInteger("ScheduledReason")];
                if (sr != ScheduledReason.UNDEFINED) {
                    lineList.add(sr.getLocal());
                }
                if (blocks.isEmpty()) {
                    return;
                }
                for (DimensionalCoord blockPos : blocks) {
                    lineList.add(String.format("Dim:%s X:%s Y:%s Z:%s", blockPos.getDimension(), blockPos.x, blockPos.y, blockPos.z));
                }
                lineList.add(GuiText.HoldShiftClick_HIGHLIGHT_INTERFACE.getLocal());
            }
        } else {
            this.hoveredNbtStack = null;
            lineList.add(GuiText.HoldShiftForTooltip.getLocal());
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/craftingcpu.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.drawSearch();
    }

    public void drawSearch() {
        this.bindTexture("guis/searchField.png");
        this.drawTexturedModalRect(this.guiLeft + this.xSize - 101, this.guiTop + 5, 0, 0, 52, 12);
        this.searchField.drawTextBox();
    }

    protected void keyTyped(char character, int key) {
        if (!this.searchField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    public void postUpdate(IAEItemStack is) {
        this.hoveredNbtStack = is.getItemStack();
    }

    public void postUpdate(int remainingOperations) {
        this.remainingOperations.setRemainingOperations(remainingOperations);
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
                    this.handleInput(this.active, l);
                }
                break;
            }
            case 2: {
                for (IAEItemStack l : list) {
                    this.handleInput(this.pending, l);
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
        if (this.hideStored) {
            this.hideStoredSorting();
        }
        this.updateSearchGoToList(false);
        this.setScrollBar();
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

    private long getTotal(IAEItemStack is) {
        IAEItemStack a = this.storage.findPrecise(is);
        IAEItemStack b = this.active.findPrecise(is);
        IAEItemStack c = this.pending.findPrecise(is);
        long total = 0L;
        if (a != null) {
            total += a.getStackSize();
        }
        if (b != null) {
            total += b.getStackSize();
        }
        if (c != null) {
            total += c.getStackSize();
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

    @Override
    public ItemStack getHoveredStack() {
        return this.hoveredStack;
    }

    private void hideStoredSorting() {
        this.visualHiddenStored = new ArrayList<IAEItemStack>();
        for (IAEItemStack refStack : this.visual) {
            if (refStack == null) continue;
            IAEItemStack activeStack = this.active.findPrecise(refStack);
            IAEItemStack pendingStack = this.pending.findPrecise(refStack);
            if ((activeStack == null || activeStack.getStackSize() <= 0L) && (pendingStack == null || pendingStack.getStackSize() <= 0L)) continue;
            this.visualHiddenStored.add(refStack);
        }
    }

    private class RemainingOperations
    implements ITooltip {
        private long refreshTick = System.currentTimeMillis();
        private long lastWorkingTick = 0L;
        private int remainingOperations = 0;

        private RemainingOperations() {
        }

        public long getLastWorkingTick() {
            return this.lastWorkingTick;
        }

        public long getRefreshTick() {
            return this.refreshTick;
        }

        public void setLastWorkingTick(long lastWorkingTick) {
            this.lastWorkingTick = lastWorkingTick;
        }

        public void setRefreshTick(long refreshTick) {
            this.refreshTick = refreshTick;
        }

        @Override
        public String getMessage() {
            return GuiText.RemainingOperations.getLocal();
        }

        @Override
        public int xPos() {
            return GuiCraftingCPU.this.guiLeft + 8 + 200 - this.getStringWidth();
        }

        @Override
        public int yPos() {
            return GuiCraftingCPU.this.guiTop + 7;
        }

        @Override
        public int getWidth() {
            return this.getStringWidth();
        }

        @Override
        public int getHeight() {
            return ((GuiCraftingCPU)GuiCraftingCPU.this).fontRendererObj.FONT_HEIGHT;
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        public void setRemainingOperations(int remainingOperations) {
            this.remainingOperations = remainingOperations;
        }

        public int getRemainingOperations() {
            return this.remainingOperations;
        }

        public int getStringWidth() {
            return GuiCraftingCPU.this.fontRendererObj.getStringWidth(String.valueOf(this.remainingOperations));
        }
    }
}

