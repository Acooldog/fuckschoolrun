package com.acooldog.toolbox.tutorial

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect as AndroidRect
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.appcompat.app.AppCompatActivity
import com.acooldog.toolbox.HomeActivity
import com.acooldog.toolbox.RouteRunActivity
import com.acooldog.toolbox.config.InternalAuthStore
import com.acooldog.toolbox.share.data.ShareApiClient
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

internal data class TutorialElement(val key: String, val name: String, val page: String, val canClick: Boolean)
internal data class TutorialStep(val id: String, val element: TutorialElement, val text: String, val autoClick: Boolean)
internal data class TutorialConfig(val revision: Int, val enabled: Boolean, val loginRequired: Boolean,
                                   val title: String, val entryMessage: String, val steps: List<TutorialStep>)

object TutorialController {
    private val worker = Executors.newSingleThreadExecutor()
    private var activityRef = WeakReference<Activity>(null)
    private var host: ComposeView? = null
    private var generation = 0
    private var identity = ""
    private var token = ""
    private var catalogue: Map<String, TutorialElement> = emptyMap()
    internal var config by mutableStateOf<TutorialConfig?>(null)
        private set
    internal var index by mutableStateOf(0)
        private set
    internal var offering by mutableStateOf(false)
        private set
    internal var running by mutableStateOf(false)
        private set
    internal var busy by mutableStateOf(false)
        private set
    private var preparedStep = ""
    private val authListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        activityRef.get()?.let { activity ->
            if (accountIdentity(activity) != identity || InternalAuthStore(activity).token != token) load(activity, false)
        }
    }

    @JvmStatic fun onResume(activity: Activity) { load(activity, false) }
    @JvmStatic fun replay(activity: Activity) { load(activity, true) }

    @JvmStatic fun onPause(activity: Activity) {
        if (activityRef.get() !== activity) return
        generation++
        activity.getSharedPreferences("internal_auth_store", Activity.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(authListener)
        detach()
        activityRef.clear()
        busy = false
    }

    @JvmStatic fun onDestroy(activity: Activity) {
        onPause(activity)
        TutorialAnchors.clear(activity)
    }

    private fun prefs(activity: Activity) = activity.getSharedPreferences("fsr_tutorial_history", Activity.MODE_PRIVATE)
    private fun accountIdentity(activity: Activity): String {
        val auth = InternalAuthStore(activity)
        return if (auth.isLoggedIn) "account:${auth.profile?.id}" else "device"
    }

    private fun load(activity: Activity, replay: Boolean) {
        if (catalogue.isEmpty()) {
            val array = JSONArray(activity.assets.open("tutorial_elements.json").bufferedReader().use { it.readText() })
            catalogue = (0 until array.length()).associate { i ->
                val json = array.getJSONObject(i)
                json.getString("key") to TutorialElement(json.getString("key"), json.getString("name"),
                    json.getString("page"), json.getBoolean("autoClickAllowed"))
            }
        }
        val nextIdentity = accountIdentity(activity)
        if (nextIdentity != identity || replay) {
            running = false
            offering = false
            config = null
        }
        identity = nextIdentity
        preparedStep = ""
        token = InternalAuthStore(activity).token
        activityRef = WeakReference(activity)
        activity.getSharedPreferences("internal_auth_store", Activity.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(authListener)
        val keepRunning = running
        val requestToken = token
        val requestIdentity = identity
        val requestGeneration = ++generation
        val appContext = activity.applicationContext
        val history = prefs(activity)
        busy = true
        detach()
        worker.execute {
            val result = runCatching {
                val api = ShareApiClient(appContext)
                val parsed = parse(api.getTutorialConfig(requestToken), requestToken)
                var progress = JSONObject(history.getString(requestIdentity, "{\"status\":\"never\"}")!!)
                if (requestToken.isNotEmpty()) {
                    // Retry pending account progress before reading cross-device history.
                    if (progress.optBoolean("pending") && progress.optInt("revision") == parsed.revision && parsed.enabled) {
                        api.saveTutorialProgress(requestToken, progressPayload(progress))
                    }
                    progress = api.getTutorialProgress(requestToken)
                    history.edit().putString(requestIdentity, progress.toString()).apply()
                }
                parsed to progress
            }
            activity.runOnUiThread {
                if (generation != requestGeneration || activity.isFinishing || activity.isDestroyed ||
                    accountIdentity(activity) != requestIdentity) return@runOnUiThread
                busy = false
                result.fold(onSuccess = { (parsed, progress) ->
                    config = parsed
                    if (!TutorialPolicy.shouldOffer(parsed.enabled, parsed.loginRequired,
                            progress.optString("status", "never"), replay || keepRunning)) {
                        running = false
                        offering = false
                        if (replay) Toast.makeText(activity,
                            if (parsed.loginRequired) "请先登录后查看教程" else "教程暂不可用", Toast.LENGTH_SHORT).show()
                        return@fold
                    }
                    index = if (replay) 0 else TutorialPolicy.resumeIndex(
                        progress.optInt("revision"), parsed.revision, progress.optInt("stepIndex"), parsed.steps.size)
                    offering = !keepRunning
                    running = keepRunning
                    attach(activity)
                    if (running) prepare(activity)
                }, onFailure = {
                    running = false
                    offering = false
                    if (replay) Toast.makeText(activity, "教程加载失败，请稍后重试", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    private fun parse(json: JSONObject, requestToken: String): TutorialConfig {
        require(json.getInt("schemaVersion") == 1)
        val revision = json.getInt("revision")
        val stepsJson = json.getJSONArray("steps")
        require(revision >= 0 && stepsJson.length() <= 100)
        val steps = (0 until stepsJson.length()).map { i ->
            val item = stepsJson.getJSONObject(i)
            val element = requireNotNull(catalogue[item.getString("elementKey")])
            val autoClick = item.getBoolean("autoClick")
            val text = item.getString("text")
            require(text.isNotBlank() && text.length <= 1200 && (!autoClick || element.canClick))
            TutorialStep(item.getString("id"), element, text, autoClick)
        }
        require(steps.map { it.id }.toSet().size == steps.size)
        val enabled = json.getBoolean("enabled")
        val loginRequired = json.getBoolean("loginRequired") || (json.getBoolean("requireLogin") && requestToken.isEmpty())
        require(!enabled || loginRequired || steps.isNotEmpty())
        return TutorialConfig(revision, enabled, loginRequired, json.getString("title"),
            json.getString("entryMessage"), steps)
    }

    private fun attach(activity: Activity, parent: ViewGroup = activity.findViewById(android.R.id.content)) {
        detach()
        host = ComposeView(activity).apply {
            if (activity is AppCompatActivity) {
                setViewTreeLifecycleOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
            }
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { TutorialOverlay(activity) }
        }
        parent.addView(host,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun detach() {
        (host?.parent as? ViewGroup)?.removeView(host)
        host = null
    }

    internal fun accept() {
        offering = false
        running = true
        save("in_progress")
        activityRef.get()?.let { prepare(it) }
    }

    internal fun decline() {
        save("declined")
        offering = false
        running = false
        detach()
    }

    internal fun exit() {
        save("abandoned")
        running = false
        offering = false
        detach()
        (activityRef.get() as? RouteRunActivity)?.dismissSimulationSettingsForTutorial()
    }

    internal fun retry() { activityRef.get()?.let { preparedStep = ""; prepare(it) } }

    internal fun anchor(activity: Activity, element: TutorialElement): TutorialAnchor? {
        if (!element.key.startsWith("view.")) return TutorialAnchors.get(activity, element.key)
        val id = activity.resources.getIdentifier(element.key.removePrefix("view."), "id", activity.packageName)
        val view = activity.findViewById<View>(id) ?: return null
        val visible = AndroidRect()
        if (!view.isShown || !view.getGlobalVisibleRect(visible)) return null
        val location = IntArray(2)
        activity.window.decorView.getLocationOnScreen(location)
        return TutorialAnchor(Rect((visible.left - location[0]).toFloat(), (visible.top - location[1]).toFloat(),
            (visible.right - location[0]).toFloat(), (visible.bottom - location[1]).toFloat()),
            view.isEnabled, { view.performClick() }, { view.requestRectangleOnScreen(AndroidRect(0, 0, view.width, view.height)) })
    }

    internal fun overlayLocation(): IntArray = IntArray(2).also { host?.getLocationInWindow(it) }

    internal fun alignWindow(activity: Activity, anchor: TutorialAnchor?) {
        val windowRoot = anchor?.source?.get()?.rootView as? ViewGroup ?: (activity.window.decorView as ViewGroup)
        val parent = if (windowRoot === activity.window.decorView) {
            activity.findViewById<ViewGroup>(android.R.id.content)
        } else windowRoot
        if (host?.parent !== parent) attach(activity, parent)
    }

    private fun prepare(activity: Activity) {
        val step = config?.steps?.getOrNull(index) ?: return
        if (step.element.key == "dialog") {
            alignWindow(activity, null)
            if (activity is RouteRunActivity) activity.dismissSimulationSettingsForTutorial()
            return
        }
        if (preparedStep == step.id) return
        preparedStep = step.id
        val pageActivities = mapOf("draw" to "HomeActivity", "simulation" to "RouteRunActivity",
            "routes" to "RouteActivity", "mine" to "MineActivity", "settings" to "SettingsActivity",
            "login" to "InternalLoginActivity", "register" to "EmailRegisterActivity", "nfc" to "NfcToolsActivity",
            "tips" to "UsageTipsActivity", "history" to "HistoryActivity",
            "simulation_settings" to "RouteRunActivity")
        val destination = pageActivities[step.element.page]
        if (destination != null && activity.javaClass.simpleName != destination &&
            !(step.element.page == "draw" && activity.javaClass.simpleName == "RouteCreateActivity")) {
            activity.startActivity(Intent().setClassName(activity, "${activity.packageName}.$destination"))
            preparedStep = ""
            return
        }
        if (step.element.page == "simulation_settings" && activity is RouteRunActivity) {
            activity.openSimulationSettingsFromDock()
        } else if (activity is RouteRunActivity) {
            activity.dismissSimulationSettingsForTutorial()
        }
        val target = anchor(activity, step.element)
        if (target != null) {
            target.reveal()
        } else if (step.element.key.startsWith("draw.") &&
            step.element.key.substringAfter('.') in setOf("save", "clear", "share", "mode", "auto_add",
                "auto_reduce", "smooth", "undo", "redo", "keep", "delete")) {
            TutorialAnchors.get(activity, "draw.tools")?.click?.invoke()
        } else if (step.element.key.startsWith("simulation.") && step.element.key != "simulation.tools") {
            TutorialAnchors.get(activity, "simulation.tools")?.click?.invoke()
        }
    }

    internal fun next() {
        if (busy) return
        val activity = activityRef.get() ?: return
        val document = config ?: return
        val step = document.steps.getOrNull(index) ?: return
        val currentIdentity = identity
        val currentToken = token
        val currentGeneration = generation
        busy = true
        worker.execute {
            val policy = runCatching { parse(ShareApiClient(activity.applicationContext).getTutorialConfig(currentToken), currentToken) }
            activity.runOnUiThread {
                if (generation != currentGeneration || accountIdentity(activity) != currentIdentity ||
                    activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                busy = false
                val latest = policy.getOrNull()
                if (latest == null) {
                    Toast.makeText(activity, "无法验证教程状态，请重试", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (!latest.enabled || latest.loginRequired || latest.revision != document.revision) {
                    running = false
                    detach()
                    Toast.makeText(activity, "教程或登录要求已更新，请重新加载", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val target = if (step.element.key == "dialog") null else anchor(activity, step.element)
                if (step.element.key != "dialog" && (target == null || target.bounds.isEmpty)) {
                    Toast.makeText(activity, "目标控件尚未出现", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (step.autoClick && (target?.enabled != true || target.click == null)) {
                    Toast.makeText(activity, "当前控件不可点击", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                // Advance before a click that may synchronously navigate to another Activity.
                index++
                preparedStep = ""
                val done = index == document.steps.size
                save(if (done) "completed" else "in_progress")
                if (done) { running = false; detach() }
                if (step.autoClick) target?.click?.invoke()
                if (!done && generation == currentGeneration) prepare(activity)
            }
        }
    }

    private fun progressPayload(progress: JSONObject) = JSONObject()
        .put("revision", progress.getInt("revision")).put("status", progress.getString("status"))
        .put("stepIndex", progress.getInt("stepIndex"))

    private fun save(status: String) {
        val activity = activityRef.get() ?: return
        val revision = config?.revision ?: return
        val key = identity
        val authToken = token
        val history = prefs(activity)
        val payload = JSONObject().put("revision", revision).put("status", status).put("stepIndex", index)
        history.edit().putString(key, JSONObject(payload.toString()).put("pending", authToken.isNotEmpty()).toString()).apply()
        if (authToken.isNotEmpty()) {
            val context = activity.applicationContext
            worker.execute {
                if (runCatching { ShareApiClient(context).saveTutorialProgress(authToken, payload) }.isSuccess &&
                    JSONObject(history.getString(key, "{}")!!).optInt("stepIndex", -1) == payload.getInt("stepIndex") &&
                    JSONObject(history.getString(key, "{}")!!).optString("status") == status) {
                    history.edit().putString(key, payload.toString()).apply()
                }
            }
        }
    }
}
