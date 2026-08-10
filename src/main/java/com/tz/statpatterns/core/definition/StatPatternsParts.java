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
import appeng.core.definitions.ItemDefinition;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.tz.statpatterns.api.ids.StatPatternsItemIds;
import com.tz.statpatterns.part.StatPatternsTerminalPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Function;

import static com.tz.statpatterns.core.definition.StatPatternsItems.createItem;

public final class StatPatternsParts {
    public static final ItemDefinition<PartItem<StatPatternsTerminalPart>> PROBABILITY_PATTERN_TERMINAL_PART = createPart("Probability Pattern Terminal Part", StatPatternsItemIds.PROBABILITY_PATTERN_TERMINAL, StatPatternsTerminalPart.class, StatPatternsTerminalPart::new);

    private StatPatternsParts() {
    }

    private static <T extends IPart> ItemDefinition<PartItem<T>> createPart(
            String englishName,
            ResourceLocation id,
            Class<T> partClass,
            Function<IPartItem<T>, T> factory) {

        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return createItem(englishName, id, props -> new PartItem<>(props, partClass, factory));
    }

    // Used to control in which order static constructors are called
    public static void init() {
    }
}
