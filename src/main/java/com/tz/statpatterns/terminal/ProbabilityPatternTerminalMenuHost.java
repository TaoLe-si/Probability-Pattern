package com.tz.statpatterns.terminal;

import java.util.function.BiConsumer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.encoding.PatternEncodingLogic;

import com.tz.statpatterns.item.ProbabilityPatternTerminalItem;

public class ProbabilityPatternTerminalMenuHost extends WirelessTerminalMenuHost<ProbabilityPatternTerminalItem>
        implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {

    private final PatternEncodingLogic logic;

    public ProbabilityPatternTerminalMenuHost(ProbabilityPatternTerminalItem item, Player player,
            ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
        this.logic = new PatternEncodingLogic(this);
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
        // ItemStack state managed by ItemMenuHost
    }
}