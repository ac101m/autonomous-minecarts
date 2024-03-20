package com.ac101m.am

import com.ac101m.am.Utils.Companion.createChunkTicket
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos


class MinecartChunkTicket(
    minecart: AbstractMinecartEntity,
    private val idleTimeoutTicks: Int,
    private val radius: Int
) {
    val world = minecart.world as ServerWorld
    private val ticketType = Utils.createTicketType("am_${minecart.id}", 0)

    private var counter: Int = idleTimeoutTicks

    var position: ChunkPos = minecart.chunkPos
        private set

    init {
        world.createChunkTicket(ticketType, position, radius)
    }

    fun tick(): Int {
        if (counter > 0) {
            counter -= 1
            world.createChunkTicket(ticketType, position, radius)
        }
        return counter
    }

    fun update(minecart: AbstractMinecartEntity) {
        position = minecart.chunkPos
        counter = idleTimeoutTicks
    }
}
