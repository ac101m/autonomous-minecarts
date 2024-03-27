package com.ac101m.am

import com.ac101m.am.Utils.Companion.getWorldByName
import com.ac101m.am.environment.ServerEnvironment
import com.ac101m.am.environment.FabricServerEnvironment
import com.ac101m.am.persistence.Config
import com.ac101m.am.persistence.StartupState
import com.ac101m.am.persistence.PersistentMinecartTicket
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.UUID
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Mod implementation logic lives in here.
 */
class AutonomousMinecarts(private val environment: ServerEnvironment) {
    companion object {
        private const val CONFIG_FILE_NAME = "autonomous-minecarts.properties"
        private const val STATE_FILE_NAME = "autonomous-minecarts-tickets.json"

        private val log = LoggerFactory.getLogger(FabricServerEnvironment::class.java)
    }

    private lateinit var config: Config
    private var startupState: StartupState? = null

    private val activeMinecarts = HashMap<UUID, MinecartChunkTicket>()

    /**
     * Loads mod configuration and active minecarts
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

    fun beforeWorldSave() {
        val tickets = ArrayList<PersistentMinecartTicket>()

        activeMinecarts.entries.forEach { (id, ticket) ->
            tickets.add(ticket.getPersistenceObject(id))
        }

        val statePath = Path.of(environment.configDirectory.toString(), STATE_FILE_NAME)

        StartupState(tickets).save(statePath)
    }

    /**
     * Load persisted minecart tickets.
     */
    private fun restoreTickets(tickets: List<PersistentMinecartTicket>) {
        log.info("Restoring minecart chunk tickets for ${tickets.size} minecarts...")

        for (ticket in tickets) {
            val world = environment.server.getWorldByName(ticket.worldName)

            if (world == null) {
                log.warn("Persisted minecart ticket references world ${ticket.worldName}, but no such world exists!")
                continue
            }

            val minecartId = try {
                UUID.fromString(ticket.minecartId)
            } catch (e: Exception) {
                log.warn("Minecart ticket at chunk (x=${ticket.x}, z=${ticket.z}) has invalid cart UUID, ignoring.")
                continue
            }

            activeMinecarts[minecartId] = MinecartChunkTicket(
                world = world,
                chunkPos = ChunkPos(ticket.x, ticket.z),
                ticketDuration = config.ticketDuration,
                timeoutDuration = config.idleTimeoutTicks,
                radius = config.chunkLoadRadius,
                idleCounterInit = ticket.idleTicks
            )
        }

        log.info("Minecart chunk tickets restored.")
    }

    /**
     * Check for moving minecarts within a world and update the position of their chunk tickets.
     */
    fun beforeWorldTick(world: ServerWorld) {
        startupState?.let {
            restoreTickets(it.tickets)
            startupState = null
        }

        val typeFilter = TypeFilter.instanceOf<Entity, AbstractMinecartEntity>(AbstractMinecartEntity::class.java)
        val minecarts = ArrayList<AbstractMinecartEntity>()

        world.collectEntitiesByType(typeFilter, EntityPredicates.VALID_ENTITY, minecarts)

        val movingMinecarts = minecarts.filter { cart ->
            cart.velocity.length() > config.idleThreshold
        }

        for (minecart in movingMinecarts) {
            if (activeMinecarts.containsKey(minecart.uuid)) {
                activeMinecarts[minecart.uuid]!!.update(minecart)
            } else {
                activeMinecarts[minecart.uuid] = MinecartChunkTicket(
                    world = world,
                    chunkPos = minecart.chunkPos,
                    radius = config.chunkLoadRadius,
                    ticketDuration = config.ticketDuration,
                    timeoutDuration = config.idleTimeoutTicks
                )
            }
        }
    }

    /**
     * Ticks all loaded minecarts, removes any that have timed out
     */
    fun beforeServerTick() {
        activeMinecarts.entries.removeIf { entry ->
            entry.value.isDone
        }

        activeMinecarts.values.forEach { it.tick() }
    }
}
