package com.ac101m.am

import com.ac101m.am.Utils.Companion.createChunkTicket
import com.ac101m.am.persistence.Config
import com.ac101m.am.persistence.PersistentMinecartTicket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import java.util.*

/**
 * Manages ticket creation as a minecart moves.
 */
class TicketHandler(
    private val world: ServerWorld,
    private var chunkPos: ChunkPos,
    private val config: Config,
    private var idleCounter: Int
) {
    private val type = Utils.createTicketType("am_minecart", config.ticketDuration)

    private var refreshCounter: Int = 0

    val isTimedOut get() = idleCounter >= config.idleTimeoutTicks

    init {
        createTicket(chunkPos)
    }

    private fun createTicket(position: ChunkPos) {
        world.createChunkTicket(type, position, config.chunkLoadRadius)
        refreshCounter = 1
    }

    /**
     * Called to update the position of the ticket.
     */
    fun updatePosition(newChunkPos: ChunkPos) {
        if (newChunkPos != chunkPos) {
            createTicket(newChunkPos)
            chunkPos = newChunkPos
        }
    }

    /**
     * Called on every tick, refreshes the ticket if appropriate.
     */
    fun tick() {
        if (refreshCounter < config.ticketDuration) {
            refreshCounter += 1
        } else {
            createTicket(chunkPos)
        }
    }

    /**
     * Reset the idle counter.
     */
    fun notifyActive() {
        idleCounter = 0
    }

    /**
     * Increment the idle counter.
     */
    fun notifyIdle() {
        if (idleCounter < config.idleTimeoutTicks) {
            idleCounter += 1
        }
    }

    fun getPersistenceObject(id: UUID): PersistentMinecartTicket {
        return PersistentMinecartTicket(
            minecartId = id.toString(),
            x = chunkPos.x,
            z = chunkPos.z,
            idleTicks = idleCounter,
            worldName = world.registryKey.value.toString()
        )
    }
}
