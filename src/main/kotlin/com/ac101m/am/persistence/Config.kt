package com.ac101m.am.persistence

import java.nio.file.Path
import java.util.*
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Remember to update the configuration section in the readme!
 */
data class Config(
    /**
     * Ticks after which chunk tickets associated with unmoving or destroyed minecarts are deleted.
     * Defaults to 6000 ticks or 5 minutes.
     */
    var idleTimeoutTicks: Int = 6000,

    /**
     * The threshold above which a minecart is considered to be moving in blocks per tick.
     * Lower values make the mod more sensitive to minecart movement.
     */
    var idleThreshold: Double = 0.2,

    /**
     * Radius around moving minecarts which will be loaded.
     * Defaults to 2, for a 3x3 area of entity ticking chunks.
     */
    var chunkLoadRadius: Int = 2,

    /**
     * The duration of created chunk tickets.
     * Higher values will cause chunk tickets to last longer and be created less frequently.
     * A value of 0 means tickets will be created in every tick.
     */
    var ticketDuration: Int = 60,

    /**
     * Smoothing factor for cart position.
     * The mod maintains an exponential moving average of cart positions. If the average position of the cart
     * does not change sufficiently (because it's moving, but staying in the same area), then the cart will be unloaded.
     */
    var smoothingFactor: Double = 0.01
) {
    companion object {
        private const val IDLE_TIMEOUT_IDENTIFIER = "idleTimeoutTicks"
        private const val IDLE_THRESHOLD_IDENTIFIER = "idleThreshold"
        private const val CHUNK_LOAD_RADIUS_IDENTIFIER = "chunkLoadRadius"
        private const val TICKET_DURATION_IDENTIFIER = "ticketDuration"

        fun load(path: Path): Config {
            return Config().also { config ->
                val properties = Properties().also { it.load(path.inputStream()) }

                properties.getProperty(IDLE_TIMEOUT_IDENTIFIER)?.let { property ->
                    config.idleTimeoutTicks = property.toInt()
                }

                properties.getProperty(IDLE_THRESHOLD_IDENTIFIER)?.let { property ->
                    config.idleThreshold = property.toDouble()
                }

                properties.getProperty(CHUNK_LOAD_RADIUS_IDENTIFIER)?.let { property ->
                    config.chunkLoadRadius = property.toInt()
                }

                properties.getProperty(TICKET_DURATION_IDENTIFIER)?.let { property ->
                    config.ticketDuration = property.toInt()
                }
            }
        }
    }

    fun save(path: Path) {
        Properties().also { properties ->
            properties.setProperty(IDLE_TIMEOUT_IDENTIFIER, idleTimeoutTicks.toString())
            properties.setProperty(IDLE_THRESHOLD_IDENTIFIER, idleThreshold.toString())
            properties.setProperty(CHUNK_LOAD_RADIUS_IDENTIFIER, chunkLoadRadius.toString())
            properties.setProperty(TICKET_DURATION_IDENTIFIER, ticketDuration.toString())
            properties.store(path.outputStream(), "Autonomous minecarts config")
        }
    }
}
