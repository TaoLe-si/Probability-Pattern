/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
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
// NOTE: This class intentionally lives in AE2's GUI package so it can extend
// GuiPatternTerm and access the package-private helpers of GuiMEMonitorable
// (setReservedSpace / getReservedSpace), a common technique for 1.7.10 AE2 addons.
package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.tz.statpatterns.network.ProbabilityPatternNetwork;
import com.tz.statpatterns.network.ProbabilityPatternPacket;
import com.tz.statpatterns.network.ProbabilityPatternPacket.Action;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerPatternTerm;

/**
 * GUI for the ME Probability Pattern Encoding Terminal.
 * <p>
 * Extends AE2 GTNH's {@link GuiPatternTerm} so the complete vanilla pattern-terminal
 * screen is reused verbatim (crafting/processing tabs, substitute toggles, encode /
 * clear / double buttons, NEI overlay, ME monitor rows). On top of that it adds the two
 * probability controls in the free row directly above the player inventory: the single
 * attempt success probability text field and the 95% / 99% confidence toggle button.
 * <p>
 * The probability values live on the {@link ProbabilityPatternTerminalPart} (shared
 * between client and server through the part NBT), so the GUI reads them from the part
 * and sends {@link ProbabilityPatternPacket} SET_PROBABILITY / SET_ALPHA95 to the
 * server when the user changes them.
 */
public class GuiProbabilityPatternTerm extends GuiPatternTerm {

    private final ProbabilityPatternTerminalPart probabilityPart;
    private GuiButton alphaButton;
    private GuiTextField probabilityField;
    private boolean alpha95Display;
    private double lastSentProbability = -1.0;

    public GuiProbabilityPatternTerm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(inventoryPlayer, te);
        this.probabilityPart = (ProbabilityPatternTerminalPart) te;
        this.alpha95Display = this.probabilityPart.isAlpha95();
    }

    /**
     * Pin the terminal height to 3 ME rows (processing terminal layout, pattern2.png).
     * Without this the GUI would grow with the screen height, pushing the probability
     * controls off the inventory bar.
     */
    @Override
    protected int getMaxRows() {
        return 3;
    }

    @Override
    public void initGui() {
        super.initGui();

        // A probability pattern is always a processing pattern, so pin the terminal to
        // processing mode (pattern2.png layout) and hide the crafting/processing tabs to
        // avoid switching into a crafting mode that this pattern cannot express.
        ((ContainerPatternTerm) this.inventorySlots).craftingMode = false;
        for (final Object o : this.buttonList) {
            if (o instanceof GuiTabButton) {
                ((GuiTabButton) o).visible = false;
            }
        }

        // Probability controls sit on the same row as the "Inventory" label, to its
        // right (GuiMEMonitorable.drawFG paints "Inventory" at x=8, ySize-93), so nothing
        // overlaps. Everything is positioned relative to ySize.
        this.alphaButton = new GuiButton(200, this.guiLeft + 122, this.guiTop + this.ySize - 94, 56, 12, "");
        this.buttonList.add(this.alphaButton);

        this.probabilityField = new GuiTextField(
            this.fontRendererObj,
            this.guiLeft + 80,
            this.guiTop + this.ySize - 94,
            40,
            12);
        this.probabilityField.setMaxStringLength(8);
        this.probabilityField.setText(formatProbability(this.probabilityPart.getProbability()));

        this.updateAlphaButton();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        if (btn == this.alphaButton) {
            this.alpha95Display = !this.alpha95Display;
            ProbabilityPatternNetwork.CHANNEL
                .sendToServer(new ProbabilityPatternPacket(Action.SET_ALPHA95, this.alpha95Display ? 1 : 0));
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
                    .sendToServer(new ProbabilityPatternPacket(Action.SET_PROBABILITY, parsed));
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

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        // "p =" label just right of the "Inventory" text (x=8, ySize-93).
        final String label = StatCollector.translateToLocal("gui.probabilitypattern.short_probability");
        this.fontRendererObj.drawString(label, 60, this.ySize - 93, 4210752);

        // Keep the text field in sync with the part's value unless the user is editing.
        if (!this.probabilityField.isFocused()) {
            final String current = formatProbability(this.probabilityPart.getProbability());
            if (!current.equals(this.probabilityField.getText())) {
                this.probabilityField.setText(current);
            }
        }
        this.probabilityField.drawTextBox();
        this.updateAlphaButton();
    }

    private void updateAlphaButton() {
        if (this.alphaButton != null) {
            this.alphaButton.displayString = StatCollector.translateToLocal(
                this.alpha95Display ? "gui.probabilitypattern.alpha95" : "gui.probabilitypattern.alpha99");
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
