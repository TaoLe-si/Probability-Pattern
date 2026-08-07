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
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingTreeSerializer;
import appeng.crafting.v2.ITreeSerializable;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;

public abstract class CraftingTask<RequestStackType extends IAEStack<RequestStackType>>
implements ITreeSerializable {
    public static final int PRIORITY_EXTRACT = 2147483547;
    public static final int PRIORITY_CRAFTING_EMITTER = 2147483347;
    public static final int PRIORITY_CRAFT_OFFSET = 0;
    public static final int PRIORITY_SIMULATE_CRAFT = -2147483448;
    public static final int PRIORITY_SIMULATE = -2147483548;
    public final CraftingRequest<RequestStackType> request;
    public final int priority;
    protected State state;
    public static final Comparator<CraftingTask> PRIORITY_COMPARATOR = Comparator.comparing(ct -> -ct.priority);

    public abstract StepOutput calculateOneStep(CraftingContext var1);

    public abstract long partialRefund(CraftingContext var1, long var2);

    public abstract void fullRefund(CraftingContext var1);

    public abstract void populatePlan(IItemList<IAEItemStack> var1);

    public abstract void startOnCpu(CraftingContext var1, CraftingCPUCluster var2, MECraftingInventory var3);

    protected CraftingTask(CraftingRequest<RequestStackType> request, int priority) {
        this.request = request;
        this.priority = priority;
        this.state = State.NEEDS_MORE_WORK;
    }

    protected CraftingTask(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
        this.request = ((CraftingRequest.UsedResolverEntry)parent).parent;
        this.priority = serializer.getBuffer().readInt();
        this.state = serializer.readEnum(State.class);
    }

    @Override
    public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
        serializer.getBuffer().writeInt(this.priority);
        serializer.writeEnum(this.state);
        return Collections.emptyList();
    }

    @Override
    public void loadChildren(List<ITreeSerializable> children) throws IOException {
    }

    public State getState() {
        return this.state;
    }

    public String getTooltipText() {
        return this.toString();
    }

    public static enum State {
        NEEDS_MORE_WORK(true),
        SUCCESS(false),
        FAILURE(false);

        public final boolean needsMoreWork;

        private State(boolean needsMoreWork) {
            this.needsMoreWork = needsMoreWork;
        }
    }

    public static final class StepOutput {
        @Nonnull
        public final List<CraftingRequest<?>> extraInputsRequired;

        public StepOutput() {
            this(Collections.emptyList());
        }

        public StepOutput(@Nonnull List<CraftingRequest<?>> extraInputsRequired) {
            this.extraInputsRequired = extraInputsRequired;
        }
    }
}

