/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  codechicken.lib.gui.GuiDraw
 *  codechicken.nei.guihook.GuiContainerManager
 *  com.google.common.base.Joiner
 *  com.google.common.base.Stopwatch
 *  cpw.mods.fml.common.Loader
 *  cpw.mods.fml.common.eventhandler.Event
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.entity.EntityClientPlayerMP
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.entity.RenderItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumChatFormatting
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 *  net.minecraftforge.common.MinecraftForge
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package appeng.client.gui;

import appeng.api.events.GuiScrollEvent;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.me.InternalSlotME;
import appeng.client.me.SlotDisconnected;
import appeng.client.me.SlotME;
import appeng.client.render.AppEngRenderItem;
import appeng.client.render.TranslatedRenderItem;
import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngCraftingSlot;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.OptionalSlotRestrictedInput;
import appeng.container.slot.SlotCraftingTerm;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotInaccessible;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketSwapSlots;
import appeng.helpers.InventoryAction;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.abstraction.INEI;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.guihook.GuiContainerManager;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.Event;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public abstract class AEBaseGui
extends GuiContainer {
    private static boolean switchingGuis;
    private final List<InternalSlotME> meSlots = new LinkedList<InternalSlotME>();
    private final Set<Slot> drag_click = new HashSet<Slot>();
    public static final AppEngRenderItem aeRenderItem;
    public static final TranslatedRenderItem translatedRenderItem;
    private final AEGuiTooltip currentToolTip = new AEGuiTooltip();
    private GuiScrollbar scrollBar = null;
    private boolean disableShiftClick = false;
    private Stopwatch dbl_clickTimer = Stopwatch.createStarted();
    private ItemStack dbl_whichItem;
    private Slot bl_clicked;
    private boolean subGui = switchingGuis;
    private static boolean hasLwjgl3;

    public AEBaseGui(Container container) {
        super(container);
        switchingGuis = false;
        AEBaseGui.aeRenderItem.parent = this;
    }

    protected static String join(Collection<String> toolTip, String delimiter) {
        Joiner joiner = Joiner.on((String)delimiter);
        return joiner.join(toolTip);
    }

    protected int getQty(GuiButton btn) {
        try {
            DecimalFormat df = new DecimalFormat("+#;-#");
            return df.parse(btn.displayString).intValue();
        }
        catch (ParseException e) {
            return 0;
        }
    }

    public boolean isSubGui() {
        return this.subGui;
    }

    public void initGui() {
        super.initGui();
        List<Slot> slots = this.getInventorySlots();
        slots.removeIf(SlotME.class::isInstance);
        for (InternalSlotME me : this.meSlots) {
            slots.add(new SlotME(me));
        }
    }

    protected List<Slot> getInventorySlots() {
        return this.inventorySlots.inventorySlots;
    }

    public void drawScreen(int mouseX, int mouseY, float btn) {
        super.drawScreen(mouseX, mouseY, btn);
        for (Object c : this.buttonList) {
            if (!(c instanceof ITooltip)) continue;
            this.handleTooltip(mouseX, mouseY, (ITooltip)c);
        }
        this.currentToolTip.draw();
    }

    protected void handleTooltip(int mouseX, int mouseY, ITooltip tooltip) {
        int x = tooltip.xPos();
        int y = tooltip.yPos();
        if (tooltip.isVisible() && x < mouseX && x + tooltip.getWidth() > mouseX && y < mouseY && y + tooltip.getHeight() > mouseY) {
            this.drawTooltip(x + 11, Math.max(y, 15) + 4, tooltip.getMessage());
        }
    }

    @Deprecated
    public void drawTooltip(int x, int y, int forceWidth, String message) {
        this.drawTooltip(x, y, message);
    }

    public void drawTooltip(int x, int y, String message) {
        if (message != null && !message.isEmpty()) {
            this.drawTooltip(x, y, message.split("\n"));
        }
    }

    public void drawTooltip(int x, int y, String[] lines) {
        if (lines != null && lines.length > 0) {
            this.currentToolTip.set(x, y, lines);
        }
    }

    public void addTexturedRectToTesselator(float x0, float y0, float x1, float y1, float zLevel, float u0, float v0, float u1, float v1) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.addVertexWithUV((double)x0, (double)y1, (double)this.zLevel, (double)u0, (double)v1);
        tessellator.addVertexWithUV((double)x1, (double)y1, (double)this.zLevel, (double)u1, (double)v1);
        tessellator.addVertexWithUV((double)x1, (double)y0, (double)this.zLevel, (double)u1, (double)v0);
        tessellator.addVertexWithUV((double)x0, (double)y0, (double)this.zLevel, (double)u0, (double)v0);
    }

    public void drawTextured9PatchRect(int x, int y, int width, int height, int textureX, int textureY, int textureW, int textureH) {
        float uvScale = 0.00390625f;
        float x03 = x;
        float x13 = (float)x + (float)textureW / 3.0f;
        float x23 = (float)(x + width) - (float)textureW / 3.0f;
        float x33 = x + width;
        float y03 = y;
        float y13 = (float)y + (float)textureH / 3.0f;
        float y23 = (float)(y + height) - (float)textureH / 3.0f;
        float y33 = y + height;
        float u03 = 0.00390625f * (float)textureX;
        float u13 = 0.00390625f * ((float)textureX + (float)textureW / 3.0f);
        float u23 = 0.00390625f * ((float)textureX + (float)(2 * textureW) / 3.0f);
        float u33 = 0.00390625f * (float)(textureX + textureW);
        float v03 = 0.00390625f * (float)textureY;
        float v13 = 0.00390625f * ((float)textureY + (float)textureH / 3.0f);
        float v23 = 0.00390625f * ((float)textureY + (float)(2 * textureH) / 3.0f);
        float v33 = 0.00390625f * (float)(textureY + textureH);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        this.addTexturedRectToTesselator(x03, y03, x13, y13, this.zLevel, u03, v03, u13, v13);
        this.addTexturedRectToTesselator(x13, y03, x23, y13, this.zLevel, u13, v03, u23, v13);
        this.addTexturedRectToTesselator(x23, y03, x33, y13, this.zLevel, u23, v03, u33, v13);
        this.addTexturedRectToTesselator(x03, y13, x13, y23, this.zLevel, u03, v13, u13, v23);
        this.addTexturedRectToTesselator(x13, y13, x23, y23, this.zLevel, u13, v13, u23, v23);
        this.addTexturedRectToTesselator(x23, y13, x33, y23, this.zLevel, u23, v13, u33, v23);
        this.addTexturedRectToTesselator(x03, y23, x13, y33, this.zLevel, u03, v23, u13, v33);
        this.addTexturedRectToTesselator(x13, y23, x23, y33, this.zLevel, u13, v23, u23, v33);
        this.addTexturedRectToTesselator(x23, y23, x33, y33, this.zLevel, u23, v23, u33, v33);
        tessellator.draw();
    }

    protected final void drawGuiContainerForegroundLayer(int x, int y) {
        int ox = this.guiLeft;
        int oy = this.guiTop;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (this.getScrollBar() != null) {
            this.getScrollBar().draw(this);
        }
        this.currentToolTip.shift(ox, oy);
        this.drawFG(ox, oy, x, y);
        this.currentToolTip.shift(0, 0);
    }

    public abstract void drawFG(int var1, int var2, int var3, int var4);

    protected final void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        int ox = this.guiLeft;
        int oy = this.guiTop;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.drawBG(ox, oy, x, y);
        List<Slot> slots = this.getInventorySlots();
        for (Slot slot : slots) {
            OptionalSlotFake fs;
            if (!(slot instanceof OptionalSlotFake) || !(fs = (OptionalSlotFake)slot).renderDisabled()) continue;
            if (fs.isEnabled()) {
                this.drawTexturedModalRect(ox + fs.xDisplayPosition - 1, oy + fs.yDisplayPosition - 1, fs.getSourceX() - 1, fs.getSourceY() - 1, 18, 18);
                continue;
            }
            GL11.glPushAttrib((int)1048575);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)0.4f);
            GL11.glEnable((int)3042);
            this.drawTexturedModalRect(ox + fs.xDisplayPosition - 1, oy + fs.yDisplayPosition - 1, fs.getSourceX() - 1, fs.getSourceY() - 1, 18, 18);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GL11.glPopAttrib();
        }
    }

    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.drag_click.clear();
        if (btn == 1) {
            for (Object o : this.buttonList) {
                GuiButton guibutton = (GuiButton)o;
                if (!guibutton.mousePressed(this.mc, xCoord, yCoord)) continue;
                super.mouseClicked(xCoord, yCoord, 0);
                return;
            }
        }
        if (this.getScrollBar() != null) {
            this.getScrollBar().click(this, xCoord - this.guiLeft, yCoord - this.guiTop);
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    protected void mouseClickMove(int x, int y, int c, long d) {
        Slot slot = this.getSlot(x, y);
        ItemStack itemstack = this.mc.thePlayer.inventory.getItemStack();
        if (this.getScrollBar() != null) {
            this.getScrollBar().clickMove(y - this.guiTop);
        }
        if (slot instanceof SlotFake && itemstack != null) {
            this.drag_click.add(slot);
            if (this.drag_click.size() > 1) {
                for (Slot dr : this.drag_click) {
                    PacketInventoryAction p = new PacketInventoryAction(c == 0 ? InventoryAction.PICKUP_OR_SET_DOWN : InventoryAction.PLACE_SINGLE, dr.slotNumber, 0L);
                    NetworkHandler.instance.sendToServer(p);
                }
            }
        } else if (slot instanceof SlotDisconnected) {
            this.drag_click.add(slot);
            if (this.drag_click.size() > 1) {
                if (itemstack != null) {
                    for (Slot dr : this.drag_click) {
                        if (slot.getStack() != null) continue;
                        InventoryAction action = InventoryAction.SPLIT_OR_PLACE_SINGLE;
                        PacketInventoryAction p = new PacketInventoryAction(action, dr.getSlotIndex(), ((SlotDisconnected)slot).getSlot().getId());
                        NetworkHandler.instance.sendToServer(p);
                    }
                } else if (AEBaseGui.isShiftKeyDown()) {
                    for (Slot dr : this.drag_click) {
                        InventoryAction action = null;
                        if (slot.getStack() != null) {
                            action = InventoryAction.SHIFT_CLICK;
                        }
                        if (action == null) continue;
                        PacketInventoryAction p = new PacketInventoryAction(action, dr.getSlotIndex(), ((SlotDisconnected)slot).getSlot().getId());
                        NetworkHandler.instance.sendToServer(p);
                    }
                }
            }
        } else {
            super.mouseClickMove(x, y, c, d);
        }
    }

    protected void handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (mouseButton == 3) {
            if ((slot instanceof OptionalSlotFake || slot instanceof SlotFakeCraftingMatrix) && slot.getHasStack()) {
                InventoryAction action = InventoryAction.SET_PATTERN_VALUE;
                if (AEBaseGui.isCtrlKeyDown()) {
                    action = InventoryAction.RENAME_PATTERN_ITEM;
                }
                AEItemStack stack = AEItemStack.create(slot.getStack());
                ((AEBaseContainer)this.inventorySlots).setTargetStack(stack);
                PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0L);
                NetworkHandler.instance.sendToServer(p);
                return;
            }
        } else if (slot instanceof SlotFake) {
            InventoryAction action;
            InventoryAction inventoryAction = action = ctrlDown == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE : InventoryAction.PICKUP_OR_SET_DOWN;
            if (this.drag_click.size() > 1) {
                return;
            }
            PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0L);
            NetworkHandler.instance.sendToServer(p);
            return;
        }
        if (slot instanceof SlotPatternTerm) {
            if (mouseButton == 6) {
                return;
            }
            try {
                NetworkHandler.instance.sendToServer(((SlotPatternTerm)slot).getRequest(AEBaseGui.isShiftKeyDown()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        } else if (slot instanceof SlotCraftingTerm) {
            if (mouseButton == 6) {
                return;
            }
            InventoryAction action = null;
            action = AEBaseGui.isShiftKeyDown() ? InventoryAction.CRAFT_SHIFT : (mouseButton == 1 ? InventoryAction.CRAFT_STACK : InventoryAction.CRAFT_ITEM);
            PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0L);
            NetworkHandler.instance.sendToServer(p);
            return;
        }
        if (Keyboard.isKeyDown((int)57) && this.enableSpaceClicking() && !(slot instanceof SlotPatternTerm)) {
            IAEItemStack stack = null;
            if (slot instanceof SlotME) {
                stack = ((SlotME)slot).getAEStack();
            }
            int slotNum = this.getInventorySlots().size();
            if (!(slot instanceof SlotME) && slot != null) {
                slotNum = slot.slotNumber;
            }
            ((AEBaseContainer)this.inventorySlots).setTargetStack(stack);
            PacketInventoryAction p = new PacketInventoryAction(InventoryAction.MOVE_REGION, slotNum, 0L);
            NetworkHandler.instance.sendToServer(p);
            return;
        }
        if (slot instanceof SlotDisconnected) {
            if (this.drag_click.size() > 1) {
                return;
            }
            InventoryAction action = null;
            switch (mouseButton) {
                case 0: {
                    ItemStack heldStack = player.inventory.getItemStack();
                    if (slot.getStack() == null && heldStack != null) {
                        action = InventoryAction.SPLIT_OR_PLACE_SINGLE;
                        break;
                    }
                    if (slot.getStack() == null || heldStack != null && heldStack.stackSize > 1) break;
                    action = InventoryAction.PICKUP_OR_SET_DOWN;
                    break;
                }
                case 1: {
                    action = ctrlDown == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
                    break;
                }
                case 3: {
                    if (!player.capabilities.isCreativeMode) break;
                    action = InventoryAction.CREATIVE_DUPLICATE;
                    break;
                }
            }
            if (action != null) {
                PacketInventoryAction p = new PacketInventoryAction(action, slot.getSlotIndex(), ((SlotDisconnected)slot).getSlot().getId());
                NetworkHandler.instance.sendToServer(p);
            }
            return;
        }
        if (slot instanceof SlotME) {
            SlotME sme = (SlotME)slot;
            InventoryAction action = null;
            IAEItemStack stack = null;
            switch (mouseButton) {
                case 0: {
                    InventoryAction inventoryAction = action = ctrlDown == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE : InventoryAction.PICKUP_OR_SET_DOWN;
                    if (sme.isPin() && player.inventory.getItemStack() != null) {
                        PacketInventoryAction p = new PacketInventoryAction(InventoryAction.SET_PIN, sme.getPinIndex(), 0L);
                        NetworkHandler.instance.sendToServer(p);
                        return;
                    }
                    stack = sme.getAEStack();
                    if (stack == null || action != InventoryAction.PICKUP_OR_SET_DOWN || stack.getStackSize() != 0L || player.inventory.getItemStack() != null) break;
                    action = InventoryAction.AUTO_CRAFT;
                    break;
                }
                case 1: {
                    if (sme.isPin() && ctrlDown == 1) {
                        PacketInventoryAction p = new PacketInventoryAction(InventoryAction.SET_PIN, sme.getPinIndex(), -1L);
                        NetworkHandler.instance.sendToServer(p);
                        return;
                    }
                    action = ctrlDown == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
                    stack = ((SlotME)slot).getAEStack();
                    break;
                }
                case 3: {
                    IAEItemStack slotItem;
                    stack = ((SlotME)slot).getAEStack();
                    if (stack != null && stack.isCraftable()) {
                        action = InventoryAction.AUTO_CRAFT;
                        break;
                    }
                    if (!player.capabilities.isCreativeMode || (slotItem = ((SlotME)slot).getAEStack()) == null) break;
                    action = InventoryAction.CREATIVE_DUPLICATE;
                    break;
                }
            }
            if (action != null) {
                ((AEBaseContainer)this.inventorySlots).setTargetStack(stack);
                PacketInventoryAction p = new PacketInventoryAction(action, this.getInventorySlots().size(), 0L);
                NetworkHandler.instance.sendToServer(p);
            }
            return;
        }
        if (!this.disableShiftClick && AEBaseGui.isShiftKeyDown()) {
            this.disableShiftClick = true;
            if (this.dbl_whichItem == null || this.bl_clicked != slot || this.dbl_clickTimer.elapsed(TimeUnit.MILLISECONDS) > 150L) {
                this.bl_clicked = slot;
                this.dbl_clickTimer = Stopwatch.createStarted();
                this.dbl_whichItem = slot != null ? (slot.getHasStack() ? slot.getStack().copy() : null) : null;
            } else if (this.dbl_whichItem != null) {
                List<Slot> slots = this.getInventorySlots();
                for (Slot inventorySlot : slots) {
                    if (inventorySlot == null || !inventorySlot.canTakeStack((EntityPlayer)this.mc.thePlayer) || !inventorySlot.getHasStack() || inventorySlot.inventory != slot.inventory || !Container.func_94527_a((Slot)inventorySlot, (ItemStack)this.dbl_whichItem, (boolean)true)) continue;
                    this.handleMouseClick(inventorySlot, inventorySlot.slotNumber, ctrlDown, 1);
                }
            }
            this.disableShiftClick = false;
        }
        super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
    }

    protected boolean checkHotbarKeys(int keyCode) {
        if (this.mc.thePlayer.inventory.getItemStack() == null && this.theSlot != null) {
            for (int j = 0; j < 9; ++j) {
                if (keyCode != this.mc.gameSettings.keyBindsHotbar[j].getKeyCode()) continue;
                List<Slot> slots = this.getInventorySlots();
                for (Slot s : slots) {
                    if (s.getSlotIndex() != j || s.inventory != ((AEBaseContainer)this.inventorySlots).getPlayerInv() || s.canTakeStack(((AEBaseContainer)this.inventorySlots).getPlayerInv().player)) continue;
                    return false;
                }
                if (this.theSlot.getSlotStackLimit() == 64) {
                    this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, j, 2);
                    return true;
                }
                for (Slot s : slots) {
                    if (s.getSlotIndex() != j || s.inventory != ((AEBaseContainer)this.inventorySlots).getPlayerInv()) continue;
                    NetworkHandler.instance.sendToServer(new PacketSwapSlots(s.slotNumber, this.theSlot.slotNumber));
                    return true;
                }
            }
        }
        return false;
    }

    public void onGuiClosed() {
        super.onGuiClosed();
        this.subGui = true;
    }

    protected Slot getSlot(int mouseX, int mouseY) {
        List<Slot> slots = this.getInventorySlots();
        for (Slot slot : slots) {
            if (!this.func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) continue;
            return slot;
        }
        return null;
    }

    public abstract void drawBG(int var1, int var2, int var3, int var4);

    public void handleMouseInput() {
        int y;
        int x;
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        if (!hasLwjgl3) {
            wheel = wheel > 0 ? (int)Platform.ceilDiv(wheel, 120L) : -((int)Platform.ceilDiv(-wheel, 120L));
        }
        if (MinecraftForge.EVENT_BUS.post((Event)new GuiScrollEvent(this, x = Mouse.getEventX() * this.width / this.mc.displayWidth, y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1, wheel))) {
            return;
        }
        if (!this.mouseWheelEvent(x, y, wheel) && this.getScrollBar() != null) {
            GuiScrollbar scrollBar = this.getScrollBar();
            if (x > this.guiLeft && y - this.guiTop > scrollBar.getTop() && x <= this.guiLeft + this.xSize && y - this.guiTop <= scrollBar.getTop() + scrollBar.getHeight()) {
                this.getScrollBar().wheel(wheel);
            }
        }
    }

    protected boolean mouseWheelEvent(int x, int y, int wheel) {
        IAEItemStack item;
        if (!AEBaseGui.isShiftKeyDown()) {
            return false;
        }
        Slot slot = this.getSlot(x, y);
        if (slot instanceof SlotME && (item = ((SlotME)slot).getAEStack()) != null) {
            ((AEBaseContainer)this.inventorySlots).setTargetStack(item);
            InventoryAction direction = wheel > 0 ? InventoryAction.ROLL_DOWN : InventoryAction.ROLL_UP;
            int times = Math.abs(wheel);
            int inventorySize = this.getInventorySlots().size();
            for (int h = 0; h < times; ++h) {
                PacketInventoryAction p = new PacketInventoryAction(direction, inventorySize, 0L);
                NetworkHandler.instance.sendToServer(p);
            }
        }
        return true;
    }

    protected boolean enableSpaceClicking() {
        return true;
    }

    public void bindTexture(String base, String file) {
        ResourceLocation loc = new ResourceLocation(base, "textures/" + file);
        this.mc.getTextureManager().bindTexture(loc);
    }

    public void drawItem(int x, int y, ItemStack is) {
        this.zLevel = 100.0f;
        AEBaseGui.itemRender.zLevel = 100.0f;
        GL11.glPushAttrib((int)1048575);
        GL11.glEnable((int)2896);
        GL11.glEnable((int)32826);
        GL11.glEnable((int)2929);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)101.0f);
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.renderEngine, is, x, y);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-101.0f);
        GL11.glPopAttrib();
        AEBaseGui.itemRender.zLevel = 0.0f;
        this.zLevel = 0.0f;
    }

    protected String getGuiDisplayName(String in) {
        return this.hasCustomInventoryName() ? this.getInventoryName() : in;
    }

    private boolean hasCustomInventoryName() {
        if (this.inventorySlots instanceof AEBaseContainer) {
            return ((AEBaseContainer)this.inventorySlots).getCustomName() != null;
        }
        return false;
    }

    private String getInventoryName() {
        return ((AEBaseContainer)this.inventorySlots).getCustomName();
    }

    private void drawSlot(Slot s) {
        if (s instanceof SlotME || s instanceof SlotFake) {
            SlotME sme;
            if (s instanceof SlotME && (sme = (SlotME)s).isPin() && !sme.getHasStack()) {
                this.drawTextureOnSlot(s, sme.getPinIcon(), sme.getOpacityOfIcon());
            }
            IAEItemStack stack = Platform.getAEStackInSlot(s);
            if (s instanceof SlotFake && stack != null && stack.getStackSize() == 1L) {
                this.safeDrawSlot(s);
                return;
            }
            RenderItem pIR = this.setItemRender(aeRenderItem);
            try {
                if (!this.isPowered()) {
                    this.zLevel = 100.0f;
                    AEBaseGui.itemRender.zLevel = 100.0f;
                    GL11.glDisable((int)2896);
                    AEBaseGui.drawRect((int)s.xDisplayPosition, (int)s.yDisplayPosition, (int)(16 + s.xDisplayPosition), (int)(16 + s.yDisplayPosition), (int)GuiColors.ItemSlotOverlayUnpowered.getColor());
                    GL11.glEnable((int)2896);
                    this.zLevel = 0.0f;
                    AEBaseGui.itemRender.zLevel = 0.0f;
                } else {
                    aeRenderItem.setAeStack(Platform.getAEStackInSlot(s));
                    this.drawAESlot(s);
                }
            }
            catch (Exception err) {
                AELog.warn("[AppEng] AE prevented crash while drawing slot: " + err.toString(), new Object[0]);
            }
            this.setItemRender(pIR);
            return;
        }
        try {
            AppEngSlot aes;
            ItemStack is = s.getStack();
            if (s instanceof AppEngSlot && ((aes = (AppEngSlot)s).renderIconWithItem() || is == null) && aes.shouldDisplay()) {
                this.drawTextureOnSlot(s, aes.getIcon(), aes.getOpacityOfIcon());
            }
            if (is != null && s instanceof AppEngSlot) {
                if (((AppEngSlot)s).getIsValid() == AppEngSlot.hasCalculatedValidness.NotAvailable) {
                    boolean isValid;
                    boolean bl = isValid = s.isItemValid(is) || s instanceof SlotOutput || s instanceof AppEngCraftingSlot || s instanceof SlotDisabled || s instanceof SlotInaccessible || s instanceof SlotRestrictedInput || s instanceof SlotDisconnected;
                    if (isValid && s instanceof SlotRestrictedInput) {
                        try {
                            isValid = ((SlotRestrictedInput)s).isValid(is, (World)this.mc.theWorld);
                        }
                        catch (Exception err) {
                            AELog.debug(err);
                        }
                    }
                    ((AppEngSlot)s).setIsValid(isValid ? AppEngSlot.hasCalculatedValidness.Valid : AppEngSlot.hasCalculatedValidness.Invalid);
                }
                if (((AppEngSlot)s).getIsValid() == AppEngSlot.hasCalculatedValidness.Invalid) {
                    this.zLevel = 100.0f;
                    AEBaseGui.itemRender.zLevel = 100.0f;
                    GL11.glDisable((int)2896);
                    AEBaseGui.drawRect((int)s.xDisplayPosition, (int)s.yDisplayPosition, (int)(16 + s.xDisplayPosition), (int)(16 + s.yDisplayPosition), (int)GuiColors.ItemSlotOverlayInvalid.getColor());
                    GL11.glEnable((int)2896);
                    this.zLevel = 0.0f;
                    AEBaseGui.itemRender.zLevel = 0.0f;
                }
            }
            if (s instanceof AppEngSlot) {
                ((AppEngSlot)s).setDisplay(true);
                this.drawMCSlot(s);
            } else {
                this.safeDrawSlot(s);
            }
            return;
        }
        catch (Exception err) {
            AELog.warn("[AppEng] AE prevented crash while drawing slot: " + err.toString(), new Object[0]);
            this.safeDrawSlot(s);
            return;
        }
    }

    public void drawTextureOnSlot(Slot s, int icon, float opacity) {
        if (icon < 0) {
            return;
        }
        this.bindTexture("guis/states.png");
        GL11.glPushAttrib((int)1048575);
        Tessellator tessellator = Tessellator.instance;
        try {
            int uv_y = (int)Math.floor((double)icon / 16.0);
            int uv_x = icon - uv_y * 16;
            GL11.glEnable((int)3042);
            GL11.glDisable((int)2896);
            GL11.glEnable((int)3553);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            float par1 = s.xDisplayPosition;
            float par2 = s.yDisplayPosition;
            float par3 = uv_x * 16;
            float par4 = uv_y * 16;
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_F(1.0f, 1.0f, 1.0f, opacity);
            float f1 = 0.00390625f;
            float f = 0.00390625f;
            float par6 = 16.0f;
            tessellator.addVertexWithUV((double)(par1 + 0.0f), (double)(par2 + 16.0f), (double)this.zLevel, (double)((par3 + 0.0f) * 0.00390625f), (double)((par4 + 16.0f) * 0.00390625f));
            float par5 = 16.0f;
            tessellator.addVertexWithUV((double)(par1 + 16.0f), (double)(par2 + 16.0f), (double)this.zLevel, (double)((par3 + 16.0f) * 0.00390625f), (double)((par4 + 16.0f) * 0.00390625f));
            tessellator.addVertexWithUV((double)(par1 + 16.0f), (double)(par2 + 0.0f), (double)this.zLevel, (double)((par3 + 16.0f) * 0.00390625f), (double)((par4 + 0.0f) * 0.00390625f));
            tessellator.addVertexWithUV((double)(par1 + 0.0f), (double)(par2 + 0.0f), (double)this.zLevel, (double)((par3 + 0.0f) * 0.00390625f), (double)((par4 + 0.0f) * 0.00390625f));
            tessellator.setColorRGBA_F(1.0f, 1.0f, 1.0f, 1.0f);
            tessellator.draw();
        }
        catch (Exception exception) {
            // empty catch block
        }
        GL11.glPopAttrib();
    }

    public void drawMCSlot(Slot slotIn) {
        int i = slotIn.xDisplayPosition;
        int j = slotIn.yDisplayPosition;
        ItemStack itemstack = slotIn.getStack();
        String s = null;
        GL11.glEnable((int)2929);
        GuiContainerManager.getManager().renderSlotUnderlay(slotIn);
        AEBaseGui.translatedRenderItem.zLevel = 100.0f;
        translatedRenderItem.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), itemstack, i, j);
        AEBaseGui.translatedRenderItem.zLevel = 200.0f;
        translatedRenderItem.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), itemstack, i, j, s, slotIn instanceof OptionalSlotRestrictedInput ? AEConfig.instance.getTerminalFontSize() : null);
        GuiContainerManager.getManager().renderSlotOverlay(slotIn);
        AEBaseGui.translatedRenderItem.zLevel = 0.0f;
    }

    public void drawAESlot(Slot slotIn) {
        int i = slotIn.xDisplayPosition;
        int j = slotIn.yDisplayPosition;
        ItemStack itemstack = slotIn.getStack();
        String s = null;
        this.zLevel = 100.0f;
        AEBaseGui.itemRender.zLevel = 100.0f;
        itemRender.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), itemstack, i, j);
        AEBaseGui.itemRender.zLevel = 0.0f;
        this.zLevel = 0.0f;
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)200.0f);
        aeRenderItem.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), itemstack, i, j, s);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-200.0f);
    }

    private RenderItem setItemRender(RenderItem item) {
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI)) {
            return ((INEI)IntegrationRegistry.INSTANCE.getInstance(IntegrationType.NEI)).setItemRender(item);
        }
        RenderItem ri = itemRender;
        itemRender = item;
        return ri;
    }

    protected boolean isPowered() {
        return true;
    }

    private void safeDrawSlot(Slot s) {
        try {
            GuiContainer.class.getDeclaredMethod("func_146977_a_original", Slot.class).invoke((Object)this, s);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void bindTexture(String file) {
        ResourceLocation loc = new ResourceLocation("appliedenergistics2", "textures/" + file);
        this.mc.getTextureManager().bindTexture(loc);
    }

    public void bindTexture(ResourceLocation loc) {
        this.mc.getTextureManager().bindTexture(loc);
    }

    public void func_146977_a(Slot s) {
        this.drawSlot(s);
    }

    protected GuiScrollbar getScrollBar() {
        return this.scrollBar;
    }

    protected void setScrollBar(GuiScrollbar myScrollBar) {
        this.scrollBar = myScrollBar;
    }

    protected List<InternalSlotME> getMeSlots() {
        return this.meSlots;
    }

    public static final synchronized boolean isSwitchingGuis() {
        return switchingGuis;
    }

    public static final synchronized void setSwitchingGuis(boolean switchingGuis) {
        AEBaseGui.switchingGuis = switchingGuis;
    }

    protected void addItemTooltip(ItemStack is, List<String> lineList) {
        if (AEBaseGui.isShiftKeyDown()) {
            List l = is.getTooltip((EntityPlayer)this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
            if (!l.isEmpty()) {
                l.remove(0);
            }
            lineList.addAll(l);
        } else {
            lineList.add(GuiText.HoldShiftForTooltip.getLocal());
        }
    }

    public FontRenderer getFontRenderer() {
        return this.fontRendererObj;
    }

    public void drawHorizontalLine(int startX, int endX, int y, int color) {
        super.drawHorizontalLine(startX, endX, y, color);
    }

    public void drawVerticalLine(int x, int startY, int endY, int color) {
        super.drawVerticalLine(x, startY, endY, color);
    }

    public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        super.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }

    public void renderToolTip(ItemStack itemIn, int x, int y) {
        super.renderToolTip(itemIn, x, y);
    }

    public void drawCreativeTabHoveringText(String tabName, int mouseX, int mouseY) {
        super.drawCreativeTabHoveringText(tabName, mouseX, mouseY);
    }

    public void drawHoveringText(List<String> textLines, int x, int y) {
        super.func_146283_a(textLines, x, y);
    }

    public void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        super.drawHoveringText(textLines, x, y, font);
    }

    public boolean isMouseOverRect(int left, int top, int right, int bottom, int pointX, int pointY) {
        return super.func_146978_c(left, top, right, bottom, pointX, pointY);
    }

    public int getGuiLeft() {
        return this.guiLeft;
    }

    public int getGuiTop() {
        return this.guiTop;
    }

    public int getXSize() {
        return this.xSize;
    }

    public int getYSize() {
        return this.ySize;
    }

    static {
        aeRenderItem = new AppEngRenderItem();
        translatedRenderItem = new TranslatedRenderItem();
        hasLwjgl3 = Loader.isModLoaded((String)"lwjgl3ify");
    }

    private static class AEGuiTooltip {
        private int shiftX = 0;
        private int shiftY = 0;
        private int x = 0;
        private int y = 0;
        public String[] lines = null;

        private AEGuiTooltip() {
        }

        public void set(int x, int y, String message) {
            this.set(x, y, message != null && !message.isEmpty() ? message.split("\n") : null);
        }

        public void set(int x, int y, String[] lines) {
            this.lines = lines;
            this.x = this.shiftX + x;
            this.y = this.shiftY + y;
        }

        public void shift(int shiftX, int shiftY) {
            this.shiftX = shiftX;
            this.shiftY = shiftY;
        }

        public void draw() {
            if (!this.isEmpty()) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(this.lines[0] + "\u00a7h");
                for (int i = 1; i < this.lines.length; ++i) {
                    list.add(EnumChatFormatting.GRAY + this.lines[i].replace("\u00a0", " "));
                }
                GuiDraw.drawMultilineTip((int)(this.x + 12), (int)(this.y - 12), list);
                this.lines = null;
            }
        }

        public boolean isEmpty() {
            return this.lines == null || this.lines.length == 0;
        }
    }
}

