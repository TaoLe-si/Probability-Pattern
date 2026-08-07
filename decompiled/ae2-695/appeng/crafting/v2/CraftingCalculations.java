/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.logging.log4j.Level
 */
package appeng.crafting.v2;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.core.AELog;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.resolvers.CraftableItemResolver;
import appeng.crafting.v2.resolvers.CraftingRequestResolver;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.crafting.v2.resolvers.EmitableItemResolver;
import appeng.crafting.v2.resolvers.ExtractItemResolver;
import appeng.crafting.v2.resolvers.IgnoreMissingItemResolver;
import appeng.crafting.v2.resolvers.SimulateMissingItemResolver;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongBiFunction;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;

public class CraftingCalculations {
    private static final InternalMultiMap<Class<? extends IAEStack<?>>, CraftingRequestResolver<?>> providers = new InternalMultiMap();
    private static final InternalMultiMap<Class<? extends IAEStack<?>>, ToLongBiFunction<CraftingRequest<?>, Long>> byteAmountAdjusters = new InternalMultiMap();

    public static <StackType extends IAEStack<StackType>> void registerProvider(CraftingRequestResolver<StackType> provider, Class<StackType> stackTypeClass) {
        providers.compute(stackTypeClass, (k, v) -> (CraftingRequestResolver[])ArrayUtils.add((Object[])(v == null ? new CraftingRequestResolver[]{} : v), (Object)provider));
    }

    public static <StackType extends IAEStack<StackType>> void registerByteAmountAdjuster(ToLongBiFunction<CraftingRequest<StackType>, Long> adjuster, Class<StackType> stackTypeClass) {
        byteAmountAdjusters.compute(stackTypeClass, (k, v) -> (ToLongBiFunction[])ArrayUtils.add((Object[])(v == null ? new ToLongBiFunction[]{} : v), (Object)adjuster));
    }

    public static <StackType extends IAEStack<StackType>> List<CraftingTask> tryResolveCraftingRequest(CraftingRequest<StackType> request, CraftingContext context) {
        Class aClass;
        ArrayList<CraftingTask> allTasks = new ArrayList<CraftingTask>(4);
        Object[] keyArray = providers.keysArray();
        int size = keyArray.length;
        for (int i = 0; i < size && (aClass = (Class)keyArray[i]) != null; ++i) {
            if (!aClass.isAssignableFrom(request.stackTypeClass)) continue;
            for (CraftingRequestResolver<?> resolver : providers.valuesArrayAt(i)) {
                try {
                    CraftingRequestResolver<?> provider = resolver;
                    List<CraftingTask> tasks = provider.provideCraftingRequestResolvers(request, context);
                    allTasks.addAll(tasks);
                }
                catch (Exception t) {
                    AELog.log(Level.WARN, t, "Error encountered when trying to generate the list of CraftingTasks for crafting {}", request.toString());
                }
            }
        }
        allTasks.sort(CraftingTask.PRIORITY_COMPARATOR);
        return Collections.unmodifiableList(allTasks);
    }

    public static <StackType extends IAEStack<StackType>> long adjustByteCost(CraftingRequest<StackType> request, long byteCost) {
        Class aClass;
        Object[] keyArray = byteAmountAdjusters.keysArray();
        int size = keyArray.length;
        for (int i = 0; i < size && (aClass = (Class)keyArray[i]) != null; ++i) {
            if (!aClass.isAssignableFrom(request.stackTypeClass)) continue;
            ToLongBiFunction<CraftingRequest<?>, Long>[] toLongBiFunctionArray = byteAmountAdjusters.valuesArrayAt(i);
            int n = toLongBiFunctionArray.length;
            for (int j = 0; j < n; ++j) {
                ToLongBiFunction<CraftingRequest<?>, Long> value;
                ToLongBiFunction<CraftingRequest<?>, Long> adjuster = value = toLongBiFunctionArray[j];
                byteCost = adjuster.applyAsLong(request, byteCost);
            }
        }
        return byteCost;
    }

    static {
        CraftingCalculations.registerProvider(new ExtractItemResolver(), IAEItemStack.class);
        CraftingCalculations.registerProvider(new SimulateMissingItemResolver(), IAEItemStack.class);
        CraftingCalculations.registerProvider(new SimulateMissingItemResolver(), IAEFluidStack.class);
        CraftingCalculations.registerProvider(new EmitableItemResolver(), IAEItemStack.class);
        CraftingCalculations.registerProvider(new CraftableItemResolver(), IAEItemStack.class);
        CraftingCalculations.registerProvider(new IgnoreMissingItemResolver(), IAEItemStack.class);
    }

    private static final class InternalMultiMap<K, V>
    extends Object2ObjectArrayMap<K, V[]> {
        private InternalMultiMap() {
        }

        public Object[] keysArray() {
            return this.key;
        }

        public V[] valuesArrayAt(int index) {
            return (Object[])this.value[index];
        }

        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public V[] remove(Object k) {
            throw new UnsupportedOperationException();
        }
    }
}

