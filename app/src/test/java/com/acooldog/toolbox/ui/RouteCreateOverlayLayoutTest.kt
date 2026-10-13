package com.acooldog.toolbox.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteCreateOverlayLayoutTest {

    @Test
    fun gearEdgeIsOnlyDetectedNearScreenEdges() {
        assertEquals(-1, routeCreateGearEdge(4f, 300f, 12f))
        assertEquals(0, routeCreateGearEdge(150f, 300f, 12f))
        assertEquals(1, routeCreateGearEdge(296f, 300f, 12f))
    }

    @Test
    fun menuUsesSpaceAboveBeforeFallingBelow() {
        assertEquals(152f, routeCreateMenuTopY(600f, 52f, 440f, 800f, 8f))
        assertEquals(80f, routeCreateMenuTopY(20f, 52f, 440f, 800f, 8f))
    }
}
