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
package com.tz.statpatterns.integration.ae2wtlib;

import java.lang.reflect.InvocationTargetException;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.tz.statpatterns.api.ids.StatPatternsItemIds;
import com.tz.statpatterns.core.definition.StatPatternsItems;
import com.tz.statpatterns.item.StatPatternsTerminalItem;

/**
 * Registers the wireless probability pattern terminal ITEM at HIGH priority.
 * <p>
 * This deliberately lives in its own class that is <b>not</b> referenced from
 * {@code StatPatternsMod} except through a {@code ModList.isLoaded} guard:
 * the class file of the @Mod class must not contain symbolic references to
 * {@code StatPatternsTerminalItem} (which extends {@code ItemWT} from
 * ae2wtlib), otherwise the JVM fails to define the class when ae2wtlib is absent.
 * <p>
 * The HIGH priority ensures the item is registered before ae2wtlib_api's NORMAL
 * priority {@code AddTerminalEvent.run()} fires, fixing the NPE caused by
 * accessing an unresolved {@code DeferredHolder}.
 *
 * @see WirelessStatPatternsTerminalScreenRegistration
 */
public final class WirelessTerminalItemRegistrar {
    private WirelessTerminalItemRegistrar() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.HIGH, RegisterEvent.class, event -> {
            if (!event.getRegistryKey().equals(Registries.ITEM)) {
                return;
            }
            var def = StatPatternsItems.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
            if (def == null) {
                return;
            }
            event.register(Registries.ITEM,
                    StatPatternsItemIds.WIRELESS_PROBABILITY_PATTERN_TERMINAL,
                    StatPatternsTerminalItem::new);

            // Manually bind the DeferredHolder since it's not managed by
            // a DeferredRegister (which would normally call bind() at NORMAL priority).
            bindHolder((DeferredHolder<Item, ?>) (Object) def.holder());
        });
    }

    @SuppressWarnings("unchecked")
    private static void bindHolder(DeferredHolder<Item, ?> holder) {
        try {
            var bindMethod = DeferredHolder.class.getDeclaredMethod("bind", boolean.class);
            bindMethod.setAccessible(true);
            bindMethod.invoke(holder, false);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to bind DeferredHolder for wireless terminal", e);
        }
    }
}
