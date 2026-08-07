/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  codechicken.nei.FormattedTextField$TextFormatter
 *  cpw.mods.fml.common.Optional$Interface
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumChatFormatting
 *  net.minecraftforge.oredict.OreDictionary
 *  org.apache.commons.lang3.StringUtils
 */
package appeng.util.prioitylist;

import appeng.api.storage.data.IAEItemStack;
import appeng.core.AELog;
import appeng.util.prioitylist.IPartitionList;
import codechicken.nei.FormattedTextField;
import cpw.mods.fml.common.Optional;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.StringUtils;

public class OreFilteredList
implements IPartitionList<IAEItemStack> {
    private final Predicate<IAEItemStack> filterPredicate;

    public OreFilteredList(String filter) {
        this.filterPredicate = OreFilteredList.makeFilter(filter.trim());
    }

    @Override
    public boolean isListed(IAEItemStack input) {
        return this.filterPredicate != null && this.filterPredicate.test(input);
    }

    @Override
    public boolean isEmpty() {
        return this.filterPredicate == null;
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return null;
    }

    public static Predicate<IAEItemStack> makeFilter(String f) {
        try {
            Predicate<ItemStack> matcher = OreFilteredList.makeMatcher(f);
            if (matcher == null) {
                return null;
            }
            return new OreListMatcher(matcher);
        }
        catch (Exception ex) {
            AELog.debug(ex);
            return null;
        }
    }

    private static Predicate<ItemStack> makeMatcher(String f) {
        Predicate<ItemStack> matcher = null;
        if (OreFilteredList.notAWildcard(f)) {
            Predicate<String> test = Pattern.compile(f).asPredicate();
            matcher = is -> is != null && IntStream.of(OreDictionary.getOreIDs((ItemStack)is)).mapToObj(OreDictionary::getOreName).anyMatch(test);
        } else if (!f.isEmpty()) {
            String[] filters = f.split("[&|]");
            String lastFilter = null;
            for (String filter : filters) {
                if ((filter = filter.trim()).isEmpty()) continue;
                boolean negated = filter.startsWith("!");
                if (negated) {
                    filter = filter.substring(1);
                }
                Predicate<ItemStack> test = OreFilteredList.filterToItemStackPredicate(filter);
                if (negated) {
                    test = test.negate();
                }
                if (matcher == null) {
                    matcher = test;
                    lastFilter = filter;
                    continue;
                }
                int endLast = f.indexOf(lastFilter) + lastFilter.length();
                int startThis = f.indexOf(filter);
                lastFilter = filter;
                if (startThis <= endLast) continue;
                boolean or = f.substring(endLast, startThis).contains("|");
                matcher = or ? matcher.or(test) : matcher.and(test);
            }
        }
        return matcher;
    }

    private static boolean notAWildcard(String f) {
        return f.contains("\\") || f.contains("^") || f.contains("$") || f.contains("+") || f.contains("(") || f.contains(")") || f.contains("[") || f.contains("]");
    }

    private static Predicate<ItemStack> filterToItemStackPredicate(String filter) {
        Predicate<String> test = OreFilteredList.filterToPredicate(filter);
        return is -> is != null && IntStream.of(OreDictionary.getOreIDs((ItemStack)is)).mapToObj(OreDictionary::getOreName).anyMatch(test);
    }

    private static Predicate<String> filterToPredicate(String filter) {
        int numStars = StringUtils.countMatches((CharSequence)filter, (CharSequence)"*");
        if (numStars == filter.length()) {
            return str -> true;
        }
        if (filter.length() > 2 && filter.startsWith("*") && filter.endsWith("*") && numStars == 2) {
            String pattern = filter.substring(1, filter.length() - 1);
            return str -> str.contains(pattern);
        }
        if (filter.length() >= 2 && filter.startsWith("*") && numStars == 1) {
            String pattern = filter.substring(1);
            return str -> str.endsWith(pattern);
        }
        if (filter.length() >= 2 && filter.endsWith("*") && numStars == 1) {
            String pattern = filter.substring(0, filter.length() - 1);
            return str -> str.startsWith(pattern);
        }
        if (numStars == 0) {
            return str -> str.equals(filter);
        }
        String filterRegexFragment = filter.replace("*", ".*");
        String regexPattern = "^" + filterRegexFragment + "$";
        Pattern pattern = Pattern.compile(regexPattern);
        return pattern.asPredicate();
    }

    private static class OreListMatcher
    implements Predicate<IAEItemStack> {
        final Map<ItemRef, Boolean> cache = new ConcurrentHashMap<ItemRef, Boolean>();
        final Predicate<ItemStack> matcher;

        public OreListMatcher(Predicate<ItemStack> matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean test(IAEItemStack t) {
            if (t == null) {
                return false;
            }
            return this.cache.compute(new ItemRef(t), (k, v) -> v != null ? v.booleanValue() : this.matcher.test(t.getItemStack()));
        }
    }

    private static class ItemRef {
        private final Item ref;
        private final int damage;
        private final int hash;

        ItemRef(IAEItemStack stack) {
            this.ref = stack.getItem();
            this.damage = stack.getItem().isDamageable() ? 0 : stack.getItemDamage();
            this.hash = this.ref.hashCode() ^ this.damage;
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            ItemRef other = (ItemRef)obj;
            return this.damage == other.damage && this.ref == other.ref;
        }

        public String toString() {
            return "ItemRef [ref=" + this.ref.getUnlocalizedName() + ", damage=" + this.damage + ", hash=" + this.hash + ']';
        }
    }

    @Optional.Interface(modid="NotEnoughItems", iface="codechicken.nei.FormattedTextField.TextFormatter")
    public static class OreFilterTextFormatter
    implements FormattedTextField.TextFormatter {
        public String format(String text) {
            if (OreFilteredList.notAWildcard(text)) {
                return text.replaceAll("([" + Pattern.quote("^$+()[].*?|:{}\\") + "])", EnumChatFormatting.AQUA + "$1" + EnumChatFormatting.RESET);
            }
            if (!text.isEmpty()) {
                String[] parts = text.split("\\|");
                StringJoiner formattedText = new StringJoiner(EnumChatFormatting.GRAY + "|" + EnumChatFormatting.RESET);
                for (String filterText : parts) {
                    formattedText.add(this.formatRules(filterText));
                }
                if (text.endsWith("|")) {
                    formattedText.add("");
                }
                return formattedText.toString();
            }
            return text;
        }

        private String formatRules(String text) {
            String[] parts = text.split("\\&");
            StringJoiner formattedText = new StringJoiner(EnumChatFormatting.GRAY + "&" + EnumChatFormatting.RESET);
            for (String filterText : parts) {
                formattedText.add(this.formatPattern(filterText));
            }
            if (text.endsWith("&")) {
                formattedText.add("");
            }
            return formattedText.toString();
        }

        private String formatPattern(String filter) {
            Matcher filterMatcher = Pattern.compile("(\\s*)(!*)(.*)").matcher(filter);
            if (filterMatcher.find()) {
                StringBuilder formattedPart = new StringBuilder();
                formattedPart.append(filterMatcher.group(1));
                if ("!".equals(filterMatcher.group(2))) {
                    formattedPart.append(EnumChatFormatting.RED + "!" + EnumChatFormatting.RESET);
                }
                formattedPart.append(filterMatcher.group(3).replace("*", EnumChatFormatting.AQUA + "*" + EnumChatFormatting.RESET));
                return formattedPart.toString();
            }
            return filter;
        }
    }
}

