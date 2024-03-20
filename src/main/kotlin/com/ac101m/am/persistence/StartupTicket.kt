package com.ac101m.am.persistence

import com.fasterxml.jackson.annotation.JsonProperty

data class StartupTicket(
    @JsonProperty("x", required = true)
    val x: Int,
    @JsonProperty("z", required = true)
    val z: Int,
    @JsonProperty("worldName", required = true)
    val worldName: String
)
