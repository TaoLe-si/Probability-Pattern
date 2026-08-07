/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.ImmutableListMultimap
 *  com.google.common.collect.ImmutableListMultimap$Builder
 */
package appeng.util.item;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.OreReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OreListMultiMap<T> {
    private ImmutableListMultimap<Integer, T> map;
    private final Map<Integer, Collection<ICraftingPatternDetails>> patternHashMap = new HashMap<Integer, Collection<ICraftingPatternDetails>>();
    private ImmutableListMultimap<Integer, T> patternMap;
    private ImmutableListMultimap.Builder<Integer, T> builder = new ImmutableListMultimap.Builder();

    private static Collection<Integer> getAEEquivalents(IAEItemStack stack) {
        AEItemStack s = !(stack instanceof AEItemStack) ? AEItemStack.create(stack.getItemStack()) : (AEItemStack)stack;
        OreReference ore = s.getDefinition().getIsOre();
        if (ore == null) {
            return Collections.emptyList();
        }
        Set<Integer> ids = ore.getOres();
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids;
    }

    public boolean isPopulated() {
        return this.patternMap != null;
    }

    public void put(IAEItemStack key, ICraftingPatternDetails val) {
        if (((AEItemStack)key).getDefinition() != null) {
            Collection tmp = this.patternHashMap.getOrDefault(((AEItemStack)key).getDefinition().getMyHash(), null);
            if (tmp == null) {
                ArrayList<ICraftingPatternDetails> list = new ArrayList<ICraftingPatternDetails>();
                list.add(val);
                this.patternHashMap.put(((AEItemStack)key).getDefinition().getMyHash(), list);
            } else {
                tmp.add(val);
            }
        }
        for (Integer realKey : OreListMultiMap.getAEEquivalents(key)) {
            this.builder.put((Object)realKey, (Object)val);
        }
    }

    public void freeze() {
        this.map = this.builder.build();
        this.builder = new ImmutableListMultimap.Builder();
        for (Map.Entry<Integer, Collection<ICraftingPatternDetails>> collection : this.patternHashMap.entrySet()) {
            for (ICraftingPatternDetails details : collection.getValue()) {
                this.builder.put((Object)collection.getKey(), (Object)details);
            }
        }
        this.patternMap = this.builder.build();
    }

    public ImmutableList<T> get(IAEItemStack key) {
        Collection<Integer> ids = OreListMultiMap.getAEEquivalents(key);
        if (ids.isEmpty()) {
            return ImmutableList.of();
        }
        if (ids.size() == 1) {
            return this.map.get((Object)ids.iterator().next());
        }
        ImmutableList.Builder b = ImmutableList.builder();
        for (Integer id : ids) {
            b.addAll((Iterable)this.map.get((Object)id));
        }
        return b.build();
    }

    public ImmutableList<ICraftingPatternDetails> getBeSubstitutePattern(IAEItemStack ias) {
        if (((AEItemStack)ias).getDefinition() == null) {
            return ImmutableList.of();
        }
        return this.patternMap.get((Object)((AEItemStack)ias).getDefinition().getMyHash());
    }

    public void clear() {
        this.map = null;
        this.patternMap = null;
        this.patternHashMap.clear();
        this.builder = new ImmutableListMultimap.Builder();
    }
}

