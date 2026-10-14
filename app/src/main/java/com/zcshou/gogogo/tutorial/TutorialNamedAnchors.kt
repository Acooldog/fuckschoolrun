package com.acooldog.toolbox.tutorial

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray

private var names: Map<Pair<String, String>, String>? = null

fun Modifier.tutorialNamedAnchor(name: String, page: String, enabled: Boolean = true,
                                onClick: (() -> Unit)? = null): Modifier = composed {
    if (names == null) {
        val elements = JSONArray(LocalContext.current.assets.open("tutorial_elements.json")
            .bufferedReader().use { it.readText() })
        names = (0 until elements.length()).associate { i ->
            val element = elements.getJSONObject(i)
            (element.getString("name") to element.getString("page")) to element.getString("key")
        }
    }
    val key = names?.get(name to page)
    if (key == null) this else this.tutorialAnchor(key, enabled, onClick)
}
