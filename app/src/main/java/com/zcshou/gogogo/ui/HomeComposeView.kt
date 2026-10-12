package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

class HomeComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    interface Actions {
        fun openDrawRoute()
        fun openSimulation()
        fun openRoutes()
        fun openSettings()
        fun openInternalLogin()
        fun openDeveloperOptions()
        fun checkForUpdates()
    }

    private var actionsState: Actions? by mutableStateOf(null)

    fun setActions(value: Actions?) {
        actionsState = value
    }

    @Composable
    override fun Content() {
        HomeScreen(actions = actionsState)
    }
}
