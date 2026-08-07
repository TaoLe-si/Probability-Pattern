/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package appeng.util.item;

import appeng.api.AEApi;
import appeng.api.features.IItemComparison;
import appeng.api.storage.data.IAETagCompound;
import appeng.util.Platform;
import appeng.util.item.SharedSearchObject;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class AESharedNBT
extends NBTTagCompound
implements IAETagCompound {
    private static final WeakHashMap<SharedSearchObject, WeakReference<SharedSearchObject>> SHARED_TAG_COMPOUND = new WeakHashMap();
    private final Item item;
    private final int meta;
    private SharedSearchObject sso;
    private int hash;
    private IItemComparison comp;

    private AESharedNBT(Item itemID, int damageValue) {
        this.item = itemID;
        this.meta = damageValue;
    }

    public AESharedNBT(int fakeValue) {
        this.item = null;
        this.meta = 0;
        this.hash = fakeValue;
    }

    public static int sharedTagLoad() {
        return SHARED_TAG_COMPOUND.size();
    }

    static synchronized NBTTagCompound getSharedTagCompound(NBTTagCompound tagCompound, ItemStack s) {
        SharedSearchObject cg;
        if (tagCompound.hasNoTags()) {
            return null;
        }
        Item item = s.getItem();
        int meta = -1;
        if (s.getItem() != null && s.isItemStackDamageable() && s.getHasSubtypes()) {
            meta = s.getItemDamage();
        }
        if (AESharedNBT.isShared(tagCompound)) {
            return tagCompound;
        }
        SharedSearchObject sso = new SharedSearchObject(item, meta, tagCompound);
        WeakReference<SharedSearchObject> c = SHARED_TAG_COMPOUND.get(sso);
        if (c != null && (cg = (SharedSearchObject)c.get()) != null) {
            return cg.getShared();
        }
        AESharedNBT clone = AESharedNBT.createFromCompound(item, meta, tagCompound);
        sso.setCompound((NBTTagCompound)sso.getCompound().copy());
        sso.setShared(clone);
        clone.sso = sso;
        SHARED_TAG_COMPOUND.put(sso, new WeakReference<SharedSearchObject>(sso));
        return clone;
    }

    public static boolean isShared(NBTTagCompound ta) {
        return ta instanceof AESharedNBT;
    }

    private static AESharedNBT createFromCompound(Item itemID, int damageValue, NBTTagCompound c) {
        AESharedNBT x = new AESharedNBT(itemID, damageValue);
        for (Object o : c.func_150296_c()) {
            String name = (String)o;
            x.setTag(name, c.getTag(name).copy());
        }
        x.hash = Platform.NBTOrderlessHash((NBTBase)c);
        ItemStack isc = new ItemStack(itemID, 1, damageValue);
        isc.setTagCompound(c);
        x.comp = AEApi.instance().registries().specialComparison().getSpecialComparison(isc);
        return x;
    }

    int getHash() {
        return this.hash;
    }

    @Override
    public NBTTagCompound getNBTTagCompoundCopy() {
        return (NBTTagCompound)this.copy();
    }

    @Override
    public IItemComparison getSpecialComparison() {
        return this.comp;
    }

    @Override
    public boolean equals(Object par1Obj) {
        if (par1Obj instanceof AESharedNBT) {
            return this == par1Obj;
        }
        return super.equals(par1Obj);
    }

    public boolean matches(Item item, int meta, int orderlessHash) {
        return item == this.item && this.meta == meta && this.hash == orderlessHash;
    }

    public boolean comparePreciseWithRegistry(AESharedNBT tagCompound) {
        if (this == tagCompound) {
            return true;
        }
        if (this.comp != null && tagCompound.comp != null) {
            return this.comp.sameAsPrecise(tagCompound.comp);
        }
        return false;
    }

    public boolean compareFuzzyWithRegistry(AESharedNBT tagCompound) {
        if (this == tagCompound) {
            return true;
        }
        if (tagCompound == null) {
            return false;
        }
        if (this.comp == tagCompound.comp) {
            return true;
        }
        if (this.comp != null) {
            return this.comp.sameAsFuzzy(tagCompound.comp);
        }
        return false;
    }
}

