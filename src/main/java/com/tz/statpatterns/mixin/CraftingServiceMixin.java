/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * ... (LGPL header) ...
 */
package com.tz.statpatterns.mixin;

import java.util.Collection;
import java.util.concurrent.Future;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.me.service.CraftingService;

import com.tz.statpatterns.crafting.StatPatternsDetails;
import com.tz.statpatterns.integration.ae2vm.StatPatternsRequester;

/**
 * AE2 VM adapter: route probability-pattern crafting requests to AE2's native
 * CraftingTreeNode path so the mod's binomial sizing (CraftingTreeNodeMixin's
 * forRequest) is applied.
 *
 * <p>AE2 VM's CraftingServiceMixin always processes requesters whose class name
 * starts with "appeng." through its JIT bytecode VM and bypasses the
 * CraftingTreeNode tree entirely. The mod's probability-pattern logic lives in
 * the tree-node substitution and therefore silently does not run under AE2 VM,
 * producing plans that under-size the binomial confidence interval.</p>
 *
 * <p>Fix: when this mixin sees a probability-pattern request AND AE2 VM is
 * present, it wraps the incoming AE2 requester in a mod-owned delegate
 * ({@link StatPatternsRequester}, class name "com.tz.statpatterns.") and
 * re-invokes beginCraftingCalculation with that wrapper. Because the mod does
 * not register with AE2VMCraftingRegistry, AE2 VM classifies the wrapped
 * requester as an unregistered third-party requester and falls back to the
 * native calculation, which builds the CraftingTreeNode tree and lets
 * CraftingTreeNodeMixin apply the per-node
 * StatPatternsDetails.forRequest(amount) resize.</p>
 *
 * <p>For non-probability requests the mixin passes through untouched so AE2 VM
 * keeps its full acceleration. When AE2 VM is absent the mixin also passes
 * through, so the existing native + tree-node-mixin path is preserved.</p>
 *
 * <p>The mixin config sets "priority": 500 (lower than AE2 VM's default 1000)
 * so this @Inject(HEAD) runs before AE2 VM's. A thread-local guard prevents
 * re-entering this branch when the mod calls the original method.</p>
 *
 * <p>This class deliberately references no AE2 VM class
 * (com.ae2vm.addon.api.*); AE2 VM is detected purely through ModList, so the
 * mixin is safe to load (and to leave dormant) when AE2 VM is not installed.</p>
 */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {

    @Shadow(remap = false)
    public abstract Collection<IPatternDetails> getCraftingFor(AEKey what);

    @Unique
    private static final ThreadLocal<Boolean> STATPATTERNS_FALLBACK =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "beginCraftingCalculation", at = @At("HEAD"), cancellable = true)
    private void statpatternsBeginCraftingCalculation(
            Level level,
            ICraftingSimulationRequester simRequester,
            AEKey what,
            long amount,
            CalculationStrategy strategy,
            CallbackInfoReturnable<Future<ICraftingPlan>> cir) {

        if (STATPATTERNS_FALLBACK.get()) return;
        if (cir.isCancelled()) return;
        if (!ModList.get().isLoaded("ae2vm")) return;

        Collection<IPatternDetails> patterns;
        try { patterns = getCraftingFor(what); } catch (Throwable t) { return; }
        if (patterns == null || patterns.isEmpty()) return;

        boolean isProbability = false;
        for (IPatternDetails p : patterns) {
            if (p instanceof StatPatternsDetails) { isProbability = true; break; }
        }
        if (!isProbability) return;

        // Wrap the AE2 requester in a mod-owned delegate. AE2 VM classifies the
        // wrapper as an unregistered third-party and falls back to the native path.
        ICraftingSimulationRequester wrapped = new StatPatternsRequester(simRequester);
        STATPATTERNS_FALLBACK.set(Boolean.TRUE);
        try {
            cir.setReturnValue(((CraftingService) (Object) this).beginCraftingCalculation(
                    level, wrapped, what, amount, strategy));
            cir.cancel();
        } catch (Throwable t) {
            // leave uncancelled as last resort
        } finally {
            STATPATTERNS_FALLBACK.remove();
        }
    }
}
