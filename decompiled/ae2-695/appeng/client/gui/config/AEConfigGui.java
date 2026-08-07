/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.client.config.GuiConfig
 *  cpw.mods.fml.client.config.IConfigElement
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraftforge.common.config.ConfigCategory
 *  net.minecraftforge.common.config.ConfigElement
 */
package appeng.client.gui.config;

import appeng.core.AEConfig;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

public class AEConfigGui
extends GuiConfig {
    public AEConfigGui(GuiScreen parent) {
        super(parent, AEConfigGui.getConfigElements(), "appliedenergistics2", false, false, GuiConfig.getAbridgedConfigPath((String)AEConfig.instance.getFilePath()));
    }

    private static List<IConfigElement> getConfigElements() {
        ArrayList<IConfigElement> list = new ArrayList<IConfigElement>();
        for (String cat : AEConfig.instance.getCategoryNames()) {
            ConfigCategory cc;
            if (cat.equals("versionchecker") || cat.equals("settings") || (cc = AEConfig.instance.getCategory(cat)).isChild()) continue;
            ConfigElement ce = new ConfigElement(cc);
            list.add((IConfigElement)ce);
        }
        return list;
    }
}

