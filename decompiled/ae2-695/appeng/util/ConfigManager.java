/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package appeng.util;

import appeng.api.config.LevelEmitterMode;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.util.IConfigManager;
import appeng.core.AELog;
import appeng.util.IConfigManagerHost;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;

public final class ConfigManager
implements IConfigManager {
    private final Map<Settings, Enum<?>> settings = new EnumMap(Settings.class);
    private final IConfigManagerHost target;

    public ConfigManager(IConfigManagerHost tile) {
        this.target = tile;
    }

    @Override
    public Set<Settings> getSettings() {
        return this.settings.keySet();
    }

    public void registerSetting(Settings settingName, Enum defaultValue) {
        this.settings.put(settingName, defaultValue);
    }

    @Override
    public Enum<?> getSetting(Settings settingName) {
        Enum<?> oldValue = this.settings.get((Object)settingName);
        if (oldValue != null) {
            return oldValue;
        }
        throw new IllegalStateException("Invalid Config setting. Expected a non-null value for " + (Object)((Object)settingName));
    }

    public Enum<?> putSetting(Settings settingName, Enum newValue) {
        Enum<?> oldValue = this.getSetting(settingName);
        this.settings.put(settingName, newValue);
        this.target.updateSetting(this, settingName, newValue);
        return oldValue;
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        for (Map.Entry<Settings, Enum<?>> entry : this.settings.entrySet()) {
            tagCompound.setString(entry.getKey().name(), this.settings.get((Object)entry.getKey()).toString());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        for (Map.Entry<Settings, Enum<?>> entry : this.settings.entrySet()) {
            try {
                if (!tagCompound.hasKey(entry.getKey().name())) continue;
                String value = tagCompound.getString(entry.getKey().name());
                if (value.equals("EXTACTABLE_ONLY")) {
                    value = StorageFilter.EXTRACTABLE_ONLY.toString();
                } else if (value.equals("STOREABLE_AMOUNT")) {
                    value = LevelEmitterMode.STORABLE_AMOUNT.toString();
                }
                Enum<?> oldValue = this.settings.get((Object)entry.getKey());
                Object newValue = Enum.valueOf(oldValue.getClass(), value);
                this.putSetting(entry.getKey(), (Enum)newValue);
            }
            catch (IllegalArgumentException e) {
                AELog.debug(e);
            }
        }
    }
}

