/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.ICrafting
 */
package appeng.container.guisync;

import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketProgressBar;
import appeng.core.sync.packets.PacketValueConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;

public class SyncData {
    private final AEBaseContainer source;
    private final Field[] indirections;
    private final Field field;
    private final String fieldName;
    private final int channel;
    private Object clientVersion = null;

    public SyncData(AEBaseContainer container, Field field, GuiSync annotation) {
        this(container, new Field[0], field, annotation.value());
    }

    public SyncData(AEBaseContainer container, Field[] indirections, Field field, int channel) {
        this.source = container;
        this.indirections = indirections;
        this.field = field;
        this.channel = channel;
        StringBuilder nameBuilder = new StringBuilder();
        for (Field indirection : this.indirections) {
            nameBuilder.append(indirection.getName());
            nameBuilder.append('.');
        }
        nameBuilder.append(this.field.getName());
        this.fieldName = nameBuilder.toString();
    }

    public int getChannel() {
        return this.channel;
    }

    private Object getValue() throws IllegalAccessException {
        Object currentObject = this.source;
        for (Field indirection : this.indirections) {
            currentObject = indirection.get(currentObject);
        }
        return this.field.get(currentObject);
    }

    private void setValue(Object newVal) throws IllegalAccessException {
        Object currentObject = this.source;
        for (Field indirection : this.indirections) {
            currentObject = indirection.get(currentObject);
        }
        this.field.set(currentObject, newVal);
    }

    public void tick(ICrafting c) {
        try {
            Object val = this.getValue();
            if (val != null && this.clientVersion == null) {
                this.send(c, val);
            } else if (!val.equals(this.clientVersion)) {
                this.send(c, val);
            }
        }
        catch (IOException | IllegalAccessException | IllegalArgumentException e) {
            AELog.debug(e);
        }
    }

    private void send(ICrafting o, Object val) throws IOException {
        if (val instanceof String) {
            if (o instanceof EntityPlayerMP) {
                NetworkHandler.instance.sendTo(new PacketValueConfig("SyncDat." + this.channel, (String)val), (EntityPlayerMP)o);
            }
        } else if (this.field.getType().isEnum()) {
            o.sendProgressBarUpdate((Container)this.source, this.channel, ((Enum)val).ordinal());
        } else if (val instanceof Long || val.getClass() == Long.TYPE) {
            NetworkHandler.instance.sendTo(new PacketProgressBar(this.channel, (Long)val), (EntityPlayerMP)o);
        } else if (val instanceof Boolean || val.getClass() == Boolean.TYPE) {
            o.sendProgressBarUpdate((Container)this.source, this.channel, (Boolean)val != false ? 1 : 0);
        } else {
            o.sendProgressBarUpdate((Container)this.source, this.channel, ((Integer)val).intValue());
        }
        this.clientVersion = val;
    }

    public void update(Object val) {
        try {
            Object oldValue = this.getValue();
            if (val instanceof String) {
                this.updateString(oldValue, (String)val);
            } else {
                this.updateValue(oldValue, (Long)val);
            }
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            AELog.debug(e);
        }
    }

    private void updateString(Object oldValue, String val) {
        try {
            this.setValue(val);
            this.source.onUpdate(this.fieldName, oldValue, this.getValue());
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            AELog.debug(e);
        }
    }

    private void updateValue(Object oldValue, long val) {
        try {
            if (this.field.getType().isEnum()) {
                EnumSet<Enum> valList = EnumSet.allOf(this.field.getType());
                for (Enum e : valList) {
                    if ((long)e.ordinal() != val) continue;
                    this.setValue(e);
                    break;
                }
            } else if (this.field.getType().equals(Integer.TYPE)) {
                this.setValue((int)val);
            } else if (this.field.getType().equals(Long.TYPE)) {
                this.setValue(val);
            } else if (this.field.getType().equals(Boolean.TYPE)) {
                this.setValue(val == 1L);
            } else if (this.field.getType().equals(Integer.class)) {
                this.setValue((int)val);
            } else if (this.field.getType().equals(Long.class)) {
                this.setValue(val);
            } else if (this.field.getType().equals(Boolean.class)) {
                this.setValue(val == 1L);
            }
            this.source.onUpdate(this.fieldName, oldValue, this.getValue());
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            AELog.debug(e);
        }
    }
}

