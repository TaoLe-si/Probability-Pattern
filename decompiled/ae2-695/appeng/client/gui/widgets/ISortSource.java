/*
 * Decompiled with CFR 0.152.
 */
package appeng.client.gui.widgets;

import appeng.api.config.TypeFilter;

public interface ISortSource {
    public Enum getSortBy();

    public Enum getSortDir();

    public Enum getSortDisplay();

    default public Enum getTypeFilter() {
        return TypeFilter.ALL;
    }
}

