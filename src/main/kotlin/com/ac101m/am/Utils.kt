package com.ac101m.am

import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos

class Utils {
    companion object {
        lateinit var AM_CHUNK_TICKET_TYPE: ChunkTicketType

        /**
         * Create a chunk ticket to a specific world.
         * @param type Type of the chunk ticket (includes name and timeout info).
         * @param position Chunk position to create the ticket for.
         * @param radius Radius of the chunk ticket.
         */
        fun ServerWorld.createChunkTicket(type: ChunkTicketType, position: ChunkPos, radius: Int) {
            chunkManager.addTicket(type, position, radius)
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
