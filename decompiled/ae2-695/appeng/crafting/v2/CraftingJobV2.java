/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.network.ByteBufUtils
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  net.minecraft.world.World
 *  org.apache.logging.log4j.Level
 */
package appeng.crafting.v2;

import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.AELog;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingTreeSerializer;
import appeng.crafting.v2.ITreeSerializable;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.hooks.TickHandler;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;

public class CraftingJobV2
implements ICraftingJob,
Future<ICraftingJob>,
ITreeSerializable {
    protected volatile long totalByteCost = -1L;
    protected CraftingContext context;
    public CraftingRequest<IAEItemStack> originalRequest;
    protected ICraftingCallback callback;
    protected String errorMessage = "";
    protected State state = State.RUNNING;

    public CraftingContext getContext() {
        return this.context;
    }

    public CraftingJobV2(World world, IGrid meGrid, BaseActionSource actionSource, IAEItemStack what, ICraftingCallback callback) {
        this(world, meGrid, actionSource, what, CraftingMode.STANDARD, callback);
    }

    public CraftingJobV2(World world, IGrid meGrid, BaseActionSource actionSource, IAEItemStack what, CraftingMode craftingMode, ICraftingCallback callback) {
        this.context = new CraftingContext(world, meGrid, actionSource);
        this.callback = callback;
        this.originalRequest = new CraftingRequest<IAEItemStack>(what, CraftingRequest.SubstitutionMode.PRECISE_FRESH, IAEItemStack.class, true, craftingMode);
        this.context.addRequest(this.originalRequest);
        this.context.itemModel.ignore(what);
    }

    public CraftingJobV2(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
        this.totalByteCost = serializer.getBuffer().readLong();
        this.state = serializer.readEnum(State.class);
        this.errorMessage = ByteBufUtils.readUTF8String((ByteBuf)serializer.getBuffer());
        this.originalRequest = new CraftingRequest(serializer, this);
    }

    public static CraftingJobV2 deserialize(World world, ByteBuf buffer) {
        CraftingJobV2 job;
        ITreeSerializable rawJob;
        if (buffer.readableBytes() < 1) {
            return null;
        }
        CraftingTreeSerializer serializer = new CraftingTreeSerializer(world, buffer);
        try {
            rawJob = serializer.readSerializableAndQueueChildren(null);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (rawJob instanceof CraftingJobV2) {
            job = (CraftingJobV2)rawJob;
        } else {
            throw new UnsupportedOperationException("Invalid job type deserialized: " + rawJob.getClass());
        }
        while (serializer.hasWork()) {
            try {
                serializer.doWork();
            }
            catch (IndexOutOfBoundsException e) {
                AELog.warn(e, "Ran out of assigned space for crafting tree serialization");
                serializer.doBestEffortWork();
                break;
            }
        }
        return job;
    }

    @Override
    public CraftingMode getCraftingMode() {
        return this.originalRequest.craftingMode;
    }

    public ByteBuf serialize() {
        try {
            CraftingTreeSerializer serializer = new CraftingTreeSerializer(this.context.world);
            try {
                serializer.writeSerializableAndQueueChildren(this);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            while (serializer.hasWork()) {
                try {
                    serializer.doWork();
                }
                catch (IndexOutOfBoundsException e) {
                    AELog.warn(e, "Ran out of assigned space for crafting tree serialization");
                    break;
                }
            }
            return serializer.getBuffer().slice();
        }
        catch (Exception e) {
            AELog.error(e, "Could not serialize the crafting job");
            return Unpooled.buffer((int)0);
        }
    }

    @Override
    public boolean isSimulation() {
        return this.context.wasSimulated;
    }

    @Override
    public long getByteTotal() {
        long byteCost = this.totalByteCost;
        if (byteCost < 0L) {
            byteCost = 0L;
            for (CraftingContext.RequestInProcessing<?> request : this.context.getLiveRequests()) {
                byteCost += request.request.getByteCost();
            }
            this.totalByteCost = byteCost;
        }
        return byteCost;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public void populatePlan(IItemList<IAEItemStack> plan) {
        for (CraftingTask task : this.context.getResolvedTasks()) {
            task.populatePlan(plan);
        }
    }

    @Override
    public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
        if (this.state == State.RUNNING) {
            throw new IllegalStateException("Can't serialize a running crafting simulation");
        }
        if (this.originalRequest == null) {
            throw new IllegalStateException("Can't serialize a null request");
        }
        serializer.getBuffer().writeLong(this.getByteTotal());
        serializer.writeEnum(this.state);
        ByteBufUtils.writeUTF8String((ByteBuf)serializer.getBuffer(), (String)this.errorMessage);
        return this.originalRequest.serializeTree(serializer);
    }

    @Override
    public ITreeSerializable getSerializationParent() {
        return this.originalRequest;
    }

    @Override
    public void loadChildren(List<ITreeSerializable> children) throws IOException {
        this.originalRequest.loadChildren(children);
    }

    @Override
    public IAEItemStack getOutput() {
        return (IAEItemStack)this.originalRequest.stack;
    }

    @Override
    public boolean simulateFor(int milli) {
        if (this.state != State.RUNNING) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        long finishTime = startTime + (long)milli;
        CraftingTask.State taskState = CraftingTask.State.NEEDS_MORE_WORK;
        try {
            do {
                taskState = this.context.doWork();
                this.totalByteCost = -1L;
            } while (taskState.needsMoreWork && System.currentTimeMillis() < finishTime && this.state == State.RUNNING);
        }
        catch (Exception e) {
            AELog.error(e, "Error while simulating crafting for " + this.originalRequest);
            this.errorMessage = e.toString();
            this.state = State.CANCELLED;
            if (this.callback != null) {
                this.callback.calculationComplete(this);
            }
            return false;
        }
        if (!taskState.needsMoreWork) {
            this.getByteTotal();
            this.state = State.FINISHED;
            if (AELog.isCraftingDebugLogEnabled()) {
                AELog.log(Level.INFO, "Crafting job for %s finished with resolved steps:", this.originalRequest.toString());
                AELog.logSimple(Level.INFO, this.context.toString());
            }
            if (this.callback != null) {
                this.callback.calculationComplete(this);
            }
        }
        return taskState.needsMoreWork;
    }

    @Override
    public Future<ICraftingJob> schedule() {
        TickHandler.INSTANCE.registerCraftingSimulation(this.context.world, this);
        return this;
    }

    @Override
    public boolean supportsCPUCluster(ICraftingCPU cluster) {
        return cluster instanceof CraftingCPUCluster;
    }

    @Override
    public void startCrafting(MECraftingInventory storage, ICraftingCPU rawCluster, BaseActionSource src) {
        if (this.state == State.RUNNING) {
            throw new IllegalStateException("Trying to start crafting a not fully calculated job for " + this.originalRequest.toString());
        }
        CraftingCPUCluster cluster = (CraftingCPUCluster)rawCluster;
        this.context.actionSource = src;
        List<CraftingTask> resolvedTasks = this.context.getResolvedTasks();
        for (CraftingTask task : resolvedTasks) {
            task.startOnCpu(this.context, cluster, storage);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (this.state != State.RUNNING) {
            return false;
        }
        this.state = State.CANCELLED;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return this.state == State.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return this.state != State.RUNNING;
    }

    @Override
    public CraftingJobV2 get() throws InterruptedException, ExecutionException {
        this.simulateFor(Integer.MAX_VALUE);
        return this;
    }

    @Override
    public CraftingJobV2 get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            this.simulateFor((int)unit.convert(timeout, TimeUnit.MILLISECONDS));
        }
        catch (Exception e) {
            throw new ExecutionException(e);
        }
        switch (this.state) {
            case RUNNING: {
                throw new TimeoutException();
            }
            case CANCELLED: {
                throw new InterruptedException();
            }
            case FINISHED: {
                break;
            }
            default: {
                throw new IllegalStateException();
            }
        }
        return this;
    }

    @Override
    public MECraftingInventory getStorageAtBeginning() {
        return this.getContext().availableCache;
    }

    protected static enum State {
        RUNNING,
        FINISHED,
        CANCELLED;

    }
}

