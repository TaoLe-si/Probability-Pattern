/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package appeng.container.implementations;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.util.ItemSorters;
import javax.annotation.Nonnull;

public class CraftingCPURecord
implements Comparable<CraftingCPURecord> {
    private final String myName;
    private final ICraftingCPU cpu;
    private final long size;
    private final int processors;

    public CraftingCPURecord(long size, int coProcessors, ICraftingCPU server) {
        this.size = size;
        this.processors = coProcessors;
        this.cpu = server;
        this.myName = server.getName();
    }

    @Override
    public int compareTo(@Nonnull CraftingCPURecord o) {
        int a = ItemSorters.compareLong(o.getProcessors(), this.getProcessors());
        if (a != 0) {
            return a;
        }
        return ItemSorters.compareLong(o.getSize(), this.getSize());
    }

    ICraftingCPU getCpu() {
        return this.cpu;
    }

    String getName() {
        return this.myName;
    }

    int getProcessors() {
        return this.processors;
    }

    long getSize() {
        return this.size;
    }
}

