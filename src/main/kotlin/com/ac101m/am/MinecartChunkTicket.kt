package com.ac101m.am

import com.ac101m.am.Utils.Companion.createChunkTicket
import com.ac101m.am.persistence.Config
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos


class MinecartChunkTicket(
    minecart: AbstractMinecartEntity,
    private val config: Config
) {
    private val type = Utils.createTicketType("am_${minecart.id}", config.chunkTicketDuration)

    val world = minecart.world as ServerWorld

    private var idleCounter: Int = config.idleTimeoutTicks
    private var ticketRefreshCounter: Int = config.chunkTicketDuration

    var chunkPos: ChunkPos = minecart.chunkPos
        private set

    init {
        createTicket(chunkPos)
    }

    private fun createTicket(position: ChunkPos) {
        world.createChunkTicket(type, position, config.chunkLoadRadius)
        ticketRefreshCounter = config.chunkTicketDuration
    }

    fun tick(): Int {
        if (ticketRefreshCounter <= 0) {
            createTicket(chunkPos)
        } else {
            ticketRefreshCounter -= 1
        }

        if (idleCounter > 0) {
            idleCounter -= 1
        }

        return idleCounter
    }

    fun update(minecart: AbstractMinecartEntity) {
        if (chunkPos != minecart.chunkPos) {
            createTicket(minecart.chunkPos)
            chunkPos = minecart.chunkPos
        }
        idleCounter = config.idleTimeoutTicks
    }
}
