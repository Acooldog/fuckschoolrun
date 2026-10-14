package com.acooldog.toolbox.tutorial

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import java.util.WeakHashMap
import java.lang.ref.WeakReference
import android.view.View
import android.graphics.Rect as AndroidRect

internal data class TutorialAnchor(
    val bounds: Rect, val enabled: Boolean, val click: (() -> Unit)?, val reveal: () -> Unit,
    val source: WeakReference<View>? = null,
)

internal object TutorialAnchors {
    private val entries = WeakHashMap<Activity, MutableMap<String, Pair<Any, TutorialAnchor>>>()
    private val nativeListeners = WeakHashMap<View, View.OnLayoutChangeListener>()
    fun set(activity: Activity, key: String, owner: Any, anchor: TutorialAnchor) {
        entries.getOrPut(activity) { mutableMapOf() }[key] = owner to anchor
    }
    fun remove(activity: Activity, key: String, owner: Any) {
        if (entries[activity]?.get(key)?.first === owner) entries[activity]?.remove(key)
    }
    fun get(activity: Activity, key: String): TutorialAnchor? = entries[activity]?.get(key)?.second
    fun update(activity: Activity, key: String, owner: Any, enabled: Boolean, click: (() -> Unit)?) {
        val entry = entries[activity]?.get(key) ?: return
        if (entry.first === owner) entries[activity]?.set(key, owner to entry.second.copy(enabled = enabled, click = click))
    }
    @JvmStatic
    fun registerView(activity: Activity, key: String, view: View) {
        val update = {
            val visible = AndroidRect()
            val location = IntArray(2)
            activity.window.decorView.getLocationOnScreen(location)
            val anchor = if (view.isShown && view.getGlobalVisibleRect(visible)) {
                TutorialAnchor(
                    Rect(
                        (visible.left - location[0]).toFloat(),
                        (visible.top - location[1]).toFloat(),
                        (visible.right - location[0]).toFloat(),
                        (visible.bottom - location[1]).toFloat()
                    ),
                    view.isEnabled,
                    { view.performClick() },
                    { view.requestRectangleOnScreen(AndroidRect(0, 0, view.width, view.height)) },
                    WeakReference(view)
                )
            } else {
                TutorialAnchor(
                    Rect.Zero,
                    false,
                    { view.performClick() },
                    {},
                    WeakReference(view)
                )
            }
            entries.getOrPut(activity) { mutableMapOf() }[key] = view to anchor
        }
        nativeListeners[view]?.let { view.removeOnLayoutChangeListener(it) }
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
        nativeListeners[view] = listener
        view.addOnLayoutChangeListener(listener)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                update()
            }

            override fun onViewDetachedFromWindow(v: View) {
                remove(activity, key, view)
                nativeListeners.remove(view)?.let { view.removeOnLayoutChangeListener(it) }
            }
        })
        view.post(update)
    }

    fun clear(activity: Activity) { entries.remove(activity) }
}

internal fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

fun Modifier.tutorialAnchor(key: String, enabled: Boolean = true, onClick: (() -> Unit)? = null): Modifier =
    composed {
        val activity = LocalContext.current.activity() ?: return@composed this
        val owner = remember(key) { Any() }
        val currentClick = rememberUpdatedState(onClick)
        val currentEnabled = rememberUpdatedState(enabled)
        val view = LocalView.current
        val requester = remember { BringIntoViewRequester() }
        val scope = rememberCoroutineScope()
        val click: (() -> Unit)? = if (onClick == null) null else { { currentClick.value?.invoke() } }
        SideEffect { TutorialAnchors.update(activity, key, owner, currentEnabled.value, click) }
        DisposableEffect(activity, key, owner) {
            onDispose { TutorialAnchors.remove(activity, key, owner) }
        }
        this.bringIntoViewRequester(requester).onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                TutorialAnchors.set(activity, key, owner, TutorialAnchor(
                    coordinates.boundsInWindow(), currentEnabled.value, click,
                    { scope.launch { requester.bringIntoView() } },
                    WeakReference(view),
                ))
            }
        }
    }
