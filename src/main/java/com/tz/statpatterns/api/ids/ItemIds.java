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

import net.minecraft.resources.ResourceLocation;

import static com.tz.statpatterns.StatPatternsMod.MOD_ID;

public class ItemIds {
    public static final ResourceLocation STAT_PATTERN_TERMINAL = id("stat_pattern_terminal");
    public static final ResourceLocation STAT_PATTERN = id("stat_pattern");
    public static final ResourceLocation WIRELESS_STAT_PATTERN_TERMINAL = id("wireless_stat_pattern_terminal");

    private static ResourceLocation id(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
