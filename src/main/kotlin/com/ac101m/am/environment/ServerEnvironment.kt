package com.ac101m.am.environment

import net.minecraft.server.MinecraftServer

/**
 * Abstraction over server frameworks/APIs
 */
interface ServerEnvironment {
    var server: MinecraftServer
}
