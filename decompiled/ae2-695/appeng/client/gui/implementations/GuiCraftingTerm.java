/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.Slot
 */
package appeng.client.gui.implementations;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class GuiCraftingTerm
extends GuiMEMonitorable {
    private GuiImgButton clearBtn;

    public GuiCraftingTerm(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerCraftingTerm(inventoryPlayer, te));
        this.setReservedSpace(73);
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        if (this.clearBtn == btn) {
            Slot s = null;
            Container c = this.inventorySlots;
            for (Object j : c.inventorySlots) {
                if (!(j instanceof SlotCraftingMatrix)) continue;
                s = (Slot)j;
            }
            if (s != null) {
                PacketInventoryAction p = new PacketInventoryAction(InventoryAction.MOVE_REGION, s.slotNumber, 0L);
                NetworkHandler.instance.sendToServer(p);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearBtn = new GuiImgButton(this.guiLeft + 92, this.guiTop + this.ySize - 156, Settings.ACTIONS, ActionItems.STASH);
        this.buttonList.add(this.clearBtn);
        this.clearBtn.setHalfSize(true);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.fontRendererObj.drawString(GuiText.CraftingTerminal.getLocal(), 8, this.ySize - 96 + 1 - this.getReservedSpace(), GuiColors.CraftingTerminalTitle.getColor());
    }

    @Override
    protected String getBackground() {
        return "guis/crafting.png";
    }
}

