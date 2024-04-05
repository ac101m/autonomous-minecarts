package com.ac101m.am

import com.ac101m.am.persistence.Config
import com.ac101m.am.persistence.PersistentMinecartTicket
import com.ac101m.am.persistence.StartupState
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.ChunkPos
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Tracks the state of a world, specifically whether to tick or not.
 */
class WorldTracker(
    private var world: ServerWorld,
    private val config: Config
) {
    companion object {
        // 300 ticks = 15 seconds
        private const val IDLE_TIMEOUT = 300

        private val minecartTypeFilter =
            TypeFilter.instanceOf<Entity, AbstractMinecartEntity>(AbstractMinecartEntity::class.java)

        private val log = LoggerFactory.getLogger(WorldTracker::class.java)
    }

    /**
     * Counts number of ticks the world has been idle.
     * World is idle if there are no players and no force-loaded chunks.
     */
    private var idleCounter = 0

    /**
     * Follows the state of the world (ticking or suspended)
     */
    private var shouldTick = true

    /**
     * Array containing all minecart entities.
     */
    private val minecarts = ArrayList<AbstractMinecartEntity>()

    /**
     * Tracked minecarts by cart UUID.
     */
    private val trackedMinecarts = HashMap<UUID, MinecartTracker>()

    /**
     * Set of active minecart tickets by cart UUID.
     */
    private val ticketHandlers = HashMap<UUID, TicketHandler>()

    /**
     * If a world is idle for 15 seconds (no force-loaded chunks and no players) then entity ticking stops in that
     * world. This function mimics this world idling behaviour (see ServerWorld.tick()), and updates the
     * [shouldTick] variable to indicate whether entities in the world will tick.
     * Should be called at the beginning of a world tick.
     */
    fun updateWorldIdle(world: ServerWorld) {
        this.world = world

        val nothingToLoad = world.players.isEmpty() && world.getForcedChunks().isEmpty()

        if (!nothingToLoad) {
            idleCounter = 0
        }

        shouldTick = !nothingToLoad || idleCounter++ < IDLE_TIMEOUT
    }

    /**
     * Updates minecarts and minecart tickets.
     */
    fun updateMinecarts(world: ServerWorld) {
        this.world = world

        if (!shouldTick) {
            return
        }

        minecarts.clear()
        world.collectEntitiesByType(minecartTypeFilter, EntityPredicates.VALID_ENTITY, minecarts)

        // Update minecart trackers for all tracked minecarts
        minecarts.forEach { cart ->
            val tracker = trackedMinecarts[cart.uuid]

            if (tracker != null) {
                tracker.update(cart)
            } else {
                trackedMinecarts[cart.uuid] =  MinecartTracker(cart, config)
            }
        }

        // Iterate through tracked minecarts
        trackedMinecarts.entries.removeIf { (id, tracker) ->

            // Delete minecart tracker if it wasn't updated this tick (minecart is gone)
            if (!tracker.getAndClearUpdated()) {
                return@removeIf true
            }

            // Create ticker handlers for active minecarts if not already present
            if (!tracker.minecartIsIdle) {
                ticketHandlers.computeIfAbsent(id) {
                    TicketHandler(
                        world = tracker.minecart.world as ServerWorld,
                        initChunkPos =  tracker.minecart.chunkPos,
                        config = config
                    )
                }
            }

            // Update ticket handlers depending on whether the cart is idle or not
            ticketHandlers[id]?.let { ticketHandler ->
                ticketHandler.updatePosition(tracker.minecart.chunkPos)
                when (tracker.minecartIsIdle) {
                    true -> ticketHandler.notifyIdle()
                    else -> ticketHandler.notifyActive()
                }
            }

            false
        }

        // Remove any ticket handlers which have timed out
        ticketHandlers.entries.removeIf { (_, ticket) ->
            ticket.tick()
            ticket.isTimedOut
        }
    }

    /**
     * Loads a persisted ticket into the world.
     */
    fun loadPersistedTicket(persistedTicket: PersistentMinecartTicket) {
        val minecartUuid = try {
            UUID.fromString(persistedTicket.minecartId)
        } catch (e: Exception) {
            log.warn("Minecart ticket at chunk (x=${persistedTicket.x}, z=${persistedTicket.z}) has invalid cart UUID, ignoring.")
            return
        }

        ticketHandlers[minecartUuid] = TicketHandler(
            world = world,
            initChunkPos = ChunkPos(persistedTicket.x, persistedTicket.z),
            config = config
        )
    }

    /**
     * Gets persisted tickets for all active minecart tickets.
     */
    fun getPersistableTickets(): List<PersistentMinecartTicket> {
        return ArrayList<PersistentMinecartTicket>().also { tickets ->
            ticketHandlers.entries.forEach { (id, ticket) ->
                tickets.add(ticket.getPersistenceObject(id))
            }
        }
    }
}
