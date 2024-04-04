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

        private val typeFilter = TypeFilter.instanceOf<Entity, AbstractMinecartEntity>(AbstractMinecartEntity::class.java)

        private val log = LoggerFactory.getLogger(FabricServerEnvironment::class.java)
    }

    private lateinit var config: Config
    private var startupState: StartupState? = null

    private val trackedMinecarts = HashMap<UUID, MinecartTracker>()
    private val activeTickets = HashMap<UUID, TicketHandler>()

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

        activeTickets.entries.forEach { (id, ticket) ->
            tickets.add(ticket.getPersistenceObject(id))
        }

        val statePath = Path.of(environment.configDirectory.toString(), STATE_FILE_NAME)

        StartupState(tickets).save(statePath)
    }

    /**
     * Restores persisted minecart tickets before the first server tick
     */
    fun beforeServerTick() {
        val tickets = startupState?.let {
            startupState = null
            it.tickets
        } ?: return

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

            activeTickets[minecartId] = TicketHandler(
                world = world,
                initChunkPos = ChunkPos(ticket.x, ticket.z),
                config = config
            )
        }

        log.info("Minecart chunk tickets restored.")
    }

    /**
     * Update minecart trackers, create trackers where necessary.
     */
    fun afterWorldTick(world: ServerWorld) {
        val minecarts = ArrayList<AbstractMinecartEntity>()

        world.collectEntitiesByType(typeFilter, EntityPredicates.VALID_ENTITY, minecarts)

        minecarts.forEach { cart ->
            val tracker = trackedMinecarts[cart.uuid]

            if (tracker != null) {
                tracker.update(cart)
            } else {
                trackedMinecarts[cart.uuid] =  MinecartTracker(cart, config)
            }
        }
    }

    /**
     * Update trackers and tickets based on what happened during world ticks.
     */
    fun afterServerTick() {
        trackedMinecarts.entries.removeIf { (id, tracker) ->
            if (!tracker.getAndClearUpdated()) {
                return@removeIf true
            }

            // Create tickets for active minecarts if not already present
            if (!tracker.minecartIsIdle) {
                activeTickets.computeIfAbsent(id) {
                    println("Creating ticket @ ${tracker.minecart.chunkPos}")
                    TicketHandler(
                        world = tracker.minecart.world as ServerWorld,
                        initChunkPos =  tracker.minecart.chunkPos,
                        config = config
                    )
                }
            }

            // Update tickets to match tracker if appropriate
            if (!tracker.minecartIsFrozen) {
                activeTickets[id]?.let { ticketHandler ->
                    ticketHandler.updatePosition(tracker.minecart.chunkPos)
                    when (tracker.minecartIsIdle) {
                        true -> ticketHandler.notifyIdle()
                        else -> ticketHandler.notifyActive()
                    }
                }
            }

            return@removeIf false
        }

        activeTickets.entries.removeIf { (_, ticket) ->
            ticket.tick()

            if (ticket.isTimedOut) {
                println("Removing timed out ticket @ ${ticket.chunkPos}")
            }

            ticket.isTimedOut
        }
    }
}
