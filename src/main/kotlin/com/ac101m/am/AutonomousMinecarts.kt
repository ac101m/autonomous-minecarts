package com.ac101m.am

import com.ac101m.am.environment.ServerEnvironment
import com.ac101m.am.environment.FabricServerEnvironment
import com.ac101m.am.persistence.Config
import com.fasterxml.jackson.databind.ObjectMapper
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypeFilter
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * Mod implementation logic lives in here.
 */
class AutonomousMinecarts(private val environment: ServerEnvironment) {
    companion object {
        private const val CONFIG_FILE_NAME = "autonomous-minecarts.json"

        private val log = LoggerFactory.getLogger(FabricServerEnvironment::class.java)
        private val objectMapper = ObjectMapper()
    }

    private lateinit var config: Config

    private val loadedMinecarts = HashMap<Int, MinecartChunkTicket>()

    /**
     * Loads mod configuration and active minecart tickets prior to last server shutdown
     */
    fun initialize() {
        log.info("Starting autonomous minecarts...")

        val configPath = Path.of(environment.configDirectory.toString(), CONFIG_FILE_NAME)

        config = try {
            Config.load(configPath)
        } catch (e: NoSuchFileException) {
            Config().apply { save(configPath) }
        }

        // TODO: Load currently active minecarts here
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
