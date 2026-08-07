/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.container.implementations;

import appeng.container.implementations.ContainerMEPortableCell;
import appeng.core.AEConfig;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerWirelessTerm
extends ContainerMEPortableCell {
    private final WirelessTerminalGuiObject wirelessTerminalGUIObject;

    public ContainerWirelessTerm(InventoryPlayer ip, WirelessTerminalGuiObject gui) {
        super(ip, gui);
        this.wirelessTerminalGUIObject = gui;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!this.wirelessTerminalGUIObject.rangeCheck()) {
            if (Platform.isServer() && this.isValidContainer()) {
                this.getPlayerInv().player.addChatMessage(PlayerMessages.OutOfRange.toChat());
            }
            this.setValidContainer(false);
        } else {
            this.setPowerMultiplier(AEConfig.instance.wireless_getDrainRate(this.wirelessTerminalGUIObject.getRange()));
        }
    }

    public WirelessTerminalGuiObject getWirelessTerminalGUIObject() {
        return this.wirelessTerminalGUIObject;
    }
}

