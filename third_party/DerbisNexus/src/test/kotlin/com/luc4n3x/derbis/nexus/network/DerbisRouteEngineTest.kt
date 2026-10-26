package com.luc4n3x.derbis.nexus.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisRouteEngineTest {
    @Test
    fun fasterReliableRouteMovesFirst() {
        var now = 1_000L
        val engine = DerbisRouteEngine { now }
        val slow = DerbisRoute("slow", "example.com", DerbisTransport.OKHTTP, DerbisAddressFamily.IPV4, 0)
        val fast = DerbisRoute("fast", "example.com", DerbisTransport.OKHTTP, DerbisAddressFamily.IPV6, 1)

        repeat(4) { engine.recordSuccess(slow, 800L) }
        repeat(4) { engine.recordSuccess(fast, 120L) }

        assertEquals(fast, engine.order(listOf(slow, fast)).first())
    }

    @Test
    fun rateLimitedRouteIsBlockedWithoutAffectingOthers() {
        var now = 10_000L
        val engine = DerbisRouteEngine { now }
        val blocked = DerbisRoute("blocked", "example.com")
        val healthy = DerbisRoute("healthy", "example.com", priority = 1)

        engine.recordFailure(blocked, DerbisRouteFailure.RATE_LIMIT)

        assertEquals(listOf(healthy), engine.order(listOf(blocked, healthy)))
        now += 11L * 60L * 1_000L
        assertTrue(blocked in engine.order(listOf(blocked, healthy)))
    }

    @Test
    fun recoveredDenialDoesNotGloballyBlockRoute() {
        val engine = DerbisRouteEngine { 5_000L }
        val denied = DerbisRoute("denied", "example.com")
        val fallback = DerbisRoute("fallback", "example.com", priority = 1)

        repeat(5) {
            engine.recordFailure(denied, DerbisRouteFailure.ACCESS_DENIED, recoveredByFallback = true)
            engine.recordSuccess(fallback, 100L)
        }

        assertTrue(denied in engine.order(listOf(denied, fallback)))
        assertEquals(fallback, engine.order(listOf(denied, fallback)).first())
    }

    @Test
    fun staleSuccessDoesNotEraseActiveRateLimitBlock() {
        val engine = DerbisRouteEngine { 10_000L }
        val route = DerbisRoute("route", "example.com")
        engine.recordFailure(route, DerbisRouteFailure.RATE_LIMIT)
        engine.recordSuccess(route, 100L)
        assertTrue(engine.order(listOf(route)).isEmpty())
    }

    @Test
    fun networkResetClearsTemporaryBlocks() {
        val engine = DerbisRouteEngine { 1_000L }
        val route = DerbisRoute("route", "example.com")
        engine.recordFailure(route, DerbisRouteFailure.RATE_LIMIT)
        assertTrue(engine.order(listOf(route)).isEmpty())
        engine.resetVolatileState()
        assertEquals(listOf(route), engine.order(listOf(route)))
    }
}
