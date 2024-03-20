package com.ac101m.am.persistence

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Persistence object for one-time wake tickets.
 */
data class StartupState(
    @JsonProperty("tickets", required = true)
    val tickets: List<StartupTicket> = ArrayList()
) {
    companion object {
        private val mapper = ObjectMapper()

        fun load(path: Path): StartupState {
            return mapper.readValue(path.inputStream(), StartupState::class.java)
        }
    }

    fun save(path: Path) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.outputStream(), this)
    }
}
