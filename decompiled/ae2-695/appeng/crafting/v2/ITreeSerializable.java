/*
 * Decompiled with CFR 0.152.
 */
package appeng.crafting.v2;

import appeng.crafting.v2.CraftingTreeSerializer;
import java.io.IOException;
import java.util.List;

public interface ITreeSerializable {
    public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer var1) throws IOException;

    public void loadChildren(List<ITreeSerializable> var1) throws IOException;

    default public ITreeSerializable getSerializationParent() {
        return this;
    }
}

