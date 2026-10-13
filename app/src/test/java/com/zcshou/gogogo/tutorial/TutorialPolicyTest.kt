package com.acooldog.toolbox.tutorial

import org.junit.Assert.*
import org.junit.Test

class TutorialPolicyTest {
    @Test fun firstUseAndResumeAreOfferedButCompletedAndDeclinedAreNot() {
        for (status in listOf("never", "in_progress", "accepted")) {
            assertTrue(TutorialPolicy.shouldOffer(true, false, status, false))
        }
        for (status in listOf("completed", "declined", "abandoned")) {
            assertFalse(TutorialPolicy.shouldOffer(true, false, status, false))
        }
    }
    @Test fun replayCannotBypassLoginOrShutdown() {
        assertFalse(TutorialPolicy.shouldOffer(true, true, "never", true))
        assertFalse(TutorialPolicy.shouldOffer(false, false, "never", true))
        assertTrue(TutorialPolicy.shouldOffer(true, false, "completed", true))
    }
    @Test fun changedRevisionAndInvalidIndexRestartAtZero() {
        assertEquals(2, TutorialPolicy.resumeIndex(1, 1, 2, 4))
        assertEquals(0, TutorialPolicy.resumeIndex(1, 2, 2, 4))
        assertEquals(0, TutorialPolicy.resumeIndex(1, 1, 4, 4))
        assertEquals(0, TutorialPolicy.resumeIndex(1, 1, -1, 4))
    }
}
