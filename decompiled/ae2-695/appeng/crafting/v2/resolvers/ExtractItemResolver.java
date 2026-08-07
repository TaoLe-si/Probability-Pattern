/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package appeng.crafting.v2.resolvers;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.localization.GuiText;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingTreeSerializer;
import appeng.crafting.v2.ITreeSerializable;
import appeng.crafting.v2.resolvers.CraftingRequestResolver;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.Platform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

public class ExtractItemResolver
implements CraftingRequestResolver<IAEItemStack> {
    @Override
    @Nonnull
    public List<CraftingTask> provideCraftingRequestResolvers(@Nonnull CraftingRequest<IAEItemStack> request, @Nonnull CraftingContext context) {
        if (request.substitutionMode == CraftingRequest.SubstitutionMode.PRECISE_FRESH) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new ExtractItemTask(request));
    }

    public static class ExtractItemTask
    extends CraftingTask<IAEItemStack> {
        public final ArrayList<IAEItemStack> removedFromSystem = new ArrayList();
        public final ArrayList<IAEItemStack> removedFromByproducts = new ArrayList();

        public ExtractItemTask(CraftingRequest<IAEItemStack> request) {
            super(request, 2147483547);
        }

        public ExtractItemTask(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
            super(serializer, parent);
            serializer.readList(this.removedFromSystem, serializer::readItemStack);
            serializer.readList(this.removedFromByproducts, serializer::readItemStack);
        }

        @Override
        public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
            super.serializeTree(serializer);
            serializer.writeList(this.removedFromSystem, serializer::writeItemStack);
            serializer.writeList(this.removedFromByproducts, serializer::writeItemStack);
            return Collections.emptyList();
        }

        @Override
        public void loadChildren(List<ITreeSerializable> children) throws IOException {
        }

        @Override
        public CraftingTask.StepOutput calculateOneStep(CraftingContext context) {
            this.state = CraftingTask.State.SUCCESS;
            if (this.request.remainingToProcess <= 0L) {
                return new CraftingTask.StepOutput(Collections.emptyList());
            }
            this.extractExact(context, context.byproductsInventory, this.removedFromByproducts);
            if (this.request.remainingToProcess > 0L) {
                this.extractExact(context, context.itemModel, this.removedFromSystem);
            }
            if (this.request.remainingToProcess > 0L && this.request.substitutionMode == CraftingRequest.SubstitutionMode.ACCEPT_FUZZY) {
                this.extractFuzzy(context, context.byproductsInventory, this.removedFromByproducts);
                if (this.request.remainingToProcess > 0L) {
                    this.extractFuzzy(context, context.itemModel, this.removedFromSystem);
                }
            }
            this.removedFromSystem.trimToSize();
            this.removedFromByproducts.trimToSize();
            return new CraftingTask.StepOutput(Collections.emptyList());
        }

        private void extractExact(CraftingContext context, MECraftingInventory source, List<IAEItemStack> removedList) {
            IAEItemStack exactMatching = source.getItemList().findPrecise((IAEItemStack)this.request.stack);
            if (exactMatching != null) {
                long requestSize = Math.min(this.request.remainingToProcess, exactMatching.getStackSize());
                IAEItemStack extracted = source.extractItems((IAEItemStack)exactMatching.copy().setStackSize(requestSize), Actionable.MODULATE, context.actionSource);
                if (extracted != null && extracted.getStackSize() > 0L) {
                    extracted.setCraftable(false);
                    this.request.fulfill(this, extracted, context);
                    removedList.add(extracted.copy());
                }
            }
        }

        private void extractFuzzy(CraftingContext context, MECraftingInventory source, List<IAEItemStack> removedList) {
            Collection<IAEItemStack> fuzzyMatching = source.getItemList().findFuzzy((IAEItemStack)this.request.stack, FuzzyMode.IGNORE_ALL);
            for (IAEItemStack candidate : fuzzyMatching) {
                if (candidate == null || !this.request.acceptableSubstituteFn.test(candidate)) continue;
                long requestSize = Math.min(this.request.remainingToProcess, candidate.getStackSize());
                IAEItemStack extracted = source.extractItems((IAEItemStack)candidate.copy().setStackSize(requestSize), Actionable.MODULATE, context.actionSource);
                if (extracted == null || extracted.getStackSize() <= 0L) continue;
                extracted.setCraftable(false);
                this.request.fulfill(this, extracted, context);
                removedList.add(extracted.copy());
            }
        }

        @Override
        public long partialRefund(CraftingContext context, long amount) {
            long originalAmount = amount;
            Collections.reverse(this.removedFromSystem);
            Collections.reverse(this.removedFromByproducts);
            amount = this.partialRefundFrom(context, amount, this.removedFromSystem, context.itemModel);
            amount = this.partialRefundFrom(context, amount, this.removedFromByproducts, context.byproductsInventory);
            Collections.reverse(this.removedFromSystem);
            Collections.reverse(this.removedFromByproducts);
            return originalAmount - amount;
        }

        private long partialRefundFrom(CraftingContext context, long amount, List<IAEItemStack> source, MECraftingInventory target) {
            Iterator<IAEItemStack> removedIt = source.iterator();
            while (removedIt.hasNext() && amount > 0L) {
                IAEItemStack available = removedIt.next();
                long availAmount = available.getStackSize();
                if (availAmount > amount) {
                    target.injectItems((IAEItemStack)available.copy().setStackSize(amount), Actionable.MODULATE, context.actionSource);
                    available.setStackSize(availAmount - amount);
                    amount = 0L;
                    continue;
                }
                target.injectItems(available, Actionable.MODULATE, context.actionSource);
                amount -= availAmount;
                removedIt.remove();
            }
            return amount;
        }

        @Override
        public void fullRefund(CraftingContext context) {
            for (IAEItemStack removed : this.removedFromByproducts) {
                context.byproductsInventory.injectItems(removed, Actionable.MODULATE, context.actionSource);
            }
            for (IAEItemStack removed : this.removedFromSystem) {
                context.itemModel.injectItems(removed, Actionable.MODULATE, context.actionSource);
            }
            this.removedFromSystem.clear();
        }

        @Override
        public void populatePlan(IItemList<IAEItemStack> targetPlan) {
            for (IAEItemStack removed : this.removedFromSystem) {
                targetPlan.add(removed.copy());
            }
        }

        @Override
        public void startOnCpu(CraftingContext context, CraftingCPUCluster cpuCluster, MECraftingInventory craftingInv) {
            for (IAEItemStack stack : this.removedFromSystem) {
                if (stack.getStackSize() <= 0L) continue;
                IAEItemStack extracted = craftingInv.extractItems(stack, Actionable.MODULATE, context.actionSource);
                if (extracted == null || extracted.getStackSize() != stack.getStackSize()) {
                    if (cpuCluster.isMissingMode()) {
                        if (extracted == null) {
                            cpuCluster.addEmitable(stack.copy());
                            stack.setStackSize(0L);
                            continue;
                        }
                        if (extracted.getStackSize() != stack.getStackSize()) {
                            cpuCluster.addEmitable((IAEItemStack)stack.copy().setStackSize(stack.getStackSize() - extracted.getStackSize()));
                            stack.setStackSize(extracted.getStackSize());
                        }
                    } else {
                        throw new CraftBranchFailure(stack, stack.getStackSize());
                    }
                }
                cpuCluster.addStorage(extracted);
            }
        }

        public String toString() {
            return "ExtractItemTask{request=" + this.request + ", removedFromSystem=" + this.removedFromSystem + ", priority=" + this.priority + ", state=" + (Object)((Object)this.state) + '}';
        }

        @Override
        public String getTooltipText() {
            long removedCount = 0L;
            long removedTypes = 0L;
            StringBuilder itemList = new StringBuilder();
            for (IAEItemStack stack : this.removedFromSystem) {
                if (stack == null) continue;
                removedCount += stack.getStackSize();
                ++removedTypes;
                itemList.append("\n ");
                itemList.append(stack);
                itemList.append(" (");
                itemList.append(Platform.getItemDisplayName(stack));
                itemList.append(')');
            }
            for (IAEItemStack stack : this.removedFromByproducts) {
                if (stack == null) continue;
                removedCount += stack.getStackSize();
                ++removedTypes;
                itemList.append("\n ");
                itemList.append(stack);
                itemList.append(" (");
                itemList.append(Platform.getItemDisplayName(stack));
                itemList.append(')');
            }
            return GuiText.StoredItems.getLocal() + ": " + removedCount + "\n " + GuiText.StoredStacks.getLocal() + ": " + removedTypes + itemList;
        }
    }
}

