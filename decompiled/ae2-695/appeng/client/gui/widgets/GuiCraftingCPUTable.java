/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.item.ItemStack
 *  org.apache.commons.lang3.time.DurationFormatUtils
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerCPUTable;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.util.ReadableNumberConverter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiCraftingCPUTable {
    private final AEBaseGui parent;
    private final ContainerCPUTable container;
    private final Predicate<CraftingCPUStatus> jobMergeable;
    public static final int CPU_TABLE_WIDTH = 94;
    public static int CPU_TABLE_HEIGHT = 164;
    public static int CPU_TABLE_SLOTS = 6;
    public static final int CPU_TABLE_SLOT_XOFF = 100;
    public static final int CPU_TABLE_SLOT_YOFF = 0;
    public static final int CPU_TABLE_SLOT_WIDTH = 67;
    public static final int CPU_TABLE_SLOT_HEIGHT = 23;
    private final GuiScrollbar cpuScrollbar;
    private String selectedCPUName = "";
    private static final DecimalFormat DF = new DecimalFormat("#.##");
    private static int processBarStartColorInt = GuiColors.ProcessBarStartColor.getColor();
    private static final int[] PROCESS_BAR_START_COLOR_INT_ARR = new int[]{processBarStartColorInt >> 24 & 0xFF, processBarStartColorInt >> 16 & 0xFF, processBarStartColorInt >> 8 & 0xFF, processBarStartColorInt & 0xFF};
    private static int processBarMiddleColorInt = GuiColors.ProcessBarMiddleColor.getColor();
    private static final int[] PROCESS_BAR_MIDDLE_COLOR_INT_ARR = new int[]{processBarMiddleColorInt >> 24 & 0xFF, processBarMiddleColorInt >> 16 & 0xFF, processBarMiddleColorInt >> 8 & 0xFF, processBarMiddleColorInt & 0xFF};
    private static int processBarEndColorInt = GuiColors.ProcessBarEndColor.getColor();
    private static final int[] PROCESS_BAR_END_COLOR_INT_ARR = new int[]{processBarEndColorInt >> 24 & 0xFF, processBarEndColorInt >> 16 & 0xFF, processBarEndColorInt >> 8 & 0xFF, processBarEndColorInt & 0xFF};

    public GuiCraftingCPUTable(AEBaseGui parent, ContainerCPUTable container, Predicate<CraftingCPUStatus> jobMergeable) {
        this.parent = parent;
        this.container = container;
        this.jobMergeable = jobMergeable;
        this.cpuScrollbar = new GuiScrollbar();
        this.cpuScrollbar.setLeft(-16);
        this.cpuScrollbar.setTop(19);
        this.cpuScrollbar.setWidth(12);
        this.updateScrollBar();
    }

    private void updateScrollBar() {
        this.cpuScrollbar.setHeight(CPU_TABLE_HEIGHT - 27);
    }

    public ContainerCPUTable getContainer() {
        return this.container;
    }

    public String getSelectedCPUName() {
        return this.selectedCPUName;
    }

    public void drawScreen() {
        List<CraftingCPUStatus> cpus = this.container.getCPUs();
        int selectedCpuSerial = this.container.selectedCpuSerial;
        this.selectedCPUName = null;
        this.cpuScrollbar.setRange(0, Integer.max(0, cpus.size() - CPU_TABLE_SLOTS), 1);
        for (CraftingCPUStatus cpu : cpus) {
            if (cpu.getSerial() != selectedCpuSerial) continue;
            this.selectedCPUName = cpu.getName();
        }
    }

    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY, int guiLeft, int guiTop) {
        if (this.cpuScrollbar != null) {
            this.cpuScrollbar.draw(this.parent);
        }
        List<CraftingCPUStatus> cpus = this.container.getCPUs();
        int selectedCpuSerial = this.container.selectedCpuSerial;
        int firstCpu = this.cpuScrollbar.getCurrentScroll();
        CraftingCPUStatus hoveredCpu = this.hitCpu(mouseX - guiLeft, mouseY - guiTop);
        ItemStack craftingAccelerator = (ItemStack)AEApi.instance().definitions().blocks().craftingAccelerator().maybeStack(1).orNull();
        ItemStack cell64k = (ItemStack)AEApi.instance().definitions().items().cell64k().maybeStack(1).orNull();
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (int i = firstCpu; i < firstCpu + CPU_TABLE_SLOTS; ++i) {
            CraftingCPUStatus cpu;
            if (i < 0 || i >= cpus.size() || (cpu = cpus.get(i)) == null) continue;
            int x = -85;
            int y = 19 + (i - firstCpu) * 23;
            if (cpu.getSerial() == selectedCpuSerial) {
                if (!this.container.getCpuFilter().test(cpu)) {
                    GL11.glColor4f((float)1.0f, (float)0.25f, (float)0.25f, (float)1.0f);
                } else if (this.jobMergeable.test(cpu)) {
                    GL11.glColor4f((float)1.0f, (float)1.0f, (float)0.25f, (float)1.0f);
                } else {
                    GL11.glColor4f((float)0.0f, (float)0.8352f, (float)1.0f, (float)1.0f);
                }
            } else if (hoveredCpu != null && hoveredCpu.getSerial() == cpu.getSerial()) {
                GL11.glColor4f((float)0.65f, (float)0.9f, (float)1.0f, (float)1.0f);
            } else if (!this.container.getCpuFilter().test(cpu)) {
                GL11.glColor4f((float)0.9f, (float)0.65f, (float)0.65f, (float)1.0f);
            } else if (this.jobMergeable.test(cpu)) {
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)0.7f, (float)1.0f);
            } else {
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            }
            this.parent.bindTexture("guis/cpu_selector.png");
            this.parent.drawTexturedModalRect(x, y, 100, 0, 67, 23);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            String name = cpu.getName();
            if (name == null || name.isEmpty()) {
                name = GuiText.CPUs.getLocal() + " #" + NumberFormat.getInstance().format(cpu.getSerial());
            }
            if (name.length() > 12) {
                name = name.substring(0, 11) + "..";
            }
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(x + 3), (float)(y + 3), (float)0.0f);
            GL11.glScalef((float)0.8f, (float)0.8f, (float)1.0f);
            font.drawString(name, 0, 0, GuiColors.CraftingStatusCPUName.getColor());
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(x + 3), (float)(y + 11), (float)0.0f);
            IAEItemStack craftingStack = cpu.getCrafting();
            if (craftingStack != null) {
                int iconIndex = 178;
                this.parent.bindTexture("guis/states.png");
                int uv_y = 11;
                int uv_x = 2;
                GL11.glScalef((float)0.5f, (float)0.5f, (float)1.0f);
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                this.parent.drawTexturedModalRect(0, 0, 32, 176, 16, 16);
                GL11.glTranslatef((float)18.0f, (float)2.0f, (float)0.0f);
                String amount = NumberFormat.getInstance().format(craftingStack.getStackSize());
                double craftingPercentage = (double)(cpu.getTotalItems() - Math.max(cpu.getRemainingItems(), 0L)) / (double)cpu.getTotalItems();
                if (amount.length() > 9) {
                    amount = ReadableNumberConverter.INSTANCE.toWideReadableForm(craftingStack.getStackSize());
                }
                GL11.glScalef((float)1.5f, (float)1.5f, (float)1.0f);
                font.drawString(amount, 0, 0, GuiColors.CraftingStatusCPUAmount.getColor());
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glTranslatef((float)(x + 67 - 19), (float)(y + 3), (float)0.0f);
                this.parent.drawItem(0, 0, craftingStack.getItemStack());
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                AEBaseGui.drawRect((int)x, (int)(y + 23 - 3), (int)(x + (int)(66.0 * craftingPercentage)), (int)(y + 23 - 2), (int)this.calculateGradientColor(craftingPercentage));
            } else {
                this.parent.bindTexture("guis/states.png");
                GL11.glScalef((float)0.5f, (float)0.5f, (float)1.0f);
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                this.parent.drawItem(0, 0, cell64k);
                switch (cpu.allowMode()) {
                    case ALLOW_ALL: {
                        this.parent.drawTexturedModalRect(111, 0, 48, 224, 16, 16);
                        break;
                    }
                    case ONLY_PLAYER: {
                        this.parent.drawTexturedModalRect(111, 0, 64, 224, 16, 16);
                        break;
                    }
                    case ONLY_NONPLAYER: {
                        this.parent.drawTexturedModalRect(111, 0, 80, 224, 16, 16);
                    }
                }
                GL11.glColor4f((float)0.5f, (float)0.5f, (float)0.5f, (float)1.0f);
                this.parent.drawItem(64, 0, craftingAccelerator);
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                GL11.glTranslatef((float)18.0f, (float)6.0f, (float)0.0f);
                GL11.glScalef((float)1.1f, (float)1.1f, (float)1.0f);
                font.drawString(cpu.formatStorage(), 0, 0, GuiColors.CraftingStatusCPUStorage.getColor());
                font.drawString(cpu.formatShorterCoprocessors(), 59, 0, GuiColors.CraftingStatusCPUStorage.getColor());
            }
            GL11.glPopMatrix();
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (hoveredCpu != null) {
            StringBuilder tooltip = new StringBuilder();
            String name = hoveredCpu.getName();
            if (name != null && !name.isEmpty()) {
                tooltip.append(name);
                tooltip.append('\n');
            } else {
                tooltip.append(GuiText.CPUs.getLocal());
                tooltip.append(" #");
                tooltip.append(NumberFormat.getInstance().format(hoveredCpu.getSerial()));
                tooltip.append('\n');
            }
            IAEItemStack crafting = hoveredCpu.getCrafting();
            if (crafting != null && crafting.getStackSize() > 0L) {
                long elapsedInMilliseconds = TimeUnit.MILLISECONDS.convert(hoveredCpu.getCraftingElapsedTime(), TimeUnit.NANOSECONDS);
                String elapsedTimeText = DurationFormatUtils.formatDuration((long)elapsedInMilliseconds, (String)GuiText.ETAFormat.getLocal());
                double craftingPercentage = 100.0 - (double)hoveredCpu.getRemainingItems() / (double)hoveredCpu.getTotalItems() * 100.0;
                tooltip.append(GuiText.Crafting.getLocal());
                tooltip.append(": ");
                tooltip.append(NumberFormat.getInstance().format(crafting.getStackSize()));
                tooltip.append(' ');
                tooltip.append(crafting.getItemStack().getDisplayName());
                tooltip.append('\n');
                tooltip.append(NumberFormat.getInstance().format(hoveredCpu.getRemainingItems()));
                tooltip.append(" / ");
                tooltip.append(NumberFormat.getInstance().format(hoveredCpu.getTotalItems()));
                tooltip.append(String.format(" %02.2f %%", craftingPercentage));
                tooltip.append('\n');
                tooltip.append(GuiText.TimeUsed.getLocal());
                tooltip.append(": ");
                tooltip.append(elapsedTimeText);
                tooltip.append('\n');
            }
            if (hoveredCpu.getUsedStorage() > 0L) {
                tooltip.append(GuiText.BytesUsed.getLocal());
                tooltip.append(": ");
                tooltip.append(hoveredCpu.formatUsedStorage());
                tooltip.append(" / ");
                tooltip.append(hoveredCpu.formatStorage());
                tooltip.append('\n');
            } else if (hoveredCpu.getStorage() > 0L) {
                tooltip.append(GuiText.Bytes.getLocal());
                tooltip.append(": ");
                tooltip.append(hoveredCpu.formatStorage());
                tooltip.append('\n');
            }
            if (hoveredCpu.getCoprocessors() > 0L) {
                tooltip.append(GuiText.CoProcessors.getLocal());
                tooltip.append(": ");
                tooltip.append(hoveredCpu.formatCoprocessors());
                tooltip.append('\n');
            }
            if (tooltip.length() > 0) {
                this.parent.drawTooltip(mouseX - offsetX, mouseY - offsetY, tooltip.toString());
            }
        }
    }

    public void drawBG(int offsetX, int offsetY) {
        this.parent.bindTexture("guis/cpu_selector.png");
        if (CPU_TABLE_HEIGHT != 164) {
            this.parent.drawTexturedModalRect(offsetX - 94, offsetY, 0, 0, 94, 41);
            int y = 41;
            int rows = CPU_TABLE_SLOTS;
            for (int row = 1; row < rows - 1; ++row) {
                this.parent.drawTexturedModalRect(offsetX - 94, offsetY + y, 0, 41, 94, 23);
                y += 23;
            }
            this.parent.drawTexturedModalRect(offsetX - 94, offsetY + y, 0, 133, 94, 31);
        } else {
            this.parent.drawTexturedModalRect(offsetX - 94, offsetY, 0, 0, 94, CPU_TABLE_HEIGHT);
        }
        this.updateScrollBar();
    }

    public CraftingCPUStatus hitCpu(int x, int y) {
        if ((x += 94) < 9 || x >= 76 || y < 19 || y >= 19 + CPU_TABLE_SLOTS * 23) {
            return null;
        }
        int scrollOffset = this.cpuScrollbar != null ? this.cpuScrollbar.getCurrentScroll() : 0;
        int cpuId = scrollOffset + (y - 19) / 23;
        List<CraftingCPUStatus> cpus = this.container.getCPUs();
        return cpuId >= 0 && cpuId < cpus.size() ? cpus.get(cpuId) : null;
    }

    public void mouseClicked(int xCoord, int yCoord, int btn) {
        CraftingCPUStatus hit;
        if (this.cpuScrollbar != null) {
            this.cpuScrollbar.click(this.parent, xCoord, yCoord);
        }
        if ((hit = this.hitCpu(xCoord, yCoord)) != null) {
            this.sendCPUSwitch(hit.getSerial());
        }
    }

    public void sendCPUSwitch(int serial) {
        try {
            NetworkHandler.instance.sendToServer(new PacketValueConfig("CPUTable.Cpu.Set", Integer.toString(serial)));
        }
        catch (IOException e) {
            AELog.warn(e);
        }
    }

    public void mouseClickMove(int xCoord, int yCoord) {
        if (this.cpuScrollbar != null) {
            this.cpuScrollbar.clickMove(yCoord);
        }
    }

    public boolean handleMouseInput(int guiLeft, int guiTop) {
        int x = Mouse.getEventX() * this.parent.width / this.parent.mc.displayWidth;
        int y = this.parent.height - Mouse.getEventY() * this.parent.height / this.parent.mc.displayHeight - 1;
        int dwheel = Mouse.getEventDWheel();
        if ((x -= guiLeft - 94) >= 9 && x < 76 && (y -= guiTop) >= 19 && y < 19 + CPU_TABLE_SLOTS * 23 && this.cpuScrollbar != null && dwheel != 0) {
            this.cpuScrollbar.wheel(dwheel);
            return true;
        }
        return false;
    }

    public boolean hideItemPanelSlot(int x, int y, int w, int h) {
        return (x += 94) + w >= 0 && x <= 94 && y + h >= 0 && y <= CPU_TABLE_HEIGHT;
    }

    public void cycleCPU(boolean backwards) {
        int next_increment;
        int current = this.container.selectedCpuSerial;
        List<CraftingCPUStatus> cpus = this.container.getCPUs();
        int n = next_increment = backwards ? cpus.size() - 1 : 1;
        if (cpus.isEmpty()) {
            return;
        }
        int next = 0;
        for (int i = 0; i < cpus.size(); ++i) {
            if (cpus.get(i).getSerial() != current) continue;
            next = i + next_increment;
            break;
        }
        boolean preferBusy = this.container.isBusyCPUsPreferred();
        for (int i = 0; i < cpus.size(); ++i) {
            CraftingCPUStatus cpu = cpus.get(next %= cpus.size());
            if (preferBusy) {
                if (!cpu.isBusy() || !this.container.getCpuFilter().test(cpu)) continue;
                break;
            }
            if (this.container.getCpuFilter().test(cpu)) break;
            next += next_increment;
        }
        this.sendCPUSwitch(cpus.get(next %= cpus.size()).getSerial());
        if (next < this.cpuScrollbar.getCurrentScroll() || next >= this.cpuScrollbar.getCurrentScroll() + CPU_TABLE_SLOTS) {
            this.cpuScrollbar.setCurrentScroll(next);
        }
    }

    private int calculateGradientColor(double percentage) {
        int[] start = null;
        int[] end = null;
        double ratio = 0.0;
        if (percentage <= 0.5) {
            start = PROCESS_BAR_START_COLOR_INT_ARR;
            end = PROCESS_BAR_MIDDLE_COLOR_INT_ARR;
            ratio = percentage * 2.0;
        } else {
            start = PROCESS_BAR_MIDDLE_COLOR_INT_ARR;
            end = PROCESS_BAR_END_COLOR_INT_ARR;
            ratio = (percentage - 0.5) * 2.0;
        }
        int a = (int)((double)start[0] + ratio * (double)(end[0] - start[0]));
        int r = (int)((double)start[1] + ratio * (double)(end[1] - start[1]));
        int g = (int)((double)start[2] + ratio * (double)(end[2] - start[2]));
        int b = (int)((double)start[3] + ratio * (double)(end[3] - start[3]));
        return a << 24 | r << 16 | g << 8 | b;
    }
}

