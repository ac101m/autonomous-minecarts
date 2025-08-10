package com.ac101m.am

import com.ac101m.am.persistence.Config
import net.minecraft.entity.vehicle.AbstractMinecartEntity

/**
 * Tracks minecart behaviour and decides whether a cart is idle or not.
 */
class MinecartTracker(
    initMinecart: AbstractMinecartEntity,
    private val config: Config
) {
    private var wasUpdated: Boolean = false

    var minecart = initMinecart
        private set

    /**
     * Vector exponential moving average of cart position.
     * Used to filter out carts which are moving but not leaving a confined area.
     */
    private var smoothedPos = minecart.pos

    /**
     * "Active" minecarts are minecarts which are moving in a way which the mods loading criteria
     */
    var minecartIsActive: Boolean = false
        private set

    /**
     * Update the state associated with the minecart (moving average, is active etc)
     */
    fun update(minecart: AbstractMinecartEntity) {
        this.minecart = minecart

        smoothedPos = minecart.pos.multiply(config.positionAverageFactor).add(smoothedPos!!.multiply(1.0 - config.positionAverageFactor))

        minecartIsActive = if (minecart.velocity.length() < config.idleThreshold) {
            false
        } else if (smoothedPos.subtract(minecart.pos).length() < config.positionAverageDistance) {
            false
        } else {
            true
        }

        wasUpdated = true
    }

    /**
     * Return whether the tracker has been updated and reset the isUpdated state.
     * Used to check if the minecart is still present in the world.
     */
    fun getAndClearUpdated(): Boolean {
        val tmp = wasUpdated
        wasUpdated = false
        return tmp
    }
}
