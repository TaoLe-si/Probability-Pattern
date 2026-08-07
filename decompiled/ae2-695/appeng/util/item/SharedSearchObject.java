/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package appeng.util.item;

import appeng.util.Platform;
import appeng.util.item.AESharedNBT;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class SharedSearchObject {
    private final int def;
    private final int hash;
    private AESharedNBT shared;
    private NBTTagCompound compound;

    public SharedSearchObject(Item itemID, int damageValue, NBTTagCompound tagCompound) {
        this.def = damageValue << 16 | Item.itemRegistry.getIDForObject((Object)itemID);
        this.hash = Platform.NBTOrderlessHash((NBTBase)tagCompound);
        this.setCompound(tagCompound);
    }

    public int hashCode() {
        return this.def ^ this.hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        SharedSearchObject other = (SharedSearchObject)obj;
        if (this.def == other.def && this.hash == other.hash) {
            return Platform.NBTEqualityTest((NBTBase)this.getCompound(), (NBTBase)other.getCompound());
        }
        return false;
    }

    AESharedNBT getShared() {
        return this.shared;
    }

    void setShared(AESharedNBT shared) {
        this.shared = shared;
    }

    NBTTagCompound getCompound() {
        return this.compound;
    }

    void setCompound(NBTTagCompound compound) {
        this.compound = compound;
    }
}

