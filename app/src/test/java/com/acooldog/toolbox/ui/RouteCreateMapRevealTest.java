package com.acooldog.toolbox.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RouteCreateMapRevealTest {
    @Test
    public void satelliteIsRevealedFromLeftToRight() {
        assertEquals(0, RouteCreateMapReveal.left(400, 0f, true));
        assertEquals(0, RouteCreateMapReveal.right(400, 0f, true));
        assertEquals(0, RouteCreateMapReveal.left(400, 0.25f, true));
        assertEquals(100, RouteCreateMapReveal.right(400, 0.25f, true));
        assertEquals(400, RouteCreateMapReveal.right(400, 1f, true));
    }

    @Test
    public void standardIsRevealedFromRightToLeft() {
        assertEquals(400, RouteCreateMapReveal.left(400, 0f, false));
        assertEquals(400, RouteCreateMapReveal.right(400, 0f, false));
        assertEquals(300, RouteCreateMapReveal.left(400, 0.25f, false));
        assertEquals(400, RouteCreateMapReveal.right(400, 0.25f, false));
        assertEquals(0, RouteCreateMapReveal.left(400, 1f, false));
    }

    @Test
    public void clipBoundsStayInsideViewport() {
        assertEquals(0, RouteCreateMapReveal.right(400, -1f, true));
        assertEquals(400, RouteCreateMapReveal.right(400, 2f, true));
        assertEquals(0, RouteCreateMapReveal.left(400, 2f, false));
        assertEquals(0, RouteCreateMapReveal.right(-1, 0.5f, true));
    }
}
