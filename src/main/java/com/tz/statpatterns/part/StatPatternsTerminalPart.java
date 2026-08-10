
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
package com.tz.statpatterns.part;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.parts.PartModel;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.tz.statpatterns.StatPatternsMod;
import com.tz.statpatterns.core.definition.StatPatternsMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class StatPatternsTerminalPart extends PatternEncodingTerminalPart implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {
    public static final ResourceLocation MODEL_OFF = StatPatternsMod.id("part/probability_pattern_terminal_off");
    public static final ResourceLocation MODEL_ON = StatPatternsMod.id("part/probability_pattern_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final StatPatternsEncodingLogic logic = new StatPatternsEncodingLogic(this);

    public StatPatternsTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return StatPatternsMenus.PROBABILITY_PATTERN_TERMINAL.get();
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }
}
