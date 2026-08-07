/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiTextField
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.implementations;

import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IGuiTooltipHandler;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerOptimizePatterns;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketOptimizePatterns;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.parts.reporting.PartTerminal;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import com.google.common.base.Joiner;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class GuiOptimizePatterns
extends AEBaseGui
implements IGuiTooltipHandler {
    private GuiTextField amountToCraft;
    private int amountToCraftI = 1;
    private final List<IAEItemStack> visual = new ArrayList<IAEItemStack>();
    private int rows = 5;
    private final boolean tallMode;
    final GuiScrollbar scrollbar;
    private GuiBridge OriginalGui;
    private GuiButton cancel;
    private GuiButton optimize;
    private int tooltip = -1;
    private IAEItemStack hoveredStack;
    private final HashSet<IAEItemStack> ignoreList = new HashSet();
    private final HashMap<IAEItemStack, Integer> multiplierMap = new HashMap();
    Comparator<IAEItemStack> comparator = (i1, i2) -> (int)(i2.getCountRequestableCrafts() - i1.getCountRequestableCrafts());

    public GuiOptimizePatterns(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerOptimizePatterns(inventoryPlayer, te));
        this.tallMode = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.TALL;
        this.xSize = 238;
        this.rows = 5;
        this.ySize = 206;
        this.scrollbar = new GuiScrollbar();
        this.setScrollBar(this.scrollbar);
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
    public void initGui() {
        if (this.tallMode) {
            int maxAvailableHeight = this.height - 64;
            this.rows = (maxAvailableHeight - 91) / 23;
            this.ySize = 91 + this.rows * 23;
        } else {
            this.rows = 5;
            this.ySize = 206;
        }
        super.initGui();
        this.setScrollBar();
        this.optimize = new GuiButton(0, this.guiLeft + this.xSize - 76, this.guiTop + this.ySize - 25, 50, 20, GuiText.Optimize.getLocal());
        this.optimize.enabled = false;
        this.buttonList.add(this.optimize);
        this.cancel = new GuiButton(0, this.guiLeft + 6, this.guiTop + this.ySize - 25, 50, 20, GuiText.Cancel.getLocal());
        this.buttonList.add(this.cancel);
        this.amountToCraft = new GuiTextField(this.fontRendererObj, this.guiLeft + 113, this.guiTop + this.ySize - 68, 100, 20);
        this.amountToCraft.setEnableBackgroundDrawing(true);
        this.amountToCraft.setMaxStringLength(16);
        this.amountToCraft.setTextColor(GuiColors.CraftAmountToCraft.getColor());
        this.amountToCraft.setVisible(true);
        this.amountToCraft.setFocused(true);
        this.amountToCraft.setText("1");
        this.amountToCraft.setSelectionPos(0);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
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
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.PatternOptimizer.getLocal(), 8, 7, GuiColors.CraftConfirmCraftingPlan.getColor());
        this.fontRendererObj.drawString(GuiText.StepsPerCraft.getLocal() + ":", 6, this.ySize - 68 + 10 - this.fontRendererObj.FONT_HEIGHT / 2, GuiColors.CraftConfirmSimulation.getColor());
        String dsp = GuiText.PatternsAffected.getLocal() + ": " + this.multiplierMap.size();
        int offset = (219 - this.fontRendererObj.getStringWidth(dsp)) / 2;
        this.fontRendererObj.drawString(dsp, offset, this.ySize - 41, GuiColors.CraftConfirmSimulation.getColor());
        int viewStart = this.getScrollBar().getCurrentScroll() * 3;
        int viewEnd = viewStart + 3 * this.rows;
        int sectionLength = 67;
        int x = 0;
        int y = 0;
        int xo = 9;
        int yo = 22;
        int offY = 23;
        String dspToolTip = "";
        LinkedList<String> lineList = new LinkedList<String>();
        int toolPosX = 0;
        int toolPosY = 0;
        this.hoveredStack = null;
        for (int z = viewStart; z < Math.min(viewEnd, this.visual.size()); ++z) {
            IAEItemStack refStack = this.visual.get(z);
            if (refStack == null) continue;
            GL11.glPushMatrix();
            GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
            int lines = 1;
            long multipliedBy = this.multiplierMap.getOrDefault(refStack, 0).intValue();
            if (this.amountToCraftI > 0 && multipliedBy > 0L) {
                ++lines;
            }
            int negY = (lines - 1) * 5 / 2;
            int downY = 0;
            String str = GuiText.ToCraftRequests.getLocal() + ": " + ReadableNumberConverter.INSTANCE.toWideReadableForm(refStack.getCountRequestableCrafts());
            int w = 4 + this.fontRendererObj.getStringWidth(str);
            this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftConfirmMissing.getColor());
            if (this.tooltip == z - viewStart) {
                lineList.add(GuiText.ToCraftRequests.getLocal() + ": " + NumberFormat.getInstance().format(refStack.getCountRequestableCrafts()));
            }
            downY += 5;
            if (this.amountToCraftI > 0 && multipliedBy > 0L) {
                str = GuiText.Multiplied.getLocal() + ": x" + ReadableNumberConverter.INSTANCE.toWideReadableForm(1L << (int)multipliedBy);
                w = 4 + this.fontRendererObj.getStringWidth(str);
                this.fontRendererObj.drawString(str, (int)(((double)(x * 68 + 9 + 67 - 19) - (double)w * 0.5) * 2.0), (y * 23 + 22 + 6 - negY + downY) * 2, GuiColors.CraftConfirmMissing.getColor());
                if (this.tooltip == z - viewStart) {
                    lineList.add(GuiText.MultipliedBy.getLocal() + ": " + NumberFormat.getInstance().format(1L << (int)multipliedBy));
                    lineList.add(GuiText.CurrentPatternOutput.getLocal() + ": " + NumberFormat.getInstance().format(refStack.getCountRequestable()));
                    lineList.add(GuiText.NewPatternOutput.getLocal() + ": " + NumberFormat.getInstance().format(refStack.getCountRequestable() << (int)multipliedBy));
                }
                downY += 5;
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
                this.hoveredStack = refStack.copy();
            }
            this.drawItem(posX, posY, is);
            if (this.ignoreList.contains(refStack) || multipliedBy == 0L) {
                int startX = x * 68 + 9;
                int startY = posY - 4;
                GuiOptimizePatterns.drawRect((int)startX, (int)startY, (int)(startX + 67), (int)(startY + 23), (int)GuiColors.CraftConfirmMissingItem.getColor());
            }
            if (++x <= 2) continue;
            ++y;
            x = 0;
        }
        if (this.tooltip >= 0 && !dspToolTip.isEmpty()) {
            this.drawTooltip(toolPosX, toolPosY + 10, dspToolTip);
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/craftingreport.png");
        if (this.tallMode) {
            this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 41);
            int y = 41;
            for (int row = 1; row < this.rows - 1; ++row) {
                this.drawTexturedModalRect(offsetX, offsetY + y, 0, 41, this.xSize, 23);
                y += 23;
            }
            this.drawTexturedModalRect(offsetX, offsetY + y, 0, 110, this.xSize, 96);
        } else {
            this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        }
        this.amountToCraft.drawTextBox();
    }

    protected void keyTyped(char character, int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28 || key == 156) {
                this.actionPerformed(this.optimize);
            }
            this.amountToCraft.textboxKeyTyped(character, key);
            super.keyTyped(character, key);
            String out = this.amountToCraft.getText();
            double resultD = Calculator.conversion(out);
            int resultI = resultD <= 0.0 || Double.isNaN(resultD) ? 0 : (int)ArithHelper.round(resultD, 0);
            this.amountToCraftI = resultI;
            this.updateMultipliers();
            this.optimize.enabled = resultI > 0 && !this.multiplierMap.isEmpty();
        }
    }

    private void updateMultipliers() {
        if (this.amountToCraftI == 0) {
            return;
        }
        this.multiplierMap.clear();
        for (IAEItemStack stack : this.visual) {
            int v;
            if (this.ignoreList.contains(stack) || (v = Math.min(ContainerOptimizePatterns.getBitMultiplier(stack.getCountRequestableCrafts(), stack.getCountRequestable(), this.amountToCraftI), (int)(stack.getStackSize() & 0x1FL))) <= 0) continue;
            this.multiplierMap.put(stack, v);
        }
    }

    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        if (btn == this.cancel) {
            if (this.OriginalGui != null) {
                NetworkHandler.instance.sendToServer(new PacketSwitchGuis(this.OriginalGui));
            }
        } else if (btn == this.optimize && this.optimize.enabled) {
            try {
                NetworkHandler.instance.sendToServer(new PacketOptimizePatterns(this.multiplierMap));
            }
            catch (Throwable e) {
                AELog.debug(e);
            }
        }
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        if (this.hoveredStack != null) {
            if (this.ignoreList.contains(this.hoveredStack)) {
                this.ignoreList.remove(this.hoveredStack);
            } else {
                this.ignoreList.add(this.hoveredStack);
            }
            this.updateMultipliers();
            this.optimize.enabled = this.amountToCraftI > 0 && !this.multiplierMap.isEmpty();
            return;
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    public void postUpdate(List<IAEItemStack> list, byte ref) {
        this.visual.clear();
        for (IAEItemStack stack : list) {
            this.visual.add(stack.copy());
        }
        this.sortItems();
        this.setScrollBar();
        this.updateMultipliers();
        this.optimize.enabled = this.amountToCraftI > 0 && !this.multiplierMap.isEmpty();
    }

    @Override
    public ItemStack getHoveredStack() {
        if (this.hoveredStack != null) {
            return this.hoveredStack.getItemStack();
        }
        return null;
    }

    private void sortItems() {
        this.visual.sort(this.comparator);
    }

    private void setScrollBar() {
        if (this.getScrollBar() == null) {
            this.setScrollBar(this.scrollbar);
        }
        int size = this.visual.size();
        this.getScrollBar().setTop(19).setLeft(218).setHeight(this.ySize - 92);
        this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
    }
}

