/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.ItemStack
 */
package appeng.container.implementations;

import appeng.api.config.PriorityCardMode;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerPriority;
import appeng.items.contents.PriorityCardObject;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerPriorityCard
extends ContainerPriority {
    private final PriorityCardObject host;
    @GuiSync(value=3)
    public PriorityCardMode cardMode = PriorityCardMode.EDIT;

    public ContainerPriorityCard(InventoryPlayer ip, PriorityCardObject host) {
        super(ip, host);
        this.host = host;
        this.lockPlayerInventorySlot(host.getInventorySlot());
    }

    @Override
    public void detectAndSendChanges() {
        ItemStack currentItem;
        if (Platform.isServer()) {
            IConfigManager cm = this.host.getConfigManager();
            this.setCardMode((PriorityCardMode)cm.getSetting(Settings.PRIORITY_CARD_MODE));
        }
        if ((currentItem = this.getPlayerInv().getStackInSlot(this.host.getInventorySlot())) != this.host.getItemStack()) {
            if (currentItem != null) {
                if (Platform.isSameItem(this.host.getItemStack(), currentItem)) {
                    this.getPlayerInv().setInventorySlotContents(this.host.getInventorySlot(), this.host.getItemStack());
                } else {
                    this.setValidContainer(false);
                }
            } else {
                this.setValidContainer(false);
            }
        }
        super.detectAndSendChanges();
    }

    public void setCardMode(PriorityCardMode mode) {
        this.cardMode = mode;
    }

    public PriorityCardMode getCardMode() {
        return this.cardMode;
    }
}

