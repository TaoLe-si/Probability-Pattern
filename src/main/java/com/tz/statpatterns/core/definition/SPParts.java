package com.tz.statpatterns.core.definition;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartModels;
import appeng.core.definitions.ItemDefinition;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.tz.statpatterns.api.ids.ItemIds;
import com.tz.statpatterns.part.ProbabilityPatternTerminalPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Function;

import static com.tz.statpatterns.core.definition.SPItems.item;

public class SPParts {
    public static final ItemDefinition<PartItem<ProbabilityPatternTerminalPart>> ProbabilityPatternTerminalPart = createPart("Probability Pattern Terminal Part", ItemIds.PROBABILITY_PATTERN_TERMINAL, ProbabilityPatternTerminalPart.class, ProbabilityPatternTerminalPart::new);

    private static <T extends IPart> ItemDefinition<PartItem<T>> createPart(
            String englishName,
            ResourceLocation id,
            Class<T> partClass,
            Function<IPartItem<T>, T> factory) {
        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return item(englishName, id, props -> new PartItem<>(props, partClass, factory));
    }

    public static void init() {
    }
}
