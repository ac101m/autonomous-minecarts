package com.ac101m.am

import com.ac101m.am.persistence.Config
import com.ac101m.am.persistence.PersistentMinecartTicket
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.ChunkPos
import org.slf4j.LoggerFactory
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
    private val worldCarts = ArrayList<AbstractMinecartEntity>()

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

        val nothingToLoad = world.players.isEmpty() && world.forcedChunks.isEmpty()

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

        // Get all carts in the world. Note that this will not find carts in unloaded areas of the map.
        worldCarts.clear()
        world.collectEntitiesByType(minecartTypeFilter, EntityPredicates.VALID_ENTITY, worldCarts)

        // Update trackers for all minecarts with nonzero velocity, or create tracker if none exists.
        worldCarts.forEach { cart ->
            if (trackedMinecarts.contains(cart.uuid)) {
                trackedMinecarts[cart.uuid]!!.update(cart)
            } else {
                trackedMinecarts[cart.uuid] = MinecartTracker(cart, config)
            }
        }

        // Iterate through tracked minecarts
        trackedMinecarts.entries.removeIf { (id, tracker) ->

            // Stop tracking carts that have been removed from the world.
            if (tracker.minecart.removalReason?.shouldSave() == false) {
                ticketHandlers.remove(tracker.minecart.uuid)
                return@removeIf true
            }

            // Create ticket handlers for active minecarts if not already present
            if (tracker.minecartIsActive) {
                ticketHandlers.computeIfAbsent(id) {
                    TicketHandler(
                        world = tracker.minecart.world as ServerWorld,
                        chunkPos =  tracker.minecart.chunkPos,
                        config = config,
                        idleCounter = 0
                    )
                }.resetIdleCounter()
            }

            // Update ticket handlers positions
            ticketHandlers[id]?.updatePosition(tracker.minecart.chunkPos)

            false
        }

        // Tick ticket handlers and remove if timed out
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
        } catch (_: Exception) {
            log.warn("Minecart ticket at chunk (x=${persistedTicket.x}, z=${persistedTicket.z}) has invalid cart UUID, ignoring.")
            return
        }

        ticketHandlers[minecartUuid] = TicketHandler(
            world = world,
            chunkPos = ChunkPos(persistedTicket.x, persistedTicket.z),
            config = config,
            idleCounter = persistedTicket.idleTicks
        )
    }

    /**
     * Gets persisted tickets for all active minecart tickets.
     */
    fun getPersistableTickets(): List<PersistentMinecartTicket> {
        return ArrayList<PersistentMinecartTicket>().also { tickets ->
            ticketHandlers.entries.forEach { (id, ticket) ->
                tickets.add(ticket.getPersistableObject(id))
            }
        }
    }
}
