package com.acooldog.toolbox.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteCreateSearchSessionTest {
    @Test
    fun initialUnfocusedFieldDoesNotCancelOpening() {
        val session = RouteCreateSearchSession()
        session.open()
        session.onFocusChanged(false)
        assertTrue(session.expanded)
    }

    @Test
    fun losingAcquiredFocusCollapsesSearch() {
        val session = RouteCreateSearchSession()
        session.open()
        session.onFocusChanged(true)
        session.onFocusChanged(false)
        assertFalse(session.expanded)
    }

    @Test
    fun externalMapOrControlActionClosesEvenBeforeFocusIsAcquired() {
        val session = RouteCreateSearchSession()
        session.open()
        session.collapse()
        assertFalse(session.expanded)
        session.open()
        session.onFocusChanged(false)
        assertTrue(session.expanded)
        session.onFocusChanged(true)
        session.collapse()
        session.onFocusChanged(false)
        assertFalse(session.expanded)
    }
}
