package com.ac101m.am

import com.ac101m.am.persistence.Config
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.util.math.Vec3d

/**
 * Tracks minecart behaviour and decides whether a cart is idle or not.
 */
class MinecartTracker(
    initMinecart: AbstractMinecartEntity,
    private val config: Config
) {
    private var wasUpdated: Boolean = true

    var minecart = initMinecart
        private set

    private var realVelocity = Vec3d(0.0, 0.0, 0.0)
    private var smoothedPos = minecart.pos
    private var prevPos = minecart.pos

    var minecartIsIdle: Boolean = true
        private set

    fun update(minecart: AbstractMinecartEntity) {
        wasUpdated = true

        // Update tracker state
        realVelocity = minecart.pos.subtract(prevPos)
        smoothedPos = minecart.pos.multiply(config.smoothingFactor).add(smoothedPos!!.multiply(1.0 - config.smoothingFactor))
        prevPos = minecart.pos

        // Compute minecart idle-ness
        minecartIsIdle = if (realVelocity.length() < config.idleThreshold) {
            true
        } else if (smoothedPos!!.subtract(minecart.pos).length() < 20) {
            true
        } else {
            false
        }
    }

    fun getAndClearUpdated(): Boolean {
        val tmp = wasUpdated
        wasUpdated = false
        return tmp
    }
}
