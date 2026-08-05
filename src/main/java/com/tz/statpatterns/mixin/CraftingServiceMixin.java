package com.tz.statpatterns.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingCalculation;
import appeng.me.service.CraftingService;
import com.tz.statpatterns.crafting.PCraftingCalculation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {
    @Shadow(remap = false)
    private IGrid grid;
    @Shadow(remap = false)
    private static ExecutorService CRAFTING_POOL;

    /**
     * @author ProbabilityPattern
     * @reason Replace CraftingCalculation with PCraftingCalculation that tracks overall success probability
     */
    @Overwrite(remap = false)
    public Future<ICraftingPlan> beginCraftingCalculation(Level level, ICraftingSimulationRequester simRequester,
            AEKey what, long amount, CalculationStrategy strategy) {
        if (level == null || simRequester == null) {
            throw new IllegalArgumentException("Invalid Crafting Job Request");
        }
        final PCraftingCalculation job = new PCraftingCalculation(level, this.grid, simRequester,
                new GenericStack(what, amount), strategy);
        return CRAFTING_POOL.submit(job::run);
    }
}
