package com.ac101m.am.environment

import com.ac101.am.AutonomousMinecarts
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer


class FabricServerEnvironment : ServerEnvironment, DedicatedServerModInitializer {
    private lateinit var plugin: AutonomousMinecarts

    override lateinit var server: MinecraftServer

    override fun onInitializeServer() {
        plugin = AutonomousMinecarts(this)

        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
            plugin.initialize()
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            plugin.shutdown()
        }

        ServerTickEvents.END_WORLD_TICK.register { world ->
            plugin.afterWorldTick(world)
        }

        ServerTickEvents.END_SERVER_TICK.register { _ ->
            plugin.afterServerTick()
        }
    }
}
