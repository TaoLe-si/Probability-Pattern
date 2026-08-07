/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 */
package appeng.container.implementations;

import appeng.api.config.AdvancedBlockingMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.util.IConfigManager;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.OptionalSlotRestrictedInput;
import appeng.container.slot.SlotNormal;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.PatternMultiplierHelper;
import appeng.util.Platform;
import java.util.ArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ContainerInterface
extends ContainerUpgradeable
implements IOptionalSlotHost {
    private final DualityInterface myDuality;
    @GuiSync(value=3)
    public YesNo bMode = YesNo.NO;
    @GuiSync(value=15)
    public YesNo sbMode = YesNo.NO;
    @GuiSync(value=4)
    public YesNo iTermMode = YesNo.YES;
    @GuiSync(value=14)
    public boolean isAllowedToMultiplyPatterns = false;
    @GuiSync(value=13)
    public YesNo patternOptimization = YesNo.YES;
    @GuiSync(value=10)
    public AdvancedBlockingMode advancedBlockingMode = AdvancedBlockingMode.DEFAULT;
    @GuiSync(value=12)
    public LockCraftingMode lockCraftingMode = LockCraftingMode.NONE;
    @GuiSync(value=16)
    public FuzzyMode fuzzyMode = FuzzyMode.IGNORE_ALL;
    @GuiSync(value=8)
    public InsertionMode insertionMode = InsertionMode.DEFAULT;
    @GuiSync(value=7)
    public int patternRows;
    @GuiSync(value=17)
    public int configSlots;
    @GuiSync(value=9)
    public boolean isEmpty;
    @GuiSync(value=18)
    public boolean isConfigEmpty;

    public ContainerInterface(InventoryPlayer ip, IInterfaceHost te) {
        super(ip, te.getInterfaceDuality().getHost());
        int x;
        this.myDuality = te.getInterfaceDuality();
        this.patternRows = this.getPatternCapacityCardsInstalled();
        this.configSlots = this.getConfigSlotsEnabled();
        for (int row = 0; row < 4; ++row) {
            for (int x2 = 0; x2 < 9; ++x2) {
                this.addSlotToContainer(new OptionalSlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, this.myDuality.getPatterns(), this, x2 + row * 9, 8 + 18 * x2, 108 - row * 18, row, this.getInventoryPlayer()).setStackLimit(1));
            }
        }
        for (x = 0; x < 9; ++x) {
            this.addSlotToContainer(new OptionalSlotFake(this.myDuality.getConfig(), this, x, 8 + 18 * x, 15, 0));
        }
        for (x = 0; x < 9; ++x) {
            this.addSlotToContainer(new SlotNormal(this.myDuality.getStorage(), x, 8 + 18 * x, 33));
        }
    }

    @Override
    protected int getHeight() {
        return 211;
    }

    @Override
    protected void setupConfig() {
        this.setupUpgrades();
    }

    @Override
    public int availableUpgrades() {
        return 4;
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        super.onUpdate(field, oldValue, newValue);
        if (Platform.isClient() && field.equals("patternRows")) {
            this.getRemovedPatterns();
        }
        if (Platform.isClient() && field.equals("configSlots")) {
            this.getRemovedConfig();
        }
    }

    @Override
    public void detectAndSendChanges() {
        TileEntity te;
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        this.isAllowedToMultiplyPatterns = true;
        if (this.patternRows != this.getPatternCapacityCardsInstalled()) {
            this.patternRows = this.getPatternCapacityCardsInstalled();
        }
        boolean bl = this.isEmpty = this.patternRows == -1;
        if (this.configSlots != this.getConfigSlotsEnabled()) {
            this.configSlots = this.getConfigSlotsEnabled();
        }
        this.isConfigEmpty = this.configSlots == -1;
        ArrayList<ItemStack> drops = this.getRemovedPatterns();
        if (!drops.isEmpty() && (te = this.myDuality.getHost().getTile()) != null) {
            Platform.spawnDrops(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord, drops);
        }
        super.detectAndSendChanges();
    }

    private ArrayList<ItemStack> getRemovedPatterns() {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        for (Object o : this.inventorySlots) {
            ItemStack s;
            OptionalSlotRestrictedInput fs;
            if (!(o instanceof OptionalSlotRestrictedInput) || (fs = (OptionalSlotRestrictedInput)((Object)o)).isEnabled() || (s = fs.inventory.getStackInSlot(fs.getSlotIndex())) == null) continue;
            drops.add(s);
            fs.inventory.setInventorySlotContents(fs.getSlotIndex(), null);
            fs.clearStack();
        }
        return drops;
    }

    private void getRemovedConfig() {
        for (Object o : this.inventorySlots) {
            OptionalSlotFake fs;
            if (!(o instanceof OptionalSlotFake) || (fs = (OptionalSlotFake)((Object)o)).isEnabled()) continue;
            fs.inventory.setInventorySlotContents(fs.getSlotIndex(), null);
            fs.clearStack();
        }
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setBlockingMode((YesNo)cm.getSetting(Settings.BLOCK));
        this.setSmartBlockingMode((YesNo)cm.getSetting(Settings.SMART_BLOCK));
        this.setInterfaceTerminalMode((YesNo)cm.getSetting(Settings.INTERFACE_TERMINAL));
        this.setInsertionMode((InsertionMode)cm.getSetting(Settings.INSERTION_MODE));
        this.setPatternOptimization((YesNo)cm.getSetting(Settings.PATTERN_OPTIMIZATION));
        this.setAdvancedBlockingMode((AdvancedBlockingMode)cm.getSetting(Settings.ADVANCED_BLOCKING_MODE));
        this.setLockCraftingMode((LockCraftingMode)cm.getSetting(Settings.LOCK_CRAFTING_MODE));
        this.setFuzzyMode((FuzzyMode)cm.getSetting(Settings.FUZZY_MODE));
    }

    public void doublePatterns(int val) {
        if (!this.isAllowedToMultiplyPatterns) {
            return;
        }
        boolean fast = (val & 1) != 0;
        boolean backwards = (val & 2) != 0;
        CraftingGridCache.pauseRebuilds();
        try {
            AppEngInternalInventory patterns = this.myDuality.getPatterns();
            TileEntity te = this.myDuality.getHost().getTile();
            for (int i = 0; i < patterns.getSizeInventory(); ++i) {
                int max;
                ICraftingPatternItem cpi;
                ICraftingPatternDetails details;
                Item item;
                ItemStack stack = patterns.getStackInSlot(i);
                if (stack == null || !((item = stack.getItem()) instanceof ICraftingPatternItem) || (details = (cpi = (ICraftingPatternItem)item).getPatternForItem(stack, te.getWorldObj())) == null || details.isCraftable()) continue;
                int n = max = backwards ? PatternMultiplierHelper.getMaxBitDivider(details) : PatternMultiplierHelper.getMaxBitMultiplier(details);
                if (max <= 0) continue;
                ItemStack copy = stack.copy();
                PatternMultiplierHelper.applyModification(copy, (fast ? Math.min(3, max) : 1) * (backwards ? -1 : 1));
                patterns.setInventorySlotContents(i, copy);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        CraftingGridCache.unpauseRebuilds();
        this.standardDetectAndSendChanges();
    }

    public YesNo getBlockingMode() {
        return this.bMode;
    }

    private void setBlockingMode(YesNo bMode) {
        this.bMode = bMode;
    }

    public YesNo getSmartBlockingMode() {
        return this.sbMode;
    }

    private void setSmartBlockingMode(YesNo sbMode) {
        this.sbMode = sbMode;
    }

    public YesNo getInterfaceTerminalMode() {
        return this.iTermMode;
    }

    private void setInterfaceTerminalMode(YesNo iTermMode) {
        this.iTermMode = iTermMode;
    }

    public YesNo getPatternOptimization() {
        return this.patternOptimization;
    }

    public void setPatternOptimization(YesNo patternOptimization) {
        this.patternOptimization = patternOptimization;
    }

    public InsertionMode getInsertionMode() {
        return this.insertionMode;
    }

    private void setInsertionMode(InsertionMode insertionMode) {
        this.insertionMode = insertionMode;
    }

    public AdvancedBlockingMode getAdvancedBlockingMode() {
        return this.advancedBlockingMode;
    }

    private void setAdvancedBlockingMode(AdvancedBlockingMode mode) {
        this.advancedBlockingMode = mode;
    }

    public LockCraftingMode getLockCraftingMode() {
        return this.lockCraftingMode;
    }

    private void setLockCraftingMode(LockCraftingMode mode) {
        this.lockCraftingMode = mode;
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return this.fuzzyMode;
    }

    @Override
    public void setFuzzyMode(FuzzyMode mode) {
        this.fuzzyMode = mode;
    }

    public int getPatternCapacityCardsInstalled() {
        if (Platform.isClient() && this.isEmpty) {
            return -1;
        }
        if (this.myDuality == null) {
            return 0;
        }
        return this.myDuality.getInstalledUpgrades(Upgrades.PATTERN_CAPACITY);
    }

    private int getConfigSlotsEnabled() {
        if (Platform.isClient() && this.isConfigEmpty) {
            return -1;
        }
        return this.myDuality.getConfigSize();
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        if (Platform.isClient() && (this.isEmpty || this.isConfigEmpty)) {
            return false;
        }
        return this.myDuality.getInstalledUpgrades(Upgrades.PATTERN_CAPACITY) >= idx;
    }
}

