/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.util.ForgeDirection
 *  org.apache.logging.log4j.Level
 */
package appeng.me.cache;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkChannelChanged;
import appeng.api.networking.events.MENetworkControllerChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.util.DimensionalCoord;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.features.AEFeature;
import appeng.core.stats.Achievements;
import appeng.me.pathfinding.AdHocChannelUpdater;
import appeng.me.pathfinding.ChannelFinalizer;
import appeng.me.pathfinding.ControllerValidator;
import appeng.me.pathfinding.PathingCalculation;
import appeng.tile.networking.TileController;
import appeng.util.Platform;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.logging.log4j.Level;

public class PathGridCache
implements IPathingGrid {
    private final Set<TileController> controllers = new HashSet<TileController>();
    private final Set<IGridNode> nodesNeedingChannels = new HashSet<IGridNode>();
    private final Set<IGridNode> cannotCarryCompressedNodes = new HashSet<IGridNode>();
    private final IGrid myGrid;
    private int channelsInUse = 0;
    private int channelsByBlocks = 0;
    private double channelPowerUsage = 0.0;
    private boolean recalculateControllerNextTick = true;
    private boolean updateNetwork = true;
    private boolean booting = false;
    private ControllerState controllerState = ControllerState.NO_CONTROLLER;
    private int lastChannels = 0;
    private final HashSet<IGridNode> ignoredNode = new HashSet();

    public PathGridCache(IGrid g) {
        this.myGrid = g;
    }

    @Override
    public void onUpdateTick() {
        if (this.recalculateControllerNextTick) {
            this.recalcController();
        }
        if (this.updateNetwork) {
            this.updateNetwork = false;
            this.booting = true;
            this.myGrid.postEvent(new MENetworkBootingStatusChange(true));
            this.channelsInUse = 0;
            if (this.myGrid.isEmpty()) {
                return;
            }
            if (this.controllerState == ControllerState.NO_CONTROLLER) {
                this.channelsInUse = this.calculateAdHocChannels();
                int nodes = this.myGrid.getNodes().size();
                this.channelsByBlocks = nodes * this.channelsInUse;
                this.setChannelPowerUsage((double)this.channelsByBlocks / 128.0);
                this.myGrid.getPivot().beginVisit(new AdHocChannelUpdater(this.channelsInUse));
            } else if (this.controllerState == ControllerState.CONTROLLER_CONFLICT) {
                this.myGrid.getPivot().beginVisit(new AdHocChannelUpdater(0));
                this.channelsInUse = 0;
                this.channelsByBlocks = 0;
            } else {
                PathingCalculation calculation = new PathingCalculation(this.myGrid);
                calculation.compute();
                this.channelsInUse = calculation.getChannelsInUse();
                this.channelsByBlocks = calculation.getChannelsByBlocks();
            }
            this.achievementPost();
            this.booting = false;
            this.setChannelPowerUsage((double)this.channelsByBlocks / 128.0);
            this.myGrid.getPivot().beginVisit(new ChannelFinalizer());
            this.myGrid.postEvent(new MENetworkBootingStatusChange(this.booting));
        }
    }

    @Override
    public void removeNode(IGridNode gridNode, IGridHost machine) {
        if (AEConfig.instance.debugPathFinding) {
            String coordinates = gridNode.getGridBlock().getLocation().toString();
            AELog.info("Repath is triggered by removing a node at [%s]", coordinates);
            AELog.printStackTrace(Level.INFO);
        }
        if (machine instanceof TileController) {
            this.controllers.remove(machine);
            this.recalculateControllerNextTick = true;
        }
        if (gridNode.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            this.nodesNeedingChannels.remove(gridNode);
        }
        if (gridNode.hasFlag(GridFlags.CANNOT_CARRY_COMPRESSED)) {
            this.cannotCarryCompressedNodes.remove(gridNode);
        }
        this.repath();
    }

    @Override
    public void addNode(IGridNode gridNode, IGridHost machine) {
        if (AEConfig.instance.debugPathFinding) {
            String coordinates = gridNode.getGridBlock().getLocation().toString();
            AELog.info("Repath is triggered by adding a node at [%s]", coordinates);
            AELog.printStackTrace(Level.INFO);
        }
        if (machine instanceof TileController) {
            this.controllers.add((TileController)machine);
            this.recalculateControllerNextTick = true;
        }
        if (gridNode.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            this.nodesNeedingChannels.add(gridNode);
        }
        if (gridNode.hasFlag(GridFlags.CANNOT_CARRY_COMPRESSED)) {
            this.cannotCarryCompressedNodes.add(gridNode);
        }
        this.repath();
    }

    @Override
    public void onSplit(IGridStorage storageB) {
    }

    @Override
    public void onJoin(IGridStorage storageB) {
    }

    @Override
    public void populateGridStorage(IGridStorage storage) {
    }

    private void recalcController() {
        this.recalculateControllerNextTick = false;
        ControllerState old = this.controllerState;
        if (this.controllers.isEmpty()) {
            this.controllerState = ControllerState.NO_CONTROLLER;
        } else {
            IGridNode startingNode = this.controllers.iterator().next().getGridNode(ForgeDirection.UNKNOWN);
            if (startingNode == null) {
                this.controllerState = ControllerState.CONTROLLER_CONFLICT;
                return;
            }
            DimensionalCoord dc = startingNode.getGridBlock().getLocation();
            ControllerValidator cv = new ControllerValidator(dc.x, dc.y, dc.z);
            startingNode.beginVisit(cv);
            this.controllerState = cv.isValid() && cv.getFound() == this.controllers.size() ? ControllerState.CONTROLLER_ONLINE : ControllerState.CONTROLLER_CONFLICT;
        }
        if (old != this.controllerState) {
            this.myGrid.postEvent(new MENetworkControllerChange());
        }
    }

    private int calculateAdHocChannels() {
        this.ignoredNode.clear();
        int maxChannels = AEConfig.instance.isFeatureEnabled(AEFeature.Channels) ? 8 : Integer.MAX_VALUE;
        int channels = 0;
        for (IGridNode node : this.nodesNeedingChannels) {
            if (this.ignoredNode.contains(node)) continue;
            if (node.hasFlag(GridFlags.COMPRESSED_CHANNEL) && !this.cannotCarryCompressedNodes.isEmpty()) {
                return 0;
            }
            if (++channels > maxChannels) {
                return 0;
            }
            if (!node.hasFlag(GridFlags.MULTIBLOCK)) continue;
            IGridMultiblock gmb = (IGridMultiblock)node.getGridBlock();
            Iterator<IGridNode> i = gmb.getMultiblockNodes();
            while (i.hasNext()) {
                this.ignoredNode.add(i.next());
            }
        }
        return channels;
    }

    private void achievementPost() {
        Achievements lastBracket;
        Achievements currentBracket;
        if (this.lastChannels != this.channelsInUse && AEConfig.instance.isFeatureEnabled(AEFeature.Channels) && (currentBracket = this.getAchievementBracket(this.channelsInUse)) != (lastBracket = this.getAchievementBracket(this.lastChannels)) && currentBracket != null) {
            HashSet<Integer> players = new HashSet<Integer>();
            for (IGridNode n : this.nodesNeedingChannels) {
                players.add(n.getPlayerID());
            }
            Iterator<IGridNode> iterator = players.iterator();
            while (iterator.hasNext()) {
                int id = (Integer)((Object)iterator.next());
                Platform.addStat(id, currentBracket.getAchievement());
            }
        }
        this.lastChannels = this.channelsInUse;
    }

    private Achievements getAchievementBracket(int ch) {
        if (ch < 8) {
            return null;
        }
        if (ch < 128) {
            return Achievements.Networking1;
        }
        if (ch < 2048) {
            return Achievements.Networking2;
        }
        return Achievements.Networking3;
    }

    @MENetworkEventSubscribe
    public void updateNodReq(MENetworkChannelChanged ev) {
        IGridNode gridNode = ev.node;
        if (AEConfig.instance.debugPathFinding) {
            String coordinates = gridNode.getGridBlock().getLocation().toString();
            AELog.info("Repath is triggered by changing a node at [%s]", coordinates);
            AELog.printStackTrace(Level.INFO);
        }
        if (gridNode.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            this.nodesNeedingChannels.add(gridNode);
        } else {
            this.nodesNeedingChannels.remove(gridNode);
        }
        this.repath();
    }

    @Override
    public boolean isNetworkBooting() {
        return this.booting;
    }

    @Override
    public ControllerState getControllerState() {
        return this.controllerState;
    }

    @Override
    public void repath() {
        this.channelsByBlocks = 0;
        this.updateNetwork = true;
    }

    double getChannelPowerUsage() {
        return this.channelPowerUsage;
    }

    private void setChannelPowerUsage(double channelPowerUsage) {
        this.channelPowerUsage = channelPowerUsage;
    }
}

