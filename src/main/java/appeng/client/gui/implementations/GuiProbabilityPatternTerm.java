/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
// NOTE: lives in AE2's GUI package to extend GuiPatternTerm and access the
// package-private helpers of GuiMEMonitorable (setReservedSpace / getReservedSpace).
package appeng.client.gui.implementations;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.zincglux.statpatterns.ProbabilityPatternMod;
import com.zincglux.statpatterns.network.PacketProbabilityPatternAutoCraft;
import com.zincglux.statpatterns.network.ProbabilityPatternNetwork;
import com.zincglux.statpatterns.network.ProbabilityPatternPacket;
import com.zincglux.statpatterns.network.ProbabilityPatternPacket.Action;
import com.zincglux.statpatterns.network.ProbabilityPatternValueSetPacket;
import com.zincglux.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.me.SlotME;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;

/**
 * GUI for the ME Probability Pattern Encoding Terminal (Spec v2.0 3.3.3).
 * <p>
 * Extends AE2 GTNH's {@link GuiPatternTerm} to inherit the pattern-terminal screen
 * plumbing, then re-lays-out the two probability controls per the spec: a probability
 * text field ({@code p = [0.8000]}) and a 95% / 99% confidence toggle on the
 * "Inventory" label row.
 * <p>
 * Because a probability pattern is always a processing pattern, the terminal is pinned
 * to processing mode (crafting/processing tabs hidden, {@code craftingMode = false}).
 * The probability values live on the {@link ProbabilityPatternTerminalPart} (shared via
 * part NBT); the GUI reads them from the part and sends packets to the server.
 */
public class GuiProbabilityPatternTerm extends GuiPatternTerm {

    /** Spec v2.0 3.3.3: probability text field at x=73, y=ySize-97, 40x12. */
    private static final int PROBABILITY_FIELD_X = 73;
    private static final int PROBABILITY_FIELD_Y_OFFSET = 97;
    private static final int PROBABILITY_FIELD_W = 40;
    private static final int PROBABILITY_FIELD_H = 12;
    /** Spec v2.0 3.3.3: confidence toggle button at x=115, y=ySize-97, 56x12. */
    private static final int ALPHA_BUTTON_X = 115;
    private static final int ALPHA_BUTTON_W = 56;
    private static final int ALPHA_BUTTON_H = 12;
    /** "p =" label, just left of the probability field. */
    private static final int LABEL_X = 50;
    private static final int LABEL_Y_OFFSET = 94;

    private final ProbabilityPatternTerminalPart probabilityPart;
    private GuiButton alphaButton;
    private MEGuiTextField probabilityField;
    private boolean alpha95Display;
    private double lastSentProbability = -1.0;

    public GuiProbabilityPatternTerm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(inventoryPlayer, te);
        this.probabilityPart = (ProbabilityPatternTerminalPart) te;
        this.alpha95Display = this.probabilityPart.isAlpha95();
    }

    @Override
    protected int getMaxRows() {
        return 3;
    }

    @Override
    public void initGui() {
        super.initGui();

        // Pin to processing mode: a probability pattern is always a processing pattern.
        ((ContainerPatternTerm) this.inventorySlots).craftingMode = false;

        final int controlY = this.guiTop + this.ySize - PROBABILITY_FIELD_Y_OFFSET;

        this.alphaButton = new GuiButton(
            200,
            this.guiLeft + ALPHA_BUTTON_X,
            controlY,
            ALPHA_BUTTON_W,
            ALPHA_BUTTON_H,
            "");
        this.buttonList.add(this.alphaButton);

        this.probabilityField = new MEGuiTextField(PROBABILITY_FIELD_W, PROBABILITY_FIELD_H);
        this.probabilityField.x = this.guiLeft + PROBABILITY_FIELD_X;
        this.probabilityField.y = controlY;
        this.probabilityField.w = PROBABILITY_FIELD_W;
        this.probabilityField.h = PROBABILITY_FIELD_H;
        this.probabilityField.setMaxStringLength(8);
        this.probabilityField.setText(formatProbability(this.probabilityPart.getProbability()));

        this.updateAlphaButton();

        // Re-run the NEE fill registration whenever the terminal opens, so any mod that
        // registered its NEI recipe handlers after our postInit is still covered (idempotent).
        if (ProbabilityPatternMod.instance != null) {
            ProbabilityPatternMod.instance.registerNEEFill();
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        if (btn == this.alphaButton) {
            this.alpha95Display = !this.alpha95Display;
            ProbabilityPatternNetwork.CHANNEL
                .sendToServer(new ProbabilityPatternPacket(Action.SET_ALPHA95, 0.0, this.alpha95Display ? 1 : 0));
            this.updateAlphaButton();
        }
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (this.probabilityField.textboxKeyTyped(character, key)) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_TAB) {
                this.probabilityField.setFocused(false);
            }

            final Double parsed = parseProbability(this.probabilityField.getText());
            if (parsed != null && Math.abs(parsed - this.lastSentProbability) > 1.0e-6) {
                this.lastSentProbability = parsed;
                ProbabilityPatternNetwork.CHANNEL
                    .sendToServer(new ProbabilityPatternPacket(Action.SET_PROBABILITY, parsed, 0));
            }
            return;
        }

        super.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        this.probabilityField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    /**
     * Intercept the middle click (mouseButton == 3, i.e. AE's convention for the middle
     * mouse button) on an encoding-grid fake slot and open OUR own amount dialog instead
     * of AE's SET_PATTERN_VALUE (whose confirmation would reopen the VANILLA terminal).
     */
    @Override
    protected void handleMouseClick(final Slot slot, final int slotIdx, final int ctrlDown, final int mouseButton) {
        if (slot instanceof SlotME) {
            final IAEItemStack stack = ((SlotME) slot).getAEStack();
            final boolean middleClickCraft = mouseButton == 3 && stack != null && stack.isCraftable();
            final boolean leftClickMissingCraft = mouseButton == 0 && ctrlDown != 1
                && stack != null
                && stack.getStackSize() == 0L
                && this.mc.thePlayer.inventory.getItemStack() == null;
            if (middleClickCraft || leftClickMissingCraft) {
                ProbabilityPatternNetwork.CHANNEL
                    .sendToServer(new PacketProbabilityPatternAutoCraft(stack.getItemStack()));
                return;
            }
        }
        if (mouseButton == 3 && (slot instanceof SlotFakeCraftingMatrix || slot instanceof OptionalSlotFake)
            && slot.getHasStack()) {
            ProbabilityPatternNetwork.CHANNEL.sendToServer(
                new ProbabilityPatternValueSetPacket(ProbabilityPatternValueSetPacket.Action.OPEN, slotIdx, 0));
            return;
        }
        super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        // The vanilla updateButtonVisibility (called inside super.drawFG) re-shows the
        // crafting/processing tab because craftingMode=false; a probability pattern is
        // always a processing pattern so keep the tabs hidden.
        for (final Object o : this.buttonList) {
            if (o instanceof GuiTabButton) {
                ((GuiTabButton) o).visible = false;
            }
        }

        if (!this.probabilityField.isFocused()) {
            final String current = formatProbability(this.probabilityPart.getProbability());
            if (!current.equals(this.probabilityField.getText())) {
                this.probabilityField.setText(current);
            }
        }
        this.updateAlphaButton();
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);

        // CRITICAL coordinate space note: drawBG runs in ABSOLUTE screen coordinates (MC
        // has NOT yet applied glTranslatef(guiLeft, guiTop)). MEGuiTextField also draws in
        // absolute coordinates (its x/y are set to guiLeft + ... in initGui), matching the
        // vanilla searchField pattern in GuiMEMonitorable.drawBG. So both the "p =" label
        // and the text box must be drawn here, NOT in drawFG (which is gui-relative).
        final String label = StatCollector.translateToLocal("gui.statpatterns.short_probability");
        this.fontRendererObj
            .drawString(label, this.guiLeft + LABEL_X, this.guiTop + this.ySize - LABEL_Y_OFFSET, 4210752);

        // MEGuiTextField only paints a background while focused (SearchboxUnfocused is
        // fully transparent), and - unlike the vanilla terminal search box - there is no
        // background-texture frame behind the probability field. Draw a visible box
        // (dark fill + light border) so it reads as a text box even when unfocused.
        final int bx = this.guiLeft + PROBABILITY_FIELD_X;
        final int by = this.guiTop + this.ySize - PROBABILITY_FIELD_Y_OFFSET;
        final int bw = PROBABILITY_FIELD_W;
        final int bh = PROBABILITY_FIELD_H;
        final int fill = 0x8B000000; // dark, slightly transparent fill
        final int border = 0xFF9A9A9A; // light grey border
        Gui.drawRect(bx, by, bx + bw, by + bh, fill);
        Gui.drawRect(bx, by, bx + bw, by + 1, border); // top
        Gui.drawRect(bx, by + bh - 1, bx + bw, by + bh, border); // bottom
        Gui.drawRect(bx, by, bx + 1, by + bh, border); // left
        Gui.drawRect(bx + bw - 1, by, bx + bw, by + bh, border); // right

        this.probabilityField.drawTextBox();
    }

    private void updateAlphaButton() {
        if (this.alphaButton != null) {
            this.alphaButton.displayString = StatCollector
                .translateToLocal(this.alpha95Display ? "gui.statpatterns.alpha95" : "gui.statpatterns.alpha99");
        }
    }

    private static String formatProbability(final double probability) {
        return String.format("%.4f", probability);
    }

    private static Double parseProbability(final String value) {
        try {
            final double parsed = Double.parseDouble(value.trim());
            if (parsed > 0.0 && parsed <= 1.0) {
                return parsed;
            }
        } catch (final NumberFormatException ignored) {}
        return null;
    }
}
