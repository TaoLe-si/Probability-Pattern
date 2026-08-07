/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package appeng.crafting.v2;

import appeng.api.config.CraftingMode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.crafting.v2.CraftingCalculations;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingTreeSerializer;
import appeng.crafting.v2.ITreeSerializable;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class CraftingRequest<StackType extends IAEStack<StackType>>
implements ITreeSerializable {
    public final Class<StackType> stackTypeClass;
    public final StackType stack;
    public final SubstitutionMode substitutionMode;
    public final Predicate<StackType> acceptableSubstituteFn;
    public final CraftingMode craftingMode;
    public final List<UsedResolverEntry<StackType>> usedResolvers = new ArrayList<UsedResolverEntry<StackType>>();
    public final boolean allowSimulation;
    public volatile long remainingToProcess;
    private volatile long byteCost = 0L;
    private volatile long untransformedByteCost = 0L;
    public volatile boolean wasSimulated = false;
    public boolean incomplete = false;
    public final Set<ICraftingPatternDetails> patternParents = new HashSet<ICraftingPatternDetails>();

    @Override
    public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
        ByteBuf buffer = serializer.getBuffer();
        serializer.writeStack((IAEStack<?>)this.stack);
        serializer.writeEnum(this.substitutionMode);
        buffer.writeBoolean(this.allowSimulation);
        buffer.writeLong(this.remainingToProcess);
        buffer.writeLong(this.byteCost);
        buffer.writeLong(this.untransformedByteCost);
        buffer.writeBoolean(this.wasSimulated);
        buffer.writeBoolean(this.incomplete);
        buffer.writeInt(this.craftingMode.ordinal());
        return this.usedResolvers;
    }

    @Override
    public void loadChildren(List<ITreeSerializable> children) throws IOException {
        for (ITreeSerializable child : children) {
            this.usedResolvers.add((UsedResolverEntry)child);
        }
    }

    public CraftingRequest(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
        ByteBuf buffer = serializer.getBuffer();
        this.stack = serializer.readStack();
        if (this.stack == null) {
            this.stackTypeClass = IAEItemStack.class;
        } else if (this.stack instanceof IAEItemStack) {
            this.stackTypeClass = IAEItemStack.class;
        } else if (this.stack instanceof IAEFluidStack) {
            this.stackTypeClass = IAEFluidStack.class;
        } else {
            throw new UnsupportedOperationException("Unknown stack type " + this.stack.getClass());
        }
        this.substitutionMode = serializer.readEnum(SubstitutionMode.class);
        this.allowSimulation = buffer.readBoolean();
        this.remainingToProcess = buffer.readLong();
        this.byteCost = buffer.readLong();
        this.untransformedByteCost = buffer.readLong();
        this.wasSimulated = buffer.readBoolean();
        this.incomplete = buffer.readBoolean();
        int index = buffer.readInt();
        this.craftingMode = index < 0 || index >= CraftingMode.values().length || CraftingMode.values()[index] == CraftingMode.STANDARD ? CraftingMode.STANDARD : CraftingMode.IGNORE_MISSING;
        this.acceptableSubstituteFn = x -> true;
    }

    public CraftingRequest(StackType stack, SubstitutionMode substitutionMode, Class<StackType> stackTypeClass, boolean allowSimulation, CraftingMode craftingMode, Predicate<StackType> acceptableSubstituteFn) {
        this.stackTypeClass = stackTypeClass;
        this.stack = stack;
        this.substitutionMode = substitutionMode;
        this.acceptableSubstituteFn = acceptableSubstituteFn;
        this.remainingToProcess = stack.getStackSize();
        this.allowSimulation = allowSimulation;
        this.craftingMode = craftingMode;
        if (stackTypeClass != IAEItemStack.class && stackTypeClass != IAEFluidStack.class) {
            throw new IllegalArgumentException("Invalid stack type for a crafting request: " + stackTypeClass.getName());
        }
    }

    public CraftingRequest(StackType request, SubstitutionMode substitutionMode, Class<StackType> stackTypeClass, boolean allowSimulation, CraftingMode craftingMode) {
        this((IAEStack)request, substitutionMode, (Class<IAEStack>)stackTypeClass, allowSimulation, craftingMode, x -> true);
        if (substitutionMode == SubstitutionMode.ACCEPT_FUZZY) {
            throw new IllegalArgumentException("Fuzzy requests must have a substitution-valid predicate");
        }
    }

    public long getByteCost() {
        return this.byteCost;
    }

    private String getReadableStackName() {
        String readableName = "?";
        if (this.stack instanceof AEItemStack) {
            try {
                readableName = ((AEItemStack)this.stack).getDisplayName();
            }
            catch (Exception e) {
                AELog.warn(e, "Trying to obtain display name for " + this.stack);
                readableName = "<EXCEPTION>";
            }
        }
        return readableName;
    }

    public String toString() {
        return "CraftingRequest{request=" + this.stack + "<" + this.getReadableStackName() + ">, substitutionMode=" + (Object)((Object)this.substitutionMode) + ", remainingToProcess=" + this.remainingToProcess + ", byteCost=" + this.byteCost + ", wasSimulated=" + this.wasSimulated + ", incomplete=" + this.incomplete + '}';
    }

    public String getTooltipText() {
        return GuiText.RequestedItem.getLocal() + ": " + this.getReadableStackName() + "\n " + GuiText.Substitute.getLocal() + " " + (this.substitutionMode == SubstitutionMode.ACCEPT_FUZZY ? GuiText.Yes.getLocal() : GuiText.No.getLocal()) + "\n " + GuiText.BytesUsed.getLocal() + ": " + this.byteCost + "\n " + GuiText.Simulation.getLocal() + ": " + (this.wasSimulated ? GuiText.Yes.getLocal() : GuiText.No.getLocal()) + "\n " + GuiText.SimulationIncomplete.getLocal() + ": " + (this.incomplete ? GuiText.Yes.getLocal() : GuiText.No.getLocal());
    }

    public void fulfill(CraftingTask origin, StackType input, CraftingContext context) {
        if (input == null || input.getStackSize() == 0L) {
            return;
        }
        if (input.getStackSize() < 0L) {
            throw new IllegalArgumentException("Can't fulfill crafting request with a negative amount of " + input + " : " + this);
        }
        if (this.remainingToProcess < input.getStackSize()) {
            throw new IllegalArgumentException("Can't fulfill crafting request with too many of " + input + " : " + this);
        }
        this.untransformedByteCost += input.getStackSize();
        this.byteCost = CraftingCalculations.adjustByteCost(this, this.untransformedByteCost);
        this.remainingToProcess -= input.getStackSize();
        this.usedResolvers.add(new UsedResolverEntry(this, origin, input.copy()));
    }

    public void partialRefund(CraftingContext context, long refundedAmount) {
        long remainingTaskAmount = refundedAmount;
        for (UsedResolverEntry<StackType> resolver : this.usedResolvers) {
            if (remainingTaskAmount <= 0L) break;
            if (resolver.resolvedStack.getStackSize() <= 0L) continue;
            long taskRefunded = resolver.task.partialRefund(context, Math.min(remainingTaskAmount, resolver.resolvedStack.getStackSize()));
            remainingTaskAmount -= taskRefunded;
            resolver.resolvedStack.setStackSize(resolver.resolvedStack.getStackSize() - taskRefunded);
        }
        if (remainingTaskAmount < 0L) {
            throw new IllegalStateException("Refunds resulted in a negative amount of an item for request " + this);
        }
        if (remainingTaskAmount != 0L) {
            throw new IllegalStateException("Partial refunds could not cover all resolved items for request " + this);
        }
        long originallyRequested = this.stack.getStackSize();
        long originallyRemainingToProcess = this.remainingToProcess;
        long originallyProcessed = originallyRequested - originallyRemainingToProcess;
        long newlyRequested = originallyRequested - refundedAmount;
        long newlyProcessed = Math.min(originallyProcessed, newlyRequested);
        long newlyRemainingToProcess = newlyRequested - newlyProcessed;
        this.stack.setStackSize(newlyRequested);
        this.remainingToProcess = newlyRemainingToProcess;
        this.untransformedByteCost -= refundedAmount;
        this.byteCost = CraftingCalculations.adjustByteCost(this, this.untransformedByteCost);
        if (this.remainingToProcess < 0L) {
            throw new IllegalArgumentException("Refunded more items than were resolved for request " + this);
        }
    }

    public void fullRefund(CraftingContext context) {
        for (UsedResolverEntry<StackType> resolver : this.usedResolvers) {
            resolver.task.fullRefund(context);
        }
        this.remainingToProcess = 0L;
        this.untransformedByteCost = 0L;
        this.byteCost = CraftingCalculations.adjustByteCost(this, this.untransformedByteCost);
        this.stack.setStackSize(0L);
        this.usedResolvers.clear();
    }

    public IAEStack<?> getOneResolvedType() {
        IAEStack<?> found = null;
        for (UsedResolverEntry<StackType> resolver : this.usedResolvers) {
            if (resolver.resolvedStack.getStackSize() <= 0L) continue;
            if (found == null) {
                found = (IAEStack<?>)resolver.resolvedStack.copy();
                continue;
            }
            throw new IllegalStateException("Found multiple item types resolving " + this);
        }
        if (found == null) {
            throw new IllegalStateException("Found no resolution for " + this);
        }
        return found;
    }

    public static enum SubstitutionMode {
        PRECISE_FRESH,
        PRECISE,
        ACCEPT_FUZZY;

    }

    public static class UsedResolverEntry<T extends IAEStack<T>>
    implements ITreeSerializable {
        public final CraftingRequest<T> parent;
        public CraftingTask<T> task;
        public final IAEStack<T> resolvedStack;

        public UsedResolverEntry(CraftingRequest<T> parent, CraftingTask<T> task, IAEStack<T> resolvedStack) {
            this.parent = parent;
            this.task = task;
            this.resolvedStack = resolvedStack;
        }

        public UsedResolverEntry(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
            this.parent = (CraftingRequest)parent;
            this.resolvedStack = serializer.readStack();
            this.task = null;
        }

        @Override
        public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
            serializer.writeStack(this.resolvedStack);
            return Collections.singletonList(this.task);
        }

        @Override
        public void loadChildren(List<ITreeSerializable> children) throws IOException {
            this.task = Objects.requireNonNull((CraftingTask)children.iterator().next());
        }
    }
}

