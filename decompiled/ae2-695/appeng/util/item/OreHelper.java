/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  com.google.common.collect.Sets
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.oredict.OreDictionary
 */
package appeng.util.item;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.OreReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class OreHelper {
    public static final OreHelper INSTANCE = new OreHelper();
    private final LoadingCache<String, List<ItemStack>> oreDictCache = CacheBuilder.newBuilder().build((CacheLoader)new CacheLoader<String, List<ItemStack>>(){

        public List<ItemStack> load(String oreName) {
            return OreDictionary.getOres((String)oreName);
        }
    });
    private final Map<ItemRef, OreReference> references = new HashMap<ItemRef, OreReference>();

    public OreReference isOre(ItemStack itemStack) {
        ItemRef ir = new ItemRef(itemStack);
        if (!this.references.containsKey(ir)) {
            OreReference ref = new OreReference();
            Set<Integer> ores = ref.getOres();
            Collection<String> set = ref.getEquivalents();
            for (int id : OreDictionary.getOreIDs((ItemStack)itemStack)) {
                ores.add(id);
                set.add(OreDictionary.getOreName((int)id));
            }
            if (!set.isEmpty()) {
                this.references.put(ir, ref);
            } else {
                this.references.put(ir, null);
            }
        }
        return this.references.get(ir);
    }

    boolean sameOre(AEItemStack aeItemStack, IAEItemStack is) {
        if (is instanceof AEItemStack) {
            return this.sameOre(aeItemStack.getDefinition().getIsOre(), ((AEItemStack)is).getDefinition().getIsOre());
        }
        return this.sameOre(aeItemStack, is.getItemStack());
    }

    public boolean sameOre(OreReference a, OreReference b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        return !Sets.intersection(a.getOres(), b.getOres()).isEmpty();
    }

    boolean sameOre(AEItemStack aeItemStack, ItemStack o) {
        OreReference a = aeItemStack.getDefinition().getIsOre();
        HashSet<Integer> set = new HashSet<Integer>();
        for (int id : OreDictionary.getOreIDs((ItemStack)o)) {
            set.add(id);
        }
        return !Sets.intersection(a.getOres(), set).isEmpty();
    }

    List<ItemStack> getCachedOres(String oreName) {
        return (List)this.oreDictCache.getUnchecked((Object)oreName);
    }

    private static class ItemRef {
        private final Item ref;
        private final int damage;
        private final int hash;

        ItemRef(ItemStack stack) {
            this.ref = stack.getItem();
            this.damage = stack.getItem().isDamageable() ? 0 : stack.getItemDamage();
            this.hash = this.ref.hashCode() ^ this.damage;
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            ItemRef other = (ItemRef)obj;
            return this.damage == other.damage && this.ref == other.ref;
        }

        public String toString() {
            return "ItemRef [ref=" + this.ref.getUnlocalizedName() + ", damage=" + this.damage + ", hash=" + this.hash + ']';
        }
    }
}

