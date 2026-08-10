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

package com.tz.statpatterns.crafting;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.AELog;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.crafting.inv.ChildCraftingSimulationState;
import appeng.crafting.inv.CraftingSimulationState;
import appeng.crafting.inv.NetworkCraftingSimulationState;
import appeng.hooks.ticking.TickHandler;
import com.google.common.base.Stopwatch;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Custom crafting calculation that tracks overall success probability.
 * Injected via CraftingServiceMixin @Overwrite.
 */
public class StatPatternsCraftingCalculation extends CraftingCalculation {
    private final NetworkCraftingSimulationState networkInv;
    private final Level level;
    private final KeyCounter missing = new KeyCounter();
    private final Object monitor = new Object();
    private final Stopwatch watch = Stopwatch.createUnstarted();
    private final StatPatternsCraftingTreeNode tree;
    private final AEKey output;
    private final long requestedAmount;
    private final CalculationStrategy strategy;
    private boolean simulate = false;
    final ICraftingSimulationRequester simRequester;
    private boolean running = false;
    private boolean done = false;
    private int time = 5;
    private int incTime = Integer.MAX_VALUE;
    private final List<CraftAttempt> attempts = AELog.isCraftingLogEnabled() ? new ArrayList<>() : null;
    private double overallSuccessProbability = 1.0;

    public StatPatternsCraftingCalculation(Level level, IGrid grid, ICraftingSimulationRequester simRequester,
            GenericStack output, CalculationStrategy strategy) {
        super(level, grid, simRequester, output, strategy);
        this.level = level;
        this.output = output.what();
        this.requestedAmount = output.amount();
        this.strategy = strategy;
        this.simRequester = simRequester;

        var storage = grid.getStorageService();
        var craftingService = grid.getCraftingService();
        this.networkInv = new NetworkCraftingSimulationState(storage, simRequester.getActionSource());
        this.tree = new StatPatternsCraftingTreeNode(craftingService, this, this.output, 1, null, -1);
    }

    void addMissing(AEKey what, long amount) {
        missing.add(what, amount);
    }

    public ICraftingPlan run() {
        try {
            TickHandler.instance().registerCraftingSimulation(this.level, this);
            this.handlePausing();
            var plan = computePlan();
            this.logCraftingJob(plan);
            return plan;
        } catch (Exception ex) {
            AELog.info(ex, "Exception during crafting calculation.");
            throw new RuntimeException(ex);
        } finally {
            this.finish();
        }
    }

    private ICraftingPlan computePlan() throws InterruptedException {
        var fullAmountPlan = runCraftAttempt(false, requestedAmount);
        if (fullAmountPlan != null) return fullAmountPlan;

        if (strategy == CalculationStrategy.CRAFT_LESS) {
            long successfulAmount = 0;
            ICraftingPlan successfulPlan = null;
            for (long increment = Long.highestOneBit(requestedAmount); increment > 0; increment /= 2) {
                long testAmount = successfulAmount + increment;
                if (testAmount < requestedAmount) {
                    var plan = runCraftAttempt(false, testAmount);
                    if (plan != null) {
                        successfulAmount = testAmount;
                        successfulPlan = plan;
                    }
                }
            }
            if (successfulPlan != null) return successfulPlan;
        }
        return runCraftAttempt(true, requestedAmount);
    }

    @Nullable
    @Contract("true, _ -> !null")
    private CraftingPlan runCraftAttempt(boolean simulate, long amount) throws InterruptedException {
        this.simulate = simulate;
        final Stopwatch timer = Stopwatch.createStarted();
        ChildCraftingSimulationState craftingInventory = new ChildCraftingSimulationState(networkInv);
        craftingInventory.ignore(this.output);

        try {
            this.tree.request(craftingInventory, amount, null);
        } catch (CraftBranchFailure failure) {
            if (AELog.isCraftingLogEnabled()) {
                this.attempts.add(new CraftAttempt(amount + " failed", timer));
            }
            return null;
        }
        craftingInventory.addBytes(this.tree.getNodeCount() * 8);

        var plan = CraftingSimulationState.buildCraftingPlan(craftingInventory, this, amount);
        this.overallSuccessProbability = this.tree.getSuccessProbability();
        if (AELog.isCraftingLogEnabled()) {
            String type = simulate ? "simulated" : "succeeded";
            this.attempts.add(new CraftAttempt("%d %s (%d bytes)".formatted(amount, type, plan.bytes()), timer));
        }
        return plan;
    }

    void handlePausing() throws InterruptedException {
        if (this.incTime > 100) {
            this.incTime = 0;
            synchronized (this.monitor) {
                if (this.watch.elapsed(TimeUnit.MICROSECONDS) > this.time) {
                    this.running = false;
                    this.watch.stop();
                    this.monitor.notify();
                }
                if (!this.running) {
                    while (!this.running) { this.monitor.wait(); }
                }
            }
            if (Thread.interrupted()) { throw new InterruptedException(); }
        }
        this.incTime++;
    }

    private void finish() {
        synchronized (this.monitor) {
            this.running = false;
            this.done = true;
            this.monitor.notify();
        }
    }

    public boolean isSimulation() { return this.simulate; }
    public AEKey getOutput() { return output; }

    public KeyCounter getMissingItems() { return missing; }
    Level getLevel() { return this.level; }

    public boolean simulateFor(int micros) {
        this.time = micros;
        synchronized (this.monitor) {
            if (this.done) return false;
            this.watch.reset(); this.watch.start(); this.running = true;
            this.monitor.notify();
            while (this.running) {
                try { this.monitor.wait(); } catch (InterruptedException ignored) {}
            }
        }
        return true;
    }

    private void logCraftingJob(ICraftingPlan plan) {
        if (AELog.isCraftingLogEnabled()) {
            var actionSource = this.simRequester.getActionSource();
            String actionSourceName = "[unknown source]";
            if (actionSource != null && actionSource.player().isPresent()) {
                actionSourceName = actionSource.player().get().toString();
            } else if (actionSource != null && actionSource.machine().isPresent()) {
                var machineSource = actionSource.machine().get();
                var actionableNode = machineSource.getActionableNode();
                actionSourceName = actionableNode != null ? actionableNode.toString() : machineSource.toString();
            }
            StringBuilder message = new StringBuilder();
            message.append("CraftingCalculation issued by %s requesting [%dx%s] breakdown:\n"
                    .formatted(actionSourceName, this.requestedAmount, this.output));
            for (var attempt : this.attempts) {
                message.append(" - %s in %d ms\n"
                        .formatted(attempt.description, attempt.stopwatch.elapsed(TimeUnit.MILLISECONDS)));
            }
            message.append(" - final plan: %d (%d bytes)".formatted(plan.finalOutput().amount(), plan.bytes()));
            message.append("\n - overall success probability: %.4f".formatted(this.overallSuccessProbability));
            AELog.crafting(message.toString());
        }
    }

    public boolean hasMultiplePaths() { return this.tree.hasMultiplePaths(); }

    private record CraftAttempt(String description, Stopwatch stopwatch) {}
}
