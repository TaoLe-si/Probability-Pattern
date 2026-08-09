/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
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
package com.zincglux.statpatterns;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

/**
 * Registers the Probability Pattern late-phase mixin (Spec v2.0 3.4.2).
 * <p>
 * AE2 is a normal mod that loads after Mixin's early phase, so its classes can only
 * be mixed into during the GTNHMixins "late" phase. This loader queues
 * {@link com.zincglux.statpatterns.mixin.CraftableItemResolverMixin} right before the first
 * mod construction event. The config is {@code mixins.statpatterns.late.json}.
 * <p>
 * <b>Important:</b> this class intentionally lives OUTSIDE the {@code com.zincglux.statpatterns.mixin}
 * package. A mixin config declares that package as a "mixin package", so classes in it cannot
 * be loaded directly via {@code Class.forName} - which UniMixins needs to instantiate this
 * {@link ILateMixinLoader}.
 */
@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.statpatterns.late.json";
    }

    @Override
    public List<String> getMixins(final Set<String> loadedMods) {
        // Simple name relative to this config's "package" (com.zincglux.statpatterns.mixin).
        // UniMixins joins package + entry to form the fully-qualified mixin class.
        // Only the v2 probability-amplification mixin is active: GTNH 695 defaults to the
        // v2 crafting calculator (AEConfig.craftingCalculatorVersion == 2), so the v1
        // CraftingTreeProcess path is never used.
        return Arrays.asList("CraftableItemResolverMixin");
    }
}
