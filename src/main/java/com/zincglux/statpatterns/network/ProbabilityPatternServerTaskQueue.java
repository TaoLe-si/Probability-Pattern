/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.zincglux.statpatterns.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/** Runs server-bound network work from the server tick instead of Netty IO threads. */
public final class ProbabilityPatternServerTaskQueue {

    private static final ProbabilityPatternServerTaskQueue INSTANCE = new ProbabilityPatternServerTaskQueue();
    private static final Queue<Runnable> PENDING_TASKS = new ConcurrentLinkedQueue<Runnable>();
    private static boolean initialized;

    private ProbabilityPatternServerTaskQueue() {}

    public static void init() {
        if (!initialized) {
            initialized = true;
            FMLCommonHandler.instance()
                .bus()
                .register(INSTANCE);
        }
    }

    public static void enqueue(final Runnable task) {
        if (task != null) {
            PENDING_TASKS.add(task);
        }
    }

    @SubscribeEvent
    public void onServerTick(final ServerTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }

        Runnable task;
        while ((task = PENDING_TASKS.poll()) != null) {
            task.run();
        }
    }
}
