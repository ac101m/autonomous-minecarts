package com.ac101m.am

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3

class Utils {
    companion object {
        lateinit var AM_CHUNK_TICKET_TYPE: TicketType

        /**
         * Create a chunk ticket to a specific world.
         * @param type Type of the chunk ticket (includes name and timeout info).
         * @param position Chunk position to create the ticket for.
         * @param radius Radius of the chunk ticket.
         */
        fun ServerLevel.createChunkTicket(type: TicketType, position: ChunkPos, radius: Int) {
            chunkSource.addTicketWithRadius(type, position, radius)
        }

        /**
         * Get a world based on the name of the world.
         * @param name Name of the world to find.
         * @return The world with the specified name, or null if no world is found.
         */
        fun MinecraftServer.getWorldByName(name: String): ServerLevel? {
            return allLevels.find { world ->
                world.dimension().location().toString() == name
            }
        }

        /**
         * Multiply Vec3 by a scalar.
         * I guess MC removed this sometime between 1.21.8 and 1.21.11!
         * @param factor The factor to multiply the vector by.
         */
        fun Vec3.multiply(factor: Double): Vec3 {
            return Vec3(x * factor, y * factor, z * factor)
        }
    }
}
