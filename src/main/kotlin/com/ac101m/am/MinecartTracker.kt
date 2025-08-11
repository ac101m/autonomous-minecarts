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
    var minecart = initMinecart
        private set

    /**
     * Vector exponential moving average of cart position.
     * Used to filter out carts which are moving but not leaving a confined area.
     */
    private var smoothedPos = minecart.pos

    /**
     * "Active" minecarts are minecarts which are moving in a way that satisfies the mods loading criteria.
     * A cart that is moving when the MinecartTracker is instantiated will be considered active for 1 tick.
     * This allows carts to be loaded immediately when transitioning between dimensions.
     */
    var minecartIsActive: Boolean = initMinecart.velocity.length() > 0.00001
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
    }
}
