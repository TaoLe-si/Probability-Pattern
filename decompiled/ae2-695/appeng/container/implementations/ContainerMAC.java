/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.World
 */
package appeng.container.implementations;

import appeng.api.config.RedstoneMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.SlotMACPattern;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.items.misc.ItemEncodedPattern;
import appeng.tile.crafting.TileMolecularAssembler;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ContainerMAC
extends ContainerUpgradeable
implements IProgressProvider {
    private static final int MAX_CRAFT_PROGRESS = 100;
    private final TileMolecularAssembler tma;
    @GuiSync(value=4)
    public int craftProgress = 0;

    public ContainerMAC(InventoryPlayer ip, TileMolecularAssembler te) {
        super(ip, te);
        this.tma = te;
    }

    public boolean isValidItemForSlot(int slotIndex, ItemStack i) {
        World w;
        ItemEncodedPattern iep;
        ICraftingPatternDetails ph;
        IInventory mac = this.getUpgradeable().getInventoryByName("mac");
        ItemStack is = mac.getStackInSlot(10);
        if (is == null) {
            return false;
        }
        Item item = is.getItem();
        if (item instanceof ItemEncodedPattern && (ph = (iep = (ItemEncodedPattern)item).getPatternForItem(is, w = this.getTileEntity().getWorldObj())).isCraftable()) {
            return ph.isValidItemForSlot(slotIndex, i, w);
        }
        return false;
    }

    @Override
    protected int getHeight() {
        return 197;
    }

    @Override
    protected void setupConfig() {
        int offX = 29;
        int offY = 30;
        IInventory mac = this.getUpgradeable().getInventoryByName("mac");
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                SlotMACPattern s = new SlotMACPattern(this, mac, x + y * 3, offX + x * 18, offY + y * 18);
                this.addSlotToContainer(s);
            }
        }
        offX = 126;
        offY = 16;
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_CRAFTING_PATTERN, mac, 10, offX, offY, this.getInventoryPlayer()));
        this.addSlotToContainer(new SlotOutput(mac, 9, offX, offY + 32, -1));
        offX = 122;
        offY = 17;
        IInventory upgrades = this.getUpgradeable().getInventoryByName("upgrades");
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 0, 187, 8, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 1, 187, 26, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 2, 187, 44, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 3, 187, 62, this.getInventoryPlayer()).setNotDraggable());
        this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, 4, 187, 80, this.getInventoryPlayer()).setNotDraggable());
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 5;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            this.setRedStoneMode((RedstoneMode)this.getUpgradeable().getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED));
        }
        this.craftProgress = this.tma.getCraftingProgress();
        this.standardDetectAndSendChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.craftProgress;
    }

    @Override
    public int getMaxProgress() {
        return 100;
    }
}

