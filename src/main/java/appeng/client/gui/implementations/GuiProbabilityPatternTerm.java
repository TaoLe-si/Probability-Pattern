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
// NOTE: This class intentionally lives in AE2's GUI package so it can access the
// package-private helpers (setReservedSpace / getReservedSpace) of GuiMEMonitorable,
// a common technique for 1.7.10 AE2 addons.
package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.tz.statpatterns.container.ContainerProbabilityPatternTerm;
import com.tz.statpatterns.network.ProbabilityPatternNetwork;
import com.tz.statpatterns.network.ProbabilityPatternPacket;
import com.tz.statpatterns.network.ProbabilityPatternPacket.Action;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.slot.AppEngSlot;

/**
 * GUI for the ME Probability Pattern Encoding Terminal.
 * <p>
 * Extends {@link GuiMEMonitorable} (the ME terminal GUI) and adds a probability
 * text field plus a 95% / 99% confidence toggle button, mirroring the 1.21.1
 * version's probability terminal screen.
 */
public class GuiProbabilityPatternTerm extends GuiMEMonitorable {

    private final ContainerProbabilityPatternTerm container;

    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;
    private GuiButton alphaButton;
    private GuiTextField probabilityField;
    private double lastSentProbability = -1.0;

    public GuiProbabilityPatternTerm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerProbabilityPatternTerm(inventoryPlayer, te));
        this.container = (ContainerProbabilityPatternTerm) this.inventorySlots;
        this.setReservedSpace(81);
    }

    /**
     * Fix the terminal height to 3 ME rows. The pattern2.png background is drawn with exactly 3
     * monitor rows on top, so without this the GUI would grow with the screen height, pushing the
     * encoding area (and the probability controls) off screen / misaligned on tall displays.
     */
    @Override
    protected int getMaxRows() {
        return 3;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.encodeBtn = new GuiImgButton(
            this.guiLeft + 147,
            this.guiTop + this.ySize - 142,
            Settings.ACTIONS,
            ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        this.clearBtn = new GuiImgButton(
            this.guiLeft + 74,
            this.guiTop + this.ySize - 163,
            Settings.ACTIONS,
            ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);

        // Layout notes (GUI-internal coords): the probability controls sit in the free row
        // directly above the player inventory (player slot row 1 renders at ySize - 83).
        // Everything is positioned relative to ySize, so the controls move together with the
        // inventory bar. The "Inventory" label drawn by GuiMEMonitorable sits at x=8, so the
        // controls start at x=74 to avoid overlapping it.
        this.alphaButton = new GuiButton(10, this.guiLeft + 50, this.guiTop + this.ySize - 98, 56, 12, "");
        this.buttonList.add(this.alphaButton);

        this.probabilityField = new GuiTextField(
            this.fontRendererObj,
            this.guiLeft + 8,
            this.guiTop + this.ySize - 98,
            40,
            12);
        this.probabilityField.setMaxStringLength(8);
        this.probabilityField.setText(formatProbability(this.container.probabilityScaled / 10000.0));

        this.updateAlphaButton();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        if (btn == this.encodeBtn) {
            ProbabilityPatternNetwork.CHANNEL.sendToServer(new ProbabilityPatternPacket(Action.ENCODE, 0));
        } else if (btn == this.clearBtn) {
            ProbabilityPatternNetwork.CHANNEL.sendToServer(new ProbabilityPatternPacket(Action.CLEAR, 0));
        } else if (btn == this.alphaButton) {
            // Server side is authoritative; the @GuiSync field will refresh the display.
            final boolean newAlpha95 = this.container.alpha95Flag == 0;
            ProbabilityPatternNetwork.CHANNEL
                .sendToServer(new ProbabilityPatternPacket(Action.SET_ALPHA95, newAlpha95 ? 1 : 0));
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

        final String title = StatCollector
            .translateToLocal("container.probabilitypattern.probability_pattern_terminal");
        this.fontRendererObj.drawString(title, 8, this.ySize - 96 + 2 - this.getReservedSpace(), 4210752);

        // Keep the text field in sync with the server-synced value unless the user is editing.
        if (!this.probabilityField.isFocused()) {
            final String current = formatProbability(this.container.probabilityScaled / 10000.0);
            if (!current.equals(this.probabilityField.getText())) {
                this.probabilityField.setText(current);
            }
        }

        final String label = StatCollector.translateToLocal("gui.probabilitypattern.short_probability");
        this.fontRendererObj.drawString(label, 8, this.ySize - 112, 4210752);
        this.probabilityField.drawTextBox();

        this.alphaButton.displayString = StatCollector.translateToLocal(
            this.container.alpha95Flag != 0 ? "gui.probabilitypattern.alpha95" : "gui.probabilitypattern.alpha99");
    }

    private void updateAlphaButton() {
        this.alphaButton.displayString = StatCollector.translateToLocal(
            this.container.alpha95Flag != 0 ? "gui.probabilitypattern.alpha95" : "gui.probabilitypattern.alpha99");
    }

    @Override
    protected String getBackground() {
        // Processing-only layout (same texture the AE2 processing pattern terminal uses).
        return "guis/pattern2.png";
    }

    @Override
    protected void repositionSlot(final AppEngSlot s) {
        if (s.isPlayerSide()) {
            s.yDisplayPosition = s.getY() + this.ySize - 78 - 5;
        } else {
            s.yDisplayPosition = s.getY() + this.ySize - 78 - 3;
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
