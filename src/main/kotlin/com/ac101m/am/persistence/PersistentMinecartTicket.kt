package com.ac101m.am.persistence

import com.fasterxml.jackson.annotation.JsonProperty

data class PersistentMinecartTicket(
    @JsonProperty("minecartId", required = true)
    val minecartId: String,
    @JsonProperty("x", required = true)
    val x: Int,
    @JsonProperty("z", required = true)
    val z: Int,
    @JsonProperty("idleTicks", required = true)
    val idleTicks: Int,
    @JsonProperty("worldName", required = true)
    val worldName: String
)
