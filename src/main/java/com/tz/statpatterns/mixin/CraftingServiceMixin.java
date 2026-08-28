/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * ... LGPL header ...
 */
package com.tz.statpatterns.mixin;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.me.service.CraftingService;

import com.tz.statpatterns.crafting.StatPatternsCraftingCalculation;
import com.tz.statpatterns.crafting.StatisticalPatternDetails;

/**
 * Hooks AE2's CraftingService.beginCraftingCalculation to run the mod's own
 * StatPatternsCraftingCalculation (which encodes the probability / binomial
 * sizing logic) for requests that resolve to a StatisticalPatternDetails, and
 * to pass non-probability requests through to AE2 VM (if present) or to the
 * mod's calculation (if AE2 VM is absent, preserving the original overwrite
 * behavior of this method).
 *
 * <p>Originally this class used {@code @Overwrite} to replace AE2's calculation
 * with StatPatternsCraftingCalculation for every request. AE2 VM injects its own
 * cancellable @Inject at HEAD of the same method and, for an AE2-owned
 * (appeng.*) requester, calls setReturnValue(vmFuture) + cancel(), which
 * short-circuited the overwrite and bypassed the probability logic for every
 * request whenever AE2 VM was installed. This inject reproduces the overwrite
 * for probability requests (always run StatPatternsCraftingCalculation) while
 * letting non-probability requests fall through to AE2 VM (when present) for
 * acceleration, or to the mod's calculation (when AE2 VM is absent) to keep
 * the original "always replace" behavior.</p>
 *
 * <p>Mixxin config priority is 1000, well below AE2 VM's 2000, so this HEAD
 * inject runs before AE2 VM's. A {@link #STATPATTERNS_FALLBACK} thread-local
 * guard prevents re-entering this branch when the calculation re-submits to
 * CRAFTING_POOL.</p>
 *
 * <p>This class references no AE2 VM class; AE2 VM presence is detected purely
 * through {@link ModList}, so the mixin is safe to load (and to leave dormant)
 * when AE2 VM is not installed.</p>
 */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {
    @Shadow(remap = false)
    private IGrid grid;
    @Shadow(remap = false)
    private static ExecutorService CRAFTING_POOL;

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

        Collection<IPatternDetails> patterns;
        try { patterns = getCraftingFor(what); } catch (Throwable t) { patterns = null; }

        boolean isProbability = false;
        if (patterns != null) {
            for (IPatternDetails p : patterns) {
                if (p instanceof StatisticalPatternDetails) { isProbability = true; break; }
            }
        }

        if (isProbability) {
            // Always run the mod's probability-aware calculation, whether AE2 VM is
            // present or not. Cancelling here prevents AE2 VM's later HEAD inject
            // (higher priority 2000) from running and bypassing the calculation.
            if (level == null || simRequester == null) {
                throw new IllegalArgumentException("Invalid Crafting Job Request");
            }
            final StatPatternsCraftingCalculation job =
                    new StatPatternsCraftingCalculation(level, this.grid, simRequester,
                            new GenericStack(what, amount), strategy);
            cir.setReturnValue(CRAFTING_POOL.submit(job::run));
            cir.cancel();
            return;
        }

        // Non-probability request: let AE2 VM accelerate if present, otherwise keep
        // the original @Overwrite behavior (always run the mod's calculation).
        if (ModList.get().isLoaded("ae2vm")) {
            return; // pass through to AE2 VM's @Inject at HEAD
        }

        if (level == null || simRequester == null) {
            throw new IllegalArgumentException("Invalid Crafting Job Request");
        }
        final StatPatternsCraftingCalculation job =
                new StatPatternsCraftingCalculation(level, this.grid, simRequester,
                        new GenericStack(what, amount), strategy);
        cir.setReturnValue(CRAFTING_POOL.submit(job::run));
        cir.cancel();
    }
}
