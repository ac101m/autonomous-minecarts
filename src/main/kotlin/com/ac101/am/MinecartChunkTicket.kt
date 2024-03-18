package com.ac101.am

import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos


class MinecartChunkTicket(minecart: AbstractMinecartEntity) {
    companion object {
        const val IDLE_TICK_COUNT = 6000    // 5 minutes
        const val TICKET_RADIUS = 2         // 3x3 square of entity ticking chunks

        private val CHUNK_POS_TO_LONG: ChunkPos.() -> Long = ChunkPos::toLong
    }

    private val world = minecart.world as ServerWorld

    private val ticketType = ChunkTicketType.create(
        "am_${minecart.id}", Comparator.comparingLong(CHUNK_POS_TO_LONG))

    private var ticketPosition = minecart.chunkPos
    private var timeout: Int = IDLE_TICK_COUNT

    init {
        createTicket()
    }

    fun tick(): Int {
        if (timeout > 0) {
            timeout -= 1
            createTicket()
        }
        return timeout
    }

    fun update(minecart: AbstractMinecartEntity) {
        ticketPosition = minecart.chunkPos
        timeout = IDLE_TICK_COUNT
    }

    private fun createTicket() {
        world.chunkManager.threadedAnvilChunkStorage.ticketManager.addTicket(
            ticketType,
            ticketPosition,
            TICKET_RADIUS,
            ticketPosition
        )
    }
}
