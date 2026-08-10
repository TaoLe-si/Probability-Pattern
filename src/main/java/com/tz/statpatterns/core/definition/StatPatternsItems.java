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

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.ItemDefinition;
import com.google.common.base.Preconditions;
import com.tz.statpatterns.StatPatternsCreativeTabs;
import com.tz.statpatterns.api.ids.ItemIds;
import com.tz.statpatterns.api.ids.StatPatternsCreativeTabIds;
import com.tz.statpatterns.crafting.StatPatternsPatternItem;
import com.tz.statpatterns.item.StatPatternsTerminalItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static com.tz.statpatterns.StatPatternsMod.MOD_ID;

public final class StatPatternsItems {
    public static final DeferredRegister<Item> DR = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    private static final List<ItemDefinition<?>> ITEMS = new ArrayList<>();

    // Not added to creative tab — blank pattern obtained via terminal mechanics
    public static final ItemDefinition<Item> STAT_PATTERN = item("Probability Pattern", ItemIds.STAT_PATTERN, (p) -> new StatPatternsPatternItem(
            p.stacksTo(64)), null);

    // Only initialized when ae2wtlib is present; null otherwise
    @Nullable
    public static final ItemDefinition<StatPatternsTerminalItem> WIRELESS_STAT_PATTERN_TERMINAL;

    static {
        if (ModList.get().isLoaded("ae2wtlib")) {
            WIRELESS_STAT_PATTERN_TERMINAL = item("Wireless Probability Pattern Terminal",
                    ItemIds.WIRELESS_STAT_PATTERN_TERMINAL,
                    StatPatternsTerminalItem::new);
        } else {
            WIRELESS_STAT_PATTERN_TERMINAL = null;
        }
    }

    private StatPatternsItems() {
    }

    static <T extends Item> ItemDefinition<T> item(String name, ResourceLocation id, Function<Item.Properties, T> factory) {
        return item(name, id, factory, StatPatternsCreativeTabIds.MAIN);
    }

    /**
     * Lazy ItemDefinition wrapper that works with Forge's DeferredRegister.
     * AE2's ItemDefinition constructor requires the actual Item object, but
     * RegistryObject.get() is not available until registries are populated.
     * We pass Items.AIR as a placeholder and override all methods to resolve
     * the real item lazily from the RegistryObject.
     */
    private static class LazyItemDefinition<T extends Item> extends ItemDefinition<T> {
        private final RegistryObject<T> ro;

        LazyItemDefinition(String name, ResourceLocation id, RegistryObject<T> ro) {
            super(name, id, (T) Items.AIR); // placeholder — never used because all accessors are overridden
            this.ro = ro;
        }

        @Override
        public T asItem() {
            return ro.get();
        }

        @Override
        public ItemStack stack() {
            return new ItemStack(ro.get());
        }

        @Override
        public ItemStack stack(int stackSize) {
            return new ItemStack(ro.get(), stackSize);
        }

        @Override
        public GenericStack genericStack(long stackSize) {
            return new GenericStack(AEItemKey.of(ro.get()), stackSize);
        }
    }

    static <T extends Item> ItemDefinition<T> item(String name, ResourceLocation id, Function<Item.Properties, T> factory, @Nullable ResourceKey<CreativeModeTab> group) {
        Item.Properties p = new Item.Properties();
        Preconditions.checkArgument(id.getNamespace().equals(MOD_ID), "Can only registered for AE2");
        RegistryObject<T> ro = DR.register(id.getPath(), () -> factory.apply(p));
        var definition = new LazyItemDefinition<>(name, id, ro);

        if (group != null) {
            StatPatternsCreativeTabs.add(definition);
        }
        ITEMS.add(definition);
        return definition;
    }
}
