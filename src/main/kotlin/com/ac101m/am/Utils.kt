package com.ac101m.am

import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import kotlin.Comparator

class Utils {
    companion object {
        private val CHUNK_POS_TO_LONG: ChunkPos.() -> Long = ChunkPos::toLong

        /**
         * Create a custom chunk ticket type
         * @param name The name of the chunk ticket type.
         * @param expiryTicks Expiry time in game ticks.
         * @return ChunkTicketType object.
         */
        fun createTicketType(name: String, expiryTicks: Int): ChunkTicketType<ChunkPos> {
            return ChunkTicketType.create(name, Comparator.comparingLong(CHUNK_POS_TO_LONG), expiryTicks)
        }

        /**
         * Create a chunk ticket to a specific world.
         * @param type Type of the chunk ticket (includes name and timeout info).
         * @param position Chunk position to create the ticket for.
         * @param radius Radius of the chunk ticket.
         */
        fun ServerWorld.createChunkTicket(type: ChunkTicketType<ChunkPos>, position: ChunkPos, radius: Int) {
            chunkManager.threadedAnvilChunkStorage.ticketManager.addTicket(type, position, radius, position)
        }

        /**
         * Get a world based on the name of the world.
         * @param name Name of the world to find.
         * @return The world with the specified name, or null if no world is found.
         */
        fun MinecraftServer.getWorldByName(name: String): ServerWorld? {
            return worlds.find { world ->
                world.registryKey.value.toString() == name
            }
        }
    }
}
