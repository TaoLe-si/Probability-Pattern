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
package com.tz.statpatterns;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class StatPatternsCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StatPatternsMod.MOD_ID);

    private static final List<ItemDefinition<?>> itemDefs = new ArrayList<>();
    private static final List<Item> rawItems = new ArrayList<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.statpatterns"))
                    .icon(() -> new ItemStack(AEItems.PROCESSING_PATTERN.get()))
                    .displayItems((parameters, output) -> {
                        for (var itemDefinition : itemDefs) {
                            output.accept(itemDefinition);
                        }
                        for (var item : rawItems) {
                            output.accept(item);
                        }
                    })
                    .build());

    public static void add(ItemDefinition<?> itemDef) {
        itemDefs.add(itemDef);
    }

    public static void addRaw(Item item) {
        rawItems.add(item);
    }

    private StatPatternsCreativeTabs() {
    }
}
