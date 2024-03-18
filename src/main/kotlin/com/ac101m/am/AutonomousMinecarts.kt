package com.ac101m.am

import com.ac101m.am.environment.ServerEnvironment
import com.ac101m.am.environment.FabricServerEnvironment
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypeFilter
import org.slf4j.LoggerFactory

/**
 * Mod implementation logic lives in here.
 */
class AutonomousMinecarts(private val environment: ServerEnvironment) {
    companion object {
        private val log = LoggerFactory.getLogger(FabricServerEnvironment::class.java)
    }

    private val loadedMinecarts = HashMap<Int, MinecartChunkTicket>()

    fun initialize() {
        log.info("Loading autonomous minecarts")
        // TODO: Save currently active minecarts here
    }

    fun shutdown() {
        log.info("Shutting down autonomous minecarts")
        // TODO: Store currently active minecarts for loading later
    }

    /**
     * Check for moving minecarts within a world and update the position of their chunk loading tickets.
     */
    fun afterWorldTick(world: ServerWorld) {
        val typeFilter = TypeFilter.instanceOf<Entity, AbstractMinecartEntity>(AbstractMinecartEntity::class.java)
        val minecarts = ArrayList<AbstractMinecartEntity>()

        world.collectEntitiesByType(typeFilter, EntityPredicates.VALID_ENTITY, minecarts)

        val movingMinecarts = minecarts.filter { cart ->
            cart.velocity.length() > 0.01
        }

        for (minecart in movingMinecarts) {
            if (loadedMinecarts.containsKey(minecart.id)) {
                loadedMinecarts[minecart.id]!!.update(minecart)
            } else {
                loadedMinecarts[minecart.id] = MinecartChunkTicket(minecart)
            }
        }
    }

    /**
     * Ticks all loaded minecarts, removes any that have timed out or are no longer alive
     */
    fun afterServerTick() {
        loadedMinecarts.entries.removeIf { entry ->
            entry.value.tick() == 0
        }
    }
}
