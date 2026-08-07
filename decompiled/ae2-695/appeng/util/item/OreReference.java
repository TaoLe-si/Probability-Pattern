/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package appeng.util.item;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.OreHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.item.ItemStack;

public class OreReference {
    private final List<String> otherOptions = new LinkedList<String>();
    private final Set<Integer> ores = new HashSet<Integer>();
    private List<IAEItemStack> aeOtherOptions = null;

    public Collection<String> getEquivalents() {
        return this.otherOptions;
    }

    List<IAEItemStack> getAEEquivalents() {
        if (this.aeOtherOptions == null) {
            this.aeOtherOptions = new ArrayList<IAEItemStack>(this.otherOptions.size());
            for (String oreName : this.otherOptions) {
                for (ItemStack is : OreHelper.INSTANCE.getCachedOres(oreName)) {
                    if (is.getItem() == null) continue;
                    this.aeOtherOptions.add(AEItemStack.create(is));
                }
            }
        }
        return this.aeOtherOptions;
    }

    Set<Integer> getOres() {
        return this.ores;
    }
}

