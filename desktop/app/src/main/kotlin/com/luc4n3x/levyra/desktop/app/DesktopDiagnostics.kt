package com.luc4n3x.levyra.desktop.app

import org.slf4j.LoggerFactory

internal object DesktopDiagnostics {
    private val logger = LoggerFactory.getLogger("Derbis")

    fun background(operation: String, error: Throwable) {
        logger.warn("Background task failed: $operation", error)
    }
}
