/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 */
package appeng.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicInteger;

public final class IterationCounter {
    private static AtomicInteger counter = new AtomicInteger(0);
    private static final ThreadLocal<Integer> globalDepth = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> globalCounter = ThreadLocal.withInitial(() -> 0);

    public static int fetchNewId() {
        return counter.getAndIncrement();
    }

    public static int incrementGlobalDepth() {
        Integer depth = globalDepth.get();
        if (depth == 0) {
            globalCounter.set(IterationCounter.fetchNewId());
        }
        globalDepth.set(depth + 1);
        return globalCounter.get();
    }

    public static void incrementGlobalDepthWith(int iteration) {
        globalCounter.set(iteration);
        globalDepth.set(globalDepth.get() + 1);
    }

    public static void decrementGlobalDepth() {
        globalDepth.set(globalDepth.get() - 1);
    }

    public static int getCurrentDepth() {
        return globalDepth.get();
    }

    public static int getCurrentIteration() {
        return globalCounter.get();
    }

    @VisibleForTesting
    static void clear() {
        counter = new AtomicInteger(0);
        globalDepth.set(0);
        globalCounter.set(0);
    }
}

