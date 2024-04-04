package com.ac101m.am.environment

import com.ac101m.am.AutonomousMinecarts
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import java.nio.file.Path


class FabricServerEnvironment : ServerEnvironment, DedicatedServerModInitializer {
    private lateinit var plugin: AutonomousMinecarts

    override lateinit var server: MinecraftServer
    override val configDirectory: Path = FabricLoader.getInstance().configDir

    override fun onInitializeServer() {
        plugin = AutonomousMinecarts(this)

        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
            plugin.onServerStart()
        }

        ServerLifecycleEvents.BEFORE_SAVE.register { _, _, _ ->
            plugin.beforeWorldSave()
        }

        ServerTickEvents.END_WORLD_TICK.register { world ->
            plugin.afterWorldTick(world)
        }

        ServerTickEvents.START_SERVER_TICK.register { _ ->
            plugin.beforeServerTick()
        }

        ServerWorldEvents.LOAD.register { _, world ->
            println("World loading: ${world.registryKey.value}")
        }

        ServerWorldEvents.UNLOAD.register { _, world ->
            println("World unloading: ${world.registryKey.value}")
        }

        ServerTickEvents.END_SERVER_TICK.register { _ ->
            plugin.afterServerTick()
        }
    }
}
