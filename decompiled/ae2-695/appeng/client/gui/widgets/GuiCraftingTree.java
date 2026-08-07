/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.shader.Framebuffer
 *  net.minecraft.event.ClickEvent
 *  net.minecraft.event.ClickEvent$Action
 *  net.minecraft.util.ChatComponentText
 *  net.minecraft.util.ChatComponentTranslation
 *  net.minecraft.util.IChatComponent
 *  net.minecraft.util.MathHelper
 *  org.apache.commons.io.FileUtils
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui.widgets;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.resolvers.CraftableItemResolver;
import appeng.crafting.v2.resolvers.EmitableItemResolver;
import appeng.crafting.v2.resolvers.ExtractItemResolver;
import appeng.crafting.v2.resolvers.IgnoreMissingItemResolver;
import appeng.crafting.v2.resolvers.SimulateMissingItemResolver;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.apache.commons.io.FileUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiCraftingTree {
    private final AEBaseGui parent;
    public int widgetX;
    public int widgetY;
    public int widgetW;
    public int widgetH;
    public float scrollX = -8.0f;
    public float scrollY = -8.0f;
    private CraftingRequest<?> request;
    private final TreeMap<Integer, ArrayList<Node>> treeNodes = new TreeMap();
    private int treeWidth;
    private int treeHeight;
    public static final int X_SPACING = 24;
    public static final int REQUEST_RESOLVER_Y_SPACING = 10;
    public static final int RESOLVER_CHILD_Y_SPACING = 12;
    public final int textColor = GuiColors.SearchboxText.getColor();
    private static long animationFrame = System.currentTimeMillis() / 500L;
    private String search = "";
    private ArrayList<Node> goToData = new ArrayList();
    private int searchGotoIndex = -1;
    private Node needHighlight;
    private float zoomLevel = 1.0f;
    private float lastDragX = Float.NEGATIVE_INFINITY;
    private float lastDragY = Float.NEGATIVE_INFINITY;
    private boolean wasLmbPressed;
    private Node tooltipNode;
    private static final DateTimeFormatter SCREENSHOT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    public GuiCraftingTree(AEBaseGui parent, int widgetX, int widgetY, int widgetW, int widgetH) {
        this.parent = parent;
        this.widgetX = widgetX;
        this.widgetY = widgetY;
        this.widgetW = widgetW;
        this.widgetH = widgetH;
    }

    private <T extends IAEStack<T>> IAEStack<T> getDisplayItemForRequest(CraftingRequest<T> request) {
        if (request.usedResolvers.isEmpty()) {
            return request.stack;
        }
        return request.usedResolvers.get((int)((int)(GuiCraftingTree.animationFrame % (long)request.usedResolvers.size()))).resolvedStack.copy().setStackSize(request.stack.getStackSize());
    }

    private String getTaskNodeDescription(TaskNode nd) {
        if (nd.resolver.task instanceof ExtractItemResolver.ExtractItemTask) {
            return GuiText.StoredItems.getLocal();
        }
        if (nd.resolver.task instanceof CraftableItemResolver.CraftFromPatternTask) {
            return GuiText.Crafting.getLocal();
        }
        if (nd.resolver.task instanceof EmitableItemResolver.EmitItemTask) {
            return GuiText.LevelEmitter.getLocal();
        }
        if (nd.resolver.task instanceof SimulateMissingItemResolver.ConjureItemTask || nd.resolver.task instanceof IgnoreMissingItemResolver.IgnoreMissingItemTask) {
            return GuiText.Missing.getLocal();
        }
        return "";
    }

    public void updateSearchGoToList(String s) {
        this.needHighlight = null;
        this.searchGotoIndex = -1;
        this.goToData.clear();
        this.search = s;
        if (this.search.isEmpty()) {
            return;
        }
        for (ArrayList<Node> row : this.treeNodes.values()) {
            for (Node node : row) {
                TaskNode tNode;
                if (node instanceof TaskNode && this.getTaskNodeDescription(tNode = (TaskNode)node).toLowerCase().contains(this.search)) {
                    this.goToData.add(node);
                    continue;
                }
                if (!(node instanceof RequestNode)) continue;
                RequestNode rNode = (RequestNode)node;
                if (!Platform.getItemDisplayName(rNode.request.stack).toLowerCase().contains(this.search)) continue;
                this.goToData.add(node);
            }
        }
        this.searchGoTo(true);
    }

    public void searchGoTo(boolean forward) {
        Node nd;
        if (this.search.isEmpty() || this.goToData.isEmpty()) {
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
        this.needHighlight = nd = this.goToData.get(this.searchGotoIndex);
        int n = nd.x;
        Objects.requireNonNull(nd);
        this.scrollX = n - 16;
        int n2 = nd.y;
        Objects.requireNonNull(nd);
        this.scrollY = n2 - 16;
    }

    public void setRequest(CraftingRequest<?> request) {
        boolean isDifferent = request != this.request;
        this.request = request;
        if (isDifferent) {
            this.treeNodes.clear();
            this.treeWidth = 0;
            this.treeHeight = 0;
            ArrayList<NodeBuilderTask> tasks = new ArrayList<NodeBuilderTask>();
            tasks.add(new NodeBuilderRequestWalker(0, 0, null, request));
            while (!tasks.isEmpty()) {
                ((NodeBuilderTask)tasks.get(tasks.size() - 1)).step(tasks);
            }
            for (ArrayList<Node> row : this.treeNodes.values()) {
                for (Node node : row) {
                    if (node.parentNode != null) {
                        node.parentNode.childNodes.add(node);
                    }
                    int n = node.x;
                    Objects.requireNonNull(node);
                    this.treeWidth = Math.max(this.treeWidth, n + 16);
                    int n2 = node.y;
                    Objects.requireNonNull(node);
                    this.treeHeight = Math.max(this.treeHeight, n2 + 16);
                }
            }
        }
    }

    public void onMouseWheel(int mouseX, int mouseY, int wheel) {
        if (GuiScreen.isShiftKeyDown()) {
            this.scrollX -= (float)(wheel * this.widgetW) / (5.0f * this.zoomLevel);
            this.scrollX = MathHelper.clamp_float((float)this.scrollX, (float)((float)(-this.widgetW) / this.zoomLevel + 4.0f), (float)(this.treeWidth - 4));
        } else if (GuiScreen.isCtrlKeyDown()) {
            float oldZoom = this.zoomLevel;
            this.zoomLevel *= (float)Math.pow(1.4, wheel);
            this.zoomLevel = MathHelper.clamp_float((float)this.zoomLevel, (float)0.1f, (float)8.0f);
            this.scrollX += (float)(mouseX -= this.widgetX) / oldZoom - (float)mouseX / this.zoomLevel;
            this.scrollY += (float)(mouseY -= this.widgetY) / oldZoom - (float)mouseY / this.zoomLevel;
        } else {
            this.scrollY -= (float)(wheel * this.widgetH) / (5.0f * this.zoomLevel);
            this.scrollY = MathHelper.clamp_float((float)this.scrollY, (float)((float)(-this.widgetH) / this.zoomLevel + 4.0f), (float)(this.treeHeight - 4));
        }
    }

    public void draw(int guiMouseX, int guiMouseY) {
        this.draw(guiMouseX, guiMouseY, false);
    }

    public void draw(int guiMouseX, int guiMouseY, boolean inScreenshotMode) {
        if (this.request == null) {
            return;
        }
        animationFrame = System.currentTimeMillis() / 500L;
        float mouseX = (float)Mouse.getX() * (float)this.parent.width / (float)this.parent.mc.displayWidth;
        float mouseY = (float)this.parent.height - (float)Mouse.getY() * (float)this.parent.height / (float)this.parent.mc.displayHeight - 1.0f;
        boolean lmbPressed = Mouse.isButtonDown((int)0);
        if (lmbPressed && !this.wasLmbPressed) {
            if (this.isPointInWidget(guiMouseX - this.parent.getGuiLeft(), guiMouseY - this.parent.getGuiTop())) {
                this.lastDragX = mouseX;
                this.lastDragY = mouseY;
            }
        } else if (!lmbPressed) {
            this.lastDragX = Float.NEGATIVE_INFINITY;
            this.lastDragY = Float.NEGATIVE_INFINITY;
        } else if (lmbPressed && this.lastDragX != Float.NEGATIVE_INFINITY) {
            this.scrollX -= 1.0f / this.zoomLevel * (mouseX - this.lastDragX);
            this.scrollY -= 1.0f / this.zoomLevel * (mouseY - this.lastDragY);
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
        }
        this.scrollX = MathHelper.clamp_float((float)this.scrollX, (float)((float)(-this.widgetW) / this.zoomLevel + 4.0f), (float)(this.treeWidth - 4));
        this.scrollY = MathHelper.clamp_float((float)this.scrollY, (float)((float)(-this.widgetH) / this.zoomLevel + 4.0f), (float)(this.treeHeight - 4));
        this.wasLmbPressed = lmbPressed;
        GL11.glPushAttrib((int)1048575);
        float guiScaleFactor = inScreenshotMode ? 1.0f : (float)this.parent.mc.displayWidth / (float)this.parent.width;
        float zoomLevel = inScreenshotMode ? 1.0f : this.zoomLevel;
        float scrollX = inScreenshotMode ? -8.0f : this.scrollX;
        float scrollY = inScreenshotMode ? -8.0f : this.scrollY;
        int widgetBottomY = this.parent.getYSize() - this.widgetY - this.widgetH;
        float zScrollX = scrollX * zoomLevel;
        float zScrollY = scrollY * zoomLevel;
        int cropYMin = (int)((zScrollY - 32.0f) / zoomLevel) - 16;
        int cropYMax = (int)((zScrollY + 32.0f) / zoomLevel) + (int)((float)this.widgetH / zoomLevel) + 16;
        int cropXMin = (int)((zScrollX - 32.0f) / zoomLevel);
        int cropXMax = (int)((zScrollX + 32.0f) / zoomLevel) + (int)((float)this.widgetW / zoomLevel);
        if (!inScreenshotMode) {
            GL11.glScissor((int)((int)((float)(this.parent.getGuiLeft() + this.widgetX) * guiScaleFactor)), (int)((int)((float)(this.parent.getGuiTop() + widgetBottomY) * guiScaleFactor)), (int)((int)((float)this.widgetW * guiScaleFactor)), (int)((int)((float)this.widgetH * guiScaleFactor)));
            GL11.glEnable((int)3089);
        }
        GL11.glPushMatrix();
        if (!inScreenshotMode) {
            GL11.glTranslatef((float)((float)this.widgetX - zScrollX), (float)((float)this.widgetY - zScrollY), (float)0.0f);
            GL11.glScalef((float)zoomLevel, (float)zoomLevel, (float)1.0f);
        }
        TreeMap<Integer, ArrayList<Node>> rows = inScreenshotMode ? this.treeNodes : this.treeNodes.subMap(cropYMin, cropYMax);
        this.tooltipNode = null;
        block0: for (Map.Entry row : rows.entrySet()) {
            for (Node node : (ArrayList)row.getValue()) {
                if (!node.visible || !inScreenshotMode && node.x <= cropXMin) continue;
                if (!inScreenshotMode && node.x > cropXMax) {
                    if (node.parentNode == null || node.parentNode.x >= cropXMax) continue block0;
                    node.drawParentLine();
                    continue block0;
                }
                node.draw();
                node.drawParentLine();
                int widgetLeft = this.parent.getGuiLeft() + this.widgetX;
                int nodeX = widgetLeft + (int)((float)node.x * zoomLevel) - (int)zScrollX;
                int widgetTop = this.parent.getGuiTop() + this.widgetY;
                int nodeY = widgetTop + (int)((float)node.y * zoomLevel) - (int)zScrollY;
                if (inScreenshotMode || guiMouseX < nodeX || guiMouseY < nodeY) continue;
                Objects.requireNonNull(node);
                if (guiMouseX > nodeX + (int)(16.0f * zoomLevel)) continue;
                Objects.requireNonNull(node);
                if (guiMouseY > nodeY + (int)(16.0f * zoomLevel) || guiMouseX < widgetLeft || guiMouseX > widgetLeft + this.widgetW || guiMouseY < widgetTop || guiMouseY > widgetTop + this.widgetH) continue;
                this.tooltipNode = node;
            }
        }
        GL11.glPopMatrix();
        if (!inScreenshotMode) {
            float scrollXPct = MathHelper.clamp_float((float)(scrollX / (float)this.treeWidth), (float)0.0f, (float)1.0f);
            float scrollYPct = MathHelper.clamp_float((float)(scrollY / (float)this.treeHeight), (float)0.0f, (float)1.0f);
            this.parent.drawHorizontalLine((int)((float)this.widgetX + scrollXPct * (float)(this.widgetW - 24)), (int)((float)this.widgetX + scrollXPct * (float)(this.widgetW - 24)) + 24, this.widgetY + this.widgetH - 2, -2236963);
            this.parent.drawVerticalLine(this.widgetX + this.widgetW - 2, (int)((float)this.widgetY + scrollYPct * (float)(this.widgetH - 24)), (int)((float)this.widgetY + scrollYPct * (float)(this.widgetH - 24)) + 24, -2236963);
        }
        GL11.glPopAttrib();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void saveScreenshot() {
        if (this.request == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!OpenGlHelper.isFramebufferEnabled()) {
            AELog.error("Could not save crafting tree screenshot: FBOs disabled/unsupported", new Object[0]);
            mc.ingameGUI.getChatGUI().printChatMessage((IChatComponent)new ChatComponentTranslation("chat.appliedenergistics2.FBOUnsupported", new Object[0]));
            return;
        }
        try {
            int screenshotZoom = 2;
            File screenshotsDir = new File(mc.mcDataDir, "screenshots");
            FileUtils.forceMkdir((File)screenshotsDir);
            int maxGlTexSize = GL11.glGetInteger((int)3379) / 2;
            int imgWidth = 2 * (this.treeWidth + 32);
            int imgHeight = 2 * (this.treeHeight + 32);
            while ((long)imgWidth * (long)imgHeight >= 0x1FFFFFFFL) {
                if (imgWidth > imgHeight) {
                    imgWidth /= 2;
                    continue;
                }
                imgHeight /= 2;
            }
            int xChunks = (int)Platform.ceilDiv(imgWidth, maxGlTexSize);
            int yChunks = (int)Platform.ceilDiv(imgHeight, maxGlTexSize);
            int fbWidth = Math.min(imgWidth, maxGlTexSize);
            int fbHeight = Math.min(imgHeight, maxGlTexSize);
            BufferedImage outputImg = new BufferedImage(imgWidth, imgHeight, 2);
            IntBuffer downloadBuffer = BufferUtils.createIntBuffer((int)(fbWidth * fbHeight));
            GL11.glPushMatrix();
            GL11.glPushAttrib((int)1048575);
            Framebuffer fb = new Framebuffer(fbWidth, fbHeight, true);
            GL11.glMatrixMode((int)5889);
            GL11.glLoadIdentity();
            GL11.glOrtho((double)0.0, (double)((float)fbWidth / 2.0f), (double)((float)fbHeight / 2.0f), (double)0.0, (double)1000.0, (double)3000.0);
            GL11.glMatrixMode((int)5888);
            GL11.glLoadIdentity();
            GL11.glDisable((int)2929);
            try {
                fb.bindFramebuffer(true);
                for (int xChunk = 0; xChunk < xChunks; ++xChunk) {
                    int xStart = xChunk * maxGlTexSize;
                    int xEnd = Math.min((xChunk + 1) * maxGlTexSize, imgWidth);
                    int xChunkSize = xEnd - xStart;
                    for (int yChunk = 0; yChunk < yChunks; ++yChunk) {
                        int yStart = yChunk * maxGlTexSize;
                        int yEnd = Math.min((yChunk + 1) * maxGlTexSize, imgHeight);
                        int yChunkSize = yEnd - yStart;
                        GL11.glPushMatrix();
                        GL11.glTranslatef((float)(8.0f - (float)xStart / 2.0f), (float)(8.0f - (float)yStart / 2.0f), (float)-2000.0f);
                        GL11.glClear((int)16640);
                        this.draw(-1000, -1000, true);
                        GL11.glPopMatrix();
                        GL11.glBindTexture((int)3553, (int)fb.framebufferTexture);
                        GL11.glGetTexImage((int)3553, (int)0, (int)32993, (int)33639, (IntBuffer)downloadBuffer);
                        for (int y = 0; y < yChunkSize; ++y) {
                            for (int x = 0; x < xChunkSize; ++x) {
                                outputImg.setRGB(x + xStart, y + yStart, downloadBuffer.get((fbHeight - 1 - y) * fbWidth + x));
                            }
                        }
                    }
                }
            }
            finally {
                fb.deleteFramebuffer();
                GL11.glViewport((int)0, (int)0, (int)mc.displayWidth, (int)mc.displayHeight);
            }
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            String date = SCREENSHOT_DATE_FORMAT.format(LocalDateTime.now());
            String filename = String.format("%s-ae2.png", date);
            File outFile = new File(screenshotsDir, filename);
            for (int i = 1; outFile.exists() && i < 99; ++i) {
                filename = String.format("%s-ae2-%d.png", date, i);
                outFile = new File(screenshotsDir, filename);
            }
            if (outFile.exists()) {
                throw new FileAlreadyExistsException(filename);
            }
            ImageIO.write((RenderedImage)outputImg, "png", outFile);
            AELog.info("Saved crafting tree screenshot to %s", filename);
            ChatComponentText chatLink = new ChatComponentText(filename);
            chatLink.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFile.getAbsolutePath()));
            chatLink.getChatStyle().setUnderlined(Boolean.valueOf(true));
            mc.ingameGUI.getChatGUI().printChatMessage((IChatComponent)new ChatComponentTranslation("screenshot.success", new Object[]{chatLink}));
        }
        catch (Exception e) {
            AELog.warn(e, "Could not save crafting tree screenshot");
            mc.ingameGUI.getChatGUI().printChatMessage((IChatComponent)new ChatComponentTranslation("screenshot.failure", new Object[]{e.getMessage()}));
        }
    }

    public void drawTooltip(int guiMouseX, int guiMouseY) {
        if (this.tooltipNode != null) {
            this.tooltipNode.drawTooltip(guiMouseX, guiMouseY);
        }
    }

    private void drawTreeLine(int x0, int y0, int x1, int y1) {
        int color = -2236963;
        if (x0 != x1) {
            int midY = (y0 + y1) / 2;
            this.parent.drawVerticalLine(x0, y0, midY, -2236963);
            this.parent.drawHorizontalLine(x0, x1, midY, -2236963);
            this.parent.drawVerticalLine(x1, midY, y1, -2236963);
        } else {
            this.parent.drawVerticalLine(x0, y0, y1, -2236963);
        }
    }

    private void drawIcon(int x, int y, int iconIndex) {
        this.parent.bindTexture("guis/states.png");
        int uv_y = iconIndex / 16;
        int uv_x = iconIndex - uv_y * 16;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.parent.drawTexturedModalRect(x, y, uv_x * 16, uv_y * 16, 16, 16);
    }

    private void drawSlotOutline(int x, int y, int rgb, boolean isOperation) {
        this.parent.bindTexture("guis/states.png");
        GL11.glColor4f((float)((float)(rgb >> 16 & 0xFF) / 255.0f), (float)((float)(rgb >> 8 & 0xFF) / 255.0f), (float)((float)(rgb & 0xFF) / 255.0f), (float)1.0f);
        this.parent.drawTexturedModalRect(x - 3, y - 3, 192, isOperation ? 32 : 56, 24, 24);
    }

    private void drawStack(int x, int y, IAEStack<?> stack, boolean drawCount) {
        int textColor = GuiColors.SearchboxText.getColor();
        if (stack instanceof IAEItemStack) {
            this.parent.drawItem(x, y, ((IAEItemStack)stack).getItemStack());
        } else if (stack instanceof IAEFluidStack) {
            // empty if block
        }
        if (drawCount) {
            this.drawSmallStackCount(x, y, stack == null ? 0L : stack.getStackSize(), textColor);
        }
    }

    private void drawSmallStackCount(int x, int y, long count, int textColor) {
        if (count < 0L) {
            count = -count;
            textColor = 0xFF0000;
        }
        String countText = ReadableNumberConverter.INSTANCE.toWideReadableForm(count);
        this.drawSmallStackCount(x, y, countText, textColor);
    }

    private void drawSmallStackCount(int x, int y, String countText, int textColor) {
        int countWidth = this.parent.getFontRenderer().getStringWidth(countText);
        GL11.glScalef((float)0.5f, (float)0.5f, (float)1.0f);
        this.parent.getFontRenderer().drawString(countText, x * 2 + 32 - countWidth, y * 2 + 22, textColor, true);
        GL11.glScalef((float)2.0f, (float)2.0f, (float)1.0f);
    }

    public boolean isPointInWidget(int x, int y) {
        return x >= this.widgetX && y >= this.widgetY && x < this.widgetX + this.widgetW && y < this.widgetY + this.widgetH;
    }

    private abstract class Node {
        public boolean visible = true;
        public int x;
        public int y;
        public final int width = 16;
        public final int height = 16;
        public final Node parentNode;
        public final List<Node> childNodes = new ArrayList<Node>(1);

        public final void drawParentLine() {
            if (this.visible && this.parentNode != null) {
                GuiCraftingTree.this.drawTreeLine(this.x + 8, this.y - 3, this.parentNode.x + 8, this.parentNode.y + 18);
            }
        }

        public final void draw() {
            if (this.visible) {
                this.drawImpl();
            }
        }

        public final void drawTooltip(int mouseX, int mouseY) {
            if (this.visible) {
                this.drawTooltipImpl(mouseX, mouseY);
            }
        }

        protected abstract void drawImpl();

        protected abstract void drawTooltipImpl(int var1, int var2);

        public Node(int x, int y, Node parentNode) {
            this.x = x;
            this.y = y;
            this.parentNode = parentNode;
        }
    }

    private class TaskNode
    extends Node {
        public CraftingRequest.UsedResolverEntry<?> resolver;
        private String tooltip;

        public TaskNode(int x, int y, Node parentNode, CraftingRequest.UsedResolverEntry<?> resolver) {
            super(x, y, parentNode);
            this.resolver = resolver;
        }

        @Override
        public void drawImpl() {
            GuiCraftingTree.this.parent.bindTexture("guis/states.png");
            int color = 0x777777;
            if (!GuiCraftingTree.this.search.isEmpty() && GuiCraftingTree.this.goToData.contains(this)) {
                color = GuiCraftingTree.this.needHighlight.equals(this) ? GuiColors.SearchGoToHighlight.getColor() : GuiColors.SearchHighlight.getColor();
            }
            GuiCraftingTree.this.drawSlotOutline(this.x, this.y, color, true);
            List<CraftingRequest<IAEItemStack>> children = null;
            long displayCount = this.resolver.resolvedStack.getStackSize();
            if (this.resolver.task instanceof ExtractItemResolver.ExtractItemTask) {
                ExtractItemResolver.ExtractItemTask task = (ExtractItemResolver.ExtractItemTask)this.resolver.task;
                GuiCraftingTree.this.drawIcon(this.x, this.y, task.removedFromSystem.isEmpty() ? 18 : 17);
            } else if (this.resolver.task instanceof CraftableItemResolver.CraftFromPatternTask) {
                CraftableItemResolver.CraftFromPatternTask task = (CraftableItemResolver.CraftFromPatternTask)this.resolver.task;
                IAEItemStack icon = task.craftingMachine;
                if (icon != null) {
                    GuiCraftingTree.this.drawStack(this.x, this.y, icon, false);
                    GL11.glScalef((float)0.5f, (float)0.5f, (float)1.0f);
                    GuiCraftingTree.this.drawIcon(this.x * 2 - 4, this.y * 2 - 4, 19);
                    GL11.glScalef((float)2.0f, (float)2.0f, (float)1.0f);
                } else {
                    GuiCraftingTree.this.drawIcon(this.x, this.y, 19);
                }
                children = task.getChildRequests();
                displayCount = task.getTotalCraftsDone();
            } else if (this.resolver.task instanceof EmitableItemResolver.EmitItemTask) {
                GuiCraftingTree.this.drawIcon(this.x, this.y, 1);
            } else if (this.resolver.task instanceof SimulateMissingItemResolver.ConjureItemTask) {
                GuiCraftingTree.this.drawIcon(this.x, this.y, 128);
            } else if (this.resolver.task instanceof IgnoreMissingItemResolver.IgnoreMissingItemTask) {
                GuiCraftingTree.this.drawIcon(this.x, this.y, 145);
            }
            GuiCraftingTree.this.drawSmallStackCount(this.x, this.y, displayCount, GuiCraftingTree.this.textColor);
        }

        @Override
        protected void drawTooltipImpl(int mouseX, int mouseY) {
            if (this.tooltip == null) {
                this.tooltip = this.resolver.task.getTooltipText();
            }
            GuiCraftingTree.this.parent.drawTooltip(mouseX, mouseY, this.tooltip);
        }
    }

    private class RequestNode
    extends Node {
        public CraftingRequest<?> request;
        private String tooltip;

        public RequestNode(int x, int y, Node parentNode, CraftingRequest<?> request) {
            super(x, y, parentNode);
            this.tooltip = null;
            this.request = request;
        }

        @Override
        public void drawImpl() {
            int color;
            int n = color = this.request.wasSimulated ? 0xCCAAAA : 0xAAAAAA;
            if (!GuiCraftingTree.this.search.isEmpty() && GuiCraftingTree.this.goToData.contains(this)) {
                color = GuiCraftingTree.this.needHighlight.equals(this) ? GuiColors.SearchGoToHighlight.getColor() : GuiColors.SearchHighlight.getColor();
            }
            GuiCraftingTree.this.drawSlotOutline(this.x, this.y, color, false);
            GuiCraftingTree.this.drawStack(this.x, this.y, GuiCraftingTree.this.getDisplayItemForRequest(this.request), true);
            if (this.request.wasSimulated) {
                GuiCraftingTree.this.parent.bindTexture("guis/states.png");
                GL11.glScalef((float)0.5f, (float)0.5f, (float)1.0f);
                GuiCraftingTree.this.drawIcon(2 * this.x + 16, 2 * this.y, 128);
                GL11.glScalef((float)2.0f, (float)2.0f, (float)1.0f);
            }
        }

        @Override
        protected void drawTooltipImpl(int mouseX, int mouseY) {
            if (this.tooltip == null) {
                this.tooltip = this.request.getTooltipText();
            }
            GuiCraftingTree.this.parent.drawTooltip(mouseX, mouseY, this.tooltip);
        }
    }

    private class NodeBuilderRequestWalker
    extends NodeBuilderTask {
        public final int x;
        public final int y;
        public final CraftingRequest<?> request;
        private int currentChild;
        private RequestNode myNode;
        private Node parentNode;

        private NodeBuilderRequestWalker(int x, int y, Node parentNode, CraftingRequest<?> request) {
            this.currentChild = 0;
            this.x = x;
            this.y = y;
            this.parentNode = parentNode;
            this.request = request;
        }

        @Override
        public void step(List<NodeBuilderTask> stack) {
            if (this.myNode == null) {
                this.myNode = new RequestNode(this.x, this.y, this.parentNode, this.request);
                GuiCraftingTree.this.treeNodes.computeIfAbsent(this.y, ignored -> new ArrayList()).add(this.myNode);
            }
            if (this.currentChild >= this.request.usedResolvers.size()) {
                stack.remove(stack.size() - 1);
            } else {
                CraftingRequest.UsedResolverEntry resolver;
                if (this.currentChild > 0) {
                    GuiCraftingTree.this.treeWidth += 24;
                }
                if ((resolver = this.request.usedResolvers.get(this.currentChild)) != null && resolver.resolvedStack != null) {
                    stack.add(new NodeBuilderTaskWalker(GuiCraftingTree.this.treeWidth, this.y + 16 + 10, this.myNode, resolver));
                }
                ++this.currentChild;
            }
        }
    }

    private abstract class NodeBuilderTask {
        private NodeBuilderTask() {
        }

        public abstract void step(List<NodeBuilderTask> var1);
    }

    private class NodeBuilderTaskWalker
    extends NodeBuilderTask {
        public final int x;
        public final int y;
        public final CraftingRequest.UsedResolverEntry<?> resolver;
        private int currentChild = 0;
        private List<CraftingRequest<IAEItemStack>> children = null;
        private TaskNode myNode;
        private Node parentNode;

        private NodeBuilderTaskWalker(int x, int y, Node parentNode, CraftingRequest.UsedResolverEntry<?> resolver) {
            this.x = x;
            this.y = y;
            this.resolver = resolver;
            this.parentNode = parentNode;
        }

        @Override
        public void step(List<NodeBuilderTask> stack) {
            if (this.myNode == null) {
                this.myNode = new TaskNode(this.x, this.y, this.parentNode, this.resolver);
                this.children = this.resolver.task instanceof CraftableItemResolver.CraftFromPatternTask ? ((CraftableItemResolver.CraftFromPatternTask)this.resolver.task).getChildRequests() : Collections.emptyList();
                GuiCraftingTree.this.treeNodes.computeIfAbsent(this.y, ignored -> new ArrayList()).add(this.myNode);
            }
            if (this.currentChild >= this.children.size()) {
                stack.remove(stack.size() - 1);
            } else {
                if (this.currentChild > 0) {
                    GuiCraftingTree.this.treeWidth += 24;
                }
                stack.add(new NodeBuilderRequestWalker(GuiCraftingTree.this.treeWidth, this.y + 16 + 12, this.myNode, this.children.get(this.currentChild)));
                ++this.currentChild;
            }
        }
    }
}

