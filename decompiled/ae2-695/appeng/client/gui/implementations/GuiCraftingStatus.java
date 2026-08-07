/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  org.lwjgl.input.Mouse
 */
package appeng.client.gui.implementations;

import appeng.api.AEApi;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingCPUTable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.ICraftingCPUTableHolder;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.parts.reporting.PartTerminal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.lwjgl.input.Mouse;

public class GuiCraftingStatus
extends GuiCraftingCPU
implements ICraftingCPUTableHolder {
    private final ContainerCraftingStatus status;
    private GuiButton selectCPU;
    private GuiAeButton follow;
    private final GuiCraftingCPUTable cpuTable;
    private GuiTabButton originalGuiBtn;
    private GuiBridge originalGui;
    private ItemStack myIcon = null;
    private boolean tallMode;
    private GuiImgButton switchTallMode;
    private List<String> playersFollowingCurrentCraft = new ArrayList<String>();

    public GuiCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(new ContainerCraftingStatus(inventoryPlayer, te));
        ItemStack stack;
        Iterator iterator;
        this.status = (ContainerCraftingStatus)this.inventorySlots;
        this.tallMode = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.TALL;
        this.recalculateScreenSize();
        Object target = this.status.getTarget();
        IDefinitions definitions = AEApi.instance().definitions();
        IParts parts = definitions.parts();
        this.cpuTable = new GuiCraftingCPUTable(this, this.status.getCPUTable(), c -> false);
        if (target instanceof WirelessTerminalGuiObject) {
            iterator = definitions.items().wirelessTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                ItemStack wirelessTerminalStack;
                this.myIcon = wirelessTerminalStack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_WIRELESS_TERM;
        }
        if (target instanceof PartTerminal) {
            iterator = parts.terminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_ME;
        }
        if (target instanceof PartCraftingTerminal) {
            iterator = parts.craftingTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
        }
        if (target instanceof PartPatternTerminal) {
            iterator = parts.patternTerminal().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }
        if (target instanceof PartPatternTerminalEx) {
            iterator = parts.patternTerminalEx().maybeStack(1).asSet().iterator();
            while (iterator.hasNext()) {
                this.myIcon = stack = (ItemStack)iterator.next();
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }
    }

    @Override
    public GuiCraftingCPUTable getCPUTable() {
        return this.cpuTable;
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        boolean leftClick = Mouse.isButtonDown((int)0);
        boolean rightClick = Mouse.isButtonDown((int)1);
        if (btn == this.selectCPU) {
            this.cpuTable.cycleCPU(rightClick);
        } else if (btn == this.follow) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("TileCrafting.Follow", this.mc.thePlayer.getCommandSenderName()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        } else if (btn == this.originalGuiBtn) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(this.originalGui));
        } else if (btn == this.switchTallMode) {
            this.tallMode = !this.tallMode;
            AEConfig.instance.getConfigManager().putSetting(Settings.TERMINAL_STYLE, this.tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
            this.switchTallMode.set(this.tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
            this.recalculateScreenSize();
            this.setWorldAndResolution(this.mc, this.width, this.height);
        } else if (btn == this.toggleHideStored) {
            this.setScrollBar();
        }
    }

    @Override
    public void initGui() {
        this.recalculateScreenSize();
        super.initGui();
        this.setScrollBar();
        this.selectCPU = new GuiButton(0, this.guiLeft + 8, this.guiTop + this.ySize - 25, 100, 20, GuiText.CraftingCPU.getLocal() + ": " + GuiText.NoCraftingCPUs);
        this.buttonList.add(this.selectCPU);
        this.follow = new GuiAeButton(1, this.guiLeft + 111, this.guiTop + this.ySize - 25, 50, 20, GuiText.ToFollow.getLocal(), ButtonToolTips.ToFollow.getLocal());
        this.buttonList.add(this.follow);
        if (this.myIcon != null) {
            this.originalGuiBtn = new GuiTabButton(this.guiLeft + 213, this.guiTop - 4, this.myIcon, this.myIcon.getDisplayName(), itemRender);
            this.buttonList.add(this.originalGuiBtn);
            this.originalGuiBtn.setHideEdge(13);
        }
        this.switchTallMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + this.ySize - 18, Settings.TERMINAL_STYLE, this.tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
        this.buttonList.add(this.switchTallMode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.follow.enabled = !this.visual.isEmpty();
        this.cpuTable.drawScreen();
        this.updateCPUButtonText();
        this.updateFollowButtonText();
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.cpuTable.drawFG(offsetX, offsetY, mouseX, mouseY, this.guiLeft, this.guiTop);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/craftingcpu.png");
        if (this.tallMode) {
            this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 41);
            int y = 41;
            for (int row = 1; row < this.rows - 1; ++row) {
                this.drawTexturedModalRect(offsetX, offsetY + y, 0, 41, this.xSize, 23);
                y += 23;
            }
            this.drawTexturedModalRect(offsetX, offsetY + y, 0, 133, this.xSize, 51);
        } else {
            this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        }
        this.cpuTable.drawBG(offsetX, offsetY);
        this.drawSearch();
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        super.mouseClicked(xCoord, yCoord, btn);
        this.cpuTable.mouseClicked(xCoord - this.guiLeft, yCoord - this.guiTop, btn);
    }

    @Override
    protected void mouseClickMove(int x, int y, int c, long d) {
        super.mouseClickMove(x, y, c, d);
        this.cpuTable.mouseClickMove(x - this.guiLeft, y - this.guiTop);
    }

    @Override
    public void handleMouseInput() {
        if (this.cpuTable.handleMouseInput(this.guiLeft, this.guiTop)) {
            return;
        }
        super.handleMouseInput();
    }

    public boolean hideItemPanelSlot(int x, int y, int w, int h) {
        return this.cpuTable.hideItemPanelSlot(x - this.guiLeft, y - this.guiTop, w, h);
    }

    private void updateCPUButtonText() {
        String btnTextText = GuiText.NoCraftingJobs.getLocal();
        int selectedSerial = this.cpuTable.getContainer().selectedCpuSerial;
        if (selectedSerial >= 0) {
            String selectedCPUName = this.cpuTable.getSelectedCPUName();
            if (selectedCPUName != null && selectedCPUName.length() > 0) {
                String name = selectedCPUName.substring(0, Math.min(20, selectedCPUName.length()));
                btnTextText = GuiText.CPUs.getLocal() + ": " + name;
            } else {
                btnTextText = GuiText.CPUs.getLocal() + ": #" + selectedSerial;
            }
        }
        if (this.status.getCPUs().isEmpty()) {
            btnTextText = GuiText.NoCraftingJobs.getLocal();
        }
        this.selectCPU.displayString = btnTextText;
    }

    private void updateFollowButtonText() {
        boolean isFollow = this.playersFollowingCurrentCraft.contains(this.mc.thePlayer.getCommandSenderName());
        this.follow.displayString = isFollow ? GuiText.ToUnfollow.getLocal() : GuiText.ToFollow.getLocal();
        this.follow.setTootipString(isFollow ? ButtonToolTips.ToUnfollow.getLocal() : ButtonToolTips.ToFollow.getLocal());
    }

    @Override
    protected String getGuiDisplayName(String in) {
        return in;
    }

    protected void recalculateScreenSize() {
        int maxAvailableHeight = this.height - 64;
        this.xSize = 238;
        if (this.tallMode) {
            this.rows = (maxAvailableHeight - 42) / 23;
            this.ySize = 47 + this.rows * 23;
        } else {
            this.rows = 6;
            this.ySize = 184;
        }
        GuiCraftingCPUTable.CPU_TABLE_SLOTS = this.rows;
        GuiCraftingCPUTable.CPU_TABLE_HEIGHT = this.rows * 23 + 27;
    }

    private void setScrollBar() {
        int size = this.hideStored ? this.visualHiddenStored.size() : this.visual.size();
        this.getScrollBar().setTop(19).setLeft(218).setHeight(this.ySize - 47);
        this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
    }

    @Override
    public void postUpdate(List<IAEItemStack> list, byte ref) {
        super.postUpdate(list, ref);
        this.setScrollBar();
    }

    public void postUpdate(NBTTagCompound playerNameListNBT) {
        this.playersFollowingCurrentCraft.clear();
        NBTTagList tagList = (NBTTagList)playerNameListNBT.getTag("playNameList");
        for (int index = 0; index < tagList.tagCount(); ++index) {
            this.playersFollowingCurrentCraft.add(tagList.getStringTagAt(index));
        }
    }
}

