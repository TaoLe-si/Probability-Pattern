/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.Nonnull
 *  net.minecraft.world.World
 *  org.apache.logging.log4j.Level
 */
package appeng.crafting.v2.resolvers;

import appeng.api.config.Actionable;
import appeng.api.config.CraftingMode;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingTreeSerializer;
import appeng.crafting.v2.ITreeSerializable;
import appeng.crafting.v2.resolvers.CraftingRequestResolver;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;

public class CraftableItemResolver
implements CraftingRequestResolver<IAEItemStack> {
    private void logComplexPattrn(ICraftingPatternDetails pattern, long count) {
        if (AEConfig.instance != null && AEConfig.instance.isFeatureEnabled(AEFeature.ComplexPatternLog)) {
            StringBuilder outputs = new StringBuilder();
            for (IAEItemStack stack : pattern.getOutputs()) {
                if (stack == null) continue;
                outputs.append(stack);
                if (stack instanceof AEItemStack) {
                    outputs.append(" <");
                    try {
                        outputs.append(((AEItemStack)stack).getDisplayName());
                    }
                    catch (Exception e) {
                        outputs.append("? " + e.getMessage());
                    }
                    outputs.append('>');
                }
                outputs.append(", ");
            }
            AELog.log(Level.INFO, "Complex crafting pattern found: %d * %s", count, outputs);
        }
    }

    @Override
    @Nonnull
    public List<CraftingTask> provideCraftingRequestResolvers(@Nonnull CraftingRequest<IAEItemStack> request, @Nonnull CraftingContext context) {
        ArrayList<CraftFromPatternTask> tasks = new ArrayList<CraftFromPatternTask>();
        Set<ICraftingPatternDetails> denyList = request.patternParents;
        ArrayList<ICraftingPatternDetails> patterns = new ArrayList<ICraftingPatternDetails>(context.getPrecisePatternsFor((IAEItemStack)request.stack));
        patterns.removeAll(denyList);
        patterns.sort(Comparator.comparing(ICraftingPatternDetails::getPriority).reversed());
        if (request.substitutionMode == CraftingRequest.SubstitutionMode.ACCEPT_FUZZY) {
            ArrayList<ICraftingPatternDetails> fuzzyPatterns = new ArrayList<ICraftingPatternDetails>(context.getFuzzyPatternsFor((IAEItemStack)request.stack));
            fuzzyPatterns.removeAll(denyList);
            fuzzyPatterns.sort(Comparator.comparing(ICraftingPatternDetails::getPriority).reversed());
            patterns.addAll(fuzzyPatterns);
        }
        int priority = 0 + patterns.size() - 1;
        tasks.ensureCapacity(patterns.size() + 1);
        for (ICraftingPatternDetails pattern : patterns) {
            if (context.isPatternComplex(pattern)) {
                this.logComplexPattrn(pattern, request.remainingToProcess);
                int i = 0;
                while ((long)i < request.remainingToProcess) {
                    CraftFromPatternTask task = new CraftFromPatternTask(request, pattern, priority, request.craftingMode == CraftingMode.IGNORE_MISSING, true);
                    if (task.getState() != CraftingTask.State.FAILURE) {
                        tasks.add(task);
                    }
                    ++i;
                }
            } else {
                CraftFromPatternTask task = new CraftFromPatternTask(request, pattern, priority, request.craftingMode == CraftingMode.IGNORE_MISSING, false);
                if (task.getState() != CraftingTask.State.FAILURE) {
                    tasks.add(task);
                }
            }
            --priority;
        }
        if (!patterns.isEmpty()) {
            ICraftingPatternDetails pattern = (ICraftingPatternDetails)patterns.get(0);
            if (context.isPatternComplex(pattern)) {
                int i = 0;
                while ((long)i < request.remainingToProcess) {
                    CraftFromPatternTask task = new CraftFromPatternTask(request, pattern, priority, true, true);
                    if (task.getState() != CraftingTask.State.FAILURE) {
                        tasks.add(task);
                    }
                    ++i;
                }
            } else {
                CraftFromPatternTask task = new CraftFromPatternTask(request, pattern, -2147483448, true, false);
                if (task.getState() != CraftingTask.State.FAILURE) {
                    tasks.add(task);
                }
            }
        }
        return Collections.unmodifiableList(tasks);
    }

    public static class CraftFromPatternTask
    extends CraftingTask<IAEItemStack> {
        public final ICraftingPatternDetails pattern;
        public final boolean allowSimulation;
        public final boolean isComplex;
        protected final IAEItemStack[] patternRecursionInputs;
        protected final IAEItemStack[] patternInputs;
        protected final IAEItemStack[] patternOutputs;
        protected final IAEItemStack matchingOutput;
        public IAEItemStack craftingMachine;
        protected final ArrayList<RequestAndPerCraftAmount> childRequests = new ArrayList();
        protected final ArrayList<CraftingRequest> complexRequestPerSlot = new ArrayList();
        protected final Map<IAEItemStack, CraftingRequest<IAEItemStack>> childRecursionRequests = new HashMap<IAEItemStack, CraftingRequest<IAEItemStack>>();
        protected final IdentityHashMap<IAEItemStack, Long> byproducts = new IdentityHashMap();
        protected boolean requestedInputs = false;
        protected long totalCraftsDone = 0L;
        protected long fulfilledAmount = 0L;
        protected long matchingOutputRemainderItems = 0L;

        public CraftFromPatternTask(CraftingRequest<IAEItemStack> request, ICraftingPatternDetails pattern, int priority, boolean allowSimulation, boolean isComplex) {
            super(request, priority);
            this.pattern = pattern;
            this.allowSimulation = allowSimulation;
            this.isComplex = isComplex;
            IAEItemStack[] patternInputs = pattern.getCondensedInputs();
            IAEItemStack[] patternOutputs = pattern.getCondensedOutputs();
            if (!CraftFromPatternTask.hasRecursiveInputs(patternInputs, patternOutputs)) {
                this.patternInputs = patternInputs;
                this.patternOutputs = patternOutputs;
                this.patternRecursionInputs = new IAEItemStack[0];
            } else {
                patternInputs = (IAEItemStack[])Arrays.stream(patternInputs).map(IAEItemStack::copy).toArray(IAEItemStack[]::new);
                patternOutputs = (IAEItemStack[])Arrays.stream(patternOutputs).map(IAEItemStack::copy).toArray(IAEItemStack[]::new);
                this.patternRecursionInputs = CraftFromPatternTask.calculateRecursiveInputs(patternInputs, patternOutputs);
                this.patternInputs = CraftFromPatternTask.filterMeaningfulStacks(patternInputs);
                this.patternOutputs = CraftFromPatternTask.filterMeaningfulStacks(patternOutputs);
            }
            IAEItemStack matchingOutput = null;
            for (IAEItemStack patternOutput : this.patternOutputs) {
                if (!this.isOutputSameAs(patternOutput)) continue;
                matchingOutput = patternOutput;
                break;
            }
            this.matchingOutput = matchingOutput;
            if (matchingOutput == null) {
                this.state = CraftingTask.State.FAILURE;
            }
        }

        public CraftFromPatternTask(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
            super(serializer, parent);
            ByteBuf buffer = serializer.getBuffer();
            this.pattern = serializer.readPattern();
            this.allowSimulation = buffer.readBoolean();
            this.isComplex = buffer.readBoolean();
            this.matchingOutput = serializer.readItemStack();
            this.craftingMachine = serializer.readItemStack();
            this.totalCraftsDone = buffer.readLong();
            IAEItemStack[] patternInputs = this.pattern.getCondensedInputs();
            IAEItemStack[] patternOutputs = this.pattern.getCondensedOutputs();
            if (!CraftFromPatternTask.hasRecursiveInputs(patternInputs, patternOutputs)) {
                this.patternInputs = patternInputs;
                this.patternOutputs = patternOutputs;
                this.patternRecursionInputs = new IAEItemStack[0];
            } else {
                patternInputs = (IAEItemStack[])Arrays.stream(patternInputs).map(IAEItemStack::copy).toArray(IAEItemStack[]::new);
                patternOutputs = (IAEItemStack[])Arrays.stream(patternOutputs).map(IAEItemStack::copy).toArray(IAEItemStack[]::new);
                this.patternRecursionInputs = CraftFromPatternTask.calculateRecursiveInputs(patternInputs, patternOutputs);
                this.patternInputs = CraftFromPatternTask.filterMeaningfulStacks(patternInputs);
                this.patternOutputs = CraftFromPatternTask.filterMeaningfulStacks(patternOutputs);
            }
        }

        private static IAEItemStack[] calculateRecursiveInputs(IAEItemStack[] pInputs, IAEItemStack[] pOutputs) {
            IAEItemStack[] recInputs = null;
            for (IAEItemStack output : pOutputs) {
                for (IAEItemStack input : pInputs) {
                    IAEItemStack recInput;
                    if (!input.equals(output)) continue;
                    long netProduced = output.getStackSize() - input.getStackSize();
                    if (netProduced > 0L) {
                        recInput = input.copy();
                        input.setStackSize(0L);
                        output.setStackSize(netProduced);
                    } else {
                        recInput = (IAEItemStack)input.copy().setStackSize(input.getStackSize() + netProduced);
                        input.setStackSize(-netProduced);
                        output.setStackSize(0L);
                    }
                    if (!recInput.isMeaningful()) continue;
                    if (recInputs == null) {
                        recInputs = new IAEItemStack[]{recInput};
                        continue;
                    }
                    recInputs = Arrays.copyOf(recInputs, recInputs.length + 1);
                    recInputs[recInputs.length - 1] = recInput;
                }
            }
            return recInputs == null ? new IAEItemStack[]{} : recInputs;
        }

        private static boolean hasRecursiveInputs(IAEItemStack[] pInputs, IAEItemStack[] pOutputs) {
            for (IAEItemStack output : pOutputs) {
                for (IAEItemStack input : pInputs) {
                    if (!input.equals(output)) continue;
                    return true;
                }
            }
            return false;
        }

        private static IAEItemStack[] filterMeaningfulStacks(IAEItemStack[] stacks) {
            int i;
            int j = 0;
            for (i = 0; i < stacks.length; ++i) {
                IAEItemStack stack = stacks[i];
                if (!stack.isMeaningful()) continue;
                stacks[j] = stack;
                ++j;
            }
            return i == j ? stacks : Arrays.copyOf(stacks, j);
        }

        @Override
        public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
            super.serializeTree(serializer);
            ByteBuf buffer = serializer.getBuffer();
            serializer.writePattern(this.pattern);
            buffer.writeBoolean(this.allowSimulation);
            buffer.writeBoolean(this.isComplex);
            serializer.writeItemStack(this.matchingOutput);
            serializer.writeItemStack(this.craftingMachine);
            buffer.writeLong(this.totalCraftsDone);
            return this.childRequests;
        }

        @Override
        public void loadChildren(List<ITreeSerializable> children) throws IOException {
            for (ITreeSerializable child : children) {
                if (!(child instanceof RequestAndPerCraftAmount)) {
                    throw new UnsupportedOperationException("Invalid craftable request child type: " + child.getClass());
                }
                this.childRequests.add((RequestAndPerCraftAmount)child);
            }
        }

        public List<CraftingRequest<IAEItemStack>> getChildRequests() {
            return this.childRequests.stream().map(r -> r.request).collect(Collectors.toList());
        }

        public long getTotalCraftsDone() {
            return this.totalCraftsDone;
        }

        public IAEItemStack getCraftingMachine() {
            return this.craftingMachine;
        }

        public boolean isOutputSameAs(IAEItemStack otherStack) {
            if (this.request.substitutionMode == CraftingRequest.SubstitutionMode.ACCEPT_FUZZY) {
                return ((IAEItemStack)this.request.stack).fuzzyComparison(otherStack, FuzzyMode.IGNORE_ALL);
            }
            return ((IAEItemStack)this.request.stack).isSameType(otherStack);
        }

        public boolean isValidSubstitute(IAEItemStack reference, IAEItemStack stack, World world) {
            if (!this.pattern.isCraftable()) {
                return true;
            }
            IAEItemStack[] rawInputs = this.pattern.getInputs();
            for (int slot = 0; slot < rawInputs.length; ++slot) {
                if (rawInputs[slot] == null || !rawInputs[slot].isSameType(reference)) continue;
                return this.pattern.isValidItemForSlot(slot, stack.getItemStack(), world);
            }
            return true;
        }

        public boolean isValidSubstitute(IAEItemStack reference, IAEItemStack stack, World world, int slot) {
            if (!this.pattern.isCraftable()) {
                return true;
            }
            IAEItemStack[] rawInputs = this.pattern.getInputs();
            return this.pattern.isValidItemForSlot(slot, stack.getItemStack(), world);
        }

        /*
         * WARNING - void declaration
         */
        @Override
        public CraftingTask.StepOutput calculateOneStep(CraftingContext context) {
            if (this.request.remainingToProcess <= 0L) {
                this.state = CraftingTask.State.SUCCESS;
                return new CraftingTask.StepOutput(Collections.emptyList());
            }
            boolean canUseSubstitutes = this.pattern.canSubstitute();
            CraftingRequest.SubstitutionMode childMode = canUseSubstitutes ? CraftingRequest.SubstitutionMode.ACCEPT_FUZZY : CraftingRequest.SubstitutionMode.PRECISE;
            long toCraft = Platform.ceilDiv(this.isComplex ? 1L : this.request.remainingToProcess, this.matchingOutput.getStackSize());
            if (this.requestedInputs) {
                long maxCraftable = toCraft;
                for (CraftingRequest<IAEItemStack> recInputChild : this.childRecursionRequests.values()) {
                    if (recInputChild.remainingToProcess <= 0L) continue;
                    maxCraftable = 0L;
                    break;
                }
                for (RequestAndPerCraftAmount inputChildPair : this.childRequests) {
                    CraftingRequest<IAEItemStack> inputChild = inputChildPair.request;
                    long l = ((IAEItemStack)inputChild.stack).getStackSize() / toCraft;
                    long available = ((IAEItemStack)inputChild.stack).getStackSize() - inputChild.remainingToProcess;
                    long fullRecipes = available / l;
                    maxCraftable = Math.min(maxCraftable, fullRecipes);
                }
                long producedMatchingOutput = Math.multiplyExact(maxCraftable, this.matchingOutput.getStackSize());
                this.matchingOutputRemainderItems = producedMatchingOutput > this.request.remainingToProcess ? producedMatchingOutput - this.request.remainingToProcess : 0L;
                this.fulfilledAmount = producedMatchingOutput - this.matchingOutputRemainderItems;
                this.request.fulfill(this, (IAEItemStack)this.matchingOutput.copy().setStackSize(this.fulfilledAmount), context);
                if (this.matchingOutputRemainderItems > 0L) {
                    context.byproductsInventory.injectItems((IAEItemStack)this.matchingOutput.copy().setStackSize(this.matchingOutputRemainderItems), Actionable.MODULATE, context.actionSource);
                }
                if (this.isComplex && this.fulfilledAmount > 0L) {
                    IAEItemStack[] iAEItemStackArray;
                    void var11_19;
                    if (maxCraftable > 1L) {
                        throw new IllegalStateException("Complex recipe got calculated with more than 1 set of inputs at a time");
                    }
                    IAEItemStack[] inputs = new IAEItemStack[9];
                    boolean bl = false;
                    while (var11_19 < this.complexRequestPerSlot.size()) {
                        IAEItemStack[] slotRequest = this.complexRequestPerSlot.get((int)var11_19);
                        if (slotRequest != null) {
                            IAEItemStack resolvedItem = (IAEItemStack)slotRequest.getOneResolvedType();
                            inputs[var11_19] = resolvedItem.copy();
                        }
                        ++var11_19;
                    }
                    for (IAEItemStack leftover : iAEItemStackArray = context.simulateComplexCrafting(inputs, this.pattern)) {
                        if (leftover == null || leftover.getStackSize() <= 0L) continue;
                        context.byproductsInventory.injectItems(leftover, Actionable.MODULATE, context.actionSource);
                        this.byproducts.put(leftover.copy(), leftover.getStackSize());
                    }
                }
                for (IAEItemStack output : this.patternOutputs) {
                    if (output == this.matchingOutput) continue;
                    IAEItemStack injected = (IAEItemStack)output.copy().setStackSize(Math.multiplyExact(maxCraftable, output.getStackSize()));
                    context.byproductsInventory.injectItems(injected, Actionable.MODULATE, context.actionSource);
                    this.byproducts.put(injected.copy(), output.getStackSize());
                }
                this.totalCraftsDone = maxCraftable;
                if (maxCraftable != toCraft) {
                    for (RequestAndPerCraftAmount requestAndPerCraftAmount : this.childRequests) {
                        CraftingRequest<IAEItemStack> inputChild = requestAndPerCraftAmount.request;
                        long actuallyNeeded = Math.multiplyExact(((IAEItemStack)inputChild.stack).getStackSize() / toCraft, maxCraftable);
                        long produced = ((IAEItemStack)inputChild.stack).getStackSize() - Math.max(inputChild.remainingToProcess, 0L);
                        if (produced <= actuallyNeeded) continue;
                        if (maxCraftable == 0L) {
                            inputChild.fullRefund(context);
                            continue;
                        }
                        inputChild.partialRefund(context, produced - actuallyNeeded);
                    }
                    if (maxCraftable == 0L) {
                        for (CraftingRequest craftingRequest : this.childRecursionRequests.values()) {
                            craftingRequest.fullRefund(context);
                        }
                    }
                }
                if (this.totalCraftsDone > 0L) {
                    for (RequestAndPerCraftAmount requestAndPerCraftAmount : this.childRequests) {
                        if (!requestAndPerCraftAmount.request.wasSimulated) continue;
                        this.request.wasSimulated = true;
                        break;
                    }
                }
                this.craftingMachine = context.getCrafterIconForPattern(this.pattern);
                this.state = CraftingTask.State.SUCCESS;
                return new CraftingTask.StepOutput(Collections.emptyList());
            }
            this.request.patternParents.add(this.pattern);
            ArrayList<CraftingRequest<IAEItemStack>> newChildren = new ArrayList<CraftingRequest<IAEItemStack>>(this.patternRecursionInputs.length + this.patternInputs.length);
            if (this.isComplex) {
                if (toCraft > 1L) {
                    throw new IllegalStateException();
                }
                IAEItemStack[] slotInputs = this.pattern.getInputs();
                for (int slot = 0; slot < slotInputs.length; ++slot) {
                    IAEItemStack input = slotInputs[slot];
                    if (input == null) {
                        this.complexRequestPerSlot.add(null);
                        continue;
                    }
                    long amount = Math.multiplyExact(input.getStackSize(), toCraft);
                    int finalSlot = slot;
                    CraftingRequest<IAEItemStack> req = new CraftingRequest<IAEItemStack>((IAEItemStack)input.copy().setStackSize(amount), childMode, IAEItemStack.class, this.allowSimulation, this.request.craftingMode, stack -> this.isValidSubstitute(input, (IAEItemStack)stack, context.world, finalSlot));
                    this.complexRequestPerSlot.add(req);
                    newChildren.add(req);
                    this.childRequests.add(new RequestAndPerCraftAmount(req, input.getStackSize()));
                }
                newChildren.sort(Comparator.comparingInt(r -> ((IAEItemStack)r.stack).getItem().hasContainerItem(((IAEItemStack)r.stack).getItemStack()) ? 1 : 0).thenComparingInt(r -> ((IAEItemStack)r.stack).getItem().getItemStackLimit(((IAEItemStack)r.stack).getItemStack()) == 1 ? 1 : 0));
            } else {
                if (this.patternRecursionInputs.length > 0) {
                    for (IAEItemStack recInput : this.patternRecursionInputs) {
                        CraftingRequest<IAEItemStack> craftingRequest = new CraftingRequest<IAEItemStack>(recInput.copy(), childMode, IAEItemStack.class, this.allowSimulation, this.request.craftingMode, stack -> this.isValidSubstitute(recInput, (IAEItemStack)stack, context.world));
                        newChildren.add(craftingRequest);
                        this.childRecursionRequests.put(recInput, craftingRequest);
                    }
                    this.state = CraftingTask.State.NEEDS_MORE_WORK;
                }
                for (IAEItemStack input : this.patternInputs) {
                    long l = Math.multiplyExact(input.getStackSize(), toCraft);
                    CraftingRequest<IAEItemStack> req = new CraftingRequest<IAEItemStack>((IAEItemStack)input.copy().setStackSize(l), childMode, IAEItemStack.class, this.allowSimulation, this.request.craftingMode, stack -> this.isValidSubstitute(input, (IAEItemStack)stack, context.world));
                    newChildren.add(req);
                    this.childRequests.add(new RequestAndPerCraftAmount(req, input.getStackSize()));
                }
            }
            this.childRequests.trimToSize();
            this.complexRequestPerSlot.trimToSize();
            this.requestedInputs = true;
            this.state = CraftingTask.State.NEEDS_MORE_WORK;
            return new CraftingTask.StepOutput(Collections.unmodifiableList(newChildren));
        }

        @Override
        public long partialRefund(CraftingContext context, long amount) {
            long oldTotalCrafts = this.totalCraftsDone;
            long oldTotalMade = this.totalCraftsDone * this.matchingOutput.getStackSize();
            long oldFulfilled = this.fulfilledAmount;
            long newFulfilled = oldFulfilled - amount;
            long newTotalCrafts = Platform.ceilDiv(newFulfilled, this.matchingOutput.getStackSize());
            long newTotalMade = newTotalCrafts * this.matchingOutput.getStackSize();
            long oldRemainder = this.matchingOutputRemainderItems;
            long newRemainder = newTotalMade - newFulfilled;
            if (newRemainder < 0L || newRemainder > this.matchingOutput.getStackSize()) {
                throw new IllegalStateException("Refund remainder invariant broken: " + newRemainder + " - " + this);
            }
            if (newTotalCrafts <= 0L) {
                this.fullRefund(context);
                return amount;
            }
            if (newRemainder != oldRemainder) {
                if (newRemainder > oldRemainder) {
                    context.byproductsInventory.injectItems((IAEItemStack)this.matchingOutput.copy().setStackSize(newRemainder - oldRemainder), Actionable.MODULATE, context.actionSource);
                } else {
                    context.byproductsInventory.extractItems((IAEItemStack)this.matchingOutput.copy().setStackSize(oldRemainder - newRemainder), Actionable.MODULATE, context.actionSource);
                }
                this.matchingOutputRemainderItems = newRemainder;
            }
            if (newTotalCrafts != oldTotalCrafts) {
                if (newTotalCrafts > oldTotalCrafts) {
                    throw new IllegalStateException("Refund total crafts invariant broken: " + newTotalCrafts + " - " + this);
                }
                this.totalCraftsDone = newTotalCrafts;
                long craftsRefunded = oldTotalCrafts - newTotalCrafts;
                for (RequestAndPerCraftAmount requestAndPerCraftAmount : this.childRequests) {
                    requestAndPerCraftAmount.request.partialRefund(context, requestAndPerCraftAmount.perCraftAmount * craftsRefunded);
                }
                for (Map.Entry entry : this.byproducts.entrySet()) {
                    IAEItemStack byproductStack = (IAEItemStack)entry.getKey();
                    long perCraft = (Long)entry.getValue();
                    context.byproductsInventory.extractItems((IAEItemStack)byproductStack.copy().setStackSize(perCraft * craftsRefunded), Actionable.MODULATE, context.actionSource);
                    ((IAEItemStack)entry.getKey()).setStackSize(byproductStack.getStackSize() - craftsRefunded * perCraft);
                }
            }
            this.fulfilledAmount = newFulfilled;
            return oldFulfilled - newFulfilled;
        }

        @Override
        public void fullRefund(CraftingContext context) {
            this.request.patternParents.remove(this.pattern);
            this.totalCraftsDone = 0L;
            this.fulfilledAmount = 0L;
            this.childRequests.forEach(req -> req.request.fullRefund(context));
            this.childRequests.clear();
            this.childRecursionRequests.values().forEach(req -> req.fullRefund(context));
            this.childRecursionRequests.clear();
            for (IAEItemStack byproduct : this.byproducts.keySet()) {
                context.byproductsInventory.extractItems(byproduct.copy(), Actionable.MODULATE, context.actionSource);
            }
            this.byproducts.clear();
            if (this.matchingOutputRemainderItems > 0L) {
                context.byproductsInventory.extractItems((IAEItemStack)this.matchingOutput.copy().setStackSize(this.matchingOutputRemainderItems), Actionable.MODULATE, context.actionSource);
                this.matchingOutputRemainderItems = 0L;
            }
        }

        @Override
        public void populatePlan(IItemList<IAEItemStack> targetPlan) {
            if (this.totalCraftsDone == 0L) {
                return;
            }
            for (IAEItemStack output : this.patternOutputs) {
                targetPlan.addRequestable((IAEItemStack)((IAEItemStack)((IAEItemStack)output.copy().setStackSize(0L)).setCountRequestable(output.getStackSize() * this.totalCraftsDone)).setCountRequestableCrafts(this.totalCraftsDone));
            }
        }

        @Override
        public void startOnCpu(CraftingContext context, CraftingCPUCluster cpuCluster, MECraftingInventory craftingInv) {
            cpuCluster.addCrafting(this.pattern, this.totalCraftsDone);
        }

        public String toString() {
            return "CraftFromPatternTask{request=" + this.request + ", pattern=" + this.pattern + ", allowSimulation=" + this.allowSimulation + ", matchingOutput=" + this.matchingOutput + ", requestedInputs=" + this.requestedInputs + ", totalCraftsDone=" + this.totalCraftsDone + ", priority=" + this.priority + ", state=" + (Object)((Object)this.state) + '}';
        }

        @Override
        public String getTooltipText() {
            return GuiText.Crafting.getLocal() + "\n " + GuiText.Crafts.getLocal() + ": " + this.totalCraftsDone + "\n " + GuiText.Interface.getLocal() + ": " + Platform.getItemDisplayName(this.craftingMachine);
        }
    }

    public static class RequestAndPerCraftAmount
    implements ITreeSerializable {
        public final CraftingRequest<IAEItemStack> request;
        public final long perCraftAmount;

        public RequestAndPerCraftAmount(CraftingRequest<IAEItemStack> request, long perCraftAmount) {
            this.request = request;
            this.perCraftAmount = perCraftAmount;
        }

        public RequestAndPerCraftAmount(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
            this.perCraftAmount = serializer.getBuffer().readLong();
            this.request = new CraftingRequest(serializer, parent);
        }

        @Override
        public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
            serializer.getBuffer().writeLong(this.perCraftAmount);
            return this.request.serializeTree(serializer);
        }

        @Override
        public void loadChildren(List<ITreeSerializable> children) throws IOException {
            this.request.loadChildren(children);
        }

        @Override
        public ITreeSerializable getSerializationParent() {
            return this.request;
        }
    }
}

