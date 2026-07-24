package com.tz.statpatterns.client;

import appeng.client.gui.style.ScreenStyle;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.tz.statpatterns.terminal.WirelessProbabilityPatternTerminalMenu;

/**
 * Wireless variant of the probability pattern terminal screen.
 * Implements ae2wtlib's IUniversalTerminalCapable for WUT support.
 */
public class WirelessProbabilityPatternTerminalScreen
        extends ProbabilityPatternTerminalScreen<WirelessProbabilityPatternTerminalMenu>
        implements IUniversalTerminalCapable {

    public WirelessProbabilityPatternTerminalScreen(WirelessProbabilityPatternTerminalMenu menu,
            Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        if (getMenu().isWUT()) {
            addToLeftToolbar(new CycleTerminalButton(btn -> cycleTerminal()));
        }
    }

    @Override
    public void cycleTerminal() {
        // Handled by CycleTerminalButton
    }

    @Override
    public boolean isHandlingRightClick() {
        return false;
    }

    @Override
    public void storeState() {
    }
}
