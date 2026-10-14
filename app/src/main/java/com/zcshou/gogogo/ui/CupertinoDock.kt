package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos

private val DockPrimary = Color(0xFF007AFF)
private val DockSecondary = Color(0xFF8E8E93)
private val DockSelectedSurface = Color(0xFFE4E4E7)

@Composable
fun CupertinoDock(
    selectedTab: Int,
    initialSelectedTab: Int = selectedTab,
    simulationPage: Boolean = false,
    onTabSelected: (Int) -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val animatedSelectedTab = remember(initialSelectedTab) {
        Animatable(initialSelectedTab.toFloat())
    }
    LaunchedEffect(selectedTab) {
        animatedSelectedTab.animateTo(
            targetValue = selectedTab.toFloat(),
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
    }
    val tabs = listOf(
        CupertinoDockTab("绘制", Icons.Outlined.AddLocationAlt),
        CupertinoDockTab("模拟", Icons.Outlined.DirectionsRun),
        CupertinoDockTab("NFC", Icons.Outlined.Nfc),
        CupertinoDockTab("我的", Icons.Outlined.Person)
    )

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(30.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(30.dp) },
                effects = {
                    vibrancy()
                    blur(18.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.84f))
                }
            )
            .padding(4.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tabWidth = (maxWidth - 6.dp) / tabs.size
            Box(
                modifier = Modifier
                    .offset(x = (tabWidth + 2.dp) * animatedSelectedTab.value)
                    .width(tabWidth)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(DockSelectedSurface)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedTab
                    val interactionSource = remember(index) { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                            .drawBehind {
                                if (pressed) {
                                    val inset = 1.5.dp.toPx()
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.10f),
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.70f)
                                            )
                                        ),
                                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                                        size = androidx.compose.ui.geometry.Size(
                                            width = size.width - inset * 2,
                                            height = size.height - inset * 2
                                        ),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 1.dp.toPx()
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                            20.dp.toPx(),
                                            20.dp.toPx()
                                        )
                                    )
                                }
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { onTabSelected(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 1) {
                            AnimatedContent(
                                targetState = simulationPage,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                                transitionSpec = {
                                    if (targetState) {
                                        (
                                            slideInVertically(
                                                animationSpec = tween(
                                                    440,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                initialOffsetY = { -it }
                                            ) + fadeIn(tween(240))
                                        ).togetherWith(
                                            slideOutVertically(
                                                animationSpec = tween(
                                                    440,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                targetOffsetY = { it }
                                            ) + fadeOut(tween(240))
                                        )
                                    } else {
                                        (
                                            slideInVertically(
                                                animationSpec = tween(
                                                    440,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                initialOffsetY = { it }
                                            ) + fadeIn(tween(240))
                                        ).togetherWith(
                                            slideOutVertically(
                                                animationSpec = tween(
                                                    440,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                targetOffsetY = { -it }
                                            ) + fadeOut(tween(240))
                                        )
                                    }
                                },
                                label = "simulation-dock-item"
                            ) { isSimulationPage ->
                                DockContent(
                                    tab = if (isSimulationPage) {
                                        CupertinoDockTab(
                                            "\u6A21\u62DF\u8BBE\u7F6E",
                                            Icons.Outlined.Settings
                                        )
                                    } else {
                                        tab.copy(label = "\u6A21\u62DF")
                                    },
                                    selected = selected
                                )
                            }
                        } else {
                            DockContent(tab, selected)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockContent(tab: CupertinoDockTab, selected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) DockPrimary else DockSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = tab.label,
            style = TextStyle(
                color = if (selected) DockPrimary else DockSecondary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

class CupertinoDockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Listener {
        fun onTabSelected(tab: Int)
    }

    private var initialSelectedTabState by mutableIntStateOf(0)
    private var selectedTabState by mutableIntStateOf(0)
    private var simulationPageState by mutableStateOf(false)
    private var targetSimulationPageState by mutableStateOf(false)
    private var animateSimulationLabelState by mutableStateOf(false)
    private var listener: Listener? = null

    fun setTabs(
        initialTab: Int,
        selectedTab: Int,
        initialSimulationPage: Boolean,
        simulationPage: Boolean,
        animateSimulationLabel: Boolean
    ) {
        initialSelectedTabState = initialTab.coerceIn(0, 3)
        selectedTabState = selectedTab.coerceIn(0, 3)
        simulationPageState = initialSimulationPage
        targetSimulationPageState = simulationPage
        animateSimulationLabelState = animateSimulationLabel
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    @Composable
    override fun Content() {
        LaunchedEffect(animateSimulationLabelState, targetSimulationPageState) {
            if (animateSimulationLabelState) {
                withFrameNanos { }
                simulationPageState = targetSimulationPageState
            }
        }
        CupertinoTheme {
            CupertinoDock(
                selectedTab = selectedTabState,
                initialSelectedTab = initialSelectedTabState,
                simulationPage = simulationPageState,
                onTabSelected = { tab ->
                    selectedTabState = tab
                    listener?.onTabSelected(tab)
                }
            )
        }
    }
}

private data class CupertinoDockTab(
    val label: String,
    val icon: ImageVector
)
