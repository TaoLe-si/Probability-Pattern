/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * ... (LGPL header) ...
 */
package com.tz.statpatterns.integration.ae2vm;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;

/**
 * Mod-owned ICraftingSimulationRequester wrapper used by the AE2 VM adapter
 * (see com.tz.statpatterns.mixin.CraftingServiceMixin) to force a
 * probability-pattern crafting request to be handled by AE2's native path
 * instead of AE2 VM's JIT bytecode VM.
 *
 * <p>AE2 VM's CraftingServiceMixin always routes requesters whose class name
 * starts with "appeng." through its VM (it treats them as AE2's own). The
 * mod's probability-pattern logic lives in the CraftingTreeNode substitution
 * (see CraftingTreeNodeMixin), which the VM bypasses. By wrapping the
 * incoming AE2 requester in this mod-owned delegate, the requester class
 * name becomes "com.tz.statpatterns.", which AE2 VM treats as a third-party
 * requester. Because the mod intentionally does not register with
 * AE2VMCraftingRegistry, AE2 VM considers it an unregistered third-party and
 * falls back to the native beginCraftingCalculation call, which builds the
 * CraftingTreeNode tree and applies the mod's per-node binomial sizing
 * correctly.</p>
 *
 * <p>Pure delegate: forwards getActionSource() to the original requester, so
 * the native calculation sees the same action source, grid node, and security
 * context as the original call.</p>
 *
 * <p>This class deliberately does not reference any AE2 VM class
 * (com.ae2vm.addon.api.*), so it is safe to load when AE2 VM is absent. The
 * mixin that uses it gates on ModList.get().isLoaded("ae2vm") to avoid
 * invoking this code path when the wrapper would serve no purpose.</p>
 */
public final class StatPatternsRequester implements ICraftingSimulationRequester {
    private final ICraftingSimulationRequester delegate;

    public StatPatternsRequester(ICraftingSimulationRequester delegate) {
        this.delegate = delegate;
    }

    @Override
    @Nullable
    public IActionSource getActionSource() {
        return delegate != null ? delegate.getActionSource() : null;
    }
}
