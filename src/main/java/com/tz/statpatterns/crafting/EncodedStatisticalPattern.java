/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.crafting;

import net.minecraft.nbt.NBTTagCompound;

/**
 * NBT keys of a probability pattern. The NBT is exactly the vanilla GTNH 695 encoded
 * pattern ("in" / "out" / "crafting" / "substitute" / "beSubstitute" / "author") with
 * the probability parameters added under the {@code sp_*} keys.
 */
public final class EncodedStatisticalPattern {

    public static final String TAG_INPUTS = "in";
    public static final String TAG_OUTPUT = "out";
    public static final String TAG_CRAFTING = "crafting";
    public static final String TAG_SUBSTITUTE = "substitute";
    public static final String TAG_SUCCESS_PROBABILITY = "sp_probability";
    public static final String TAG_ALPHA = "sp_alpha";
    public static final String TAG_ALPHA95 = "sp_alpha95";
    public static final String TAG_SMALL_SAMPLE_LIMIT = "sp_smallSampleLimit";

    private EncodedStatisticalPattern() {}

    /**
     * @return true if the given pattern tag carries the probability parameters (i.e. it is
     *         an encoded probability pattern, not a blank pattern or a vanilla pattern).
     */
    public static boolean isProbabilityPattern(final NBTTagCompound tag) {
        return tag != null && tag.hasKey(TAG_SUCCESS_PROBABILITY);
    }
}
