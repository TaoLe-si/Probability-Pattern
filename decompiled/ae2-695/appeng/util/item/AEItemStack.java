/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.registry.GameRegistry
 *  cpw.mods.fml.common.registry.GameRegistry$UniqueIdentifier
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.Nullable
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.StatCollector
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAETagCompound;
import appeng.util.Platform;
import appeng.util.item.AEItemDef;
import appeng.util.item.AESharedNBT;
import appeng.util.item.AEStack;
import appeng.util.item.OreHelper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

public final class AEItemStack
extends AEStack<IAEItemStack>
implements IAEItemStack,
Comparable<AEItemStack> {
    private AEItemDef def;

    private AEItemStack(AEItemStack is) {
        this.setDefinition(is.getDefinition());
        this.setStackSize(is.getStackSize());
        this.setCraftable(is.isCraftable());
        this.setCountRequestable(is.getCountRequestable());
        this.setCountRequestableCrafts(is.getCountRequestableCrafts());
        this.setUsedPercent(is.getUsedPercent());
    }

    private AEItemStack(ItemStack is) {
        if (is == null) {
            throw new InvalidParameterException("null is not a valid ItemStack for AEItemStack.");
        }
        Item item = is.getItem();
        if (item == null) {
            throw new InvalidParameterException("Contained item is null, thus not a valid ItemStack for AEItemStack.");
        }
        this.setDefinition(new AEItemDef(item));
        if (this.getDefinition().getItem() == null) {
            throw new InvalidParameterException("This ItemStack is bad, it has a null item.");
        }
        this.getDefinition().setDamageValue(this.getDefinition().getDamageValueHack(is));
        this.getDefinition().setDisplayDamage(is.getItemDamageForDisplay());
        this.getDefinition().setMaxDamage(is.getMaxDamage());
        NBTTagCompound tagCompound = is.getTagCompound();
        if (tagCompound != null) {
            this.getDefinition().setTagCompound((AESharedNBT)AESharedNBT.getSharedTagCompound(tagCompound, is));
        }
        this.setStackSize(is.stackSize);
        this.setCraftable(false);
        this.setCountRequestable(0L);
        this.setCountRequestableCrafts(0L);
        this.setUsedPercent(0.0f);
        this.getDefinition().reHash();
        this.getDefinition().setIsOre(OreHelper.INSTANCE.isOre(is));
    }

    public static IAEItemStack loadItemStackFromNBT(NBTTagCompound i) {
        if (i == null) {
            return null;
        }
        ItemStack itemstack = ItemStack.loadItemStackFromNBT((NBTTagCompound)i);
        if (itemstack == null) {
            return null;
        }
        AEItemStack item = AEItemStack.create(itemstack);
        item.setStackSize(i.getLong("Cnt"));
        item.setCountRequestable(i.getLong("Req"));
        item.setCraftable(i.getBoolean("Craft"));
        item.setCountRequestableCrafts(i.getLong("ReqMade"));
        item.setUsedPercent(i.getFloat("UsedPercent"));
        return item;
    }

    @Nullable
    public static AEItemStack create(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        return new AEItemStack(stack);
    }

    public static IAEItemStack loadItemStackFromPacket(ByteBuf data) throws IOException {
        byte mask = data.readByte();
        byte stackType = (byte)((mask & 0xC) >> 2);
        byte countReqType = (byte)((mask & 0x30) >> 4);
        boolean isCraftable = (mask & 0x40) > 0;
        boolean hasTagCompound = (mask & 0x80) > 0;
        NBTTagCompound d = new NBTTagCompound();
        d.setShort("id", data.readShort());
        d.setShort("Damage", data.readShort());
        d.setByte("Count", (byte)0);
        if (hasTagCompound) {
            int len = data.readInt();
            byte[] bd = new byte[len];
            data.readBytes(bd);
            ByteArrayInputStream di = new ByteArrayInputStream(bd);
            d.setTag("tag", (NBTBase)CompressedStreamTools.read((DataInputStream)new DataInputStream(di)));
        }
        long stackSize = AEItemStack.getPacketValue(stackType, data);
        long countRequestable = AEItemStack.getPacketValue(countReqType, data);
        byte mask2 = data.readByte();
        byte countReqMadeType = (byte)(mask2 & 3);
        byte usedPercentType = (byte)((mask2 & 0xC) >> 2);
        long countRequestableCrafts = AEItemStack.getPacketValue(countReqMadeType, data);
        long longUsedPercent = AEItemStack.getPacketValue(usedPercentType, data);
        ItemStack itemstack = ItemStack.loadItemStackFromNBT((NBTTagCompound)d);
        if (itemstack == null) {
            return null;
        }
        AEItemStack item = AEItemStack.create(itemstack);
        item.setStackSize(stackSize);
        item.setCountRequestable(countRequestable);
        item.setCraftable(isCraftable);
        item.setCountRequestableCrafts(countRequestableCrafts);
        item.setUsedPercent((float)longUsedPercent / 10000.0f);
        return item;
    }

    @Override
    public void add(IAEItemStack option) {
        if (option == null) {
            return;
        }
        this.incStackSize(option.getStackSize());
        this.setCountRequestable(this.getCountRequestable() + option.getCountRequestable());
        this.setCraftable(this.isCraftable() || option.isCraftable());
        this.setCountRequestableCrafts(this.getCountRequestableCrafts() + option.getCountRequestableCrafts());
        this.setUsedPercent(this.getUsedPercent() + option.getUsedPercent());
    }

    @Override
    public void writeToNBT(NBTTagCompound i) {
        i.setShort("id", (short)Item.itemRegistry.getIDForObject((Object)this.getDefinition().getItem()));
        i.setByte("Count", (byte)0);
        i.setLong("Cnt", this.getStackSize());
        i.setLong("Req", this.getCountRequestable());
        i.setBoolean("Craft", this.isCraftable());
        i.setShort("Damage", (short)this.getDefinition().getDamageValue());
        if (this.getDefinition().getTagCompound() != null) {
            i.setTag("tag", (NBTBase)this.getDefinition().getTagCompound());
        } else {
            i.removeTag("tag");
        }
        if (this.getCountRequestableCrafts() != 0L) {
            i.setLong("ReqMade", this.getCountRequestableCrafts());
        }
        if (this.getUsedPercent() != 0.0f) {
            i.setFloat("UsedPercent", this.getUsedPercent());
        }
    }

    @Override
    public boolean fuzzyComparison(Object st, FuzzyMode mode) {
        IAEItemStack o;
        if (st instanceof IAEItemStack) {
            o = (IAEItemStack)st;
            if (this.sameOre(o)) {
                return true;
            }
            if (o.getItem() == this.getItem()) {
                if (this.getDefinition().getItem().isDamageable()) {
                    ItemStack a = this.getItemStack();
                    ItemStack b = o.getItemStack();
                    try {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        }
                        if (mode == FuzzyMode.PERCENT_99) {
                            return a.getItemDamageForDisplay() > 1 == b.getItemDamageForDisplay() > 1;
                        }
                        float percentDamageOfA = 1.0f - (float)a.getItemDamageForDisplay() / (float)a.getMaxDamage();
                        float percentDamageOfB = 1.0f - (float)b.getItemDamageForDisplay() / (float)b.getMaxDamage();
                        return percentDamageOfA > mode.breakPoint == percentDamageOfB > mode.breakPoint;
                    }
                    catch (Throwable e) {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        }
                        if (mode == FuzzyMode.PERCENT_99) {
                            return a.getItemDamage() > 1 == b.getItemDamage() > 1;
                        }
                        float percentDamageOfA = (float)a.getItemDamage() / (float)a.getMaxDamage();
                        float percentDamageOfB = (float)b.getItemDamage() / (float)b.getMaxDamage();
                        return percentDamageOfA > mode.breakPoint == percentDamageOfB > mode.breakPoint;
                    }
                }
                return this.getItemDamage() == o.getItemDamage();
            }
        }
        if (st instanceof ItemStack) {
            o = (ItemStack)st;
            OreHelper.INSTANCE.sameOre(this, (ItemStack)o);
            if (o.getItem() == this.getItem()) {
                if (this.getDefinition().getItem().isDamageable()) {
                    ItemStack a = this.getItemStack();
                    try {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        }
                        if (mode == FuzzyMode.PERCENT_99) {
                            return a.getItemDamageForDisplay() > 1 == o.getItemDamageForDisplay() > 1;
                        }
                        float percentDamageOfA = 1.0f - (float)a.getItemDamageForDisplay() / (float)a.getMaxDamage();
                        float percentDamageOfB = 1.0f - (float)o.getItemDamageForDisplay() / (float)o.getMaxDamage();
                        return percentDamageOfA > mode.breakPoint == percentDamageOfB > mode.breakPoint;
                    }
                    catch (Throwable e) {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        }
                        if (mode == FuzzyMode.PERCENT_99) {
                            return a.getItemDamage() > 1 == o.getItemDamage() > 1;
                        }
                        float percentDamageOfA = (float)a.getItemDamage() / (float)a.getMaxDamage();
                        float percentDamageOfB = (float)o.getItemDamage() / (float)o.getMaxDamage();
                        return percentDamageOfA > mode.breakPoint == percentDamageOfB > mode.breakPoint;
                    }
                }
                return this.getItemDamage() == o.getItemDamage();
            }
        }
        return false;
    }

    @Override
    public IAEItemStack copy() {
        return new AEItemStack(this);
    }

    @Override
    public IAEItemStack empty() {
        IAEItemStack dup = this.copy();
        dup.reset();
        return dup;
    }

    @Override
    public IAETagCompound getTagCompound() {
        return this.getDefinition().getTagCompound();
    }

    @Override
    public boolean isItem() {
        return true;
    }

    @Override
    public boolean isFluid() {
        return false;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public String getLocalizedName() {
        String name = this.getDefinition().getDisplayName();
        if (name == null) {
            name = StatCollector.translateToLocal((String)(this.getItem().getUnlocalizedName() + ".name"));
        }
        return name;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack is = new ItemStack(this.getDefinition().getItem(), (int)Math.min(Integer.MAX_VALUE, this.getStackSize()), this.getDefinition().getDamageValue());
        if (this.getDefinition().getTagCompound() != null) {
            is.setTagCompound(this.getDefinition().getTagCompound().getNBTTagCompoundCopy());
        }
        return is;
    }

    @Override
    public Item getItem() {
        return this.getDefinition().getItem();
    }

    @Override
    public int getItemDamage() {
        return this.getDefinition().getDamageValue();
    }

    @Override
    public boolean sameOre(IAEItemStack is) {
        return OreHelper.INSTANCE.sameOre(this, is);
    }

    @Override
    public boolean isSameType(IAEItemStack otherStack) {
        if (otherStack == null) {
            return false;
        }
        return this.getDefinition().equals(((AEItemStack)otherStack).getDefinition());
    }

    @Override
    public boolean isSameType(ItemStack otherStack) {
        if (otherStack == null) {
            return false;
        }
        return this.getDefinition().isItem(otherStack);
    }

    public int hashCode() {
        return this.getDefinition().getMyHash();
    }

    @Override
    public boolean equals(Object ia) {
        ItemStack is;
        if (ia instanceof AEItemStack) {
            return ((AEItemStack)ia).getDefinition().equals(this.getDefinition());
        }
        if (ia instanceof ItemStack && (is = (ItemStack)ia).getItem() == this.getDefinition().getItem() && is.getItemDamage() == this.getDefinition().getDamageValue()) {
            NBTTagCompound tb;
            AESharedNBT ta = this.getDefinition().getTagCompound();
            if (ta == (tb = is.getTagCompound())) {
                return true;
            }
            if (ta == null && tb == null || ta != null && ta.hasNoTags() && tb == null || tb != null && tb.hasNoTags() && ta == null || ta != null && ta.hasNoTags() && tb != null && tb.hasNoTags()) {
                return true;
            }
            if (ta == null && tb != null || ta != null && tb == null) {
                return false;
            }
            if (AESharedNBT.isShared(tb)) {
                return ta == tb;
            }
            return Platform.NBTEqualityTest((NBTBase)ta, (NBTBase)tb);
        }
        return false;
    }

    public String toString() {
        return this.getItemStack().toString();
    }

    @Override
    public int compareTo(AEItemStack b) {
        int id = this.getDefinition().getItemID() - b.getDefinition().getItemID();
        if (id != 0) {
            return id;
        }
        int damageValue = this.getDefinition().getDamageValue() - b.getDefinition().getDamageValue();
        if (damageValue != 0) {
            return damageValue;
        }
        int displayDamage = this.getDefinition().getDisplayDamage() - b.getDefinition().getDisplayDamage();
        if (displayDamage != 0) {
            return displayDamage;
        }
        return this.getDefinition().getTagCompound() == b.getDefinition().getTagCompound() ? 0 : this.compareNBT(b.getDefinition());
    }

    private int compareNBT(AEItemDef b) {
        int nbt = this.compare(this.getDefinition().getTagCompound() == null ? 0 : this.getDefinition().getTagCompound().getHash(), b.getTagCompound() == null ? 0 : b.getTagCompound().getHash());
        if (nbt == 0) {
            return this.compare(System.identityHashCode(this.getDefinition().getTagCompound()), System.identityHashCode(b.getTagCompound()));
        }
        return nbt;
    }

    private int compare(int l, int m) {
        return Integer.compare(l, m);
    }

    @SideOnly(value=Side.CLIENT)
    public List<String> getToolTip() {
        if (this.getDefinition().getTooltip() != null) {
            return this.getDefinition().getTooltip();
        }
        return this.getDefinition().setTooltip(Platform.getTooltip(this.getItemStack()));
    }

    public String getDisplayName() {
        if (this.getDefinition().getDisplayName() == null) {
            this.getDefinition().setDisplayName(Platform.getItemDisplayName(this.getItemStack()));
        }
        return this.getDefinition().getDisplayName();
    }

    public String getModID() {
        if (this.getDefinition().getUniqueID() != null) {
            return this.getModName(this.getDefinition().getUniqueID());
        }
        return this.getModName(this.getDefinition().setUniqueID(GameRegistry.findUniqueIdentifierFor((Item)this.getDefinition().getItem())));
    }

    private String getModName(GameRegistry.UniqueIdentifier uniqueIdentifier) {
        if (uniqueIdentifier == null) {
            return "** Null";
        }
        return uniqueIdentifier.modId == null ? "** Null" : uniqueIdentifier.modId;
    }

    IAEItemStack getLow(FuzzyMode fuzzy, boolean ignoreMeta) {
        AEItemStack bottom = new AEItemStack(this);
        AEItemDef newDef = bottom.setDefinition(bottom.getDefinition().copy());
        if (ignoreMeta) {
            newDef.setDisplayDamage(newDef.setDamageValue(0));
            newDef.reHash();
            return bottom;
        }
        if (newDef.getItem().isDamageable()) {
            if (fuzzy == FuzzyMode.IGNORE_ALL) {
                newDef.setDisplayDamage(0);
            } else if (fuzzy == FuzzyMode.PERCENT_99) {
                if (this.getDefinition().getDamageValue() == 0) {
                    newDef.setDisplayDamage(0);
                } else {
                    newDef.setDisplayDamage(1);
                }
            } else {
                int breakpoint = fuzzy.calculateBreakPoint(this.getDefinition().getMaxDamage());
                newDef.setDisplayDamage(breakpoint <= this.getDefinition().getDisplayDamage() ? breakpoint : 0);
            }
            newDef.setDamageValue(newDef.getDisplayDamage());
        }
        newDef.setTagCompound(AEItemDef.LOW_TAG);
        newDef.reHash();
        return bottom;
    }

    IAEItemStack getHigh(FuzzyMode fuzzy, boolean ignoreMeta) {
        AEItemStack top = new AEItemStack(this);
        AEItemDef newDef = top.setDefinition(top.getDefinition().copy());
        if (ignoreMeta) {
            newDef.setDisplayDamage(newDef.setDamageValue(Integer.MAX_VALUE));
            newDef.reHash();
            return top;
        }
        if (newDef.getItem().isDamageable()) {
            if (fuzzy == FuzzyMode.IGNORE_ALL) {
                newDef.setDisplayDamage(this.getDefinition().getMaxDamage() + 1);
            } else if (fuzzy == FuzzyMode.PERCENT_99) {
                if (this.getDefinition().getDamageValue() == 0) {
                    newDef.setDisplayDamage(0);
                } else {
                    newDef.setDisplayDamage(this.getDefinition().getMaxDamage() + 1);
                }
            } else {
                int breakpoint = fuzzy.calculateBreakPoint(this.getDefinition().getMaxDamage());
                newDef.setDisplayDamage(this.getDefinition().getDisplayDamage() < breakpoint ? breakpoint - 1 : this.getDefinition().getMaxDamage() + 1);
            }
            newDef.setDamageValue(newDef.getDisplayDamage());
        }
        newDef.setTagCompound(AEItemDef.HIGH_TAG);
        newDef.reHash();
        return top;
    }

    public boolean isOre() {
        return this.getDefinition().getIsOre() != null;
    }

    @Override
    void writeIdentity(ByteBuf i) throws IOException {
        i.writeShort(Item.itemRegistry.getIDForObject((Object)this.getDefinition().getItem()));
        i.writeShort(this.getItemDamage());
    }

    @Override
    void readNBT(ByteBuf i) throws IOException {
        if (this.hasTagCompound()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            CompressedStreamTools.write((NBTTagCompound)((NBTTagCompound)this.getTagCompound()), (DataOutput)data);
            byte[] tagBytes = bytes.toByteArray();
            int size = tagBytes.length;
            i.writeInt(size);
            i.writeBytes(tagBytes);
        }
    }

    @Override
    public boolean hasTagCompound() {
        return this.getDefinition().getTagCompound() != null;
    }

    AEItemDef getDefinition() {
        return this.def;
    }

    private AEItemDef setDefinition(AEItemDef def) {
        this.def = def;
        return def;
    }
}

