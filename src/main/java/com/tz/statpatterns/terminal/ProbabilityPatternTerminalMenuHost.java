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

import appeng.parts.encoding.EncodingMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.encoding.PatternEncodingLogic;

import com.tz.statpatterns.api.ids.Components;

import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;

/**
 * Menu host for the handheld probability pattern terminal.
 * Extends WTMenuHost for ae2wtlib Quantum Bridge support.
 */
public class ProbabilityPatternTerminalMenuHost extends WTMenuHost implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {
    private final PatternEncodingLogic logic;
    private boolean isLoading = false;
    private double probability = 0.8;
    private boolean alpha95 = true;

    public ProbabilityPatternTerminalMenuHost(ItemWT item, Player player, ItemMenuHostLocator locator, BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
        this.logic = new PatternEncodingLogic(this);
        loadFromItem();
    }

    public double getProbability() {
        return probability;
    }

    public boolean isAlpha95() {
        return alpha95;
    }

    public void setProbability(double probability) {
        this.probability = Math.clamp(probability, 0.01, 0.9999);
        markForSave();
    }
    public void setAlpha95(boolean value) {
        this.alpha95 = value;
        markForSave();
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
     * Load the pattern encoding logic state from the item stack's data component.
     */
    private void loadFromItem() {
        ItemStack stack = getItemStack();
        CompoundTag tag = stack.get(Components.PATTERN_LOGIC_STATE);
        if (tag != null) {
            isLoading = true;
            try {
                logic.readFromNBT(tag, getPlayer().level().registryAccess());
                this.setProbability(tag.getDouble("probability"));
                this.setAlpha95(tag.getBoolean("alpha95"));
            } finally {
                isLoading = false;
            }
        }
    }

    /**
     * Save the pattern encoding logic state to the item stack's data component.
     */
    private void saveToItem() {
        ItemStack stack = getItemStack();
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag, getPlayer().level().registryAccess());
        tag.putDouble("probability",  this.getProbability());
        tag.putBoolean("alpha95",  this.isAlpha95());
        stack.set(Components.PATTERN_LOGIC_STATE, tag);
    }

    public InternalInventory getSingularityInventory() {
        return getSubInventory(WTMenuHost.INV_SINGULARITY);
    }
}