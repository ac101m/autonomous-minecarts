package com.ac101m.am

import com.ac101m.am.Utils.Companion.getWorldByName
import com.ac101m.am.environment.ServerEnvironment
import com.ac101m.am.persistence.Config
import com.ac101m.am.persistence.StartupState
import com.ac101m.am.persistence.PersistentMinecartTicket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Mod implementation logic lives in here.
 */
class AutonomousMinecarts(private val environment: ServerEnvironment) {
    companion object {
        private const val CONFIG_FILE_NAME = "autonomous-minecarts.properties"
        private const val STATE_FILE_NAME = "autonomous-minecarts-tickets.json"

        private val log = LoggerFactory.getLogger(AutonomousMinecarts::class.java)
    }

    private lateinit var config: Config
    private var startupState: StartupState? = null

    private val trackedWorlds = HashMap<Identifier, WorldTracker>()

    /**
     * Load mod configuration and minecart tickets
     */
    fun onServerStart() {
        log.info("Starting autonomous minecarts...")

        val configPath = Path.of(environment.configDirectory.toString(), CONFIG_FILE_NAME)
        val statePath = Path.of(environment.configDirectory.toString(), STATE_FILE_NAME)

        config = try {
            log.info("Loading configuration: $configPath")
            Config.load(configPath)
        } catch (e: NoSuchFileException) {
            log.info("Configuration file missing, loading defaults.")
            Config().apply { save(configPath) }
        }

        startupState = try {
            log.info("Loading persisted minecart tickets: $statePath")
            StartupState.load(statePath)
        } catch (e: NoSuchFileException) {
            log.info("Minecart ticket state file, ignoring.")
            return
        }

        log.info("Autonomous minecarts started!")
    }

    /**
     * Gather tickets from tracked worlds and persist them.
     */
    fun beforeWorldSave() {
        val allTickets = ArrayList<PersistentMinecartTicket>()

        trackedWorlds.forEach { (_, worldTracker) ->
            allTickets.addAll(worldTracker.getPersistableTickets())
        }

        val statePath = Path.of(environment.configDirectory.toString(), STATE_FILE_NAME)

        StartupState(allTickets).save(statePath)
    }

    /**
     * Restores persisted minecart tickets before the first server tick
     */
    fun beforeServerTick() {
        val persistedTickets = startupState?.let {
            startupState = null
            it.tickets
        } ?: return

        log.info("Restoring persisted chunk tickets for ${persistedTickets.size} minecarts...")

        for (ticket in persistedTickets) {
            val world = environment.server.getWorldByName(ticket.worldName)

            if (world == null) {
                log.warn("Persisted minecart ticket references world ${ticket.worldName}, but no such world exists!")
                continue
            }

            trackedWorlds.computeIfAbsent(world.registryKey.value) {
                WorldTracker(world, config)
            }.loadPersistedTicket(ticket)
        }

        log.info("Minecart chunk tickets restored.")
    }

    /**
     * Update tracked worlds and world idle state.
     */
    fun beforeWorldTick(world: ServerWorld) {
        trackedWorlds.computeIfAbsent(world.registryKey.value) {
            WorldTracker(world, config)
        }.updateWorldIdle(world)
    }

    /**
     * Update minecart trackers, create trackers where necessary.
     */
    fun afterWorldTick(world: ServerWorld) {
        trackedWorlds[world.registryKey.value]!!.updateMinecarts(world)
    }
}
