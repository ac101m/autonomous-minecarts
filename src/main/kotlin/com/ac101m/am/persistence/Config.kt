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
     * Ticks after which chunk tickets after which inactive or destroyed minecarts are unloaded.
     * Defaults to 6000 ticks or 5 minutes.
     */
    var idleTimeoutTicks: Int = 6000,

    /**
     * The speed threshold above which a minecart is considered to be moving in blocks per tick.
     * Minecarts moving slower than this will be considered inactive.
     */
    var idleThreshold: Double = 0.2,

    /**
     * Radius around minecarts which will be loaded.
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
     * Smoothing factor for cart position average.
     * This parameter can be used in conjunction with positionAverageDistance to tweak rejection of moving carts which
     * hang around in one area. The mod maintains an exponential moving average of cart positions. If the average
     * position of the cart does not change sufficiently (because it's moving, but staying in the same area), then the
     * cart will be unloaded. Lower values increase the duration of the average.
     * Warning: Setting the value too low relative to positionAverageDistance may result in carts not loading!
     */
    var positionAverageFactor: Double = 0.01,

    /**
     * Position average escape distance.
     * The distance in blocks away from the moving average (see positionAverageFactor) of the cart position which a cart
     * must be in order to be considered active. Higher values increase the distance from the moving average which a
     * cart must be in order to be considered active.
     * Warning: Setting the value too high relative to positionAverageFactor may result in carts not loading!
     */
    var positionAverageDistance: Double = 20.0
) {
    companion object {
        private const val IDLE_TIMEOUT_IDENTIFIER = "idleTimeoutTicks"
        private const val IDLE_THRESHOLD_IDENTIFIER = "idleThreshold"
        private const val CHUNK_LOAD_RADIUS_IDENTIFIER = "chunkLoadRadius"
        private const val TICKET_DURATION_IDENTIFIER = "ticketDuration"
        private const val POSITION_AVERAGE_FACTOR_IDENTIFIER = "positionAverageFactor"
        private const val POSITION_AVERAGE_DISTANCE_IDENTIFIER = "positionAverageDistance"

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

                properties.getProperty(POSITION_AVERAGE_FACTOR_IDENTIFIER)?.let { property ->
                    config.positionAverageFactor = property.toDouble()
                }

                properties.getProperty(POSITION_AVERAGE_DISTANCE_IDENTIFIER)?.let { property ->
                    config.positionAverageDistance = property.toDouble()
                }

                config.save(path)
            }
        }
    }

    fun save(path: Path) {
        Properties().also { properties ->
            properties.setProperty(IDLE_TIMEOUT_IDENTIFIER, idleTimeoutTicks.toString())
            properties.setProperty(IDLE_THRESHOLD_IDENTIFIER, idleThreshold.toString())
            properties.setProperty(CHUNK_LOAD_RADIUS_IDENTIFIER, chunkLoadRadius.toString())
            properties.setProperty(TICKET_DURATION_IDENTIFIER, ticketDuration.toString())
            properties.setProperty(POSITION_AVERAGE_FACTOR_IDENTIFIER, positionAverageFactor.toString())
            properties.setProperty(POSITION_AVERAGE_DISTANCE_IDENTIFIER, positionAverageDistance.toString())
            properties.store(path.outputStream(), "Autonomous minecarts config")
        }
    }
}
