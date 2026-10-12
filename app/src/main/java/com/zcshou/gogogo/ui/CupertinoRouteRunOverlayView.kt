package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class CupertinoRouteRunOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    interface Actions {
        fun onStartSimulation()
        fun onSelectRoute()
        fun onStartRealRun()
        fun onNfcEntry()
    }

    private var actions: Actions? = null
    private var state by mutableStateOf(State())

    fun setActions(actions: Actions?) {
        this.actions = actions
    }

    fun render(
        running: Boolean,
        realRunLinkRunning: Boolean,
        panelCollapsed: Boolean,
        liveRouteEditEnabled: Boolean,
        cadenceMode: Boolean,
        speed: String,
        cadence: String
    ) {
        state = state.copy(
            running = running,
            realRunLinkRunning = realRunLinkRunning,
            panelCollapsed = panelCollapsed
        )
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            RouteRunOverlay(state, actions)
        }
    }

    data class State(
        val running: Boolean = false,
        val realRunLinkRunning: Boolean = false,
        val panelCollapsed: Boolean = true
    )
}

@Composable
private fun RouteRunOverlay(
    state: CupertinoRouteRunOverlayView.State,
    actions: CupertinoRouteRunOverlayView.Actions?
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(end = 18.dp, bottom = 120.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.padding(bottom = 58.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(min = 180.dp, max = 236.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                RunAction(
                    label = if (state.running) "停止模拟" else "开始模拟",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        expanded = false
                        actions?.onStartSimulation()
                    }
                )
                RunAction(
                    label = "选择路线",
                    icon = Icons.Outlined.List,
                    onClick = {
                        expanded = false
                        actions?.onSelectRoute()
                    }
                )
                RunAction(
                    label = if (state.realRunLinkRunning) "停止跑步联动" else "跑步联动",
                    icon = Icons.Outlined.Accessibility,
                    onClick = {
                        expanded = false
                        actions?.onStartRealRun()
                    }
                )
                RunAction(
                    label = "NFC 联动",
                    icon = Icons.Outlined.Nfc,
                    onClick = {
                        expanded = false
                        actions?.onNfcEntry()
                    }
                )
            }
        }

        CupertinoIconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .padding(2.dp)
                .background(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = if (expanded) CupertinoIcons.Outlined.Gearshape
                else CupertinoIcons.Outlined.Gearshape,
                contentDescription = "模拟操作"
            )
        }
    }
}

@Composable
private fun RunAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    CupertinoButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 180.dp, max = 236.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label)
            BasicText(
                label,
                style = TextStyle(fontSize = 15.sp, color = Color.Unspecified)
            )
        }
    }
}
