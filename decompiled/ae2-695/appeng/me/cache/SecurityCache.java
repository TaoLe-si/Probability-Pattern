/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.entity.player.EntityPlayer
 */
package appeng.me.cache;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkSecurityChange;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.ISecurityProvider;
import appeng.core.worlddata.WorldData;
import appeng.me.GridNode;
import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;

public class SecurityCache
implements ISecurityGrid {
    private final IGrid myGrid;
    private final List<ISecurityProvider> securityProvider = new ArrayList<ISecurityProvider>();
    private final HashMap<Integer, EnumSet<SecurityPermissions>> playerPerms = new HashMap();
    private long securityKey = -1L;
    static final int STARTUP_DELAY = 20;
    private int startupTicks = 0;
    private static final Set<GameProfile> opPlayers = new HashSet<GameProfile>();

    public SecurityCache(IGrid g) {
        this.myGrid = g;
    }

    @MENetworkEventSubscribe
    public void updatePermissions(MENetworkSecurityChange ev) {
        this.playerPerms.clear();
        if (this.securityProvider.isEmpty()) {
            return;
        }
        this.securityProvider.get(0).readPermissions(this.playerPerms);
    }

    public long getSecurityKey() {
        return this.securityKey;
    }

    @Override
    public void onUpdateTick() {
        if (this.startupTicks < 20) {
            ++this.startupTicks;
        }
    }

    @Override
    public void removeNode(IGridNode gridNode, IGridHost machine) {
        if (machine instanceof ISecurityProvider) {
            this.securityProvider.remove(machine);
            this.updateSecurityKey();
        }
    }

    private void updateSecurityKey() {
        long lastCode = this.securityKey;
        this.securityKey = this.securityProvider.size() == 1 ? this.securityProvider.get(0).getSecurityKey() : -1L;
        if (lastCode != this.securityKey) {
            this.getGrid().postEvent(new MENetworkSecurityChange());
            for (IGridNode n : this.getGrid().getNodes()) {
                ((GridNode)n).setLastSecurityKey(this.securityKey);
            }
        }
    }

    @Override
    public void addNode(IGridNode gridNode, IGridHost machine) {
        if (machine instanceof ISecurityProvider) {
            this.securityProvider.add((ISecurityProvider)((Object)machine));
            this.updateSecurityKey();
        } else {
            ((GridNode)gridNode).setLastSecurityKey(this.securityKey);
        }
    }

    @Override
    public void onSplit(IGridStorage destinationStorage) {
    }

    @Override
    public void onJoin(IGridStorage sourceStorage) {
    }

    @Override
    public void populateGridStorage(IGridStorage destinationStorage) {
    }

    @Override
    public boolean isAvailable() {
        return this.startupTicks >= 20 && this.securityProvider.size() == 1 && this.securityProvider.get(0).isSecurityEnabled();
    }

    @Override
    public boolean hasPermission(EntityPlayer player, SecurityPermissions perm) {
        Preconditions.checkNotNull((Object)player);
        Preconditions.checkNotNull((Object)((Object)perm));
        GameProfile profile = player.getGameProfile();
        if (SecurityCache.isPlayerOP(profile)) {
            return true;
        }
        return this.hasPermission(WorldData.instance().playerData().getPlayerID(profile), perm);
    }

    @Override
    public boolean hasPermission(int playerID, SecurityPermissions perm) {
        if (this.isAvailable()) {
            EnumSet<SecurityPermissions> perms = this.playerPerms.get(playerID);
            if (perms == null) {
                if (playerID == -1) {
                    return false;
                }
                return this.hasPermission(-1, perm);
            }
            return perms.contains((Object)perm);
        }
        return true;
    }

    @Override
    public int getOwner() {
        if (this.isAvailable()) {
            return this.securityProvider.get(0).getOwner();
        }
        return -1;
    }

    public IGrid getGrid() {
        return this.myGrid;
    }

    public static void registerOpPlayer(GameProfile profile) {
        opPlayers.add(profile);
    }

    public static void unregisterOpPlayer(GameProfile profile) {
        opPlayers.remove(profile);
    }

    public static boolean isPlayerOP(GameProfile profile) {
        return opPlayers.contains(profile);
    }
}

