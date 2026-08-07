/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package appeng.crafting.v2.resolvers;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.core.localization.GuiText;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingTreeSerializer;
import appeng.crafting.v2.ITreeSerializable;
import appeng.crafting.v2.resolvers.CraftingRequestResolver;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class SimulateMissingItemResolver<StackType extends IAEStack<StackType>>
implements CraftingRequestResolver<StackType> {
    @Override
    @Nonnull
    public List<CraftingTask> provideCraftingRequestResolvers(@Nonnull CraftingRequest<StackType> request, @Nonnull CraftingContext context) {
        if (request.allowSimulation) {
            return Collections.singletonList(new ConjureItemTask<StackType>(request));
        }
        return Collections.emptyList();
    }

    public static class ConjureItemTask<StackType extends IAEStack<StackType>>
    extends CraftingTask<StackType> {
        private long fulfilled = 0L;

        public ConjureItemTask(CraftingRequest<StackType> request) {
            super(request, -2147483548);
        }

        public ConjureItemTask(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
            super(serializer, parent);
            this.fulfilled = serializer.getBuffer().readLong();
        }

        @Override
        public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
            super.serializeTree(serializer);
            serializer.getBuffer().writeLong(this.fulfilled);
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
            this.request.wasSimulated = true;
            context.wasSimulated = true;
            this.fulfilled = this.request.remainingToProcess;
            this.request.fulfill(this, this.request.stack.copy().setStackSize(this.request.remainingToProcess), context);
            return new CraftingTask.StepOutput(Collections.emptyList());
        }

        @Override
        public long partialRefund(CraftingContext context, long amount) {
            if (amount > this.fulfilled) {
                amount = this.fulfilled;
            }
            this.fulfilled -= amount;
            return amount;
        }

        @Override
        public void fullRefund(CraftingContext context) {
            this.fulfilled = 0L;
        }

        @Override
        public void populatePlan(IItemList<IAEItemStack> targetPlan) {
            if (this.fulfilled > 0L && this.request.stack instanceof IAEItemStack) {
                targetPlan.add((IAEItemStack)this.request.stack.copy().setStackSize(this.fulfilled));
            }
        }

        @Override
        public void startOnCpu(CraftingContext context, CraftingCPUCluster cpuCluster, MECraftingInventory craftingInv) {
            throw new IllegalStateException("Trying to start crafting a schedule with simulated items");
        }

        public String toString() {
            return "ConjureItemTask{fulfilled=" + this.fulfilled + ", request=" + this.request + ", priority=" + this.priority + ", state=" + (Object)((Object)this.state) + '}';
        }

        @Override
        public String getTooltipText() {
            return GuiText.Simulation.getLocal() + "\n " + GuiText.Missing.getLocal() + ": " + this.fulfilled;
        }
    }
}

