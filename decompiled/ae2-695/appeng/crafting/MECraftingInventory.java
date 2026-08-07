/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.ChatComponentTranslation
 *  net.minecraft.util.EnumChatFormatting
 *  net.minecraft.util.IChatComponent
 */
package appeng.crafting;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.AELog;
import appeng.core.localization.PlayerMessages;
import appeng.util.IterationCounter;
import java.text.NumberFormat;
import java.util.Locale;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class MECraftingInventory
implements IMEInventory<IAEItemStack> {
    private final MECraftingInventory par;
    private final IMEInventory<IAEItemStack> target;
    private final IItemList<IAEItemStack> localCache;
    private final boolean logExtracted;
    private final IItemList<IAEItemStack> extractedCache;
    private final boolean logInjections;
    private final IItemList<IAEItemStack> injectedCache;
    private final boolean logMissing;
    private final IItemList<IAEItemStack> missingCache;
    private final IItemList<IAEItemStack> failedToExtract = AEApi.instance().storage().createItemList();
    private MECraftingInventory cpuinv;
    private boolean isMissingMode;

    public MECraftingInventory() {
        this.localCache = AEApi.instance().storage().createItemList();
        this.extractedCache = null;
        this.injectedCache = null;
        this.missingCache = null;
        this.logExtracted = false;
        this.logInjections = false;
        this.logMissing = false;
        this.target = null;
        this.par = null;
    }

    public MECraftingInventory(MECraftingInventory parent) {
        this.target = parent;
        this.logExtracted = parent.logExtracted;
        this.logInjections = parent.logInjections;
        this.logMissing = parent.logMissing;
        this.missingCache = this.logMissing ? AEApi.instance().storage().createItemList() : null;
        this.extractedCache = this.logExtracted ? AEApi.instance().storage().createItemList() : null;
        this.injectedCache = this.logInjections ? AEApi.instance().storage().createItemList() : null;
        this.localCache = this.target.getAvailableItems(AEApi.instance().storage().createItemList(), IterationCounter.fetchNewId());
        this.par = parent;
    }

    public MECraftingInventory(IMEMonitor<IAEItemStack> target, BaseActionSource src, boolean logExtracted, boolean logInjections, boolean logMissing) {
        this.target = target;
        this.logExtracted = logExtracted;
        this.logInjections = logInjections;
        this.logMissing = logMissing;
        this.missingCache = logMissing ? AEApi.instance().storage().createItemList() : null;
        this.extractedCache = logExtracted ? AEApi.instance().storage().createItemList() : null;
        this.injectedCache = logInjections ? AEApi.instance().storage().createItemList() : null;
        this.localCache = AEApi.instance().storage().createItemList();
        for (IAEItemStack is : target.getStorageList()) {
            this.localCache.add(target.extractItems(is, Actionable.SIMULATE, src));
        }
        this.par = null;
    }

    public MECraftingInventory(IMEInventory<IAEItemStack> target, boolean logExtracted, boolean logInjections, boolean logMissing) {
        this.target = target;
        this.logExtracted = logExtracted;
        this.logInjections = logInjections;
        this.logMissing = logMissing;
        this.missingCache = logMissing ? AEApi.instance().storage().createItemList() : null;
        this.extractedCache = logExtracted ? AEApi.instance().storage().createItemList() : null;
        this.injectedCache = logInjections ? AEApi.instance().storage().createItemList() : null;
        this.localCache = target.getAvailableItems(AEApi.instance().storage().createItemList(), IterationCounter.fetchNewId());
        this.par = null;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        if (input == null) {
            return null;
        }
        if (mode == Actionable.MODULATE) {
            if (this.logInjections) {
                this.injectedCache.add(input);
            }
            this.localCache.add(input);
        }
        return null;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null) {
            return null;
        }
        IAEItemStack list = this.localCache.findPrecise(request);
        if (list == null || list.getStackSize() == 0L) {
            return null;
        }
        if (list.getStackSize() >= request.getStackSize()) {
            if (mode == Actionable.MODULATE) {
                list.decStackSize(request.getStackSize());
                if (this.logExtracted) {
                    this.extractedCache.add(request);
                }
            }
            return request;
        }
        IAEItemStack ret = request.copy();
        ret.setStackSize(list.getStackSize());
        if (mode == Actionable.MODULATE) {
            list.reset();
            if (this.logExtracted) {
                this.extractedCache.add(ret);
            }
        }
        return ret;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int iteration) {
        for (IAEItemStack is : this.localCache) {
            out.add(is);
        }
        return out;
    }

    @Override
    public IAEItemStack getAvailableItem(@Nonnull IAEItemStack request, int iteration) {
        long count = 0L;
        for (IAEItemStack is : this.localCache) {
            if (is == null || is.getStackSize() <= 0L || !is.isSameType(request) || (count += is.getStackSize()) >= 0L) continue;
            count = Long.MAX_VALUE;
            break;
        }
        return count == 0L ? null : (IAEItemStack)request.copy().setStackSize(count);
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    public IItemList<IAEItemStack> getExtractFailedList() {
        return this.failedToExtract;
    }

    public void setMissingMode(boolean b) {
        this.isMissingMode = b;
    }

    public void setCpuInventory(MECraftingInventory cp) {
        this.cpuinv = cp;
    }

    public IItemList<IAEItemStack> getItemList() {
        return this.localCache;
    }

    public boolean commit(BaseActionSource src) {
        IAEItemStack result;
        IItemList<IAEItemStack> added = AEApi.instance().storage().createItemList();
        IItemList<IAEItemStack> pulled = AEApi.instance().storage().createItemList();
        this.failedToExtract.resetStatus();
        boolean failed = false;
        if (this.logInjections) {
            for (IAEItemStack inject : this.injectedCache) {
                result = null;
                result = this.target.injectItems(inject, Actionable.MODULATE, src);
                added.add(result);
                if (result == null) continue;
                failed = true;
                break;
            }
        }
        if (failed) {
            for (IAEItemStack is : added) {
                this.target.extractItems(is, Actionable.MODULATE, src);
            }
            return false;
        }
        if (this.logExtracted) {
            for (IAEItemStack extra : this.extractedCache) {
                result = null;
                result = this.target.extractItems(extra, Actionable.MODULATE, src);
                pulled.add(result);
                if (result != null && result.getStackSize() == extra.getStackSize()) continue;
                if (this.isMissingMode) {
                    if (result == null) {
                        this.failedToExtract.add(extra.copy());
                        this.cpuinv.localCache.findPrecise(extra).setStackSize(0L);
                        extra.setStackSize(0L);
                        continue;
                    }
                    if (result.getStackSize() == extra.getStackSize()) continue;
                    this.failedToExtract.add((IAEItemStack)extra.copy().setStackSize(extra.getStackSize() - result.getStackSize()));
                    this.cpuinv.localCache.findPrecise(extra).setStackSize(result.getStackSize());
                    extra.setStackSize(result.getStackSize());
                    continue;
                }
                failed = true;
                this.handleCraftExtractFailure(extra, result, src);
                break;
            }
        }
        if (failed) {
            for (IAEItemStack is : added) {
                this.target.extractItems(is, Actionable.MODULATE, src);
            }
            for (IAEItemStack is : pulled) {
                this.target.injectItems(is, Actionable.MODULATE, src);
            }
            return false;
        }
        if (this.logMissing && this.par != null) {
            for (IAEItemStack extra : this.missingCache) {
                this.par.addMissing(extra);
            }
        }
        return true;
    }

    private void addMissing(IAEItemStack extra) {
        this.missingCache.add(extra);
    }

    public void ignore(IAEItemStack what) {
        IAEItemStack list = this.localCache.findPrecise(what);
        if (list != null) {
            list.setStackSize(0L);
        }
    }

    private void handleCraftExtractFailure(IAEItemStack expected, IAEItemStack extracted, BaseActionSource src) {
        if (!(src instanceof PlayerSource)) {
            return;
        }
        try {
            EntityPlayer player = ((PlayerSource)src).player;
            if (player == null || expected == null || expected.getItem() == null) {
                return;
            }
            IChatComponent missingItem = expected.getItemStack().func_151000_E();
            missingItem.getChatStyle().setColor(EnumChatFormatting.GOLD);
            String expectedCount = EnumChatFormatting.RED + NumberFormat.getNumberInstance(Locale.getDefault()).format(expected.getStackSize()) + EnumChatFormatting.RESET;
            String extractedCount = EnumChatFormatting.RED + NumberFormat.getNumberInstance(Locale.getDefault()).format(extracted.getStackSize()) + EnumChatFormatting.RESET;
            player.addChatMessage((IChatComponent)new ChatComponentTranslation(PlayerMessages.CraftingCantExtract.getUnlocalized(), new Object[]{extractedCount, expectedCount, missingItem}));
        }
        catch (Exception ex) {
            AELog.error(ex, "Could not notify player of crafting failure");
        }
    }
}

