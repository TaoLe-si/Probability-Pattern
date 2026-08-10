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

package com.tz.statpatterns.terminal;

import java.util.function.BiConsumer;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.part.StatPatternsEncodingLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;

import de.mari_023.ae2wtlib.terminal.WTMenuHost;

/**
 * Menu host for the handheld probability pattern terminal.
 * Extends WTMenuHost for ae2wtlib Quantum Bridge (singularity) support.
 * Implements {@link IViewCellStorage} so the terminal exposes the View Cell
 * (显示元件) slots like ae2wtlib's wireless pattern encoding terminal.
 */
public class StatPatternsTerminalMenuHost extends WTMenuHost
        implements IPatternTerminalMenuHost, IPatternTerminalLogicHost, IViewCellStorage {
    private final StatPatternsEncodingLogic logic;
    private boolean isLoading = false;

    public StatPatternsTerminalMenuHost(Player player, Integer slot, ItemStack stack,
            BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, stack, returnToMainMenu);
        this.logic = new StatPatternsEncodingLogic(this);
        loadFromItem();
    }

    public double getProbability() {
        return logic.getProbability();
    }

    public boolean isAlpha95() {
        return logic.isAlpha95();
    }

    public void setProbability(double probability) {
        logic.setProbability(probability);
    }

    public void setAlpha95(boolean value) {
        logic.setAlpha95(value);
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public Level getLevel() {
        return getPlayer().level();
    }

    @Override
    public void markForSave() {
        if (!isLoading) {
            saveToItem();
        }
    }

    /**
     * Load the pattern encoding logic state from the item stack's NBT.
     */
    private void loadFromItem() {
        CompoundTag tag = Components.readPatternLogicState(getItemStack());
        if (tag != null) {
            isLoading = true;
            try {
                logic.readFromNBT(tag);
            } finally {
                isLoading = false;
            }
        }
    }

    /**
     * Save the pattern encoding logic state to the item stack's NBT.
     */
    private void saveToItem() {
        ItemStack stack = getItemStack();
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag);
        Components.writePatternLogicState(stack, tag);
    }

    /** Returns the singularity inventory for Quantum Bridge Card. */
    public InternalInventory getSingularityInventory() {
        return getSubInventory(WTMenuHost.INV_SINGULARITY);
    }

    /**
     * {@link IViewCellStorage} — the 5-slot View Cell inventory is inherited
     * from {@link WTMenuHost} (its public {@code getViewCellStorage()} method
     * satisfies this interface contract).
     */
}
