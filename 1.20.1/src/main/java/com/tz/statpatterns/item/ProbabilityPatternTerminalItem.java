package com.tz.statpatterns.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.locator.MenuLocator;

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenuHost;

import de.mari_023.ae2wtlib.terminal.ItemWT;

/**
 * Handheld probability pattern terminal item.
 * Extends ItemWT for ae2wtlib Universal Terminal compatibility.
 */
public class ProbabilityPatternTerminalItem extends ItemWT {

    public ProbabilityPatternTerminalItem(Properties props) {
        super();
    }

    @Override
    public MenuType<?> getMenuType(ItemStack stack) {
        return SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL;
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int slot, ItemStack stack, net.minecraft.core.BlockPos pos) {
        return new ProbabilityPatternTerminalMenuHost(player, slot, stack,
                (p, sm) -> {});
    }
}
