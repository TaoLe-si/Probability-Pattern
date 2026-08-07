/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.InventoryPlayer
 */
package appeng.client.gui.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.SortOrder;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.container.implementations.ContainerSecurity;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiSecurity
extends GuiMEMonitorable {
    private GuiToggleButton inject;
    private GuiToggleButton extract;
    private GuiToggleButton craft;
    private GuiToggleButton build;
    private GuiToggleButton security;

    public GuiSecurity(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerSecurity(inventoryPlayer, te));
        this.setCustomSortOrder(false);
        this.setReservedSpace(33);
        this.xSize += 56;
        this.setStandardSize(this.xSize);
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        SecurityPermissions toggleSetting = null;
        if (btn == this.inject) {
            toggleSetting = SecurityPermissions.INJECT;
        }
        if (btn == this.extract) {
            toggleSetting = SecurityPermissions.EXTRACT;
        }
        if (btn == this.craft) {
            toggleSetting = SecurityPermissions.CRAFT;
        }
        if (btn == this.build) {
            toggleSetting = SecurityPermissions.BUILD;
        }
        if (btn == this.security) {
            toggleSetting = SecurityPermissions.SECURITY;
        }
        if (toggleSetting != null) {
            try {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("TileSecurity.ToggleOption", toggleSetting.name()));
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        int top = this.guiTop + this.ySize - 116;
        this.inject = new GuiToggleButton(this.guiLeft + 56, top, 176, 192, SecurityPermissions.INJECT.getUnlocalizedName(), SecurityPermissions.INJECT.getUnlocalizedTip());
        this.buttonList.add(this.inject);
        this.extract = new GuiToggleButton(this.guiLeft + 56 + 18, top, 177, 193, SecurityPermissions.EXTRACT.getUnlocalizedName(), SecurityPermissions.EXTRACT.getUnlocalizedTip());
        this.buttonList.add(this.extract);
        this.craft = new GuiToggleButton(this.guiLeft + 56 + 36, top, 178, 194, SecurityPermissions.CRAFT.getUnlocalizedName(), SecurityPermissions.CRAFT.getUnlocalizedTip());
        this.buttonList.add(this.craft);
        this.build = new GuiToggleButton(this.guiLeft + 56 + 54, top, 179, 195, SecurityPermissions.BUILD.getUnlocalizedName(), SecurityPermissions.BUILD.getUnlocalizedTip());
        this.buttonList.add(this.build);
        this.security = new GuiToggleButton(this.guiLeft + 56 + 72, top, 180, 196, SecurityPermissions.SECURITY.getUnlocalizedName(), SecurityPermissions.SECURITY.getUnlocalizedTip());
        this.buttonList.add(this.security);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.fontRendererObj.drawString(GuiText.SecurityCardEditor.getLocal(), 8, this.ySize - 96 + 1 - this.getReservedSpace(), GuiColors.SecurityCardEditorTitle.getColor());
    }

    @Override
    protected String getBackground() {
        ContainerSecurity cs = (ContainerSecurity)this.inventorySlots;
        this.inject.setState((cs.getPermissionMode() & 1 << SecurityPermissions.INJECT.ordinal()) > 0);
        this.extract.setState((cs.getPermissionMode() & 1 << SecurityPermissions.EXTRACT.ordinal()) > 0);
        this.craft.setState((cs.getPermissionMode() & 1 << SecurityPermissions.CRAFT.ordinal()) > 0);
        this.build.setState((cs.getPermissionMode() & 1 << SecurityPermissions.BUILD.ordinal()) > 0);
        this.security.setState((cs.getPermissionMode() & 1 << SecurityPermissions.SECURITY.ordinal()) > 0);
        return "guis/security.png";
    }

    @Override
    public Enum getSortBy() {
        return SortOrder.NAME;
    }
}

