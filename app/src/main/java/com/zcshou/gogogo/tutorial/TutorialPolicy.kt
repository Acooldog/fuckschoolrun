package com.acooldog.toolbox.tutorial

internal object TutorialPolicy {
    fun shouldOffer(enabled: Boolean, loginRequired: Boolean, status: String, replay: Boolean): Boolean =
        enabled && !loginRequired && (replay || status == "never" || status == "in_progress" || status == "accepted")

    fun resumeIndex(savedRevision: Int, revision: Int, index: Int, count: Int): Int =
        if (savedRevision == revision && index in 0 until count) index else 0
}
