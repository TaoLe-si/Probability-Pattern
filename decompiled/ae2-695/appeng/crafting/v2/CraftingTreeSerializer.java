/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Throwables
 *  cpw.mods.fml.common.network.ByteBufUtils
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  net.minecraft.world.World
 */
package appeng.crafting.v2;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.core.AEConfig;
import appeng.crafting.v2.CraftingJobV2;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.ITreeSerializable;
import appeng.crafting.v2.resolvers.CraftableItemResolver;
import appeng.crafting.v2.resolvers.EmitableItemResolver;
import appeng.crafting.v2.resolvers.ExtractItemResolver;
import appeng.crafting.v2.resolvers.IgnoreMissingItemResolver;
import appeng.crafting.v2.resolvers.SimulateMissingItemResolver;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import com.google.common.base.Throwables;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.World;

public final class CraftingTreeSerializer {
    private static final Map<Class<? extends ITreeSerializable>, String> serializableKeys = new HashMap<Class<? extends ITreeSerializable>, String>();
    private static final Map<String, MethodHandle> serializableConstructors = new HashMap<String, MethodHandle>();
    private final World world;
    private final boolean reading;
    private final ByteBuf buffer;
    private ArrayList<JobFn> workStack = new ArrayList(32);
    private final byte ST_NULL = 0;
    private final byte ST_ITEM = 1;
    private final byte ST_FLUID = (byte)2;

    public static void registerSerializable(String id, Class<? extends ITreeSerializable> klass) {
        MethodHandle constructor;
        try {
            constructor = MethodHandles.publicLookup().findConstructor(klass, MethodType.methodType(Void.TYPE, CraftingTreeSerializer.class, ITreeSerializable.class)).asType(MethodType.methodType(ITreeSerializable.class, CraftingTreeSerializer.class, ITreeSerializable.class));
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Invalid ITreeSerializable implementation, does not provide a public constructor with a signature of (CraftingTreeSerializer serializer, ITreeSerializable parent)", e);
        }
        if (serializableKeys.put(klass, id) != null || serializableConstructors.put(id, constructor) != null) {
            throw new IllegalArgumentException("Duplicate ITreeSerializable id: " + id);
        }
    }

    public CraftingTreeSerializer(World world) {
        this.buffer = Unpooled.buffer((int)4096, (int)AEConfig.instance.maxCraftingTreeVisualizationSize).order(ByteOrder.LITTLE_ENDIAN);
        this.reading = false;
        this.world = world;
    }

    public CraftingTreeSerializer(World world, ByteBuf toDeserialize) {
        toDeserialize.order(ByteOrder.LITTLE_ENDIAN);
        this.buffer = toDeserialize;
        this.reading = true;
        this.world = world;
    }

    public ByteBuf getBuffer() {
        return this.buffer;
    }

    public void writeSerializableAndQueueChildren(ITreeSerializable obj) throws IOException {
        String key = serializableKeys.get(obj.getClass());
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Unregistered ITreeSerializable: " + obj.getClass());
        }
        ByteBufUtils.writeUTF8String((ByteBuf)this.buffer, (String)key);
        List<? extends ITreeSerializable> children = obj.serializeTree(this);
        ByteBufUtils.writeVarInt((ByteBuf)this.buffer, (int)children.size(), (int)5);
        for (int i = children.size() - 1; i >= 0; --i) {
            ITreeSerializable child = children.get(i);
            this.workStack.add(() -> this.writeSerializableAndQueueChildren(child));
        }
    }

    public ITreeSerializable readSerializableAndQueueChildren(ITreeSerializable parent) throws IOException {
        ITreeSerializable value;
        String key = ByteBufUtils.readUTF8String((ByteBuf)this.buffer);
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("No key provided");
        }
        MethodHandle constructor = serializableConstructors.get(key);
        if (constructor == null) {
            throw new IllegalArgumentException("No constructor for key " + key);
        }
        try {
            value = constructor.invokeExact(this, parent);
        }
        catch (Throwable e) {
            throw Throwables.propagate((Throwable)e);
        }
        int childCount = ByteBufUtils.readVarInt((ByteBuf)this.buffer, (int)5);
        ArrayList childList = new ArrayList(childCount);
        this.workStack.add(new ChildListPopulatorJob(value, childList));
        ITreeSerializable childParent = value.getSerializationParent();
        for (int i = 0; i < childCount; ++i) {
            this.workStack.add(() -> childList.add(this.readSerializableAndQueueChildren(childParent)));
        }
        return value;
    }

    public void writeEnum(Enum<?> value) throws IOException {
        this.buffer.writeByte(value.ordinal());
    }

    public <T extends Enum<T>> T readEnum(Class<T> type) {
        byte ordinal = this.buffer.readByte();
        return (T)((Enum[])type.getEnumConstants())[ordinal];
    }

    public void writeStack(IAEStack<?> stack) throws IOException {
        if (stack == null) {
            this.buffer.writeByte(0);
        } else if (stack instanceof AEItemStack) {
            this.buffer.writeByte(1);
        } else if (stack instanceof AEFluidStack) {
            this.buffer.writeByte(2);
        } else {
            throw new UnsupportedOperationException("Can't serialize a stack of type " + stack.getClass());
        }
        stack.writeToPacket(this.buffer);
    }

    public IAEStack<?> readStack() throws IOException {
        IAEStack<IAEItemStack> iAEStack;
        byte stackType = this.buffer.readByte();
        switch (stackType) {
            case 0: {
                iAEStack = null;
                break;
            }
            case 1: {
                iAEStack = AEItemStack.loadItemStackFromPacket(this.buffer);
                break;
            }
            case 2: {
                iAEStack = AEFluidStack.loadFluidStackFromPacket(this.buffer);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown stack type " + stackType);
            }
        }
        return iAEStack;
    }

    public void writeItemStack(IAEItemStack stack) throws IOException {
        stack.writeToPacket(this.buffer);
    }

    public IAEItemStack readItemStack() throws IOException {
        return AEItemStack.loadItemStackFromPacket(this.buffer);
    }

    public void writePattern(ICraftingPatternDetails pattern) throws IOException {
        this.writeItemStack(AEItemStack.create(pattern.getPattern()));
    }

    public ICraftingPatternDetails readPattern() throws IOException {
        IAEItemStack stack = this.readItemStack();
        if (stack != null && stack.getItem() instanceof ICraftingPatternItem) {
            return ((ICraftingPatternItem)stack.getItem()).getPatternForItem(stack.getItemStack(), this.world);
        }
        throw new UnsupportedOperationException("Illegal pattern type " + stack);
    }

    public <T> void writeArray(T[] array, SerializingFn<T> elementWriter) throws IOException {
        if (array == null) {
            ByteBufUtils.writeVarInt((ByteBuf)this.buffer, (int)0, (int)5);
            return;
        }
        ByteBufUtils.writeVarInt((ByteBuf)this.buffer, (int)array.length, (int)5);
        for (T elem : array) {
            elementWriter.accept(elem);
        }
    }

    public <T> void writeList(List<T> array, SerializingFn<T> elementWriter) throws IOException {
        if (array == null) {
            ByteBufUtils.writeVarInt((ByteBuf)this.buffer, (int)0, (int)5);
            return;
        }
        ByteBufUtils.writeVarInt((ByteBuf)this.buffer, (int)array.size(), (int)5);
        for (T elem : array) {
            elementWriter.accept(elem);
        }
    }

    public <T> T[] readArray(T[] template, DeserializingFn<T> elementMaker) throws IOException {
        int len = ByteBufUtils.readVarInt((ByteBuf)this.buffer, (int)5);
        if (len == 0) {
            return template;
        }
        T[] ret = Arrays.copyOf(template, len);
        for (int i = 0; i < len; ++i) {
            ret[i] = elementMaker.get();
        }
        return ret;
    }

    public <T> void readList(List<T> target, DeserializingFn<T> elementMaker) throws IOException {
        int len = ByteBufUtils.readVarInt((ByteBuf)this.buffer, (int)5);
        if (len == 0) {
            return;
        }
        for (int i = 0; i < len; ++i) {
            target.add(elementMaker.get());
        }
    }

    public boolean hasWork() {
        return !this.workStack.isEmpty();
    }

    public void doWork() {
        if (this.workStack.isEmpty()) {
            return;
        }
        JobFn job = this.workStack.get(this.workStack.size() - 1);
        this.workStack.remove(this.workStack.size() - 1);
        try {
            job.run();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void doBestEffortWork() {
        if (this.workStack.isEmpty()) {
            return;
        }
        for (int i = this.workStack.size() - 1; i >= 0; --i) {
            JobFn job = this.workStack.get(i);
            if (!(job instanceof ChildListPopulatorJob)) continue;
            try {
                job.run();
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.workStack.clear();
    }

    public World getWorld() {
        return this.world;
    }

    static {
        CraftingTreeSerializer.registerSerializable(":j", CraftingJobV2.class);
        CraftingTreeSerializer.registerSerializable(":r", CraftingRequest.class);
        CraftingTreeSerializer.registerSerializable(":re", CraftingRequest.UsedResolverEntry.class);
        CraftingTreeSerializer.registerSerializable(":tc", CraftableItemResolver.CraftFromPatternTask.class);
        CraftingTreeSerializer.registerSerializable(":tcr", CraftableItemResolver.RequestAndPerCraftAmount.class);
        CraftingTreeSerializer.registerSerializable(":te", EmitableItemResolver.EmitItemTask.class);
        CraftingTreeSerializer.registerSerializable(":tx", ExtractItemResolver.ExtractItemTask.class);
        CraftingTreeSerializer.registerSerializable(":ts", SimulateMissingItemResolver.ConjureItemTask.class);
        CraftingTreeSerializer.registerSerializable(":tp", IgnoreMissingItemResolver.IgnoreMissingItemTask.class);
    }

    @FunctionalInterface
    public static interface JobFn {
        public void run() throws IOException;
    }

    private static class ChildListPopulatorJob
    implements JobFn {
        public final ITreeSerializable value;
        public final ArrayList<ITreeSerializable> childList;

        private ChildListPopulatorJob(ITreeSerializable value, ArrayList<ITreeSerializable> childList) {
            this.value = value;
            this.childList = childList;
        }

        @Override
        public void run() throws IOException {
            this.value.loadChildren(this.childList);
        }
    }

    @FunctionalInterface
    public static interface SerializingFn<T> {
        public void accept(T var1) throws IOException;
    }

    @FunctionalInterface
    public static interface DeserializingFn<T> {
        public T get() throws IOException;
    }
}

