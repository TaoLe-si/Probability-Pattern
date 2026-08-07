/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  buildcraft.api.tools.IToolWrench
 *  cofh.api.item.IToolHammer
 *  com.mojang.authlib.GameProfile
 *  cpw.mods.fml.common.FMLCommonHandler
 *  cpw.mods.fml.common.Loader
 *  cpw.mods.fml.common.ModContainer
 *  cpw.mods.fml.relauncher.ReflectionHelper
 *  cpw.mods.fml.relauncher.Side
 *  cpw.mods.fml.relauncher.SideOnly
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  net.minecraft.block.Block
 *  net.minecraft.client.Minecraft
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ISidedInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.inventory.InventoryLargeChest
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.CraftingManager
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTBase$NBTPrimitive
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.server.S21PacketChunkData
 *  net.minecraft.server.management.PlayerManager
 *  net.minecraft.stats.Achievement
 *  net.minecraft.stats.StatBase
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityChest
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.MathHelper
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.StatCollector
 *  net.minecraft.util.Vec3
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 *  net.minecraft.world.chunk.Chunk
 *  net.minecraftforge.common.util.FakePlayer
 *  net.minecraftforge.common.util.FakePlayerFactory
 *  net.minecraftforge.common.util.ForgeDirection
 */
package appeng.util;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.config.SearchBoxMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.SortOrder;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IMaterials;
import appeng.api.definitions.IParts;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.implementations.items.IAEWrench;
import appeng.api.implementations.tiles.ITileStorageMonitorable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAETagCompound;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.client.me.SlotME;
import appeng.container.slot.SlotFake;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.features.AEFeature;
import appeng.core.stats.Stats;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.GuiHostType;
import appeng.hooks.TickHandler;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.me.GridAccessException;
import appeng.me.GridNode;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.BlockUpdate;
import appeng.util.InvTweakSortingModule;
import appeng.util.IterationCounter;
import appeng.util.LookDirection;
import appeng.util.item.AEItemStack;
import appeng.util.item.AESharedNBT;
import appeng.util.item.OreHelper;
import appeng.util.item.OreReference;
import appeng.util.prioitylist.IPartitionList;
import buildcraft.api.tools.IToolWrench;
import cofh.api.item.IToolHammer;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.StatBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

public class Platform {
    public static final Block AIR_BLOCK = Blocks.air;
    public static final int DEF_OFFSET = 16;
    private static final Random RANDOM_GENERATOR = new Random();
    private static final WeakHashMap<World, WeakReference<EntityPlayer>> FAKE_PLAYERS = new WeakHashMap();
    private static Class playerInstance;
    private static Method getOrCreateChunkWatcher;
    private static Method sendToAllPlayersWatchingChunk;
    private static GameProfile fakeProfile;
    private static final String[] BYTE_UNIT;
    private static final char[] NUM_UNIT;
    private static final double[] BYTE_LIMIT;
    private static final int DIVISION_BASE = 1000;
    private static final DecimalFormat df;
    private static IRecipe lastUsedRecipe;

    public static Random getRandom() {
        return RANDOM_GENERATOR;
    }

    public static float getRandomFloat() {
        return RANDOM_GENERATOR.nextFloat();
    }

    public static void seedFromGrid(Random rng, long worldSeed, long x, long z) {
        rng.setSeed(worldSeed);
        long xSeed = rng.nextLong() >> 3;
        long zSeed = rng.nextLong() >> 3;
        long gridSeed = xSeed * x + zSeed * z ^ worldSeed;
        rng.setSeed(gridSeed);
    }

    public static String formatPowerLong(long n, boolean isRate) {
        double p = (double)n / 100.0;
        PowerUnits displayUnits = AEConfig.instance.selectedPowerUnit();
        p = PowerUnits.AE.convertTo(displayUnits, p);
        String unitName = displayUnits.name();
        if (displayUnits == PowerUnits.WA) {
            unitName = "J";
        }
        if (displayUnits == PowerUnits.MK) {
            unitName = "J";
        }
        String[] preFixes = new String[]{"k", "M", "G", "T", "P", "T", "P", "E", "Z", "Y"};
        String level = "";
        for (int offset = 0; p > 1000.0 && offset < preFixes.length; p /= 1000.0, ++offset) {
            level = preFixes[offset];
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(p) + ' ' + level + unitName + (isRate ? "/t" : "");
    }

    public static ForgeDirection crossProduct(ForgeDirection forward, ForgeDirection up) {
        ForgeDirection forgeDirection;
        int west_x = forward.offsetY * up.offsetZ - forward.offsetZ * up.offsetY;
        int west_y = forward.offsetZ * up.offsetX - forward.offsetX * up.offsetZ;
        int west_z = forward.offsetX * up.offsetY - forward.offsetY * up.offsetX;
        switch (west_x + west_y * 2 + west_z * 3) {
            case 1: {
                forgeDirection = ForgeDirection.EAST;
                break;
            }
            case -1: {
                forgeDirection = ForgeDirection.WEST;
                break;
            }
            case 2: {
                forgeDirection = ForgeDirection.UP;
                break;
            }
            case -2: {
                forgeDirection = ForgeDirection.DOWN;
                break;
            }
            case 3: {
                forgeDirection = ForgeDirection.SOUTH;
                break;
            }
            case -3: {
                forgeDirection = ForgeDirection.NORTH;
                break;
            }
            default: {
                forgeDirection = ForgeDirection.UNKNOWN;
            }
        }
        return forgeDirection;
    }

    public static <T extends Enum> T rotateEnum(T ce, boolean backwards, EnumSet validOptions) {
        while (!validOptions.contains(ce = backwards ? Platform.prevEnum(ce) : Platform.nextEnum(ce)) || Platform.isNotValidSetting(ce)) {
        }
        return ce;
    }

    private static <T extends Enum> T prevEnum(T ce) {
        EnumSet valList = EnumSet.allOf(ce.getClass());
        int pLoc = ce.ordinal() - 1;
        if (pLoc < 0) {
            pLoc = valList.size() - 1;
        }
        if (pLoc < 0 || pLoc >= valList.size()) {
            pLoc = 0;
        }
        int pos = 0;
        for (Object g : valList) {
            if (pos == pLoc) {
                return (T)((Enum)g);
            }
            ++pos;
        }
        return null;
    }

    public static <T extends Enum> T nextEnum(T ce) {
        EnumSet valList = EnumSet.allOf(ce.getClass());
        int pLoc = ce.ordinal() + 1;
        if (pLoc >= valList.size()) {
            pLoc = 0;
        }
        if (pLoc < 0 || pLoc >= valList.size()) {
            pLoc = 0;
        }
        int pos = 0;
        for (Object g : valList) {
            if (pos == pLoc) {
                return (T)((Enum)g);
            }
            ++pos;
        }
        return null;
    }

    private static boolean isNotValidSetting(Enum e) {
        if (e == SortOrder.INVTWEAKS && !InvTweakSortingModule.isLoaded()) {
            return true;
        }
        if (e == SearchBoxMode.NEI_AUTOSEARCH && !IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI)) {
            return true;
        }
        return e == SearchBoxMode.NEI_MANUAL_SEARCH && !IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI);
    }

    public static void openGUI(@Nonnull EntityPlayer p, @Nullable TileEntity tile, @Nullable ForgeDirection side, @Nonnull GuiBridge type) {
        if (Platform.isClient()) {
            return;
        }
        int x = (int)p.posX;
        int y = (int)p.posY;
        int z = (int)p.posZ;
        if (tile != null) {
            x = tile.xCoord;
            y = tile.yCoord;
            z = tile.zCoord;
        }
        if (type.getType().isItem() && tile == null || type.hasPermissions(tile, x, y, z, side, p)) {
            if (tile == null && type.getType() == GuiHostType.ITEM) {
                p.openGui((Object)AppEng.instance(), type.ordinal() << 5 | 0x10, p.getEntityWorld(), p.inventory.currentItem, 0, 0);
            } else if (tile == null || type.getType() == GuiHostType.ITEM) {
                p.openGui((Object)AppEng.instance(), type.ordinal() << 5 | 8, p.getEntityWorld(), x, y, z);
            } else {
                p.openGui((Object)AppEng.instance(), type.ordinal() << 5 | side.ordinal(), tile.getWorldObj(), x, y, z);
            }
        }
    }

    public static boolean isClient() {
        return FMLCommonHandler.instance().getEffectiveSide().isClient();
    }

    public static boolean hasPermissions(DimensionalCoord dc, EntityPlayer player) {
        return dc.getWorld().canMineBlock(player, dc.x, dc.y, dc.z);
    }

    public static boolean isBlockAir(World w, int x, int y, int z) {
        try {
            return w.getBlock(x, y, z).isAir((IBlockAccess)w, x, y, z);
        }
        catch (Throwable e) {
            return false;
        }
    }

    private static boolean sameStackStags(ItemStack a, ItemStack b) {
        NBTTagCompound tb;
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        NBTTagCompound ta = a.getTagCompound();
        if (ta == (tb = b.getTagCompound())) {
            return true;
        }
        if (ta == null && tb == null || ta != null && ta.hasNoTags() && tb == null || tb != null && tb.hasNoTags() && ta == null || ta != null && ta.hasNoTags() && tb != null && tb.hasNoTags()) {
            return true;
        }
        if (ta == null && tb != null || ta != null && tb == null) {
            return false;
        }
        if (AESharedNBT.isShared(ta) && AESharedNBT.isShared(tb)) {
            return ta == tb;
        }
        return Platform.NBTEqualityTest((NBTBase)ta, (NBTBase)tb);
    }

    public static boolean NBTEqualityTest(NBTBase left, NBTBase right) {
        byte id = left.getId();
        if (id == right.getId()) {
            switch (id) {
                case 10: {
                    NBTTagCompound ctA = (NBTTagCompound)left;
                    NBTTagCompound ctB = (NBTTagCompound)right;
                    Set cA = ctA.func_150296_c();
                    Set cB = ctB.func_150296_c();
                    if (cA.size() != cB.size()) {
                        return false;
                    }
                    for (String name : cA) {
                        NBTBase tag = ctA.getTag(name);
                        NBTBase aTag = ctB.getTag(name);
                        if (aTag == null) {
                            return false;
                        }
                        if (Platform.NBTEqualityTest(tag, aTag)) continue;
                        return false;
                    }
                    return true;
                }
                case 9: {
                    NBTTagList lA = (NBTTagList)left;
                    NBTTagList lB = (NBTTagList)right;
                    if (lA.tagCount() != lB.tagCount()) {
                        return false;
                    }
                    List tag = lA.tagList;
                    List aTag = lB.tagList;
                    if (tag.size() != aTag.size()) {
                        return false;
                    }
                    for (int x = 0; x < tag.size(); ++x) {
                        if (aTag.get(x) == null) {
                            return false;
                        }
                        if (Platform.NBTEqualityTest((NBTBase)tag.get(x), (NBTBase)aTag.get(x))) continue;
                        return false;
                    }
                    return true;
                }
                case 1: {
                    return ((NBTBase.NBTPrimitive)left).func_150287_d() == ((NBTBase.NBTPrimitive)right).func_150287_d();
                }
                case 4: {
                    return ((NBTBase.NBTPrimitive)left).func_150291_c() == ((NBTBase.NBTPrimitive)right).func_150291_c();
                }
                case 8: {
                    return ((NBTTagString)left).func_150285_a_().equals(((NBTTagString)right).func_150285_a_()) || ((NBTTagString)left).func_150285_a_().equals(((NBTTagString)right).func_150285_a_());
                }
                case 6: {
                    return ((NBTBase.NBTPrimitive)left).func_150286_g() == ((NBTBase.NBTPrimitive)right).func_150286_g();
                }
                case 5: {
                    return ((NBTBase.NBTPrimitive)left).func_150288_h() == ((NBTBase.NBTPrimitive)right).func_150288_h();
                }
                case 3: {
                    return ((NBTBase.NBTPrimitive)left).func_150287_d() == ((NBTBase.NBTPrimitive)right).func_150287_d();
                }
            }
            return left.equals((Object)right);
        }
        return false;
    }

    public static int NBTOrderlessHash(NBTBase nbt) {
        int hash = 0;
        byte id = nbt.getId();
        hash += id;
        switch (id) {
            case 10: {
                NBTTagCompound ctA = (NBTTagCompound)nbt;
                Set cA = ctA.func_150296_c();
                for (String name : cA) {
                    hash += name.hashCode() ^ Platform.NBTOrderlessHash(ctA.getTag(name));
                }
                return hash;
            }
            case 9: {
                NBTTagList lA = (NBTTagList)nbt;
                hash += 9 * lA.tagCount();
                List l = lA.tagList;
                for (int x = 0; x < l.size(); ++x) {
                    hash += Integer.valueOf(x).hashCode() ^ Platform.NBTOrderlessHash((NBTBase)l.get(x));
                }
                return hash;
            }
            case 1: {
                return hash + ((NBTBase.NBTPrimitive)nbt).func_150290_f();
            }
            case 4: {
                return hash + (int)((NBTBase.NBTPrimitive)nbt).func_150291_c();
            }
            case 8: {
                return hash + ((NBTTagString)nbt).func_150285_a_().hashCode();
            }
            case 6: {
                return hash + (int)((NBTBase.NBTPrimitive)nbt).func_150286_g();
            }
            case 5: {
                return hash + (int)((NBTBase.NBTPrimitive)nbt).func_150288_h();
            }
            case 3: {
                return hash + ((NBTBase.NBTPrimitive)nbt).func_150287_d();
            }
        }
        return hash;
    }

    public static IRecipe findMatchingRecipe(InventoryCrafting inventoryCrafting, World par2World) {
        if (lastUsedRecipe != null && lastUsedRecipe.matches(inventoryCrafting, par2World)) {
            return lastUsedRecipe;
        }
        CraftingManager cm = CraftingManager.getInstance();
        List rl = cm.getRecipeList();
        for (IRecipe r : rl) {
            if (!r.matches(inventoryCrafting, par2World)) continue;
            lastUsedRecipe = r;
            return r;
        }
        return null;
    }

    public static ItemStack[] getBlockDrops(World w, int x, int y, int z) {
        ArrayList out = new ArrayList();
        Block which = w.getBlock(x, y, z);
        if (which != null) {
            out = which.getDrops(w, x, y, z, w.getBlockMetadata(x, y, z), 0);
        }
        if (out == null) {
            return new ItemStack[0];
        }
        return out.toArray(new ItemStack[0]);
    }

    public static ForgeDirection cycleOrientations(ForgeDirection dir, boolean upAndDown) {
        ForgeDirection forgeDirection;
        if (upAndDown) {
            ForgeDirection forgeDirection2;
            switch (dir) {
                default: {
                    throw new IncompatibleClassChangeError();
                }
                case NORTH: {
                    forgeDirection2 = ForgeDirection.SOUTH;
                    break;
                }
                case SOUTH: {
                    forgeDirection2 = ForgeDirection.EAST;
                    break;
                }
                case EAST: {
                    forgeDirection2 = ForgeDirection.WEST;
                    break;
                }
                case WEST: {
                    forgeDirection2 = ForgeDirection.NORTH;
                    break;
                }
                case UP: {
                    forgeDirection2 = ForgeDirection.UP;
                    break;
                }
                case DOWN: {
                    forgeDirection2 = ForgeDirection.DOWN;
                    break;
                }
                case UNKNOWN: {
                    forgeDirection2 = ForgeDirection.UNKNOWN;
                }
            }
            return forgeDirection2;
        }
        switch (dir) {
            default: {
                throw new IncompatibleClassChangeError();
            }
            case UP: {
                forgeDirection = ForgeDirection.DOWN;
                break;
            }
            case DOWN: {
                forgeDirection = ForgeDirection.NORTH;
                break;
            }
            case NORTH: {
                forgeDirection = ForgeDirection.SOUTH;
                break;
            }
            case SOUTH: {
                forgeDirection = ForgeDirection.EAST;
                break;
            }
            case EAST: {
                forgeDirection = ForgeDirection.WEST;
                break;
            }
            case WEST: {
                forgeDirection = ForgeDirection.UP;
                break;
            }
            case UNKNOWN: {
                forgeDirection = ForgeDirection.UNKNOWN;
            }
        }
        return forgeDirection;
    }

    public static NBTTagCompound openNbtData(ItemStack i) {
        NBTTagCompound compound = i.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            i.setTagCompound(compound);
        }
        return compound;
    }

    public static void spawnDrops(World w, int x, int y, int z, List<ItemStack> drops) {
        if (Platform.isServer()) {
            for (ItemStack i : drops) {
                if (i == null || i.stackSize <= 0) continue;
                double offset_x = (Platform.getRandomInt() % 32 - 16) / 82;
                double offset_y = (Platform.getRandomInt() % 32 - 16) / 82;
                double offset_z = (Platform.getRandomInt() % 32 - 16) / 82;
                EntityItem ei = new EntityItem(w, 0.5 + offset_x + (double)x, 0.5 + offset_y + (double)y, 0.2 + offset_z + (double)z, i.copy());
                w.spawnEntityInWorld((Entity)ei);
            }
        }
    }

    public static boolean isServer() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer();
    }

    public static int getRandomInt() {
        return Math.abs(RANDOM_GENERATOR.nextInt());
    }

    public static IInventory GetChestInv(Object te) {
        TileEntityChest x;
        TileEntityChest teA = (TileEntityChest)te;
        TileEntity teB = null;
        Block myBlockID = teA.getWorldObj().getBlock(teA.xCoord, teA.yCoord, teA.zCoord);
        if (teA.getWorldObj().getBlock(teA.xCoord + 1, teA.yCoord, teA.zCoord) == myBlockID && !((teB = teA.getWorldObj().getTileEntity(teA.xCoord + 1, teA.yCoord, teA.zCoord)) instanceof TileEntityChest)) {
            teB = null;
        }
        if (teB == null && teA.getWorldObj().getBlock(teA.xCoord - 1, teA.yCoord, teA.zCoord) == myBlockID) {
            teB = teA.getWorldObj().getTileEntity(teA.xCoord - 1, teA.yCoord, teA.zCoord);
            if (!(teB instanceof TileEntityChest)) {
                teB = null;
            } else {
                x = teA;
                teA = (TileEntityChest)teB;
                teB = x;
            }
        }
        if (teB == null && teA.getWorldObj().getBlock(teA.xCoord, teA.yCoord, teA.zCoord + 1) == myBlockID && !((teB = teA.getWorldObj().getTileEntity(teA.xCoord, teA.yCoord, teA.zCoord + 1)) instanceof TileEntityChest)) {
            teB = null;
        }
        if (teB == null && teA.getWorldObj().getBlock(teA.xCoord, teA.yCoord, teA.zCoord - 1) == myBlockID) {
            teB = teA.getWorldObj().getTileEntity(teA.xCoord, teA.yCoord, teA.zCoord - 1);
            if (!(teB instanceof TileEntityChest)) {
                teB = null;
            } else {
                x = teA;
                teA = (TileEntityChest)teB;
                teB = x;
            }
        }
        if (teB == null) {
            return teA;
        }
        return new InventoryLargeChest("", (IInventory)teA, (IInventory)teB);
    }

    public static boolean isModLoaded(String modid) {
        try {
            return Loader.isModLoaded((String)modid);
        }
        catch (Throwable throwable) {
            for (ModContainer f : Loader.instance().getActiveModList()) {
                if (!f.getModId().equals(modid)) continue;
                return true;
            }
            return false;
        }
    }

    public static ItemStack findMatchingRecipeOutput(InventoryCrafting ic, World worldObj) {
        return CraftingManager.getInstance().findMatchingRecipe(ic, worldObj);
    }

    @SideOnly(value=Side.CLIENT)
    public static List<String> getTooltip(Object o) {
        if (o == null) {
            return new ArrayList<String>();
        }
        ItemStack itemStack = null;
        if (o instanceof AEItemStack) {
            AEItemStack ais = (AEItemStack)o;
            return ais.getToolTip();
        }
        if (!(o instanceof ItemStack)) {
            return new ArrayList<String>();
        }
        itemStack = (ItemStack)o;
        try {
            return itemStack.getTooltip((EntityPlayer)Minecraft.getMinecraft().thePlayer, false);
        }
        catch (Exception errB) {
            return new ArrayList<String>();
        }
    }

    public static String getModId(IAEItemStack is) {
        if (is == null) {
            return "** Null";
        }
        String n = ((AEItemStack)is).getModID();
        return n == null ? "** Null" : n;
    }

    public static String getItemDisplayName(Object o) {
        if (o == null) {
            return "** Null";
        }
        ItemStack itemStack = null;
        if (o instanceof AEItemStack) {
            String n = ((AEItemStack)o).getDisplayName();
            return n == null ? "** Null" : n;
        }
        if (!(o instanceof ItemStack)) {
            return "**Invalid Object";
        }
        itemStack = (ItemStack)o;
        try {
            String name = itemStack.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = itemStack.getItem().getUnlocalizedName(itemStack);
            }
            return name == null ? "** Null" : name;
        }
        catch (Exception errA) {
            try {
                String n = itemStack.getUnlocalizedName();
                return n == null ? "** Null" : n;
            }
            catch (Exception errB) {
                return "** Exception";
            }
        }
    }

    public static boolean hasSpecialComparison(IAEItemStack willAdd) {
        if (willAdd == null) {
            return false;
        }
        IAETagCompound tag = willAdd.getTagCompound();
        return tag != null && tag.getSpecialComparison() != null;
    }

    public static boolean hasSpecialComparison(ItemStack willAdd) {
        return AESharedNBT.isShared(willAdd.getTagCompound()) && ((IAETagCompound)willAdd.getTagCompound()).getSpecialComparison() != null;
    }

    public static boolean isWrench(EntityPlayer player, ItemStack stack, int x, int y, int z) {
        if (stack != null) {
            Item itemWrench = stack.getItem();
            if (itemWrench instanceof IAEWrench) {
                IAEWrench wrench = (IAEWrench)itemWrench;
                return wrench.canWrench(stack, player, x, y, z);
            }
            if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.CoFHWrench) && itemWrench instanceof IToolHammer) {
                IToolHammer wrench = (IToolHammer)itemWrench;
                return wrench.isUsable(stack, (EntityLivingBase)player, x, y, z);
            }
            if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.BuildCraftCore) && itemWrench instanceof IToolWrench) {
                IToolWrench wrench = (IToolWrench)itemWrench;
                return wrench.canWrench(player, x, y, z);
            }
        }
        return false;
    }

    public static boolean isChargeable(ItemStack i) {
        if (i == null) {
            return false;
        }
        Item it = i.getItem();
        if (it instanceof IAEItemPowerStorage) {
            return ((IAEItemPowerStorage)it).getPowerFlow(i) != AccessRestriction.READ;
        }
        return false;
    }

    public static EntityPlayer getPlayer(WorldServer w) {
        EntityPlayer fakePlayer;
        if (w == null) {
            throw new InvalidParameterException("World is null.");
        }
        WeakReference<EntityPlayer> weakRef = FAKE_PLAYERS.get(w);
        if (weakRef != null && (fakePlayer = (EntityPlayer)weakRef.get()) != null) {
            return fakePlayer;
        }
        FakePlayer p = FakePlayerFactory.get((WorldServer)w, (GameProfile)fakeProfile);
        FAKE_PLAYERS.put((World)w, new WeakReference<FakePlayer>(p));
        return p;
    }

    public static int MC2MEColor(int color) {
        switch (color) {
            case 4: {
                return 0;
            }
            case 0: {
                return 1;
            }
            case 15: {
                return 2;
            }
            case 3: {
                return 3;
            }
            case 1: {
                return 4;
            }
            case 11: {
                return 5;
            }
            case 2: {
                return 6;
            }
        }
        return -1;
    }

    public static int findEmpty(Object[] l) {
        for (int x = 0; x < l.length; ++x) {
            if (l[x] != null) continue;
            return x;
        }
        return -1;
    }

    public static <T> T pickRandom(Collection<T> outs) {
        int index;
        Iterator<T> i = outs.iterator();
        for (index = RANDOM_GENERATOR.nextInt(outs.size()); i.hasNext() && index > 0; --index) {
            i.next();
        }
        --index;
        if (i.hasNext()) {
            return i.next();
        }
        return null;
    }

    public static ForgeDirection rotateAround(ForgeDirection forward, ForgeDirection axis) {
        if (axis == ForgeDirection.UNKNOWN || forward == ForgeDirection.UNKNOWN) {
            return forward;
        }
        switch (forward) {
            case DOWN: {
                switch (axis) {
                    case DOWN: {
                        return forward;
                    }
                    case UP: {
                        return forward;
                    }
                    case NORTH: {
                        return ForgeDirection.EAST;
                    }
                    case SOUTH: {
                        return ForgeDirection.WEST;
                    }
                    case EAST: {
                        return ForgeDirection.NORTH;
                    }
                    case WEST: {
                        return ForgeDirection.SOUTH;
                    }
                }
                break;
            }
            case UP: {
                switch (axis) {
                    case NORTH: {
                        return ForgeDirection.WEST;
                    }
                    case SOUTH: {
                        return ForgeDirection.EAST;
                    }
                    case EAST: {
                        return ForgeDirection.SOUTH;
                    }
                    case WEST: {
                        return ForgeDirection.NORTH;
                    }
                }
                break;
            }
            case NORTH: {
                switch (axis) {
                    case UP: {
                        return ForgeDirection.WEST;
                    }
                    case DOWN: {
                        return ForgeDirection.EAST;
                    }
                    case EAST: {
                        return ForgeDirection.UP;
                    }
                    case WEST: {
                        return ForgeDirection.DOWN;
                    }
                }
                break;
            }
            case SOUTH: {
                switch (axis) {
                    case UP: {
                        return ForgeDirection.EAST;
                    }
                    case DOWN: {
                        return ForgeDirection.WEST;
                    }
                    case EAST: {
                        return ForgeDirection.DOWN;
                    }
                    case WEST: {
                        return ForgeDirection.UP;
                    }
                }
                break;
            }
            case EAST: {
                switch (axis) {
                    case UP: {
                        return ForgeDirection.NORTH;
                    }
                    case DOWN: {
                        return ForgeDirection.SOUTH;
                    }
                    case NORTH: {
                        return ForgeDirection.UP;
                    }
                    case SOUTH: {
                        return ForgeDirection.DOWN;
                    }
                }
            }
            case WEST: {
                switch (axis) {
                    case UP: {
                        return ForgeDirection.SOUTH;
                    }
                    case DOWN: {
                        return ForgeDirection.NORTH;
                    }
                    case NORTH: {
                        return ForgeDirection.DOWN;
                    }
                    case SOUTH: {
                        return ForgeDirection.UP;
                    }
                }
            }
        }
        return forward;
    }

    @SideOnly(value=Side.CLIENT)
    public static String gui_localize(String string) {
        return StatCollector.translateToLocal((String)string);
    }

    public static boolean isSameItemPrecise(@Nullable ItemStack is, @Nullable ItemStack filter) {
        return Platform.isSameItem(is, filter) && Platform.sameStackStags(is, filter);
    }

    public static boolean isSameItemFuzzy(ItemStack a, ItemStack b, FuzzyMode mode) {
        OreReference bOR;
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        if (b == null) {
            return false;
        }
        if (a.getItem() != null && b.getItem() != null && a.getItem().isDamageable() && a.getItem() == b.getItem()) {
            try {
                if (mode == FuzzyMode.IGNORE_ALL) {
                    return true;
                }
                if (mode == FuzzyMode.PERCENT_99) {
                    return a.getItemDamageForDisplay() > 1 == b.getItemDamageForDisplay() > 1;
                }
                float percentDamagedOfA = 1.0f - (float)a.getItemDamageForDisplay() / (float)a.getMaxDamage();
                float percentDamagedOfB = 1.0f - (float)b.getItemDamageForDisplay() / (float)b.getMaxDamage();
                return percentDamagedOfA > mode.breakPoint == percentDamagedOfB > mode.breakPoint;
            }
            catch (Throwable e) {
                if (mode == FuzzyMode.IGNORE_ALL) {
                    return true;
                }
                if (mode == FuzzyMode.PERCENT_99) {
                    return a.getItemDamage() > 1 == b.getItemDamage() > 1;
                }
                float percentDamagedOfA = (float)a.getItemDamage() / (float)a.getMaxDamage();
                float percentDamagedOfB = (float)b.getItemDamage() / (float)b.getMaxDamage();
                return percentDamagedOfA > mode.breakPoint == percentDamagedOfB > mode.breakPoint;
            }
        }
        OreReference aOR = OreHelper.INSTANCE.isOre(a);
        if (OreHelper.INSTANCE.sameOre(aOR, bOR = OreHelper.INSTANCE.isOre(b))) {
            return true;
        }
        return a.isItemEqual(b);
    }

    public static LookDirection getPlayerRay(EntityPlayer player, float eyeOffset) {
        float f = 1.0f;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * 1.0f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * 1.0f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * 1.0;
        double d1 = eyeOffset;
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * 1.0;
        Vec3 vec3 = Vec3.createVectorHelper((double)d0, (double)d1, (double)d2);
        float f3 = MathHelper.cos((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f4 = MathHelper.sin((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f5 = -MathHelper.cos((float)(-f1 * ((float)Math.PI / 180)));
        float f6 = MathHelper.sin((float)(-f1 * ((float)Math.PI / 180)));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        if (player instanceof EntityPlayerMP) {
            d3 = ((EntityPlayerMP)player).theItemInWorldManager.getBlockReachDistance();
        }
        Vec3 vec31 = vec3.addVector((double)f7 * d3, (double)f6 * d3, (double)f8 * d3);
        return new LookDirection(vec3, vec31);
    }

    public static MovingObjectPosition rayTrace(EntityPlayer p, boolean hitBlocks, boolean hitEntities) {
        World w = p.getEntityWorld();
        float f = 1.0f;
        float f1 = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * 1.0f;
        float f2 = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * 1.0f;
        double d0 = p.prevPosX + (p.posX - p.prevPosX) * 1.0;
        double d1 = p.prevPosY + (p.posY - p.prevPosY) * 1.0 + 1.62 - (double)p.yOffset;
        double d2 = p.prevPosZ + (p.posZ - p.prevPosZ) * 1.0;
        Vec3 vec3 = Vec3.createVectorHelper((double)d0, (double)d1, (double)d2);
        float f3 = MathHelper.cos((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f4 = MathHelper.sin((float)(-f2 * ((float)Math.PI / 180) - (float)Math.PI));
        float f5 = -MathHelper.cos((float)(-f1 * ((float)Math.PI / 180)));
        float f6 = MathHelper.sin((float)(-f1 * ((float)Math.PI / 180)));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 32.0;
        Vec3 vec31 = vec3.addVector((double)f7 * 32.0, (double)f6 * 32.0, (double)f8 * 32.0);
        AxisAlignedBB bb = AxisAlignedBB.getBoundingBox((double)Math.min(vec3.xCoord, vec31.xCoord), (double)Math.min(vec3.yCoord, vec31.yCoord), (double)Math.min(vec3.zCoord, vec31.zCoord), (double)Math.max(vec3.xCoord, vec31.xCoord), (double)Math.max(vec3.yCoord, vec31.yCoord), (double)Math.max(vec3.zCoord, vec31.zCoord)).expand(16.0, 16.0, 16.0);
        Entity entity = null;
        double closest = 9999999.0;
        if (hitEntities) {
            List list = w.getEntitiesWithinAABBExcludingEntity((Entity)p, bb);
            for (Object o : list) {
                double nd;
                AxisAlignedBB boundingBox;
                MovingObjectPosition movingObjectPosition;
                Entity entity1 = (Entity)o;
                if (entity1.isDead || entity1 == p || entity1 instanceof EntityItem || !entity1.isEntityAlive() || entity1.riddenByEntity == p || (movingObjectPosition = (boundingBox = entity1.boundingBox.expand((double)(f1 = 0.3f), (double)f1, (double)f1)).calculateIntercept(vec3, vec31)) == null || !((nd = vec3.squareDistanceTo(movingObjectPosition.hitVec)) < closest)) continue;
                entity = entity1;
                closest = nd;
            }
        }
        MovingObjectPosition pos = null;
        Vec3 vec = null;
        if (hitBlocks) {
            vec = Vec3.createVectorHelper((double)d0, (double)d1, (double)d2);
            pos = w.rayTraceBlocks(vec3, vec31, true);
        }
        if (entity != null && pos != null && pos.hitVec.squareDistanceTo(vec) > closest) {
            pos = new MovingObjectPosition(entity);
        } else if (entity != null && pos == null) {
            pos = new MovingObjectPosition(entity);
        }
        return pos;
    }

    public static long nanoTime() {
        return 0L;
    }

    public static <StackType extends IAEStack> StackType poweredExtraction(IEnergySource energy, IMEInventory<StackType> cell, StackType request, BaseActionSource src) {
        double availablePower;
        long itemToExtract;
        StackType possible = cell.extractItems(request.copy(), Actionable.SIMULATE, src);
        long retrieved = 0L;
        if (possible != null) {
            retrieved = possible.getStackSize();
        }
        if ((itemToExtract = Math.min((long)((availablePower = energy.extractAEPower(retrieved, Actionable.SIMULATE, PowerMultiplier.CONFIG)) + 0.9), retrieved)) > 0L) {
            energy.extractAEPower(retrieved, Actionable.MODULATE, PowerMultiplier.CONFIG);
            possible.setStackSize(itemToExtract);
            StackType ret = cell.extractItems(possible, Actionable.MODULATE, src);
            if (ret != null && src.isPlayer()) {
                Stats.ItemsExtracted.addToPlayer(((PlayerSource)src).player, (int)ret.getStackSize());
            }
            return ret;
        }
        return null;
    }

    public static <StackType extends IAEStack> StackType poweredInsert(IEnergySource energy, IMEInventory<StackType> cell, StackType input, BaseActionSource src) {
        long typeMultiplier;
        double availablePower;
        long itemToAdd;
        StackType possible = cell.injectItems(input.copy(), Actionable.SIMULATE, src);
        long stored = input.getStackSize();
        if (possible != null) {
            stored -= possible.getStackSize();
        }
        if ((itemToAdd = Math.min((long)((availablePower = energy.extractAEPower(Platform.ceilDiv(stored, typeMultiplier = input instanceof IAEFluidStack ? 1000L : 1L), Actionable.SIMULATE, PowerMultiplier.CONFIG)) * (double)typeMultiplier + 0.9), stored)) > 0L) {
            energy.extractAEPower(Platform.ceilDiv(stored, typeMultiplier), Actionable.MODULATE, PowerMultiplier.CONFIG);
            if (itemToAdd < input.getStackSize()) {
                long original = input.getStackSize();
                StackType split = input.copy();
                split.decStackSize(itemToAdd);
                input.setStackSize(itemToAdd);
                split.add(cell.injectItems(input, Actionable.MODULATE, src));
                if (src.isPlayer()) {
                    long diff = original - split.getStackSize();
                    Stats.ItemsInserted.addToPlayer(((PlayerSource)src).player, (int)diff);
                }
                return split;
            }
            StackType ret = cell.injectItems(input, Actionable.MODULATE, src);
            if (src.isPlayer()) {
                long diff = ret == null ? input.getStackSize() : input.getStackSize() - ret.getStackSize();
                Stats.ItemsInserted.addToPlayer(((PlayerSource)src).player, (int)diff);
            }
            return ret;
        }
        return input;
    }

    public static void postChanges(IStorageGrid gs, ItemStack removed, ItemStack added, BaseActionSource src) {
        IItemList<IAEItemStack> itemChanges = AEApi.instance().storage().createItemList();
        IItemList<IAEFluidStack> fluidChanges = AEApi.instance().storage().createFluidList();
        IMEInventoryHandler myItems = null;
        IMEInventoryHandler myFluids = null;
        if (removed != null) {
            myItems = AEApi.instance().registries().cell().getCellInventory(removed, null, StorageChannel.ITEMS);
            if (myItems != null) {
                for (IAEItemStack iAEItemStack : myItems.getAvailableItems(itemChanges, IterationCounter.fetchNewId())) {
                    iAEItemStack.setStackSize(-iAEItemStack.getStackSize());
                }
            }
            if ((myFluids = AEApi.instance().registries().cell().getCellInventory(removed, null, StorageChannel.FLUIDS)) != null) {
                for (IAEFluidStack iAEFluidStack : myFluids.getAvailableItems(fluidChanges, IterationCounter.fetchNewId())) {
                    iAEFluidStack.setStackSize(-iAEFluidStack.getStackSize());
                }
            }
        }
        if (added != null) {
            myItems = AEApi.instance().registries().cell().getCellInventory(added, null, StorageChannel.ITEMS);
            if (myItems != null) {
                myItems.getAvailableItems(itemChanges, IterationCounter.fetchNewId());
            }
            if ((myFluids = AEApi.instance().registries().cell().getCellInventory(added, null, StorageChannel.FLUIDS)) != null) {
                myFluids.getAvailableItems(fluidChanges, IterationCounter.fetchNewId());
            }
        }
        if (myItems == null) {
            gs.postAlterationOfStoredItems(StorageChannel.FLUIDS, fluidChanges, src);
        } else {
            gs.postAlterationOfStoredItems(StorageChannel.ITEMS, itemChanges, src);
        }
    }

    public static <T extends IAEStack<T>> void postListChanges(IItemList<T> before, IItemList<T> after, IMEMonitorHandlerReceiver<T> meMonitorPassthrough, BaseActionSource source) {
        LinkedList<IAEStack> changes = new LinkedList<IAEStack>();
        for (IAEStack is : before) {
            is.setStackSize(-is.getStackSize());
        }
        for (IAEStack is : after) {
            before.add(is);
        }
        for (IAEStack is : before) {
            if (is.getStackSize() == 0L) continue;
            changes.add(is);
        }
        if (!changes.isEmpty()) {
            meMonitorPassthrough.postChange(null, changes, source);
        }
    }

    public static int generateTileHash(TileEntity target) {
        int hash;
        block9: {
            block7: {
                TileEntityChest chest;
                block11: {
                    block10: {
                        block8: {
                            if (target == null) {
                                return 0;
                            }
                            hash = target.hashCode();
                            if (target instanceof ITileStorageMonitorable) {
                                return 0;
                            }
                            if (!(target instanceof TileEntityChest)) break block7;
                            chest = (TileEntityChest)target;
                            chest.checkForAdjacentChests();
                            if (chest.adjacentChestZNeg == null) break block8;
                            hash ^= chest.adjacentChestZNeg.hashCode();
                            break block9;
                        }
                        if (chest.adjacentChestZPos == null) break block10;
                        hash ^= chest.adjacentChestZPos.hashCode();
                        break block9;
                    }
                    if (chest.adjacentChestXPos == null) break block11;
                    hash ^= chest.adjacentChestXPos.hashCode();
                    break block9;
                }
                if (chest.adjacentChestXNeg == null) break block9;
                hash ^= chest.adjacentChestXNeg.hashCode();
                break block9;
            }
            if (target instanceof IInventory) {
                hash ^= ((IInventory)target).getSizeInventory();
                if (target instanceof ISidedInventory) {
                    for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                        int[] sides = ((ISidedInventory)target).getAccessibleSlotsFromSide(dir.ordinal());
                        if (sides == null) {
                            return 0;
                        }
                        int offset = 0;
                        for (int side : sides) {
                            int c = side << offset % 8 ^ 1 << dir.ordinal();
                            ++offset;
                            hash = c + (hash << 6) + (hash << 16) - hash;
                        }
                    }
                }
            }
        }
        return hash;
    }

    public static boolean securityCheck(GridNode a, GridNode b) {
        boolean b_isSecure;
        if (a.getLastSecurityKey() == -1L && b.getLastSecurityKey() == -1L) {
            return false;
        }
        if (a.getLastSecurityKey() == b.getLastSecurityKey()) {
            return false;
        }
        boolean a_isSecure = Platform.isPowered(a.getGrid()) && a.getLastSecurityKey() != -1L;
        boolean bl = b_isSecure = Platform.isPowered(b.getGrid()) && b.getLastSecurityKey() != -1L;
        if (AEConfig.instance.isFeatureEnabled(AEFeature.LogSecurityAudits)) {
            AELog.info("Audit: " + a_isSecure + " : " + b_isSecure + " @ " + a.getLastSecurityKey() + " vs " + b.getLastSecurityKey() + " & " + a.getPlayerID() + " vs " + b.getPlayerID(), new Object[0]);
        }
        if (a_isSecure && b_isSecure) {
            return true;
        }
        if (!a_isSecure && b_isSecure) {
            return Platform.checkPlayerPermissions(b.getGrid(), a.getPlayerID());
        }
        if (a_isSecure && !b_isSecure) {
            return Platform.checkPlayerPermissions(a.getGrid(), b.getPlayerID());
        }
        return false;
    }

    private static boolean isPowered(IGrid grid) {
        if (grid == null) {
            return false;
        }
        IEnergyGrid eg = (IEnergyGrid)grid.getCache(IEnergyGrid.class);
        return eg.isNetworkPowered();
    }

    private static boolean checkPlayerPermissions(IGrid grid, int playerID) {
        if (grid == null) {
            return false;
        }
        ISecurityGrid gs = (ISecurityGrid)grid.getCache(ISecurityGrid.class);
        if (gs == null) {
            return false;
        }
        if (!gs.isAvailable()) {
            return false;
        }
        return !gs.hasPermission(playerID, SecurityPermissions.BUILD);
    }

    public static void configurePlayer(EntityPlayer player, ForgeDirection side, TileEntity tile) {
        player.yOffset = 1.8f;
        float yaw = 0.0f;
        float pitch = 0.0f;
        switch (side) {
            case DOWN: {
                pitch = 90.0f;
                player.yOffset = -1.8f;
                break;
            }
            case EAST: {
                yaw = -90.0f;
                break;
            }
            case NORTH: {
                yaw = 180.0f;
                break;
            }
            case SOUTH: {
                yaw = 0.0f;
                break;
            }
            case UNKNOWN: {
                break;
            }
            case UP: {
                pitch = 90.0f;
                break;
            }
            case WEST: {
                yaw = 90.0f;
            }
        }
        player.posX = (double)tile.xCoord + 0.5;
        player.posY = (double)tile.yCoord + 0.5;
        player.posZ = (double)tile.zCoord + 0.5;
        player.prevCameraPitch = player.cameraPitch = pitch;
        player.rotationPitch = player.cameraPitch;
        player.prevCameraYaw = player.cameraYaw = yaw;
        player.rotationYaw = player.cameraYaw;
    }

    public static boolean canAccess(AENetworkProxy gridProxy, BaseActionSource src) {
        try {
            if (src.isPlayer()) {
                return gridProxy.getSecurity().hasPermission(((PlayerSource)src).player, SecurityPermissions.BUILD);
            }
            if (src.isMachine()) {
                IActionHost te = ((MachineSource)src).via;
                IGridNode n = te.getActionableNode();
                if (n == null) {
                    return false;
                }
                int playerID = n.getPlayerID();
                return gridProxy.getSecurity().hasPermission(playerID, SecurityPermissions.BUILD);
            }
            return false;
        }
        catch (GridAccessException gae) {
            return false;
        }
    }

    public static ItemStack extractItemsByRecipe(IEnergySource energySrc, BaseActionSource mySrc, IMEMonitor<IAEItemStack> src, World w, IRecipe r, ItemStack output, InventoryCrafting ci, ItemStack providedTemplate, int slot, IItemList<IAEItemStack> items, Actionable realForFake, IPartitionList<IAEItemStack> filter) {
        return Platform.extractItemsByRecipe(energySrc, mySrc, src, w, r, output, ci, providedTemplate, slot, items, realForFake, filter, 1);
    }

    public static ItemStack extractItemsByRecipe(IEnergySource energySrc, BaseActionSource mySrc, IMEMonitor<IAEItemStack> src, World w, IRecipe r, ItemStack output, InventoryCrafting ci, ItemStack providedTemplate, int slot, IItemList<IAEItemStack> items, Actionable realForFake, IPartitionList<IAEItemStack> filter, int multiple) {
        if (energySrc.extractAEPower(multiple, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.9) {
            boolean checkFuzzy;
            ItemStack extracted;
            IAEItemStack ae_ext;
            if (providedTemplate == null) {
                return null;
            }
            AEItemStack ae_req = AEItemStack.create(providedTemplate);
            ae_req.setStackSize(multiple);
            if ((filter == null || filter.isListed(ae_req)) && (ae_ext = (IAEItemStack)src.extractItems(ae_req, realForFake, mySrc)) != null && (extracted = ae_ext.getItemStack()) != null) {
                energySrc.extractAEPower(multiple, realForFake, PowerMultiplier.CONFIG);
                return extracted;
            }
            boolean bl = checkFuzzy = ae_req.isOre() || providedTemplate.getItemDamage() == Short.MAX_VALUE || providedTemplate.hasTagCompound() || providedTemplate.isItemStackDamageable();
            if (items != null && checkFuzzy) {
                for (IAEItemStack x : items) {
                    ItemStack sh = x.getItemStack();
                    if (!Platform.isSameItemType(providedTemplate, sh) && !ae_req.sameOre(x) || Platform.isSameItem(sh, output)) continue;
                    ItemStack cp = Platform.cloneItemStack(sh);
                    cp.stackSize = 1;
                    ci.setInventorySlotContents(slot, cp);
                    if (r.matches(ci, w) && Platform.isSameItem(r.getCraftingResult(ci), output)) {
                        IAEItemStack ex;
                        IAEItemStack ax = x.copy();
                        ax.setStackSize(multiple);
                        if ((filter == null || filter.isListed(ax)) && (ex = src.extractItems(ax, realForFake, mySrc)) != null) {
                            energySrc.extractAEPower(multiple, realForFake, PowerMultiplier.CONFIG);
                            return ex.getItemStack();
                        }
                    }
                    ci.setInventorySlotContents(slot, providedTemplate);
                }
            }
        }
        return null;
    }

    public static boolean isSameItemType(ItemStack that, ItemStack other) {
        if (that != null && other != null && that.getItem() == other.getItem()) {
            if (that.isItemStackDamageable()) {
                return true;
            }
            return that.getItemDamage() == other.getItemDamage();
        }
        return false;
    }

    public static boolean isSameItem(@Nullable ItemStack left, @Nullable ItemStack right) {
        return left != null && right != null && left.isItemEqual(right);
    }

    public static ItemStack cloneItemStack(ItemStack a) {
        return a.copy();
    }

    public static ItemStack getContainerItem(ItemStack stackInSlot) {
        if (stackInSlot == null) {
            return null;
        }
        Item i = stackInSlot.getItem();
        if (i == null || !i.hasContainerItem(stackInSlot)) {
            if (stackInSlot.stackSize > 1) {
                --stackInSlot.stackSize;
                return stackInSlot;
            }
            return null;
        }
        ItemStack ci = i.getContainerItem(stackInSlot.copy());
        if (ci != null && ci.isItemStackDamageable() && ci.getItemDamage() > ci.getMaxDamage()) {
            ci = null;
        }
        return ci;
    }

    public static void notifyBlocksOfNeighbors(World worldObj, int xCoord, int yCoord, int zCoord) {
        if (!worldObj.isRemote) {
            TickHandler.INSTANCE.addCallable(worldObj, new BlockUpdate(xCoord, yCoord, zCoord));
        }
    }

    public static boolean canRepair(AEFeature type, ItemStack a, ItemStack b) {
        if (b == null || a == null) {
            return false;
        }
        if (type == AEFeature.CertusQuartzTools) {
            IItemDefinition certusQuartzCrystal = AEApi.instance().definitions().materials().certusQuartzCrystal();
            return certusQuartzCrystal.isSameAs(b);
        }
        if (type == AEFeature.NetherQuartzTools) {
            return Items.quartz == b.getItem();
        }
        return false;
    }

    public static Object findPreferred(ItemStack[] is) {
        IParts parts = AEApi.instance().definitions().parts();
        for (ItemStack stack : is) {
            if (parts.cableGlass().sameAs(AEColor.Transparent, stack)) {
                return stack;
            }
            if (parts.cableCovered().sameAs(AEColor.Transparent, stack)) {
                return stack;
            }
            if (parts.cableSmart().sameAs(AEColor.Transparent, stack)) {
                return stack;
            }
            if (parts.cableDense().sameAs(AEColor.Transparent, stack)) {
                return stack;
            }
            if (!parts.cableDenseCovered().sameAs(AEColor.Transparent, stack)) continue;
            return stack;
        }
        return is;
    }

    public static void sendChunk(Chunk c, int verticalBits) {
        try {
            Object playerInstance;
            WorldServer ws = (WorldServer)c.worldObj;
            PlayerManager pm = ws.getPlayerManager();
            if (getOrCreateChunkWatcher == null) {
                getOrCreateChunkWatcher = ReflectionHelper.findMethod(PlayerManager.class, (Object)pm, (String[])new String[]{"getOrCreateChunkWatcher", "func_72690_a"}, (Class[])new Class[]{Integer.TYPE, Integer.TYPE, Boolean.TYPE});
            }
            if (getOrCreateChunkWatcher != null && (playerInstance = getOrCreateChunkWatcher.invoke((Object)pm, c.xPosition, c.zPosition, false)) != null) {
                Platform.playerInstance = playerInstance.getClass();
                if (sendToAllPlayersWatchingChunk == null) {
                    sendToAllPlayersWatchingChunk = ReflectionHelper.findMethod((Class)Platform.playerInstance, (Object)playerInstance, (String[])new String[]{"sendToAllPlayersWatchingChunk", "func_151251_a"}, (Class[])new Class[]{Packet.class});
                }
                if (sendToAllPlayersWatchingChunk != null) {
                    sendToAllPlayersWatchingChunk.invoke(playerInstance, new S21PacketChunkData(c, false, verticalBits));
                }
            }
        }
        catch (Throwable t) {
            AELog.debug(t);
        }
    }

    public static AxisAlignedBB getPrimaryBox(ForgeDirection side, int facadeThickness) {
        switch (side) {
            case DOWN: {
                return AxisAlignedBB.getBoundingBox((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)((double)facadeThickness / 16.0), (double)1.0);
            }
            case EAST: {
                return AxisAlignedBB.getBoundingBox((double)((16.0 - (double)facadeThickness) / 16.0), (double)0.0, (double)0.0, (double)1.0, (double)1.0, (double)1.0);
            }
            case NORTH: {
                return AxisAlignedBB.getBoundingBox((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)1.0, (double)((double)facadeThickness / 16.0));
            }
            case SOUTH: {
                return AxisAlignedBB.getBoundingBox((double)0.0, (double)0.0, (double)((16.0 - (double)facadeThickness) / 16.0), (double)1.0, (double)1.0, (double)1.0);
            }
            case UP: {
                return AxisAlignedBB.getBoundingBox((double)0.0, (double)((16.0 - (double)facadeThickness) / 16.0), (double)0.0, (double)1.0, (double)1.0, (double)1.0);
            }
            case WEST: {
                return AxisAlignedBB.getBoundingBox((double)0.0, (double)0.0, (double)0.0, (double)((double)facadeThickness / 16.0), (double)1.0, (double)1.0);
            }
        }
        return AxisAlignedBB.getBoundingBox((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)1.0, (double)1.0);
    }

    public static float getEyeOffset(EntityPlayer player) {
        assert (player.worldObj.isRemote) : "Valid only on client";
        return (float)(player.posY + (double)player.getEyeHeight() - (double)player.getDefaultEyeHeight());
    }

    public static void addStat(int playerID, Achievement achievement) {
        EntityPlayer p = AEApi.instance().registries().players().findPlayer(playerID);
        if (p != null) {
            p.addStat((StatBase)achievement, 1);
        }
    }

    public static boolean isRecipePrioritized(ItemStack what) {
        IMaterials materials = AEApi.instance().definitions().materials();
        boolean isPurified = materials.purifiedCertusQuartzCrystal().isSameAs(what);
        isPurified |= materials.purifiedFluixCrystal().isSameAs(what);
        return isPurified |= materials.purifiedNetherQuartzCrystal().isSameAs(what);
    }

    public static ItemStack loadItemStackFromNBT(NBTTagCompound tagCompound) {
        ItemStack stack = ItemStack.loadItemStackFromNBT((NBTTagCompound)tagCompound);
        if (stack != null) {
            stack.stackSize = tagCompound.getInteger("Count");
        }
        return stack;
    }

    public static NBTTagCompound writeItemStackToNBT(ItemStack is, NBTTagCompound tagCompound) {
        is.writeToNBT(tagCompound);
        tagCompound.setInteger("Count", is.stackSize);
        return tagCompound;
    }

    public static IAEItemStack getAEStackInSlot(Slot slot) {
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        if (slot instanceof SlotME) {
            return ((SlotME)slot).getAEStack();
        }
        if (slot instanceof SlotFake) {
            return ((SlotFake)slot).getAEStack();
        }
        return AEItemStack.create(slot.getStack());
    }

    public static long ceilDiv(long a, long b) {
        return Math.addExact(Math.addExact(a, b), -1L) / b;
    }

    public static String formatByteDouble(double n) {
        for (int i = 1; i < BYTE_LIMIT.length; ++i) {
            if (!(n < BYTE_LIMIT[i])) continue;
            return df.format(n / BYTE_LIMIT[i - 1]) + " " + BYTE_UNIT[i - 1];
        }
        return n / BYTE_LIMIT[0] + " " + BYTE_UNIT[0];
    }

    public static String formatNumberDouble(double n) {
        return Platform.formatNumberDoubleRestrictedByWidth(n, 3);
    }

    public static String formatNumberDoubleRestrictedByWidth(double n, int width) {
        String slimResult;
        double base;
        String numberString = df.format(n);
        int numberSize = numberString.length();
        if (numberSize <= width) {
            return numberString;
        }
        double last = base * 1000.0;
        int exponent = -1;
        String postFix = "";
        for (base = n; base >= 1000.0; base /= 1000.0) {
            last = base;
            postFix = String.valueOf(NUM_UNIT[++exponent]);
        }
        String withPrecision = df.format(last / 1000.0);
        String withoutPrecision = Long.toString((long)base);
        String string = slimResult = withPrecision.length() <= width ? withPrecision : withoutPrecision;
        assert (slimResult.length() <= width);
        return slimResult + postFix;
    }

    static {
        fakeProfile = new GameProfile(UUID.fromString("839eb18c-50bc-400c-8291-9383f09763e7"), "[AE2Player]");
        BYTE_UNIT = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"};
        NUM_UNIT = "kMGTPE".toCharArray();
        df = new DecimalFormat("#.##");
        BYTE_LIMIT = new double[10];
        for (int i = 0; i < 10; ++i) {
            Platform.BYTE_LIMIT[i] = Math.pow(2.0, i * 10);
        }
        lastUsedRecipe = null;
    }
}

