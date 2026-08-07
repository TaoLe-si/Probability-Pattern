/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.client.gui.implementations.GuiMEPortableCell;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiWirelessTerm
extends GuiMEPortableCell {
    public GuiWirelessTerm(InventoryPlayer inventoryPlayer, IPortableCell te) {
        super(inventoryPlayer, te);
    }

    @Override
    int getMaxRows() {
        return this.defaultGetMaxRows();
    }
}

