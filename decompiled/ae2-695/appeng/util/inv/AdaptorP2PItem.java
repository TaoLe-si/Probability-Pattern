/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 */
package appeng.util.inv;

import appeng.parts.p2p.PartP2PItems;
import appeng.util.inv.AdaptorIInventory;
import net.minecraft.inventory.IInventory;

public class AdaptorP2PItem
extends AdaptorIInventory {
    public AdaptorP2PItem(PartP2PItems p2p) {
        super((IInventory)p2p, p2p.getInventoryStackLimit());
    }
}

