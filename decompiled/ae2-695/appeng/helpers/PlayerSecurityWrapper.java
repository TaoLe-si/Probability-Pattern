/*
 * Decompiled with CFR 0.152.
 */
package appeng.helpers;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.ISecurityRegistry;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class PlayerSecurityWrapper
implements ISecurityRegistry {
    private final Map<Integer, EnumSet<SecurityPermissions>> target;

    public PlayerSecurityWrapper(HashMap<Integer, EnumSet<SecurityPermissions>> playerPerms) {
        this.target = playerPerms;
    }

    @Override
    public void addPlayer(int playerID, EnumSet<SecurityPermissions> permissions) {
        this.target.put(playerID, permissions);
    }
}

