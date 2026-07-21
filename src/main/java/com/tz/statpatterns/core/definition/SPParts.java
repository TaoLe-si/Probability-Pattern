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

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartModels;
import appeng.core.definitions.ColoredItemDefinition;
import appeng.core.definitions.ItemDefinition;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.tz.statpatterns.api.ids.ItemIds;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.tz.statpatterns.core.definition.SPItems.item;

public class SPParts {
    public static final List<ColoredItemDefinition<?>> COLORED_PARTS = new ArrayList<>();

    public static final ItemDefinition<PartItem<ProbabilityPatternTerminalPart>> ProbabilityPatternTerminalPart = createPart("Probability Pattern Terminal Part", ItemIds.PROBABILITY_PATTERN_TERMINAL, ProbabilityPatternTerminalPart.class, ProbabilityPatternTerminalPart::new);

    private static <T extends IPart> ItemDefinition<PartItem<T>> createPart(
            String englishName,
            ResourceLocation id,
            Class<T> partClass,
            Function<IPartItem<T>, T> factory) {

        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return item(englishName, id, props -> new PartItem<>(props, partClass, factory));
    }

    private static <T extends IPart> ItemDefinition<PartItem<T>> createCustomPartItem(
            String englishName,
            ResourceLocation id,
            Class<T> partClass,
            Function<Item.Properties, PartItem<T>> factory) {

        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return item(englishName, id, factory);
    }

    // Used to control in which order static constructors are called
    public static void init() {
    }
}
