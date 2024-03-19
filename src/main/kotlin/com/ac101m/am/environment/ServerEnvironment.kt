package com.ac101m.am.environment

import net.minecraft.server.MinecraftServer
import java.nio.file.Path

/**
 * Abstraction over server frameworks/APIs
 */
interface ServerEnvironment {
    var server: MinecraftServer
    val configDirectory: Path
}
