/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import appeng.api.config.SortDir;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.InvTweakSortingModule;
import appeng.util.Platform;
import java.util.Comparator;

public class ItemSorters {
    private static SortDir direction = SortDir.ASCENDING;
    public static final Comparator<IAEItemStack> CONFIG_BASED_SORT_BY_NAME = Comparator.comparing(Platform::getItemDisplayName, (a, b) -> a.compareToIgnoreCase((String)b) * ItemSorters.direction.sortHint);
    public static final Comparator<IAEItemStack> CONFIG_BASED_SORT_BY_MOD = Comparator.comparing(Platform::getModId, (a, b) -> a.compareToIgnoreCase((String)b) * ItemSorters.direction.sortHint).thenComparing(Platform::getItemDisplayName);
    public static final Comparator<IAEItemStack> CONFIG_BASED_SORT_BY_SIZE = Comparator.comparing(IAEStack::getStackSize, (a, b) -> Long.compare(b, a) * ItemSorters.direction.sortHint);
    public static final Comparator<IAEItemStack> CONFIG_BASED_SORT_BY_INV_TWEAKS = new Comparator<IAEItemStack>(){

        @Override
        public int compare(IAEItemStack o1, IAEItemStack o2) {
            if (!InvTweakSortingModule.isLoaded()) {
                return CONFIG_BASED_SORT_BY_NAME.compare(o1, o2);
            }
            return InvTweakSortingModule.compareItems(o1.getItemStack(), o2.getItemStack()) * direction.sortHint;
        }
    };

    public static int compareInt(int a, int b) {
        return Integer.compare(a, b);
    }

    public static int compareLong(long a, long b) {
        return Long.compare(a, b);
    }

    public static int compareDouble(double a, double b) {
        return Double.compare(a, b);
    }

    public static void setDirection(SortDir direction) {
        ItemSorters.direction = direction;
    }
}

