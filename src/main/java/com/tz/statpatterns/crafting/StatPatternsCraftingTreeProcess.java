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

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.inv.CraftingSimulationState;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatPatternsCraftingTreeProcess {
    private final StatPatternsCraftingTreeNode parent;
    final IPatternDetails details;
    private final StatPatternsCraftingCalculation job;
    private final Map<StatPatternsCraftingTreeNode, Long> nodes = new LinkedHashMap<>();
    boolean possible = true;
    private boolean containerItems;
    private boolean limitQty;

    public StatPatternsCraftingTreeProcess(ICraftingService cc, StatPatternsCraftingCalculation job,
            IPatternDetails details, StatPatternsCraftingTreeNode craftingTreeNode) {
        this.parent = craftingTreeNode;
        this.details = details;
        this.job = job;
        updateLimitQty();

        final IPatternDetails.IInput[] inputs = this.details.getInputs();
        for (int x = 0; x < inputs.length; ++x) {
            var input = inputs[x];
            var firstInput = input.getPossibleInputs()[0];
            this.nodes.put(new StatPatternsCraftingTreeNode(cc, job, firstInput.what(), firstInput.amount(), this, x),
                    input.getMultiplier());
        }
    }

    boolean notRecursive(IPatternDetails details) {
        return this.parent == null || this.parent.notRecursive(details);
    }

    private void updateLimitQty() {
        for (IPatternDetails.IInput input : details.getInputs()) {
            var primaryInput = input.getPossibleInputs()[0];
            boolean isAnInput = false;
            for (var output : details.getOutputs()) {
                if (output.what().matches(primaryInput)) { isAnInput = true; break; }
            }
            if (isAnInput) this.limitQty = true;
            if (input.getRemainingKey(primaryInput.what()) != null) {
                this.limitQty = this.containerItems = true;
            }
        }
    }

    boolean limitsQuantity() { return this.limitQty; }

    void request(CraftingSimulationState inv, long times) throws CraftBranchFailure, InterruptedException {
        this.job.handlePausing();
        var containerItems = this.containerItems ? new KeyCounter() : null;

        for (var entry : this.nodes.entrySet()) {
            entry.getKey().request(inv, entry.getValue() * times, containerItems);
        }

        if (containerItems != null) {
            for (var stack : containerItems) {
                inv.insert(stack.getKey(), stack.getLongValue(), Actionable.MODULATE);
                inv.addStackBytes(stack.getKey(), stack.getLongValue(), 1);
            }
        }

        for (var out : this.details.getOutputs()) {
            inv.insert(out.what(), out.amount() * times, Actionable.MODULATE);
        }
        inv.addCrafting(details, times);
        inv.addBytes(times);
    }

    long getNodeCount() {
        long tot = 0;
        for (StatPatternsCraftingTreeNode node : this.nodes.keySet()) { tot += node.getNodeCount(); }
        return tot;
    }

    long getOutputCount(AEKey what) {
        long tot = 0;
        for (var is : this.details.getOutputs()) {
            if (what.matches(is)) tot += is.amount();
        }
        return tot;
    }

    boolean hasMultiplePaths() {
        for (var entry : nodes.entrySet()) {
            if (entry.getKey().hasMultiplePaths()) return true;
        }
        return false;
    }

    double getSuccessProbability() {
        double ownProb = 1.0;
        if (this.details instanceof StatisticalPatternDetails spd) {
            var sizingResult = spd.sizing();
            ownProb = 1.0 - sizingResult.underproductionRisk();
        }
        double childProb = 1.0;
        for (var entry : this.nodes.entrySet()) {
            childProb *= entry.getKey().getSuccessProbability();
        }
        return ownProb * childProb;
    }
}
