package com.ac101m.am

import com.ac101m.am.Utils.Companion.createChunkTicket
import com.ac101m.am.persistence.PersistentMinecartTicket
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import java.util.*


class MinecartChunkTicket(
    val world: ServerWorld,
    private var chunkPos: ChunkPos,
    private val ticketDuration: Int,
    private val timeoutDuration: Int,
    private val radius: Int,
    idleCounterInit: Int = 0
) {
    private val type = Utils.createTicketType("am_minecart", ticketDuration)

    private var ticketRefreshCounter: Int = 0
    private var idleCounter: Int = idleCounterInit

    var isDone: Boolean = false
        private set

    init {
        createTicket(chunkPos)
    }

    private fun createTicket(position: ChunkPos) {
        world.createChunkTicket(type, position, radius)
        ticketRefreshCounter = 0
    }

    fun tick() {
        if (ticketRefreshCounter < ticketDuration) {
            ticketRefreshCounter += 1
        } else {
            createTicket(chunkPos)
        }

        if (idleCounter < timeoutDuration) {
            idleCounter += 1
        } else {
            isDone = true
        }
    }

    fun update(minecart: AbstractMinecartEntity) {
        if (chunkPos != minecart.chunkPos) {
            createTicket(minecart.chunkPos)
            chunkPos = minecart.chunkPos
        }
        idleCounter = 0
    }

    fun getPersistenceObject(minecartId: UUID): PersistentMinecartTicket {
        return PersistentMinecartTicket(
            minecartId = minecartId.toString(),
            x = chunkPos.x,
            z = chunkPos.z,
            idleTicks = idleCounter,
            worldName = world.registryKey.value.toString()
        )
    }
}
