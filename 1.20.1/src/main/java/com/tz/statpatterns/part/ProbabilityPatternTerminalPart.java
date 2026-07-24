package com.tz.statpatterns.part;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.parts.encoding.PatternEncodingLogic;
import com.tz.statpatterns.core.definition.SPMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import appeng.parts.encoding.PatternEncodingTerminalPart;

import com.tz.statpatterns.ProbabilityPatternMod;

public class ProbabilityPatternTerminalPart extends PatternEncodingTerminalPart implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {
    public static final ResourceLocation MODEL_OFF = ProbabilityPatternMod.id("part/probability_pattern_terminal_off");
    public static final ResourceLocation MODEL_ON = ProbabilityPatternMod.id("part/probability_pattern_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final ProbabilityPatternEncodingLogic logic = new ProbabilityPatternEncodingLogic(this);

    public ProbabilityPatternTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return SPMenus.PROBABILITY_PATTERN_TERMINAL;
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public double getProbability() {
        return logic.getProbability();
    }

    public boolean isAlpha95() {
        return logic.isAlpha95();
    }

    public void setProbability(double probability) {
        logic.setProbability(probability);
        markForSave();
    }

    public void setAlpha95(boolean value) {
        logic.setAlpha95(value);
        markForSave();
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public void markForSave() {
        super.markForSave();
    }
}
