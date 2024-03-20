package com.ac101m.am

import com.ac101m.am.Utils.Companion.createChunkTicket
import com.ac101m.am.Utils.Companion.getWorldByName
import com.ac101m.am.environment.ServerEnvironment
import com.ac101m.am.environment.FabricServerEnvironment
import com.ac101m.am.persistence.Config
import com.ac101m.am.persistence.StartupState
import com.ac101m.am.persistence.StartupTicket
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.ChunkPos
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * Mod implementation logic lives in here.
 */
class AutonomousMinecarts(private val environment: ServerEnvironment) {
    companion object {
        private const val CONFIG_FILE_NAME = "autonomous-minecarts.json"
        private const val STATE_FILE_NAME = "autonomous-minecarts-tickets.json"

        private val log = LoggerFactory.getLogger(FabricServerEnvironment::class.java)
    }

    private lateinit var config: Config
    private var startupState: StartupState? = null

    private val activeMinecarts = HashMap<Int, MinecartChunkTicket>()

    /**
     * Loads mod configuration and active minecart tickets prior to last server shutdown
     */
    fun initialize() {
        log.info("Starting autonomous minecarts...")

        val configPath = Path.of(environment.configDirectory.toString(), CONFIG_FILE_NAME)
        val statePath = Path.of(environment.configDirectory.toString(), STATE_FILE_NAME)

        config = try {
            Config.load(configPath)
        } catch (e: NoSuchFileException) {
            Config().apply { save(configPath) }
        }

        startupState = try {
            StartupState.load(statePath)
        } catch (e: NoSuchFileException) {
            return
        }
    }

    fun saveActiveTickets() {
        val tickets = ArrayList<StartupTicket>()

        activeMinecarts.values.forEach { ticket ->
            val startupTicket = StartupTicket(
                x = ticket.position.x,
                z = ticket.position.z,
                worldName = ticket.world.registryKey.value.toString()
            )
            tickets.add(startupTicket)
        }

        val statePath = Path.of(environment.configDirectory.toString(), STATE_FILE_NAME)

        StartupState(tickets).save(statePath)
    }

    private fun createStartupTickets(state: StartupState) {
        val startupTicketType = Utils.createTicketType("am_startup", 200)

        for (ticket in state.tickets) {
            environment.server.getWorldByName(ticket.worldName)?.let { world ->
                val chunkPos = ChunkPos(ticket.x, ticket.z)
                world.createChunkTicket(startupTicketType, chunkPos, config.chunkLoadRadius)
            }
        }
    }

    /**
     * Check for moving minecarts within a world and update the position of their chunk loading tickets.
     */
    fun afterWorldTick(world: ServerWorld) {
        startupState?.let {
            createStartupTickets(it)
            startupState = null
        }

        val typeFilter = TypeFilter.instanceOf<Entity, AbstractMinecartEntity>(AbstractMinecartEntity::class.java)
        val minecarts = ArrayList<AbstractMinecartEntity>()

        world.collectEntitiesByType(typeFilter, EntityPredicates.VALID_ENTITY, minecarts)

        val movingMinecarts = minecarts.filter { cart ->
            cart.velocity.length() > 0.01
        }

        for (minecart in movingMinecarts) {
            if (activeMinecarts.containsKey(minecart.id)) {
                activeMinecarts[minecart.id]!!.update(minecart)
            } else {
                activeMinecarts[minecart.id] = MinecartChunkTicket(minecart, config.idleTimeoutTicks, config.chunkLoadRadius)
            }
        }
    }

    /**
     * Ticks all loaded minecarts, removes any that have timed out or are no longer alive
     */
    fun afterServerTick() {
        activeMinecarts.entries.removeIf { entry ->
            entry.value.tick() == 0
        }
    }
}
