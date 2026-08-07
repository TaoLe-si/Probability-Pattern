/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.implementations.ContainerMEPortableCell;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiMEPortableCell
extends GuiMEMonitorable {
    public GuiMEPortableCell(InventoryPlayer inventoryPlayer, IPortableCell te) {
        super(inventoryPlayer, te, new ContainerMEPortableCell(inventoryPlayer, te));
    }

    int defaultGetMaxRows() {
        return super.getMaxRows();
    }

    @Override
    int getMaxRows() {
        return 3;
    }
}

