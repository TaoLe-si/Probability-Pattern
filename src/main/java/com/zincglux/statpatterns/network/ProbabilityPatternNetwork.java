/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.zincglux.statpatterns.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Registers the mod's SimpleNetworkWrapper channel (Spec v2.0 3.3.5).
 */
public final class ProbabilityPatternNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("statpatterns");

    private ProbabilityPatternNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(ProbabilityPatternPacketHandler.class, ProbabilityPatternPacket.class, 0, Side.SERVER);
        CHANNEL.registerMessage(
            ProbabilityPatternValueSetPacketHandler.class,
            ProbabilityPatternValueSetPacket.class,
            1,
            Side.SERVER);
        // Middle/left-click auto-craft on storage items: bridge to AE2's craft-amount GUI.
        CHANNEL.registerMessage(
            PacketProbabilityPatternAutoCraftHandler.class,
            PacketProbabilityPatternAutoCraft.class,
            2,
            Side.SERVER);
        ProbabilityPatternServerTaskQueue.init();
    }
}
