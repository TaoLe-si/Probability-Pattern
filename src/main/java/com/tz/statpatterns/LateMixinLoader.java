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
package com.tz.statpatterns;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

/**
 * Registers the Probability Pattern late-phase mixin.
 * <p>
 * AE2 is a normal mod that loads after Mixin's early phase, so its classes can only
 * be mixed into during the GTNHMixins "late" phase. This loader queues
 * {@link com.tz.statpatterns.mixin.CraftingTreeProcessMixin} right before the first
 * mod construction event.
 * <p>
 * <b>Important:</b> this class intentionally lives OUTSIDE the {@code com.tz.statpatterns.mixin}
 * package. A mixin config declares that package as a "mixin package", so classes in it cannot
 * be loaded directly via {@code Class.forName} - which UniMixins needs to instantiate this
 * {@link ILateMixinLoader}.
 */
@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.probabilitypattern.late.json";
    }

    @Override
    public List<String> getMixins(final Set<String> loadedMods) {
        // Simple name relative to this config's "package" (com.tz.statpatterns.mixin).
        // UniMixins joins package + entry to form the fully-qualified mixin class.
        return Collections.singletonList("CraftingTreeProcessMixin");
    }
}
