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
package com.tz.statpatterns.api.ids;

import com.tz.statpatterns.crafting.EncodedStatisticalPattern;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

import static com.tz.statpatterns.ProbabilityPatternMod.MOD_ID;

public class Components {
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DataComponentType<EncodedStatisticalPattern> ENCODED_STATISTICAL_PATTERN = register("encoded_statistical_pattern", builder -> builder.persistent(EncodedStatisticalPattern.CODEC).networkSynchronized(EncodedStatisticalPattern.STREAM_CODEC));

    /**
     * Stores the PatternEncodingLogic state (blank/encoded pattern inventories, mode, etc.)
     * on the portable probability pattern terminal item stack.
     */
    public static final DataComponentType<CompoundTag> PATTERN_LOGIC_STATE = register("pattern_logic_state", builder -> builder.persistent(CompoundTag.CODEC));

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DR.register(name, () -> componentType);
        return componentType;
    }
}
