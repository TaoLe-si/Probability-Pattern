/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  codechicken.nei.ItemStackMap
 *  codechicken.nei.ItemStackSet
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  org.apache.commons.lang3.tuple.Pair
 */
package appeng.container.implementations;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IInterfaceViewable;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;
import appeng.core.features.registries.InterfaceTerminalRegistry;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingJobV2;
import appeng.crafting.v2.resolvers.CraftableItemResolver;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.cache.CraftingGridCache;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.parts.reporting.PartTerminal;
import appeng.tile.misc.TilePatternOptimizationMatrix;
import appeng.util.PatternMultiplierHelper;
import appeng.util.Platform;
import codechicken.nei.ItemStackMap;
import codechicken.nei.ItemStackSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.apache.commons.lang3.tuple.Pair;

public class ContainerOptimizePatterns
extends AEBaseContainer {
    private ICraftingJob result;
    HashMap<IAEItemStack, Pattern> patterns = new HashMap();

    public ContainerOptimizePatterns(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
    }

    public void setResult(ICraftingJob result) {
        this.result = result;
        this.patterns.clear();
        ICraftingJob iCraftingJob = this.result;
        if (iCraftingJob instanceof CraftingJobV2) {
            CraftingJobV2 cj = (CraftingJobV2)iCraftingJob;
            CraftingContext context = cj.getContext();
            ItemStackSet blacklistedPatterns = new ItemStackSet();
            Set<Class<? extends IInterfaceViewable>> supported = InterfaceTerminalRegistry.instance().getSupportedClasses();
            for (Class<? extends IInterfaceViewable> c : supported) {
                for (IGridNode node : context.meGrid.getMachines(c)) {
                    IInterfaceViewable machine = (IInterfaceViewable)node.getMachine();
                    if (machine.allowsPatternOptimization()) continue;
                    IInventory patternInv = machine.getPatterns();
                    for (int i2 = 0; i2 < patternInv.getSizeInventory(); ++i2) {
                        ItemStack stack = patternInv.getStackInSlot(i2);
                        if (stack == null) continue;
                        blacklistedPatterns.add(stack);
                    }
                }
            }
            for (CraftingTask resolvedTask : context.getResolvedTasks()) {
                if (!(resolvedTask instanceof CraftableItemResolver.CraftFromPatternTask)) continue;
                CraftableItemResolver.CraftFromPatternTask cfpt = (CraftableItemResolver.CraftFromPatternTask)resolvedTask;
                if (blacklistedPatterns.contains(cfpt.pattern.getPattern())) continue;
                this.patterns.computeIfAbsent((IAEItemStack)cfpt.request.stack, i -> new Pattern()).addCraftingTask(cfpt);
            }
            this.patterns.entrySet().removeIf(entry -> ((Pattern)entry.getValue()).patternDetails.size() != 1);
            this.patterns.entrySet().removeIf(entry -> ((Pattern)entry.getValue()).getPattern().isCraftable());
            try {
                PacketMEInventoryUpdate patternsUpdate = new PacketMEInventoryUpdate(0);
                for (Map.Entry<IAEItemStack, Pattern> entry2 : this.patterns.entrySet()) {
                    IAEItemStack stack = entry2.getKey().copy();
                    stack.setCountRequestableCrafts(entry2.getValue().requestedCrafts);
                    long perCraft = entry2.getValue().getCraftAmountForItem(stack);
                    int hash = entry2.getKey().hashCode();
                    if (hash < 0) {
                        stack.setStackSize((long)(-hash) << 6 | 0x20L | (long)entry2.getValue().getMaxBitMultiplier());
                    } else {
                        stack.setStackSize((long)hash << 6 | (long)entry2.getValue().getMaxBitMultiplier());
                    }
                    stack.setCountRequestable(perCraft);
                    patternsUpdate.appendItem(stack);
                }
                for (Map.Entry<IAEItemStack, Pattern> player : this.crafters) {
                    if (!(player instanceof EntityPlayerMP)) continue;
                    EntityPlayerMP playerMP = (EntityPlayerMP)player;
                    NetworkHandler.instance.sendTo(patternsUpdate, playerMP);
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    public void optimizePatterns(HashMap<Integer, Integer> hashCodeToMultipliers) {
        IGrid grid = this.getGrid();
        if (grid == null || grid.getMachines(TilePatternOptimizationMatrix.class).isEmpty()) {
            return;
        }
        Map<IAEItemStack, Integer> multipliersMap = this.patterns.keySet().stream().filter(i -> hashCodeToMultipliers.containsKey(i.hashCode())).collect(Collectors.toMap(i -> i, i -> (Integer)hashCodeToMultipliers.get(i.hashCode())));
        ItemStackMap lookupMap = new ItemStackMap();
        for (Map.Entry<IAEItemStack, Integer> entry : multipliersMap.entrySet()) {
            Pattern pattern = this.patterns.get(entry.getKey());
            lookupMap.put(pattern.getPattern().getPattern(), (Object)Pair.of((Object)pattern, (Object)entry.getValue()));
        }
        IdentityHashMap<ItemStack, Boolean> alreadyDone = new IdentityHashMap<ItemStack, Boolean>();
        Set<Class<? extends IInterfaceViewable>> supported = InterfaceTerminalRegistry.instance().getSupportedClasses();
        CraftingGridCache.pauseRebuilds();
        try {
            for (Class clazz : supported) {
                for (IGridNode node : grid.getMachines(clazz)) {
                    IInterfaceViewable machine = (IInterfaceViewable)node.getMachine();
                    if (!machine.allowsPatternOptimization()) continue;
                    IInventory patternInv = machine.getPatterns();
                    for (int i2 = 0; i2 < patternInv.getSizeInventory(); ++i2) {
                        Pair pair;
                        ItemStack stack = patternInv.getStackInSlot(i2);
                        if (stack == null || alreadyDone.containsKey(stack) || (pair = (Pair)lookupMap.get(stack)) == null) continue;
                        ItemStack sCopy = stack.copy();
                        ((Pattern)pair.getKey()).applyModification(sCopy, (Integer)pair.getValue());
                        patternInv.setInventorySlotContents(i2, sCopy);
                        alreadyDone.put(stack, true);
                    }
                }
            }
        }
        catch (Throwable t) {
            AELog.debug(t);
        }
        CraftingGridCache.unpauseRebuilds();
        this.switchToOriginalGUI();
    }

    private IGrid getGrid() {
        IActionHost h = (IActionHost)this.getTarget();
        if (h == null || h.getActionableNode() == null) {
            return null;
        }
        return h.getActionableNode().getGrid();
    }

    public void switchToOriginalGUI() {
        GuiBridge originalGui = null;
        IActionHost ah = this.getActionHost();
        if (ah instanceof WirelessTerminalGuiObject) {
            originalGui = GuiBridge.GUI_WIRELESS_TERM;
        }
        if (ah instanceof PartTerminal) {
            originalGui = GuiBridge.GUI_ME;
        }
        if (ah instanceof PartCraftingTerminal) {
            originalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
        }
        if (ah instanceof PartPatternTerminal) {
            originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }
        if (ah instanceof PartPatternTerminalEx) {
            originalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }
        if (originalGui != null && this.getOpenContext() != null) {
            NetworkHandler.instance.sendTo(new PacketSwitchGuis(originalGui), (EntityPlayerMP)this.getInventoryPlayer().player);
            TileEntity te = this.getOpenContext().getTile();
            Platform.openGUI(this.getInventoryPlayer().player, te, this.getOpenContext().getSide(), originalGui);
        }
    }

    public static int getBitMultiplier(long currentCrafts, long perCraft, long maximumCrafts) {
        int multi = 0;
        long crafted = currentCrafts * perCraft;
        while (Math.ceil((double)crafted / (double)perCraft) > (double)maximumCrafts) {
            perCraft <<= 1;
            ++multi;
        }
        return multi;
    }

    private static class Pattern {
        private HashSet<ICraftingPatternDetails> patternDetails = new HashSet();
        private long requestedCrafts = 0L;

        private Pattern() {
        }

        private void addCraftingTask(CraftableItemResolver.CraftFromPatternTask task) {
            this.patternDetails.add(task.pattern);
            this.requestedCrafts += task.getTotalCraftsDone();
        }

        private long getCraftAmountForItem(IAEItemStack stack) {
            IAEItemStack s = Arrays.stream(((ICraftingPatternDetails)this.patternDetails.stream().findFirst().get()).getCondensedOutputs()).filter(i -> i.isSameType(stack)).findFirst().orElse(null);
            if (s != null) {
                return s.getStackSize();
            }
            return 0L;
        }

        private ICraftingPatternDetails getPattern() {
            return (ICraftingPatternDetails)this.patternDetails.stream().findFirst().get();
        }

        private int getMaxBitMultiplier() {
            return PatternMultiplierHelper.getMaxBitMultiplier(this.getPattern());
        }

        private void applyModification(ItemStack stack, int multiplier) {
            PatternMultiplierHelper.applyModification(stack, multiplier);
        }
    }
}

