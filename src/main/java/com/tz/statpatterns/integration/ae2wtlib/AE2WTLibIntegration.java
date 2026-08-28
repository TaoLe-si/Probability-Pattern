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

import net.minecraftforge.fml.ModList;

/**
 * Facade for the optional AE2 Wireless Terminal Library (ae2wtlib) integration.
 *
 * IMPORTANT: this class must not reference any ae2wtlib type - neither in
 * method bodies nor in field/parameter signatures. The JVM links and verifies
 * a class as soon as one of its methods is invoked; verification of code that
 * mentions ae2wtlib types resolves those classes and crashes with
 * {@code NoClassDefFoundError} when ae2wtlib is not installed - before any
 * runtime {@code ModList} guard could run. All ae2wtlib-dependent code lives
 * in {@link AE2WTLibCompat}, which is only ever linked after the ModList check
 * below has passed (its constant-pool entry resolves lazily on first execution).
 */
public final class AE2WTLibIntegration {
    private static final String AE2WTLIB_MOD_ID = "ae2wtlib";

    private AE2WTLibIntegration() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(AE2WTLIB_MOD_ID);
    }

    /**
     * Register ae2wtlib upgrade cards FOR the PP wireless terminal.
     * No-op when ae2wtlib is absent.
     */
    public static void registerUpgrades() {
        if (!isLoaded()) return;
        AE2WTLibCompat.registerUpgrades();
    }

    /**
     * Register the wireless stat pattern terminal with ae2wtlib's WUT (Wireless
     * Universal Terminal) system so it can be merged into a WUT together with
     * other wireless terminals and opened through it. No-op when ae2wtlib is
     * absent. See {@link AE2WTLibCompat#registerWithWUT()} for details.
     */
    public static void registerWithWUT() {
        if (!isLoaded()) return;
        AE2WTLibCompat.registerWithWUT();
    }
}
