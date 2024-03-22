package com.ac101m.am.persistence

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


/**
 * Class represents global config.
 */
data class Config(
    /**
     * Ticks after which chunk tickets associated with unmoving or destroyed minecarts are deleted.
     * Defaults to 6000 ticks or 5 minutes.
     */
    @JsonProperty("idleTimeoutTicks", required = true)
    val idleTimeoutTicks: Int = 6000,

    /**
     * Radius around moving minecarts which will be loaded.
     * Defaults to 2, for a 3x3 area of entity ticking chunks.
     */
    @JsonProperty("chunkLoadRadius", required = true)
    val chunkLoadRadius: Int = 2,

    /**
     * The duration of created chunk tickets.
     * Higher values will cause chunk tickets to last longer and be created less frequently.
     * A value of 0 means tickets will be created in every tick.
     */
    @JsonProperty("ticketDuration", required = true)
    val chunkTicketDuration: Int = 60
) {
    companion object {
        private val mapper = ObjectMapper()

        fun load(path: Path): Config {
            return mapper.readValue(path.inputStream(), Config::class.java)
        }
    }

    fun save(path: Path) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.outputStream(), this)
    }
}
