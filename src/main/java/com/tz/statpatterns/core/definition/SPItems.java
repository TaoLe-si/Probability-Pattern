
/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.core.definition;

import appeng.core.definitions.ItemDefinition;
import com.google.common.base.Preconditions;
import com.tz.statpatterns.SPCreativeTabs;
import com.tz.statpatterns.api.ids.ItemIds;
import com.tz.statpatterns.api.ids.SPCreativeTabIds;
import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.tz.statpatterns.crafting.StatisticalPatternDetails;
import com.tz.statpatterns.item.ProbabilityPatternTerminalItem;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static com.tz.statpatterns.ProbabilityPatternMod.MOD_ID;

public final class SPItems {
    public static final DeferredRegister.Items DR = DeferredRegister.createItems(MOD_ID);

    private static final List<ItemDefinition<?>> ITEMS = new ArrayList<>();

    // Not added to creative tab — blank pattern obtained via terminal mechanics, no standalone recipe
    public static final ItemDefinition<Item> PROBABILITY_PATTERN = item("Probability Pattern", ItemIds.PROBABILITY_PATTERN, (p) -> new ProbabilityPatternItem(
            p.stacksTo(64),
            StatisticalPatternDetails::decode,
            StatisticalPatternDetails::getInvalidPatternTooltip), null);

    // Only initialized when ae2wtlib is present; null otherwise.
    // NOT registered through DR.registerItem() because the DeferredRegister uses NORMAL
    // priority while ae2wtlib_api's AddTerminalEvent.run() also fires at NORMAL priority —
    // but ae2wtlib_api loads first, so its listener fires before ours, causing
    // DeferredHolder.get() to NPE. Instead we use DeferredItem.createItem() and register
    // the item in a HIGH priority listener in ProbabilityPatternMod.
    @Nullable
    public static final ItemDefinition<ProbabilityPatternTerminalItem> WIRELESS_PROBABILITY_PATTERN_TERMINAL;

    static {
        if (ModList.get().isLoaded("ae2wtlib")) {
            var deferredItem = DeferredItem.<ProbabilityPatternTerminalItem>createItem(
                    ItemIds.WIRELESS_PROBABILITY_PATTERN_TERMINAL);
            WIRELESS_PROBABILITY_PATTERN_TERMINAL = itemDef(
                    "Wireless Probability Pattern Terminal", deferredItem);
        } else {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL = null;
        }
    }

    private SPItems() {
    }

    /**
     * Like {@link #item} but wraps a pre-existing {@link DeferredItem} that was not
     * created through {@link #DR}. Used for items whose registration must happen at
     * a specific event priority (e.g. HIGH priority to beat ae2wtlib_api's
     * AddTerminalEvent.run()).
     */
    static <T extends Item> ItemDefinition<T> itemDef(String name, DeferredItem<T> deferredItem) {
        Preconditions.checkArgument(deferredItem.getId().getNamespace().equals(MOD_ID),
                "Can only register for AE2");
        var definition = new ItemDefinition<>(name, deferredItem);
        SPCreativeTabs.add(definition);
        ITEMS.add(definition);
        return definition;
    }

    static <T extends Item> ItemDefinition<T> item(String name, ResourceLocation id, Function<Item.Properties, T> factory) {
        return item(name, id, factory, SPCreativeTabIds.MAIN);
    }

    static <T extends Item> ItemDefinition<T> item(String name, ResourceLocation id, Function<Item.Properties, T> factory, @Nullable ResourceKey<CreativeModeTab> group) {

        Item.Properties p = new Item.Properties();

        Preconditions.checkArgument(id.getNamespace().equals(MOD_ID), "Can only register for AE2");
        var definition = new ItemDefinition<>(name, DR.registerItem(id.getPath(), factory));

        if (group != null) {
            SPCreativeTabs.add(definition);
        }

        ITEMS.add(definition);

        return definition;
    }
}
