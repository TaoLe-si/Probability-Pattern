/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.registry.GameRegistry$UniqueIdentifier
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 */
package appeng.util.item;

import appeng.util.Platform;
import appeng.util.item.AESharedNBT;
import appeng.util.item.OreReference;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;

public class AEItemDef {
    static final AESharedNBT LOW_TAG = new AESharedNBT(Integer.MIN_VALUE);
    static final AESharedNBT HIGH_TAG = new AESharedNBT(Integer.MAX_VALUE);
    private final int itemID;
    private final Item item;
    private int myHash;
    private int def;
    private int damageValue;
    private int displayDamage;
    private int maxDamage;
    private AESharedNBT tagCompound;
    private String displayName;
    @SideOnly(value=Side.CLIENT)
    private List<String> tooltip;
    @SideOnly(value=Side.CLIENT)
    private GameRegistry.UniqueIdentifier uniqueID;
    private OreReference isOre;

    public AEItemDef(Item it) {
        this.item = it;
        this.itemID = Item.getIdFromItem((Item)it);
    }

    AEItemDef copy() {
        AEItemDef t = new AEItemDef(this.getItem());
        t.def = this.def;
        t.setDamageValue(this.getDamageValue());
        t.setDisplayDamage(this.getDisplayDamage());
        t.setMaxDamage(this.getMaxDamage());
        t.setTagCompound(this.getTagCompound());
        t.setIsOre(this.getIsOre());
        return t;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        AEItemDef other = (AEItemDef)obj;
        return other.getDamageValue() == this.getDamageValue() && other.getItem() == this.getItem() && this.getTagCompound() == other.getTagCompound();
    }

    boolean isItem(ItemStack otherStack) {
        int dmg = this.getDamageValueHack(otherStack);
        if (this.getItem() == otherStack.getItem() && dmg == this.getDamageValue()) {
            if (this.getTagCompound() != null != otherStack.hasTagCompound()) {
                return false;
            }
            if (this.getTagCompound() != null && otherStack.hasTagCompound()) {
                return Platform.NBTEqualityTest((NBTBase)this.getTagCompound(), (NBTBase)otherStack.getTagCompound());
            }
            return true;
        }
        return false;
    }

    int getDamageValueHack(ItemStack is) {
        return Items.blaze_rod.getDamage(is);
    }

    void reHash() {
        this.def = this.getItemID() << 16 | this.getDamageValue();
        this.myHash = this.def ^ (this.getTagCompound() == null ? 0 : System.identityHashCode(this.getTagCompound()));
    }

    AESharedNBT getTagCompound() {
        return this.tagCompound;
    }

    void setTagCompound(AESharedNBT tagCompound) {
        this.tagCompound = tagCompound;
    }

    int getDamageValue() {
        return this.damageValue;
    }

    int setDamageValue(int damageValue) {
        this.damageValue = damageValue;
        return damageValue;
    }

    Item getItem() {
        return this.item;
    }

    int getDisplayDamage() {
        return this.displayDamage;
    }

    void setDisplayDamage(int displayDamage) {
        this.displayDamage = displayDamage;
    }

    String getDisplayName() {
        return this.displayName;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    List<String> getTooltip() {
        return this.tooltip;
    }

    List<String> setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
        return tooltip;
    }

    GameRegistry.UniqueIdentifier getUniqueID() {
        return this.uniqueID;
    }

    GameRegistry.UniqueIdentifier setUniqueID(GameRegistry.UniqueIdentifier uniqueID) {
        this.uniqueID = uniqueID;
        return uniqueID;
    }

    OreReference getIsOre() {
        return this.isOre;
    }

    void setIsOre(OreReference isOre) {
        this.isOre = isOre;
    }

    int getItemID() {
        return this.itemID;
    }

    int getMaxDamage() {
        return this.maxDamage;
    }

    void setMaxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
    }

    int getMyHash() {
        return this.myHash;
    }
}

