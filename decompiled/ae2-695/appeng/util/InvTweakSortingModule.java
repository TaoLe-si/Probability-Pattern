/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.eventhandler.SubscribeEvent
 *  javax.annotation.Nonnull
 *  net.minecraft.client.Minecraft
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.oredict.OreDictionary
 *  net.minecraftforge.oredict.OreDictionary$OreRegisterEvent
 *  org.apache.commons.lang3.ObjectUtils
 *  org.apache.logging.log4j.Level
 */
package appeng.util;

import appeng.core.AELog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.annotation.Nonnull;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.Level;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class InvTweakSortingModule {
    private static InvTweaksItemTree tree = null;
    public static final File MINECRAFT_DIR = Minecraft.getMinecraft().mcDataDir;
    public static final File MINECRAFT_CONFIG_DIR = new File(MINECRAFT_DIR, "config/AppliedEnergistics2");
    public static final File CONFIG_TREE_FILE = new File(MINECRAFT_CONFIG_DIR, "InvTweaksTree.txt");

    public static void init() {
        try {
            if (CONFIG_TREE_FILE.exists()) {
                tree = InvTweaksItemTreeLoader.load(CONFIG_TREE_FILE);
            } else {
                AELog.info("Tree file is missing!", new Object[0]);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isLoaded() {
        return tree != null;
    }

    public static int compareItems(ItemStack i, ItemStack j) {
        return InvTweaks.compareItems(i, j, InvTweaks.getItemOrder(i), InvTweaks.getItemOrder(j));
    }

    private static class InvTweaksItemTree {
        public static final String UNKNOWN_ITEM = "unknown";
        private final Map<String, InvTweaksItemTreeCategory> categories = new HashMap<String, InvTweaksItemTreeCategory>();
        private final Map<String, Vector<InvTweaksItemTreeItem>> itemsById = new HashMap<String, Vector<InvTweaksItemTreeItem>>(500);
        private static Vector<InvTweaksItemTreeItem> defaultItems = null;
        private final Map<String, Vector<InvTweaksItemTreeItem>> itemsByName = new HashMap<String, Vector<InvTweaksItemTreeItem>>(500);
        private String rootCategory;
        private List<OreDictInfo> oresRegistered = new ArrayList<OreDictInfo>();

        public InvTweaksItemTree() {
            this.reset();
        }

        public void reset() {
            if (defaultItems == null) {
                defaultItems = new Vector();
                defaultItems.add(new InvTweaksItemTreeItem(UNKNOWN_ITEM, null, Short.MAX_VALUE, Integer.MAX_VALUE));
            }
            this.categories.clear();
            this.itemsByName.clear();
            this.itemsById.clear();
        }

        public boolean matches(List<InvTweaksItemTreeItem> items, String keyword) {
            if (items == null) {
                return false;
            }
            for (InvTweaksItemTreeItem item : items) {
                if (item.getName() == null || !item.getName().equals(keyword)) continue;
                return true;
            }
            InvTweaksItemTreeCategory category = this.getCategory(keyword);
            if (category != null) {
                for (InvTweaksItemTreeItem item : items) {
                    if (!category.contains(item)) continue;
                    return true;
                }
            }
            return keyword.equals(this.rootCategory);
        }

        public InvTweaksItemTreeCategory getRootCategory() {
            return this.categories.get(this.rootCategory);
        }

        public InvTweaksItemTreeCategory getCategory(String keyword) {
            return this.categories.get(keyword);
        }

        public int getItemOrder(String id, int damage) {
            List<InvTweaksItemTreeItem> addedItems;
            if (id == null) {
                return 0;
            }
            List items = this.itemsById.get(id);
            if (items != null) {
                for (InvTweaksItemTreeItem item : items) {
                    if (!item.matchesDamage(damage)) continue;
                    return item.getOrder();
                }
            }
            if (!(addedItems = this.addUnrecognizedItem(id, damage)).isEmpty()) {
                return addedItems.get(0).getOrder();
            }
            return 0;
        }

        public void setRootCategory(InvTweaksItemTreeCategory category) {
            this.rootCategory = category.getName();
            this.categories.put(this.rootCategory, category);
        }

        public void addCategory(String parentCategory, InvTweaksItemTreeCategory newCategory) throws NullPointerException {
            this.categories.get(parentCategory.toLowerCase()).addCategory(newCategory);
            this.categories.put(newCategory.getName(), newCategory);
        }

        public void addItem(String parentCategory, InvTweaksItemTreeItem newItem) throws NullPointerException {
            Vector<InvTweaksItemTreeItem> list;
            this.categories.get(parentCategory.toLowerCase()).addItem(newItem);
            if (this.itemsByName.containsKey(newItem.getName())) {
                this.itemsByName.get(newItem.getName()).add(newItem);
            } else {
                list = new Vector<InvTweaksItemTreeItem>();
                list.add(newItem);
                this.itemsByName.put(newItem.getName(), list);
            }
            if (this.itemsById.containsKey(newItem.getId())) {
                this.itemsById.get(newItem.getId()).add(newItem);
            } else {
                list = new Vector();
                list.add(newItem);
                this.itemsById.put(newItem.getId(), list);
            }
        }

        @Nonnull
        private List<InvTweaksItemTreeItem> addUnrecognizedItem(String id, int damage) {
            InvTweaksItemTreeItem newItemId = new InvTweaksItemTreeItem(String.format("%s-%d", id, damage), id, damage, 5000 + damage);
            InvTweaksItemTreeItem newItemDamage = new InvTweaksItemTreeItem(id, id, Short.MAX_VALUE, 5000);
            this.addItem(this.getRootCategory().getName(), newItemId);
            this.addItem(this.getRootCategory().getName(), newItemDamage);
            return Arrays.asList(newItemId, newItemDamage);
        }

        public void registerOre(String category, String name, String oreName, int order) {
            for (ItemStack i : OreDictionary.getOres((String)oreName)) {
                if (i != null) {
                    this.addItem(category, new InvTweaksItemTreeItem(name, Item.itemRegistry.getNameForObject((Object)i.getItem()), i.getItemDamage(), order));
                    continue;
                }
                AELog.logSimple(Level.WARN, String.format("An OreDictionary entry for %s is null", oreName));
            }
            this.oresRegistered.add(new OreDictInfo(category, name, oreName, order));
        }

        @SubscribeEvent
        public void oreRegistered(OreDictionary.OreRegisterEvent ev) {
            for (OreDictInfo ore : this.oresRegistered) {
                if (!ore.oreName.equals(ev.Name)) continue;
                if (ev.Ore.getItem() != null) {
                    this.addItem(ore.category, new InvTweaksItemTreeItem(ore.name, Item.itemRegistry.getNameForObject((Object)ev.Ore.getItem()), ev.Ore.getItemDamage(), ore.order));
                    continue;
                }
                AELog.logSimple(Level.WARN, String.format("An OreDictionary entry for %s is null", ev.Name));
            }
        }

        private static class OreDictInfo {
            String category;
            String name;
            String oreName;
            int order;

            OreDictInfo(String category, String name, String oreName, int order) {
                this.category = category;
                this.name = name;
                this.oreName = oreName;
                this.order = order;
            }
        }
    }

    private static class InvTweaksItemTreeLoader
    extends DefaultHandler {
        private static final String ATTR_ID = "id";
        private static final String ATTR_DAMAGE = "damage";
        private static final String ATTR_RANGE_DMIN = "dmin";
        private static final String ATTR_RANGE_DMAX = "dmax";
        private static final String ATTR_OREDICT_NAME = "oreDictName";
        private static InvTweaksItemTree tree;
        private static int itemOrder;
        private static LinkedList<String> categoryStack;

        private InvTweaksItemTreeLoader() {
        }

        private static void init() {
            tree = new InvTweaksItemTree();
            itemOrder = 0;
            categoryStack = new LinkedList();
        }

        public static synchronized InvTweaksItemTree load(File file) throws Exception {
            InvTweaksItemTreeLoader.init();
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(file, (DefaultHandler)new InvTweaksItemTreeLoader());
            return tree;
        }

        @Override
        public synchronized void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            String rangeDMinAttr = attributes.getValue(ATTR_RANGE_DMIN);
            String oreDictNameAttr = attributes.getValue(ATTR_OREDICT_NAME);
            if (attributes.getLength() == 0 || rangeDMinAttr != null) {
                if (categoryStack.isEmpty()) {
                    tree.setRootCategory(new InvTweaksItemTreeCategory(name));
                } else {
                    tree.addCategory(categoryStack.getLast(), new InvTweaksItemTreeCategory(name));
                }
                if (rangeDMinAttr != null) {
                    String id = attributes.getValue(ATTR_ID);
                    int rangeDMin = Integer.parseInt(rangeDMinAttr);
                    int rangeDMax = Integer.parseInt(attributes.getValue(ATTR_RANGE_DMAX));
                    tree.addItem(name, new InvTweaksItemTreeItem((name + id + "-" + rangeDMin + "-" + rangeDMax).toLowerCase(), id, rangeDMin, rangeDMax, itemOrder++));
                }
                categoryStack.add(name);
            } else if (attributes.getValue(ATTR_ID) != null) {
                String id = attributes.getValue(ATTR_ID);
                int damage = Short.MAX_VALUE;
                if (attributes.getValue(ATTR_DAMAGE) != null) {
                    damage = Integer.parseInt(attributes.getValue(ATTR_DAMAGE));
                }
                tree.addItem(categoryStack.getLast(), new InvTweaksItemTreeItem(name.toLowerCase(), id, damage, itemOrder++));
            } else if (oreDictNameAttr != null) {
                tree.registerOre(categoryStack.getLast(), name.toLowerCase(), oreDictNameAttr, itemOrder++);
            }
        }

        @Override
        public synchronized void endElement(String uri, String localName, String name) throws SAXException {
            if (!categoryStack.isEmpty() && name.equals(categoryStack.getLast())) {
                categoryStack.removeLast();
            }
        }
    }

    private static class InvTweaks {
        private InvTweaks() {
        }

        private static int getItemOrder(ItemStack itemStack) {
            if (itemStack == null) {
                return Integer.MAX_VALUE;
            }
            return tree.getItemOrder(Item.itemRegistry.getNameForObject((Object)itemStack.getItem()), itemStack.getItemDamage());
        }

        static int compareItems(ItemStack i, ItemStack j, int orderI, int orderJ) {
            if (j == null) {
                return -1;
            }
            if (i == null || orderI == -1) {
                return 1;
            }
            if (orderI == orderJ) {
                if (i.getItem() == j.getItem()) {
                    boolean iHasName = i.hasDisplayName();
                    boolean jHasName = j.hasDisplayName();
                    if (iHasName || jHasName) {
                        String jDisplayName;
                        if (!iHasName) {
                            return -1;
                        }
                        if (!jHasName) {
                            return 1;
                        }
                        String iDisplayName = i.getDisplayName();
                        if (!iDisplayName.equals(jDisplayName = j.getDisplayName())) {
                            return iDisplayName.compareTo(jDisplayName);
                        }
                    }
                    Map iEnchs = EnchantmentHelper.getEnchantments((ItemStack)i);
                    Map jEnchs = EnchantmentHelper.getEnchantments((ItemStack)j);
                    if (iEnchs.size() == jEnchs.size()) {
                        int iEnchMaxId = 0;
                        int iEnchMaxLvl = 0;
                        int jEnchMaxId = 0;
                        int jEnchMaxLvl = 0;
                        for (Map.Entry ench : iEnchs.entrySet()) {
                            if ((Integer)ench.getValue() > iEnchMaxLvl) {
                                iEnchMaxId = (Integer)ench.getKey();
                                iEnchMaxLvl = (Integer)ench.getValue();
                                continue;
                            }
                            if ((Integer)ench.getValue() != iEnchMaxLvl || (Integer)ench.getKey() <= iEnchMaxId) continue;
                            iEnchMaxId = (Integer)ench.getKey();
                        }
                        for (Map.Entry ench : jEnchs.entrySet()) {
                            if ((Integer)ench.getValue() > jEnchMaxLvl) {
                                jEnchMaxId = (Integer)ench.getKey();
                                jEnchMaxLvl = (Integer)ench.getValue();
                                continue;
                            }
                            if ((Integer)ench.getValue() != jEnchMaxLvl || (Integer)ench.getKey() <= jEnchMaxId) continue;
                            jEnchMaxId = (Integer)ench.getKey();
                        }
                        if (iEnchMaxId == jEnchMaxId) {
                            if (iEnchMaxLvl == jEnchMaxLvl) {
                                if (i.getItemDamage() != j.getItemDamage()) {
                                    if (i.isItemStackDamageable()) {
                                        return j.getItemDamage() - i.getItemDamage();
                                    }
                                    return i.getItemDamage() - j.getItemDamage();
                                }
                                return j.stackSize - i.stackSize;
                            }
                            return jEnchMaxLvl - iEnchMaxLvl;
                        }
                        return jEnchMaxId - iEnchMaxId;
                    }
                    return jEnchs.size() - iEnchs.size();
                }
                return ObjectUtils.compare((Comparable)((Object)Item.itemRegistry.getNameForObject((Object)i.getItem())), (Comparable)((Object)Item.itemRegistry.getNameForObject((Object)j.getItem())));
            }
            return orderI - orderJ;
        }
    }

    private static class InvTweaksItemTreeItem {
        private String name;
        private String id;
        private int damageMin;
        private int damageMax;
        private int order;

        public InvTweaksItemTreeItem(String name, String id, int damage, int order) {
            this.name = name;
            this.id = InvTweaksItemTreeItem.getNamespacedID(id);
            this.damageMin = damage;
            this.damageMax = damage;
            this.order = order;
        }

        public InvTweaksItemTreeItem(String name, String id, int damageMin, int damageMax, int order) {
            this.name = name;
            this.id = InvTweaksItemTreeItem.getNamespacedID(id);
            this.damageMin = damageMin;
            this.damageMax = damageMax;
            this.order = order;
        }

        public static String getNamespacedID(String id) {
            if (id == null) {
                return null;
            }
            if (id.indexOf(58) == -1) {
                return "minecraft:" + id;
            }
            return id;
        }

        public String getName() {
            return this.name;
        }

        public String getId() {
            return this.id;
        }

        public int getDamage() {
            return this.damageMin;
        }

        public boolean matchesDamage(int damage) {
            if (damage == Short.MAX_VALUE || this.damageMin == Short.MAX_VALUE || this.damageMax == Short.MAX_VALUE) {
                return true;
            }
            return damage >= this.damageMin && damage <= this.damageMax;
        }

        public int getOrder() {
            return this.order;
        }

        public boolean equals(Object o) {
            if (!(o instanceof InvTweaksItemTreeItem)) {
                return false;
            }
            InvTweaksItemTreeItem item = (InvTweaksItemTreeItem)o;
            return ObjectUtils.equals((Object)this.id, (Object)item.getId()) && (this.damageMin == Short.MAX_VALUE || this.damageMin <= item.getDamage() && this.damageMax >= item.getDamage());
        }

        public String toString() {
            return this.name;
        }

        public int compareTo(InvTweaksItemTreeItem item) {
            return item.getOrder() - this.getOrder();
        }
    }

    private static class InvTweaksItemTreeCategory {
        private final Map<String, List<InvTweaksItemTreeItem>> items = new HashMap<String, List<InvTweaksItemTreeItem>>();
        private final Vector<String> matchingItems = new Vector();
        private final Vector<InvTweaksItemTreeCategory> subCategories = new Vector();
        private String name;
        private int order = -1;

        public InvTweaksItemTreeCategory(String name) {
            this.name = name != null ? name.toLowerCase() : null;
        }

        public boolean contains(InvTweaksItemTreeItem item) {
            List<InvTweaksItemTreeItem> storedItems = this.items.get(item.getId());
            if (storedItems != null) {
                for (InvTweaksItemTreeItem storedItem : storedItems) {
                    if (!storedItem.equals(item)) continue;
                    return true;
                }
            }
            for (InvTweaksItemTreeCategory category : this.subCategories) {
                if (!category.contains(item)) continue;
                return true;
            }
            return false;
        }

        public void addCategory(InvTweaksItemTreeCategory category) {
            this.subCategories.add(category);
        }

        public void addItem(InvTweaksItemTreeItem item) {
            if (this.items.get(item.getId()) == null) {
                ArrayList<InvTweaksItemTreeItem> itemList = new ArrayList<InvTweaksItemTreeItem>();
                itemList.add(item);
                this.items.put(item.getId(), itemList);
            } else {
                this.items.get(item.getId()).add(item);
            }
            this.matchingItems.add(item.getName());
            if (this.order == -1 || this.order > item.getOrder()) {
                this.order = item.getOrder();
            }
        }

        public Collection<InvTweaksItemTreeCategory> getSubCategories() {
            return this.subCategories;
        }

        public Collection<List<InvTweaksItemTreeItem>> getItems() {
            return this.items.values();
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return this.name + " (" + this.subCategories.size() + " cats, " + this.items.size() + " items)";
        }
    }
}

